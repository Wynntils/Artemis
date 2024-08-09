/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.SetSpawnEvent;
import com.wynntils.models.activities.type.ActivityDifficulty;
import com.wynntils.models.activities.type.ActivityInfo;
import com.wynntils.models.activities.type.ActivityLength;
import com.wynntils.models.activities.type.ActivityRequirements;
import com.wynntils.models.activities.type.ActivityType;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.utils.EnumUtils;
import com.wynntils.utils.FileUtils;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.StyledTextUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.Pair;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

// Note: To run this sucessfully, you have to have a class with all content trackable in the content book
@StartDisabled
@ConfigCategory(Category.DEBUG)
public class ContentBookDumpFeature extends Feature {
    // Temporary hack...
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CustomColor.class, new CustomColor.CustomColorSerializer())
            .registerTypeAdapterFactory(new EnumUtils.EnumTypeAdapterFactory<>())
            .enableComplexMapKeySerialization()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private static final File SAVE_FOLDER = WynntilsMod.getModStorageDir("debug");

    @RegisterKeyBind
    private final KeyBind dumpContentBook =
            new KeyBind("Dump Content Book", GLFW.GLFW_KEY_UNKNOWN, true, this::dumpContentBook);

    private List<DumpableActivityInfo> currentDump = List.of();

    private Location lastTrackedLocation = null;
    private DumpableActivityInfo currentlyTracking = null;
    private Queue<DumpableActivityInfo> manualTrackingRequired = new LinkedList<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSetSpawn(SetSpawnEvent event) {
        if (currentlyTracking == null) return;

        Optional<Location> spawnLocationOpt = Models.Activity.ACTIVITY_MARKER_PROVIDER.getSpawnLocation();
        if (spawnLocationOpt.isEmpty()) {
            WynntilsMod.error("Could not get spawn location for " + currentlyTracking.name());
            return;
        }

        Location currentTracker = spawnLocationOpt.get();

        if (lastTrackedLocation != currentTracker && currentTracker != null) {
            currentDump.remove(currentlyTracking);

            currentDump.add(new DumpableActivityInfo(
                    currentlyTracking.type(),
                    currentlyTracking.name(),
                    currentlyTracking.specialInfo(),
                    currentlyTracking.description(),
                    currentlyTracking.length(),
                    currentlyTracking.lengthInfo(),
                    currentlyTracking.difficulty(),
                    currentlyTracking.requirements(),
                    currentlyTracking.rewards(),
                    currentTracker));

            WynntilsMod.info("Got location for " + currentlyTracking.name() + ": " + currentTracker);

            trackManually();
        } else {
            WynntilsMod.warn("Could not get updated location for " + currentlyTracking.name() + ": " + currentTracker);
        }
    }

    private void dumpContentBook() {
        currentDump = new ArrayList<>();

        Models.Activity.scanContentBook(ActivityType.RECOMMENDED, (activityInfos, progress) -> {
            currentDump.addAll(activityInfos.stream()
                    .map(DumpableActivityInfo::fromActivityInfo)
                    .toList());

            Models.Activity.scanContentBook(ActivityType.TERRITORIAL_DISCOVERY, (activityInfos2, progress2) -> {
                currentDump.addAll(activityInfos2.stream()
                        .map(DumpableActivityInfo::fromActivityInfo)
                        .toList());

                filterEntriesNeedingManualTracking();

                trackManually();
            });
        });
    }

    private void filterEntriesNeedingManualTracking() {
        manualTrackingRequired = new LinkedList<>();

        List<DumpableActivityInfo> trackingNeeded = new ArrayList<>();

        for (DumpableActivityInfo info : currentDump) {
            // Tracking is bugged for this at the time of writing
            if (Objects.equals(info.name(), "Galleon\u0027s Graveyard")) continue;

            switch (info.type()) {
                case BOSS_ALTAR, LOOTRUN_CAMP, DUNGEON, RAID -> trackingNeeded.add(info);
            }
        }

        trackingNeeded.sort(Comparator.comparing(DumpableActivityInfo::type));

        manualTrackingRequired.addAll(trackingNeeded);
    }

    private void trackManually() {
        DumpableActivityInfo info = manualTrackingRequired.poll();

        // We are done
        if (info == null) {
            endDumping();
            return;
        }

        // Track the activity
        currentlyTracking = info;
        lastTrackedLocation =
                Models.Activity.ACTIVITY_MARKER_PROVIDER.getSpawnLocation().orElse(null);
        WynntilsMod.info("Tracking " + info.name());
        Models.Activity.startTracking(info.name(), info.type());
    }

    private void endDumping() {
        saveToDisk();

        // Free up memory / Reset
        currentDump = List.of();
        manualTrackingRequired = new LinkedList<>();
        currentlyTracking = null;
        lastTrackedLocation = null;
    }

    private void saveToDisk() {
        // Save the dump to a file
        Map<ActivityType, List<DumpableActivityInfo>> mappedActivities = getMappedDumpedActivities();

        JsonElement element = GSON.toJsonTree(mappedActivities);

        String fileName = "content_book_dump.json";
        File jsonFile = new File(SAVE_FOLDER, fileName);

        FileUtils.mkdir(jsonFile.getParentFile());

        try (OutputStreamWriter fileWriter =
                new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
            GSON.toJson(element.getAsJsonObject(), fileWriter);
        } catch (IOException e) {
            WynntilsMod.error("Failed to save json file " + jsonFile, e);
        }

        McUtils.sendMessageToClient(Component.literal("Saved content book dump to " + jsonFile.getAbsolutePath()));
    }

    // This method creates a consistent mapping and ordering for the dumped activities
    private Map<ActivityType, List<DumpableActivityInfo>> getMappedDumpedActivities() {
        Map<ActivityType, List<DumpableActivityInfo>> mappedDumpedActivities = new EnumMap<>(ActivityType.class);
        for (ActivityType value : ActivityType.values()) {
            mappedDumpedActivities.put(value, new ArrayList<>());
        }

        for (DumpableActivityInfo activityInfo : currentDump) {
            ActivityType type = activityInfo.type();

            // Merge STORYLINE_QUEST and QUEST
            if (type == ActivityType.STORYLINE_QUEST) {
                type = ActivityType.QUEST;
            }

            mappedDumpedActivities.get(type).add(activityInfo);
        }

        // Remove unused types
        mappedDumpedActivities.remove(ActivityType.RECOMMENDED);
        mappedDumpedActivities.remove(ActivityType.STORYLINE_QUEST);

        for (List<DumpableActivityInfo> value : mappedDumpedActivities.values()) {
            value.sort(Comparator.comparing(DumpableActivityInfo::name));
        }

        return mappedDumpedActivities;
    }

    private record DumpableActivityInfo(
            ActivityType type,
            String name,
            String specialInfo,
            String description,
            ActivityLength length,
            String lengthInfo,
            ActivityDifficulty difficulty,
            DumpableActivityRequirements requirements,
            List<String> rewards,
            Location location) {
        private static DumpableActivityInfo fromActivityInfo(ActivityInfo activityInfo) {
            return new DumpableActivityInfo(
                    activityInfo.type(),
                    activityInfo.name(),
                    activityInfo.specialInfo().orElse(""),
                    activityInfo.description().map(StyledText::getString).orElse(""),
                    activityInfo.length().orElse(null),
                    activityInfo.lengthInfo().orElse(""),
                    activityInfo.difficulty().orElse(null),
                    DumpableActivityRequirements.fromActivityRequirements(activityInfo.requirements()),
                    activityInfo.rewards(),
                    StyledTextUtils.extractLocation(activityInfo.description().orElse(StyledText.EMPTY))
                            .orElse(null));
        }
    }

    private record DumpableActivityRequirements(
            int level, Map<ProfessionType, Integer> professionLevels, List<String> quests) {
        private static DumpableActivityRequirements fromActivityRequirements(ActivityRequirements requirements) {
            return new DumpableActivityRequirements(
                    requirements.level().a(),
                    requirements.professionLevels().stream().collect(Collectors.toMap(o -> o.a().a(), o -> o.a().b())),
                    requirements.quests().stream().map(Pair::a).toList());
        }
    }
}
