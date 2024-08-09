/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.quests;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.event.ActivityUpdatedEvent;
import com.wynntils.models.activities.type.ActivityDifficulty;
import com.wynntils.models.activities.type.ActivityInfo;
import com.wynntils.models.activities.type.ActivityLength;
import com.wynntils.models.activities.type.ActivitySortOrder;
import com.wynntils.models.activities.type.ActivityType;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;

public final class QuestModel extends Model {
    private static final String MINI_QUEST_PREFIX = "Mini-Quest - ";
    private static final String WIKI_APOSTROPHE = "&#039;";

    private final Map<String, QuestStorage> questStorage = new HashMap<>();

    public QuestModel() {
        super(List.of());
    }

    public void rescanQuestBook(boolean includeQuests, boolean includeMiniQuests) {
        WynntilsMod.info("Requesting rescan of Quests in Content Book");
        if (includeQuests) {
            Models.Activity.scanContentBook(ActivityType.QUEST, this::updateQuestsFromQuery);
        }
        if (includeMiniQuests) {
            Models.Activity.scanContentBook(ActivityType.MINI_QUEST, this::updateMiniQuestsFromQuery);
        }
    }

    public Optional<QuestInfo> getQuestFromName(String name) {
        return questStorage.getOrDefault(Models.Character.getId(), QuestStorage.EMPTY).quests().stream()
                .filter(quest -> quest.name().equals(name))
                .findFirst();
    }

    public List<QuestInfo> getQuestsRaw() {
        return Collections.unmodifiableList(questStorage
                .getOrDefault(Models.Character.getId(), QuestStorage.EMPTY)
                .quests());
    }

    public List<QuestInfo> getQuests(ActivitySortOrder sortOrder) {
        return sortQuestInfoList(sortOrder, getQuestsRaw());
    }

    public List<QuestInfo> getMiniQuestsRaw() {
        return Collections.unmodifiableList(questStorage
                .getOrDefault(Models.Character.getId(), QuestStorage.EMPTY)
                .miniQuests());
    }

    public List<QuestInfo> getMiniQuests(ActivitySortOrder sortOrder) {
        return sortQuestInfoList(sortOrder, getMiniQuestsRaw());
    }

    public List<QuestInfo> getSortedQuests(
            ActivitySortOrder sortOrder, boolean includeQuests, boolean includeMiniQuests) {
        List<QuestInfo> quests = includeQuests ? getQuestsRaw() : List.of();
        List<QuestInfo> miniQuests = includeMiniQuests ? getMiniQuestsRaw() : List.of();

        return sortQuestInfoList(
                sortOrder, Stream.concat(quests.stream(), miniQuests.stream()).toList());
    }

    private List<QuestInfo> sortQuestInfoList(ActivitySortOrder sortOrder, List<QuestInfo> questList) {
        // All quests are always sorted by status (available then unavailable), and then
        // the given sort order, and finally a third way if the given sort order is equal.

        QuestInfo trackedQuestInfo = Models.Activity.getTrackedQuestInfo();
        String trackedQuestName = trackedQuestInfo != null ? trackedQuestInfo.name() : "";
        Comparator<QuestInfo> baseComparator =
                Comparator.comparing(questInfo -> !questInfo.name().equals(trackedQuestName));
        return switch (sortOrder) {
            case LEVEL -> questList.stream()
                    .sorted(baseComparator
                            .thenComparing(QuestInfo::status)
                            .thenComparing(QuestInfo::sortLevel)
                            .thenComparing(QuestInfo::name))
                    .toList();
            case DISTANCE -> questList.stream()
                    .sorted(baseComparator
                            .thenComparing(QuestInfo::status)
                            .thenComparing(new LocationComparator())
                            .thenComparing(QuestInfo::name))
                    .toList();
            case ALPHABETIC -> questList.stream()
                    .sorted(baseComparator
                            .thenComparing(QuestInfo::status)
                            .thenComparing(QuestInfo::name)
                            .thenComparing(QuestInfo::sortLevel))
                    .toList();
        };
    }

    public void startTracking(QuestInfo questInfo) {
        Models.Activity.startTracking(
                questInfo.name(), questInfo.isMiniQuest() ? ActivityType.MINI_QUEST : ActivityType.QUEST);
    }

    public void stopTracking() {
        Models.Activity.stopTracking();
    }

