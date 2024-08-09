/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.net.event.NetResultProcessedEvent;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearMetaInfo;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.stats.type.FixedStats;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.models.wynnitem.AbstractItemInfoDeserializer;
import com.wynntils.utils.JsonUtils;
import com.wynntils.utils.type.Pair;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.neoforged.bus.api.SubscribeEvent;

public class GearInfoRegistry {
    private List<GearInfo> gearInfoRegistry = List.of();
    private Map<String, GearInfo> gearInfoLookup = Map.of();
    private Map<String, GearInfo> gearInfoLookupApiName = Map.of();

    public GearInfoRegistry() {
        WynntilsMod.registerEventListener(this);

        loadData();
    }

    public void loadData() {
        // We do not explicitly load the gear DB here,
        // but when all of it's dependencies are loaded,
        // the NetResultProcessedEvent will trigger the load.
    }

    @SubscribeEvent
    public void onDataLoaded(NetResultProcessedEvent.ForUrlId event) {
        UrlId urlId = event.getUrlId();
        if (urlId == UrlId.DATA_STATIC_ITEM_SETS) {
            if (!Models.Set.hasSetData()) return;

            loadGearRegistry();
            return;
        }
    }

    public GearInfo getFromDisplayName(String gearName) {
        return gearInfoLookup.get(gearName);
    }

    public GearInfo getFromApiName(String apiName) {
        GearInfo gearInfo = gearInfoLookupApiName.get(apiName);
        if (gearInfo != null) return gearInfo;

        // The name is only stored in gearInfoLookupApiName if it differs from the display name
        // Otherwise the api name is the same as the display name
        return gearInfoLookup.get(apiName);
    }

    public Stream<GearInfo> getGearInfoStream() {
        return gearInfoRegistry.stream();
    }

    private void loadGearRegistry() {
        Download dl = Managers.Net.download(UrlId.DATA_STATIC_GEAR_ADVANCED);
        dl.handleJsonObject(json -> {
            Gson gson = new GsonBuilder()
                    .registerTypeHierarchyAdapter(GearInfo.class, new GearInfoDeserializer())
                    .create();

            List<GearInfo> registry = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                JsonObject itemObject = entry.getValue().getAsJsonObject();

                // Inject the name into the object
                itemObject.addProperty("name", entry.getKey());

                // Deserialize the item
                GearInfo gearInfo = gson.fromJson(itemObject, GearInfo.class);

                // Add the item to the registry
                registry.add(gearInfo);
            }

            // Create fast lookup maps
            Map<String, GearInfo> lookupMap = new HashMap<>();
            Map<String, GearInfo> altLookupMap = new HashMap<>();
            for (GearInfo gearInfo : registry) {
                lookupMap.put(gearInfo.name(), gearInfo);
                if (gearInfo.metaInfo().apiName().isPresent()) {
                    altLookupMap.put(gearInfo.metaInfo().apiName().get(), gearInfo);
                }
            }

            // Make the result visisble to the world
            gearInfoRegistry = registry;
            gearInfoLookup = lookupMap;
            gearInfoLookupApiName = altLookupMap;
        });
    }

    private static final class GearInfoDeserializer extends AbstractItemInfoDeserializer<GearInfo> {
        @Override
        public GearInfo deserialize(JsonElement jsonElement, Type jsonType, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject json = jsonElement.getAsJsonObject();

            Pair<String, String> names = parseNames(json);
            String displayName = names.key();
            String internalName = names.value();

            GearType type = parseType(json);
            if (type == null) {
                throw new RuntimeException("Invalid Wynncraft data: item has no gear type");
            }

            GearTier tier = GearTier.fromString(json.get("tier").getAsString());
            if (tier == null) {
                throw new RuntimeException("Invalid Wynncraft data: item has no gear tier");
            }

            int powderSlots = JsonUtils.getNullableJsonInt(json, "powderSlots");

            GearMetaInfo metaInfo = parseMetaInfo(json, internalName, type);
            GearRequirements requirements = parseRequirements(json, type);
            FixedStats fixedStats = parseFixedStats(json);
            List<Pair<StatType, StatPossibleValues>> variableStats = parseVariableStats(json, "identifications");

            return new GearInfo(
                    displayName,
                    type,
                    tier,
                    powderSlots,
                    metaInfo,
                    requirements,
                    fixedStats,
                    variableStats,
                    Optional.ofNullable(Models.Set.getSetInfoForItem(displayName)));
        }
    }
}
