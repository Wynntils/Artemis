/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.ui.WynntilsContentBookFeature;
import com.wynntils.handlers.scoreboard.ScoreboardPart;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.mc.event.SetSpawnEvent;
import com.wynntils.models.activities.caves.CaveInfo;
import com.wynntils.models.activities.event.ActivityTrackerUpdatedEvent;
import com.wynntils.models.activities.event.DialogueHistoryReloadedEvent;
import com.wynntils.models.activities.markers.ActivityMarkerProvider;
import com.wynntils.models.activities.quests.QuestInfo;
import com.wynntils.models.activities.type.ActivityDifficulty;
import com.wynntils.models.activities.type.ActivityDistance;
import com.wynntils.models.activities.type.ActivityInfo;
import com.wynntils.models.activities.type.ActivityLength;
import com.wynntils.models.activities.type.ActivityRequirements;
import com.wynntils.models.activities.type.ActivityStatus;
import com.wynntils.models.activities.type.ActivityTrackingState;
import com.wynntils.models.activities.type.ActivityType;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.models.marker.MarkerModel;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.StyledTextUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/* An "Activity" is the name we've given to the kind of stuff that appears in the Wynncraft
 * "Content Book". In user fronting text, it could be referred to as a "Content Book Activity",
 * to make the connection to the Content Book clear.
 *
 * Examples of activities are quests, caves, dungeons and raids, but also the various
 * discoveries.
 */
public final class ActivityModel extends Model {
    public static final String CONTENT_BOOK_TITLE = "\uDAFF\uDFEE\uE004";

    private static final Location WORLD_SPAWN = new Location(-1572, 41, -1668);
    private static final Location HUB_SPAWN = new Location(295, 34, 321);

    private static final Pattern LEVEL_REQ_PATTERN =
            Pattern.compile("^§(.).À?§7(?: Recommended)? Combat Lv(?:\\. Min)?: (\\d+)$");
    private static final Pattern PROFESSION_REQ_PATTERN = Pattern.compile("^§(.).À?§7 (\\w+)? Lv\\. Min: (\\d+)$");
    private static final Pattern QUEST_REQ_PATTERN = Pattern.compile("^§(.).À?§7 Quest Req: (.+)$");
    private static final Pattern DISTANCE_PATTERN =
            Pattern.compile("^\uDB00\uDC0E§7Distance: §.([\\w\\s]*)(?:§8 \\((.+)\\))?$");
    private static final Pattern LENGTH_PATTERN = Pattern.compile("^\uDB00\uDC0E§7Length: (\\w*)(?:§8 \\((.+)\\))?$");
    private static final Pattern DIFFICULTY_PATTERN = Pattern.compile("^\uDB00\uDC0E§7Difficulty: (\\w*)$");
    private static final Pattern REWARD_HEADER_PATTERN = Pattern.compile("^\uDB00\uDC0E§dRewards:$");
    private static final Pattern REWARD_PATTERN = Pattern.compile("^§d\uDB00\uDC04(?<newline>- )?§7\\+?(?<reward>.+)$");
    private static final Pattern TRACKING_PATTERN = Pattern.compile("^.*§(?:#.{8}|.)§lCLICK TO (UN)?TRACK$");
    private static final Pattern OVERALL_PROGRESS_PATTERN =
            Pattern.compile("^\uDB00\uDC1F*§7(\\d+) of (\\d+) completed$");

    private static final ScoreboardPart TRACKER_SCOREBOARD_PART = new ActivityTrackerScoreboardPart();
    private static final ContentBookQueries CONTAINER_QUERIES = new ContentBookQueries();
    private static final DialogueHistoryQueries DIALOGUE_HISTORY_QUERIES = new DialogueHistoryQueries();
    public static final ActivityMarkerProvider ACTIVITY_MARKER_PROVIDER = new ActivityMarkerProvider();

    private TrackedActivity trackedActivity;
    private List<List<StyledText>> dialogueHistory = List.of();
    private CappedValue overallProgress = CappedValue.EMPTY;
    private boolean overallProgressOutdated = true;

    public ActivityModel(MarkerModel markerModel) {
        super(List.of(markerModel));

        Handlers.Scoreboard.addPart(TRACKER_SCOREBOARD_PART);
        Models.Marker.registerMarkerProvider(ACTIVITY_MARKER_PROVIDER);
    }

    @SubscribeEvent
    public void onSetSpawn(SetSpawnEvent e) {
        Location spawn = new Location(e.getSpawnPos());
        if (spawn.equals(WORLD_SPAWN) || spawn.equals(HUB_SPAWN)) {
            ACTIVITY_MARKER_PROVIDER.setSpawnLocation(null);
            return;
        }

        Location player = Location.containing(McUtils.player().position());
        if (spawn.equals(player)) {
            // Wynncraft "resets" tracking by setting the compass to your current
            // location. In theory, this can fail if you happen to be standing on
            // the spot that is the target of the activity you start tracking...
            ACTIVITY_MARKER_PROVIDER.setSpawnLocation(null);
            return;
        }

        ACTIVITY_MARKER_PROVIDER.setSpawnLocation(spawn);
    }