    public void openQuestOnWiki(QuestInfo questInfo) {
        if (questInfo.isMiniQuest()) {
            String type = questInfo.name().split(" ")[0];

            String wikiName = "Quests#" + type + "ing_Posts";

            Managers.Net.openLink(UrlId.LINK_WIKI_LOOKUP, Map.of("title", wikiName));
        } else {
            ApiResponse apiResponse =
                    Managers.Net.callApi(UrlId.API_WIKI_QUEST_PAGE_QUERY, Map.of("name", questInfo.name()));
            apiResponse.handleJsonArray(json -> {
                String pageTitle = json.get(0)
                        .getAsJsonObject()
                        .get("_pageTitle")
                        .getAsString()
                        .replace(WIKI_APOSTROPHE, "'");
                Managers.Net.openLink(UrlId.LINK_WIKI_LOOKUP, Map.of("title", pageTitle));
            });
        }
    }

    public Optional<QuestInfo> getQuestInfoFromName(String name) {
        return Stream.concat(getQuestsRaw().stream(), getMiniQuestsRaw().stream())
                .filter(quest -> quest.name().equals(stripPrefix(name)))
                .findFirst();
    }

    private String stripPrefix(String name) {
        return StringUtils.replaceOnce(name, MINI_QUEST_PREFIX, "");
    }

    private void updateQuestsFromQuery(List<ActivityInfo> newActivities, List<StyledText> progress) {
        List<QuestInfo> newQuests = new ArrayList<>();

        for (ActivityInfo activity : newActivities) {
            if (activity.type() != ActivityType.QUEST && activity.type() != ActivityType.STORYLINE_QUEST) {
                WynntilsMod.warn("Incorrect quest activity type received: " + activity);
                continue;
            }
            QuestInfo questInfo = getQuestInfoFromActivity(activity);
            newQuests.add(questInfo);
        }
        questStorage.put(
                Models.Character.getId(),
                new QuestStorage(Collections.unmodifiableList(newQuests), getMiniQuestsRaw()));
        WynntilsMod.postEvent(new ActivityUpdatedEvent(ActivityType.QUEST));
        WynntilsMod.info("Updated quests from query, got " + newQuests.size() + " quests.");
    }

    private void updateMiniQuestsFromQuery(List<ActivityInfo> newActivities, List<StyledText> progress) {
        List<QuestInfo> newMiniQuests = new ArrayList<>();

        for (ActivityInfo activity : newActivities) {
            if (activity.type() != ActivityType.MINI_QUEST) {
                WynntilsMod.warn("Incorrect mini-quest activity type received: " + activity);
                continue;
            }
            QuestInfo questInfo = getQuestInfoFromActivity(activity);
            newMiniQuests.add(questInfo);
        }

        questStorage.put(
                Models.Character.getId(),
                new QuestStorage(getQuestsRaw(), Collections.unmodifiableList(newMiniQuests)));
        WynntilsMod.postEvent(new ActivityUpdatedEvent(ActivityType.MINI_QUEST));
        WynntilsMod.info("Updated mini-quests from query, got " + newMiniQuests.size() + " mini-quests.");
    }

    private static QuestInfo getQuestInfoFromActivity(ActivityInfo activity) {
        // We should always have a length, but if not, better fake one than crashing

        return new QuestInfo(
                activity.name(),
                activity.specialInfo().orElse(null),
                activity.difficulty().orElse(ActivityDifficulty.EASY),
                activity.status(),
                activity.length().orElse(ActivityLength.SHORT),
                activity.requirements().level().key(),
                activity.description().orElse(StyledText.EMPTY),
                activity.requirements(),
                activity.type() == ActivityType.MINI_QUEST,
                activity.rewards());
    }

    private record QuestStorage(List<QuestInfo> quests, List<QuestInfo> miniQuests) {
        private static final QuestStorage EMPTY = new QuestStorage(List.of(), List.of());
    }

    private static class LocationComparator implements Comparator<QuestInfo> {
        private final Vec3 playerLocation = McUtils.player().position();

        private double getDistance(Optional<Location> loc) {
            // Quests with no location always counts as closest
            if (loc.isEmpty()) return 0f;

            Location location = loc.get();
            return playerLocation.distanceToSqr(location.toVec3());
        }

        @Override
        public int compare(QuestInfo quest1, QuestInfo quest2) {
            Optional<Location> loc1 = quest1.nextLocation();
            Optional<Location> loc2 = quest2.nextLocation();
            return (int) (getDistance(loc1) - getDistance(loc2));
        }
    }
}
