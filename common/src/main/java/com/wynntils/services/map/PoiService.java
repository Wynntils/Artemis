/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.map;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Service;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.net.event.NetResultProcessedEvent;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.services.map.pois.CombatPoi;
import com.wynntils.services.map.pois.CustomPoi;
import com.wynntils.services.map.pois.LabelPoi;
import com.wynntils.services.map.pois.ServicePoi;
import com.wynntils.services.map.type.CombatKind;
import com.wynntils.services.map.type.CustomPoiProvider;
import com.wynntils.services.map.type.ServiceKind;
import com.wynntils.services.mapdata.providers.builtin.CombatListProvider;
import com.wynntils.services.mapdata.providers.builtin.PlaceListProvider;
import com.wynntils.services.mapdata.providers.builtin.ServiceListProvider;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.Texture;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.neoforged.bus.api.SubscribeEvent;

public class PoiService extends Service {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Label.class, new Label.LabelDeserializer())
            .enableComplexMapKeySerialization()
            .create();

    public static final List<Texture> POI_ICONS = List.of(
            Texture.FLAG,
            Texture.DIAMOND,
            Texture.FIREBALL,
            Texture.SIGN,
            Texture.STAR,
            Texture.WALL,
            Texture.CHEST_T1,
            Texture.CHEST_T2,
            Texture.CHEST_T3,
            Texture.CHEST_T4,
            Texture.FARMING,
            Texture.FISHING,
            Texture.MINING,
            Texture.WOODCUTTING);

    private final Set<LabelPoi> labelPois = new HashSet<>();
    private final Set<ServicePoi> servicePois = new HashSet<>();
    private final Set<CombatPoi> combatPois = new HashSet<>();
    private final Set<CombatPoi> cavePois = new HashSet<>();
    private final Map<CustomPoiProvider, List<CustomPoi>> providedCustomPois = new ConcurrentHashMap<>();

    @Persisted
    private final Storage<List<CustomPoiProvider>> customPoiProviders = new Storage<>(new ArrayList<>());

    public PoiService() {
        super(List.of());

        loadData();
    }

    @Override
    public void reloadData() {
        loadData();
    }

    @Override
    public void onStorageLoad() {
        loadCustomPoiProviders();
    }

    private void loadData() {
        loadPlaces();
        // Slightly hacky way to reduce risk of class loading race in development environment
        // These are loaded serially after places instad
        if (WynntilsMod.isDevelopmentEnvironment()) return;

        loadCaves();
        loadServices();
        loadCombat();
        loadCustomPoiProviders();
    }

    private void loadCaves() {
        Download dl = Managers.Net.download(UrlId.DATA_STATIC_CAVE_INFO);
        dl.handleReader(reader -> {
            Type type = new TypeToken<List<CaveProfile>>() {}.getType();

            List<CaveProfile> profiles = GSON.fromJson(reader, type);

            cavePois.addAll(profiles.stream()
                    .map(profile -> {
                        CombatListProvider.registerFeature(profile.location, CombatKind.CAVES, profile.name);
                        return new CombatPoi(
                                PoiLocation.fromLocation(profile.location), profile.name, CombatKind.CAVES);
                    })
                    .collect(Collectors.toUnmodifiableSet()));
        });
    }

    public void loadCustomPoiProviders() {
        for (CustomPoiProvider poiProvider : customPoiProviders.get()) {
            Managers.Net.download(poiProvider.getUrl(), poiProvider.getName()).handleJsonArray(elements -> {
                List<CustomPoi> pois = new ArrayList<>();

                for (JsonElement jsonElement : elements) {
                    CustomPoi poi = GSON.fromJson(jsonElement, CustomPoi.class);
                    pois.add(poi);
                }

                providedCustomPois.put(poiProvider, ImmutableList.copyOf(pois));
            });
        }
    }

    @SubscribeEvent
    public void onDataLoaded(NetResultProcessedEvent.ForUrlId event) {
        if (!WynntilsMod.isDevelopmentEnvironment()) return;
        // Serialize loading of POIs when on dev env

        if (event.getUrlId() == UrlId.DATA_STATIC_PLACES) {
            loadCaves();
            return;
        }
        if (event.getUrlId() == UrlId.DATA_STATIC_CAVE_INFO) {
            loadServices();
            return;
        }
        if (event.getUrlId() == UrlId.DATA_STATIC_SERVICES) {
            loadCombat();
            return;
        }
        if (event.getUrlId() == UrlId.DATA_STATIC_COMBAT_LOCATIONS) {
            loadCustomPoiProviders();
            return;
        }
    }

    public Stream<LabelPoi> getLabelPois() {
        return labelPois.stream();
    }

    public Stream<ServicePoi> getServicePois() {
        return servicePois.stream();
    }

    public Stream<CombatPoi> getCombatPois() {
        return Stream.concat(combatPois.stream(), cavePois.stream());
    }

    public List<CustomPoi> getProvidedCustomPois() {
        return customPoiProviders.get().stream()
                .filter(CustomPoiProvider::isEnabled)
                .map(provider -> providedCustomPois.getOrDefault(provider, List.of()))
                .flatMap(List::stream)
                .toList();
    }

    public List<CustomPoiProvider> getCustomPoiProviders() {
        return customPoiProviders.get();
    }

    public void addCustomPoiProvider(CustomPoiProvider poiProvider) {
        customPoiProviders.get().add(poiProvider);
        customPoiProviders.touched();
        loadCustomPoiProviders();
    }

    public boolean isPoiProvided(CustomPoi customPoi) {
        return getProvidedCustomPois().contains(customPoi);
    }

    private void loadPlaces() {
        Download dl = Managers.Net.download(UrlId.DATA_STATIC_PLACES);
        dl.handleReader(reader -> {
            PlacesProfile places = GSON.fromJson(reader, PlacesProfile.class);
            for (Label label : places.labels) {
                labelPois.add(new LabelPoi(label));
                PlaceListProvider.registerFeature(label);
            }
        });
    }

    private void loadServices() {
        Download dl = Managers.Net.download(UrlId.DATA_STATIC_SERVICES);
        dl.handleReader(reader -> {
            Type type = new TypeToken<List<ServiceProfile>>() {}.getType();

            List<ServiceProfile> serviceList = GSON.fromJson(reader, type);
            for (ServiceProfile service : serviceList) {
                ServiceKind kind = ServiceKind.fromString(service.type);
                if (kind != null) {
                    for (PoiLocation location : service.locations) {
                        servicePois.add(new ServicePoi(location, kind));
                        ServiceListProvider.registerFeature(new Location(location), kind);
                    }
                } else {
                    WynntilsMod.warn("Unknown service type in services.json: " + service.type);
                }
            }
        });
    }

    private void loadCombat() {
        Download dl = Managers.Net.download(UrlId.DATA_STATIC_COMBAT_LOCATIONS);
        dl.handleReader(reader -> {
            Type type = new TypeToken<List<CombatProfileList>>() {}.getType();

            List<CombatProfileList> combatProfileLists = GSON.fromJson(reader, type);
            for (CombatProfileList combatList : combatProfileLists) {
                CombatKind kind = CombatKind.fromString(combatList.type);
                // We load caves separately... until the refactor
                if (kind != null && kind != CombatKind.CAVES) {
                    for (CombatProfile profile : combatList.locations) {
                        combatPois.add(new CombatPoi(profile.coordinates, profile.name, kind));
                        CombatListProvider.registerFeature(new Location(profile.coordinates), kind, profile.name);
                    }
                } else {
                    WynntilsMod.warn("Unknown combat type in combat.json: " + combatList.type);
                }
            }
        });
    }

    public boolean removeCustomPoiProvider(String name) {
        Optional<CustomPoiProvider> provider = customPoiProviders.get().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();

        if (provider.isEmpty()) return false;

        customPoiProviders.get().remove(provider.get());
        providedCustomPois.remove(provider.get());

        return true;
    }

    private static class PlacesProfile {
        List<Label> labels;
    }

    private static class ServiceProfile {
        String type;
        List<PoiLocation> locations;
    }

    private static class CombatProfile {
        String name;
        PoiLocation coordinates;
    }

    private static class CombatProfileList {
        String type;
        List<CombatProfile> locations;
    }

    private record CaveProfile(
            String type,
            String name,
            String specialInfo,
            String description,
            String length,
            String lengthInfo,
            String difficulty,
            int requirements,
            List<String> rewards,
            Location location) {}
}
