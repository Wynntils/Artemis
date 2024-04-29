/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.territories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.AdvancementUpdateEvent;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.services.map.type.TerritoryDefenseFilterType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.core.Position;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TerritoryModel extends Model {
    private static final int TERRITORY_UPDATE_MS = 15000;
    private static final Gson TERRITORY_PROFILE_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(TerritoryProfile.class, new TerritoryProfile.TerritoryDeserializer())
            .create();

    // This is territory POIs as returned by the advancement from Wynncraft
    private final Map<String, TerritoryPoi> territoryPoiMap = new ConcurrentHashMap<>();

    // This is the profiles as downloaded from Athena
    private Map<String, TerritoryProfile> territoryProfileMap = new HashMap<>();

    // This is just a cache of TerritoryPois created for all territoryProfileMap values
    private Set<TerritoryPoi> allTerritoryPois = new HashSet<>();

    private final ScheduledExecutorService timerExecutor = new ScheduledThreadPoolExecutor(1);

    public TerritoryModel() {
        super(List.of());

        timerExecutor.scheduleWithFixedDelay(
                this::updateTerritoryProfileMap, 0, TERRITORY_UPDATE_MS, TimeUnit.MILLISECONDS);
    }

    public TerritoryProfile getTerritoryProfile(String name) {
        return territoryProfileMap.get(name);
    }

    /**
     * Get the territory profile from a short name. This is used when the territory name is cut off, like scoreboards.
     *
     * @param shortName           The short name of the territory
     * @param excludedTerritories Territories to exclude from the search
     * @return The territory profile, or null if not found
     */
    public TerritoryProfile getTerritoryProfileFromShortName(
            String shortName, Collection<TerritoryProfile> excludedTerritories) {
        return territoryProfileMap.values().stream()
                .filter(profile -> !excludedTerritories.contains(profile))
                .filter(profile -> profile.getName().startsWith(shortName))
                .min(Comparator.comparing(TerritoryProfile::getName))
                .orElse(null);
    }

    public Stream<String> getTerritoryNames() {
        return territoryProfileMap.keySet().stream();
    }

    public Set<TerritoryPoi> getTerritoryPois() {
        return allTerritoryPois;
    }

    public List<TerritoryPoi> getTerritoryPoisFromAdvancement() {
        return new ArrayList<>(territoryPoiMap.values());
    }

    public List<TerritoryPoi> getFilteredTerritoryPoisFromAdvancement(
            int filterLevel, TerritoryDefenseFilterType filterType) {
        return switch (filterType) {
            case HIGHER -> territoryPoiMap.values().stream()
                    .filter(poi -> poi.getTerritoryInfo().getDefences().getLevel() >= filterLevel)
                    .collect(Collectors.toList());
            case LOWER -> territoryPoiMap.values().stream()
                    .filter(poi -> poi.getTerritoryInfo().getDefences().getLevel() <= filterLevel)
                    .collect(Collectors.toList());
            case DEFAULT -> territoryPoiMap.values().stream()
                    .filter(poi -> poi.getTerritoryInfo().getDefences().getLevel() == filterLevel)
                    .collect(Collectors.toList());
        };
    }

    public TerritoryPoi getTerritoryPoiFromAdvancement(String name) {
        return territoryPoiMap.get(name);
    }

    public TerritoryProfile getTerritoryProfileForPosition(Position position) {
        return territoryProfileMap.values().stream()
                .filter(profile -> profile.insideArea(position))
                .findFirst()
                .orElse(null);
    }

    @SubscribeEvent
    public void onAdvancementUpdate(AdvancementUpdateEvent event) {
        Map<String, TerritoryInfo> tempMap = new HashMap<>();

        for (AdvancementHolder added : event.getAdded()) {
            Advancement advancement = added.value();

            if (advancement.display().isEmpty()) continue;

            DisplayInfo displayInfo = advancement.display().get();
            String territoryName = StyledText.fromComponent(displayInfo.getTitle())
                    .replaceAll("\\[", "")
                    .replaceAll("\\]", "")
                    .trim()
                    .getStringWithoutFormatting();

            // Do not parse same thing twice
            if (tempMap.containsKey(territoryName)) continue;

            // ignore empty display texts they are used to generate the "lines"
            if (territoryName.isEmpty()) continue;

            // headquarters frame is challenge
            boolean headquarters = displayInfo.getFrame() == FrameType.CHALLENGE;

            // description is a raw string with \n, so we have to split
            StyledText description = StyledText.fromComponent(displayInfo.getDescription());
            StyledText[] colored = description.split("\n");
            String[] raw = description.getStringWithoutFormatting().split("\n");

            TerritoryInfo container = new TerritoryInfo(raw, colored, headquarters);
            tempMap.put(territoryName, container);
        }

        for (Map.Entry<String, TerritoryInfo> entry : tempMap.entrySet()) {
            TerritoryProfile territoryProfile = getTerritoryProfile(entry.getKey());

            if (territoryProfile == null) continue;

            territoryPoiMap.put(
                    entry.getKey(), new TerritoryPoi(() -> getTerritoryProfile(entry.getKey()), entry.getValue()));
        }
    }

    private void updateTerritoryProfileMap() {
        Download dl = Managers.Net.download(UrlId.DATA_WYNNCRAFT_TERRITORY_LIST);
        dl.handleJsonObject(
                json -> {
                    Map<String, TerritoryProfile> tempMap = new HashMap<>();
                    for (Map.Entry<String, JsonElement> entry :
                            json.getAsJsonObject().entrySet()) {
                        JsonObject territoryObject = entry.getValue().getAsJsonObject();

                        // Inject back the name for the deserializer
                        territoryObject.addProperty("name", entry.getKey());

                        TerritoryProfile territoryProfile =
                                TERRITORY_PROFILE_GSON.fromJson(territoryObject, TerritoryProfile.class);
                        tempMap.put(entry.getKey(), territoryProfile);
                    }

                    territoryProfileMap = tempMap;
                    allTerritoryPois = territoryProfileMap.values().stream()
                            .map(TerritoryPoi::new)
                            .collect(Collectors.toSet());
                },
                onError -> WynntilsMod.warn("Failed to update territory data."));
    }
}
