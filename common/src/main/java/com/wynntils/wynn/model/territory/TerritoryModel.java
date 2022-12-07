/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.territory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wynntils.core.managers.CoreManager;
import com.wynntils.core.net.Reference;
import com.wynntils.core.net.downloader.DownloadableResource;
import com.wynntils.core.net.downloader.Downloader;
import com.wynntils.mc.event.AdvancementUpdateEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.wynn.model.map.poi.Poi;
import com.wynntils.wynn.model.map.poi.TerritoryPoi;
import com.wynntils.wynn.model.territory.objects.TerritoryInfo;
import com.wynntils.wynn.objects.profiles.TerritoryProfile;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TerritoryModel extends CoreManager {
    private static final int TERRITORY_UPDATE_MS = 15000;

    // This is territory POIs as returned by the advancement from Wynncraft
    private static Map<String, TerritoryPoi> territoryPoiMap = new ConcurrentHashMap<>();

    // This is the profiles as downloaded from Athena
    private static Map<String, TerritoryProfile> territoryProfileMap = new HashMap<>();

    // This is just a cache of TerritoryPois created for all territoryProfileMap values
    private static Set<TerritoryPoi> allTerritoryPois = new HashSet<>();

    private static ScheduledThreadPoolExecutor timerExecutor = new ScheduledThreadPoolExecutor(1);

    public static void init() {
        timerExecutor.scheduleWithFixedDelay(
                TerritoryModel::updateTerritoryProfileMap, 0, TERRITORY_UPDATE_MS, TimeUnit.MILLISECONDS);
    }

    public static TerritoryProfile getTerritoryProfile(String name) {
        return territoryProfileMap.get(name);
    }

    public static Stream<String> getTerritoryNames() {
        return territoryProfileMap.keySet().stream();
    }

    public static Set<TerritoryPoi> getTerritoryPois() {
        return allTerritoryPois;
    }

    public static List<Poi> getTerritoryPoisFromAdvancement() {
        // FIXME: Clarify how this differs from getTerritoryPois()...
        return new ArrayList<>(territoryPoiMap.values());
    }

    public static TerritoryPoi getTerritoryPoiFromAdvancement(String name) {
        return territoryPoiMap.get(name);
    }

    @SubscribeEvent
    public static void onAdvancementUpdate(AdvancementUpdateEvent event) {
        Map<String, TerritoryInfo> tempMap = new HashMap<>();

        for (Map.Entry<ResourceLocation, Advancement.Builder> added :
                event.getAdded().entrySet()) {
            added.getValue().parent((ResourceLocation) null);
            Advancement built = added.getValue().build(added.getKey());

            if (built.getDisplay() == null) continue;

            String territoryName = ComponentUtils.getUnformatted(
                            built.getDisplay().getTitle())
                    .replace("[", "")
                    .replace("]", "")
                    .trim();

            // Do not parse same thing twice
            if (tempMap.containsKey(territoryName)) continue;

            // ignore empty display texts they are used to generate the "lines"
            if (territoryName.isEmpty()) continue;

            // headquarters frame is challenge
            boolean headquarters = built.getDisplay().getFrame() == FrameType.CHALLENGE;

            // description is a raw string with \n, so we have to split
            String description = ComponentUtils.getCoded(built.getDisplay().getDescription());
            String[] colored = description.split("\n");
            String[] raw = ComponentUtils.stripFormatting(description).split("\n");

            TerritoryInfo container = new TerritoryInfo(raw, colored, headquarters);
            tempMap.put(territoryName, container);
        }

        for (Map.Entry<String, TerritoryInfo> entry : tempMap.entrySet()) {
            TerritoryProfile territoryProfile = getTerritoryProfile(entry.getKey());

            if (territoryProfile == null) continue;

            territoryPoiMap.put(entry.getKey(), new TerritoryPoi(territoryProfile, entry.getValue()));
        }
    }

    private static void updateTerritoryProfileMap() {
        // This will get us a version of https://api.wynncraft.com/public_api.php?action=territoryList
        // as cached on Athena
        String url = Reference.URLs.getAthena() + "/cache/get/territoryList";
        DownloadableResource dl = Downloader.download(url, "territories.json", "territory");
        dl.handleJsonObject(json -> {
            if (!json.has("territories")) return false;

            Type type = new TypeToken<HashMap<String, TerritoryProfile>>() {}.getType();

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeHierarchyAdapter(TerritoryProfile.class, new TerritoryProfile.TerritoryDeserializer());
            Gson gson = builder.create();

            territoryProfileMap = gson.fromJson(json.get("territories"), type);
            allTerritoryPois =
                    territoryProfileMap.values().stream().map(TerritoryPoi::new).collect(Collectors.toSet());
            // TODO: Add events
            return true;
        });
    }
}