    @SubscribeEvent
    public void onScreenClosed(ScreenClosedEvent event) {
        // The progress cannot be outdated if we are in the content book
        // This speeds up navigation in the content book
        overallProgressOutdated = true;
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        // We need to rescan the overall progress when the world state changes
        overallProgressOutdated = true;
    }

    public ActivityInfo parseItem(String name, ActivityType type, ItemStack itemStack) {
        Deque<StyledText> lore = LoreUtils.getLore(itemStack);

        StyledText statusLine = lore.pop();

        StyledText[] statusLineParts = statusLine.split(" - ");

        String specialInfo;
        String statusMessage;

        ActivityStatus status;

        if (type == ActivityType.WORLD_EVENT) {
            // World events have a slightly different format,
            // with the first description line being the region,
            // and the second being the status line.
            specialInfo = statusLine.getString();
            statusMessage = lore.pop().getString();

            status = ActivityStatus.fromWorldEvent(statusMessage);
        } else {
            // Handle every other activity type
            if (statusLineParts.length == 1) {
                specialInfo = null;
                statusMessage = statusLineParts[0].getString();
            } else {
                specialInfo = statusLineParts[0].getString();
                statusMessage = statusLineParts[1].getString();
            }

            status = ActivityStatus.from(statusMessage);
        }

        if (status == null) return null;

        if (!lore.pop().isBlank()) return null;

        Pair<Integer, Boolean> levelReq = Pair.of(0, true);
        ActivityDistance distance = null;
        String distanceInfo = null;
        ActivityLength length = null;
        String lengthInfo = null;
        ActivityDifficulty difficulty = null;
        ActivityTrackingState trackingState = ActivityTrackingState.UNTRACKABLE;
        List<Pair<Pair<ProfessionType, Integer>, Boolean>> professionLevels = new ArrayList<>();
        List<Pair<String, Boolean>> quests = new ArrayList<>();
        List<String> rewards = new ArrayList<>();
        List<StyledText> descriptionLines = new ArrayList<>();

        for (StyledText line : lore) {
            // Must be tested before profession requirement pattern
            Matcher levelReqMatcher = line.getMatcher(LEVEL_REQ_PATTERN);
            if (levelReqMatcher.matches()) {
                boolean fulfilled = isFulfilled(levelReqMatcher);
                int level = Integer.parseInt(levelReqMatcher.group(2));
                levelReq = Pair.of(level, fulfilled);
                continue;
            }

            Matcher professionReqMatcher = line.getMatcher(PROFESSION_REQ_PATTERN);
            if (professionReqMatcher.matches()) {
                boolean fulfilled = isFulfilled(professionReqMatcher);
                ProfessionType profession = ProfessionType.fromString(professionReqMatcher.group(2));
                int level = Integer.parseInt(professionReqMatcher.group(3));
                professionLevels.add(Pair.of(Pair.of(profession, level), fulfilled));
                continue;
            }

            Matcher questReqMatcher = line.getMatcher(QUEST_REQ_PATTERN);
            if (questReqMatcher.matches()) {
                boolean fulfilled = isFulfilled(questReqMatcher);
                String quest = questReqMatcher.group(2);
                quests.add(Pair.of(quest, fulfilled));
                continue;
            }

            Matcher distanceMatcher = line.getMatcher(DISTANCE_PATTERN);
            if (distanceMatcher.matches()) {
                distance = ActivityDistance.from(distanceMatcher.group(1));
                distanceInfo = distanceMatcher.group(2);
                continue;
            }

            Matcher lengthMatcher = line.getMatcher(LENGTH_PATTERN);
            if (lengthMatcher.matches()) {
                length = ActivityLength.from(lengthMatcher.group(1));
                lengthInfo = lengthMatcher.group(2);
                continue;
            }

            Matcher difficultyMatcher = line.getMatcher(DIFFICULTY_PATTERN);
            if (difficultyMatcher.matches()) {
                difficulty = ActivityDifficulty.from(difficultyMatcher.group(1));
                continue;
            }

            Matcher rewardHeaderMatcher = line.getMatcher(REWARD_HEADER_PATTERN);
            if (rewardHeaderMatcher.matches()) {
                // Just ignore the header
                continue;
            }

            Matcher rewardMatcher = line.getMatcher(REWARD_PATTERN);
            if (rewardMatcher.matches()) {
                boolean extendLastLine = rewardMatcher.group("newline") == null;
                if (extendLastLine && !rewards.isEmpty()) {
                    rewards.set(rewards.size() - 1, rewards.getLast() + " " + rewardMatcher.group("reward"));
                } else {
                    rewards.add(rewardMatcher.group("reward"));
                }
                continue;
            }

            Matcher trackingMatcher = line.getMatcher(TRACKING_PATTERN);
            if (trackingMatcher.matches()) {
                trackingState = trackingMatcher.group(1) == null
                        ? ActivityTrackingState.TRACKABLE
                        : ActivityTrackingState.TRACKED;
                continue;
            }

            if (line.isEmpty()) continue;

            // For all other lines, append it to the description
            descriptionLines.add(line);
        }

        StyledText description = StyledTextUtils.joinLines(descriptionLines).getNormalized();
        if (description.isEmpty()) {
            description = null;
        }

        ActivityRequirements requirements = new ActivityRequirements(levelReq, professionLevels, quests);
        return new ActivityInfo(
                type,
                name,
                status,
                Optional.ofNullable(specialInfo),
                Optional.ofNullable(description),
                Optional.ofNullable(length),
                Optional.ofNullable(lengthInfo),
                Optional.ofNullable(distance),
                Optional.ofNullable(distanceInfo),
                Optional.ofNullable(difficulty),
                requirements,
                rewards,
                trackingState);
    }

