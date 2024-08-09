/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.caves;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.event.ActivityUpdatedEvent;
import com.wynntils.models.activities.type.ActivityDifficulty;
import com.wynntils.models.activities.type.ActivityDistance;
import com.wynntils.models.activities.type.ActivityInfo;
import com.wynntils.models.activities.type.ActivityLength;
import com.wynntils.models.activities.type.ActivitySortOrder;
import com.wynntils.models.activities.type.ActivityType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CaveModel extends Model {
    private final Map<String, CaveStorage> caveStorage = new HashMap<>();

    public CaveModel() {
        super(List.of());
    }

    public void reloadCaves() {
        WynntilsMod.info("Requesting rescan of caves in Activity Book");
        Models.Activity.scanContentBook(ActivityType.CAVE, this::updateCavesFromQuery);
    }

    private void updateCavesFromQuery(List<ActivityInfo> newActivities, List<StyledText> progress) {
        List<CaveInfo> newCaves = new ArrayList<>();

        for (ActivityInfo activity : newActivities) {
            if (activity.type() != ActivityType.CAVE) {
                WynntilsMod.warn("Incorrect cave activity type recieved: " + activity);
                continue;
            }
            CaveInfo caveInfo = getCaveInfoFromActivity(activity);
            newCaves.add(caveInfo);
        }
        caveStorage.put(
                Models.Character.getId(),
                new CaveStorage(Collections.unmodifiableList(newCaves), Collections.unmodifiableList(progress)));
        WynntilsMod.postEvent(new ActivityUpdatedEvent(ActivityType.CAVE));
        WynntilsMod.info("Updated caves from query, got " + newCaves.size() + " caves.");
    }

    public Optional<CaveInfo> getCaveInfoFromName(String name) {
        return getCavesRaw().stream().filter(cave -> cave.name().equals(name)).findFirst();
    }

    public List<CaveInfo> getSortedCaves(ActivitySortOrder sortOrder) {
        return sortCaveInfoList(sortOrder, getCavesRaw());
    }

    private List<CaveInfo> sortCaveInfoList(ActivitySortOrder sortOrder, List<CaveInfo> caveList) {
        // All caves are always sorted by status (available then unavailable), and then
        // the given sort order, and finally a third way if the given sort order is equal.

        CaveInfo trackedCaveInfo = Models.Activity.getTrackedCaveInfo();
        String trackedCaveName = trackedCaveInfo != null ? trackedCaveInfo.name() : "";
        Comparator<CaveInfo> baseComparator =
                Comparator.comparing(caveInfo -> !caveInfo.name().equals(trackedCaveName));
        return switch (sortOrder) {
            case LEVEL -> caveList.stream()
                    .sorted(baseComparator
                            .thenComparing(CaveInfo::status)
                            .thenComparing(CaveInfo::recommendedLevel)
                            .thenComparing(CaveInfo::name))
                    .toList();
            case DISTANCE -> caveList.stream()
                    .sorted(baseComparator
                            .thenComparing(CaveInfo::status)
                            .thenComparing(CaveInfo::distance)
                            .thenComparing(CaveInfo::name))
                    .toList();
            case ALPHABETIC -> caveList.stream()
                    .sorted(baseComparator
                            .thenComparing(CaveInfo::status)
                            .thenComparing(CaveInfo::name)
                            .thenComparing(CaveInfo::recommendedLevel))
                    .toList();
        };
    }

    public List<StyledText> getCaveProgress() {
        return Collections.unmodifiableList(caveStorage
                .getOrDefault(Models.Character.getId(), CaveStorage.EMPTY)
                .progress());
    }

    public List<CaveInfo> getCavesRaw() {
        return Collections.unmodifiableList(caveStorage
                .getOrDefault(Models.Character.getId(), CaveStorage.EMPTY)
                .caves());
    }

    private CaveInfo getCaveInfoFromActivity(ActivityInfo activity) {
        return new CaveInfo(
                activity.name(),
                activity.status(),
                activity.description().orElse(StyledText.EMPTY).getString(),
                activity.requirements().level().key(),
                activity.distance().orElse(ActivityDistance.NEAR),
                activity.length().orElse(ActivityLength.SHORT),
                activity.difficulty().orElse(ActivityDifficulty.EASY),
                activity.rewards());
    }

    private record CaveStorage(List<CaveInfo> caves, List<StyledText> progress) {
        public static final CaveStorage EMPTY = new CaveStorage(List.of(), List.of());
    }
}