    private boolean isFulfilled(Matcher colorCodeMatcher) {
        // Check if the requirement is colored green
        return colorCodeMatcher.group(1).charAt(0) == ChatFormatting.GREEN.getChar();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onWorldStateChanged(WorldStateEvent e) {
        resetTracker();
    }

    @SubscribeEvent
    public void onCharacterUpdated(CharacterUpdateEvent event) {
        // First thing to do when we just loaded a class
        scanOverallProgress();
    }

    public CappedValue getOverallProgress() {
        return overallProgress;
    }

    public String getTrackedName() {
        if (trackedActivity == null) return "";

        return trackedActivity.trackedName();
    }

    public ActivityType getTrackedType() {
        if (trackedActivity == null) return null;
        return trackedActivity.trackedType();
    }

    public StyledText getTrackedTask() {
        if (trackedActivity == null) return StyledText.EMPTY;

        return trackedActivity.trackedTask();
    }

    public QuestInfo getTrackedQuestInfo() {
        if (trackedActivity == null) return null;

        return Models.Quest.getQuestInfoFromName(trackedActivity.trackedName()).orElse(null);
    }

    public CaveInfo getTrackedCaveInfo() {
        if (trackedActivity == null) return null;

        return Models.Cave.getCaveInfoFromName(trackedActivity.trackedName()).orElse(null);
    }

    void updateTracker(String name, String type, StyledText nextTask) {
        ActivityType trackedType = ActivityType.from(type);
        trackedActivity = new TrackedActivity(name, trackedType, nextTask);
        ACTIVITY_MARKER_PROVIDER.setTrackedActivityLocation(getTrackedLocation(), trackedType.getColor());

        WynntilsMod.postEvent(new ActivityTrackerUpdatedEvent(
                trackedActivity.trackedType(), trackedActivity.trackedName(), trackedActivity.trackedTask()));
    }

    void resetTracker() {
        trackedActivity = null;
        ACTIVITY_MARKER_PROVIDER.setTrackedActivityLocation(null, null);
    }

    public void scanContentBook(
            ActivityType activityType, BiConsumer<List<ActivityInfo>, List<StyledText>> processResult) {
        // Feature dependency until Model configs
        boolean showUpdates = Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                .showContentBookLoadingUpdates
                .get();
        CONTAINER_QUERIES.queryContentBook(activityType, processResult, showUpdates, false);
    }

    public void startTracking(String name, ActivityType activityType) {
        CONTAINER_QUERIES.toggleTracking(name, activityType);
    }

    public void stopTracking() {
        CONTAINER_QUERIES.toggleTracking(trackedActivity.trackedName(), trackedActivity.trackedType());
    }

    public boolean isTracking() {
        return trackedActivity != null;
    }

    public List<List<StyledText>> getDialogueHistory() {
        return dialogueHistory;
    }

    public void rescanDialogueHistory() {
        DIALOGUE_HISTORY_QUERIES.scanDialogueHistory();
    }

    public void scanOverallProgress() {
        if (!overallProgressOutdated) return;

        overallProgressOutdated = false;
        CONTAINER_QUERIES.queryContentBook(
                ActivityType.RECOMMENDED,
                (ignored, progress) -> {
                    for (StyledText line : progress) {
                        Matcher m = line.getMatcher(OVERALL_PROGRESS_PATTERN);
                        if (m.matches()) {
                            int completed = Integer.parseInt(m.group(1));
                            int total = Integer.parseInt(m.group(2));
                            overallProgress = new CappedValue(completed, total);
                            return;
                        }
                    }
                },
                false,
                true);
    }

    void setDialogueHistory(List<List<StyledText>> newDialogueHistory) {
        dialogueHistory = newDialogueHistory;
        WynntilsMod.postEvent(new DialogueHistoryReloadedEvent());
    }

    private Location getTrackedLocation() {
        return StyledTextUtils.extractLocation(trackedActivity.trackedTask()).orElse(null);
    }

    private record TrackedActivity(String trackedName, ActivityType trackedType, StyledText trackedTask) {}
}
