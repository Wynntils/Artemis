/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.providers.MapDataProvider;
import com.wynntils.services.mapdata.providers.builtin.BuiltInProvider;
import com.wynntils.services.mapdata.providers.builtin.CategoriesProvider;
import com.wynntils.services.mapdata.providers.builtin.CharacterProvider;
import com.wynntils.services.mapdata.providers.builtin.CombatListProvider;
import com.wynntils.services.mapdata.providers.builtin.MapIconsProvider;
import com.wynntils.services.mapdata.providers.builtin.PlaceListProvider;
import com.wynntils.services.mapdata.providers.builtin.ServiceListProvider;
import com.wynntils.services.mapdata.providers.json.JsonProvider;
import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.services.mapdata.type.MapFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MapDataProviders {
    private static final MapDataProvider ONLINE_PLACEHOLDER_PROVIDER = new PlaceholderProvider();

    private final List<String> providerOrder = new ArrayList<>();
    private final Map<String, MapDataProvider> allProviders = new HashMap<>();

    public MapDataProviders() {
        createBuiltInProviders();

        // FIXME: demo
        createLocalProvider("local-1", "mapdata.json");
        createOnlineProvider(
                "online-1",
                "https://gist.githubusercontent.com/magicus/a2c810380a34a7474a2651400d36d72c/raw/online-mapdata.json");
    }

    Stream<MapDataProvider> getProviders() {
        return providerOrder.stream().map(allProviders::get);
    }

    // per-account, per-character or shared
    // can be added just from disk, or downloaded from an url
    public void createLocalProvider(String id, String filename) {
        String completeId = "local:" + id;
        JsonProvider provider = JsonProvider.loadLocalResource(completeId, filename);
        registerProvider(completeId, provider);
    }

    public void createOnlineProvider(String id, String url) {
        String completeId = "online:" + id;
        JsonProvider.loadOnlineResource(completeId, url, this::registerProvider);
        // Register a dummy provider; this will be replaced once loading has finished
        registerProvider(completeId, ONLINE_PLACEHOLDER_PROVIDER);
    }

    private void createBuiltInProviders() {
        // Metadata
        registerBuiltInProvider(new CategoriesProvider());
        registerBuiltInProvider(new MapIconsProvider());

        // Locations
        registerBuiltInProvider(new ServiceListProvider());
        registerBuiltInProvider(new CombatListProvider());
        registerBuiltInProvider(new PlaceListProvider());
        registerBuiltInProvider(new CharacterProvider());
    }

    private void registerBuiltInProvider(BuiltInProvider provider) {
        registerProvider("built-in:" + provider.getProviderId(), provider);
    }

    private void registerProvider(String providerId, MapDataProvider provider) {
        if (!allProviders.containsKey(providerId)) {
            // It is not previously known, so add it last
            providerOrder.add(providerId);
        }
        // Add or update the provider
        allProviders.put(providerId, provider);
    }

    private static class PlaceholderProvider implements MapDataProvider {
        @Override
        public Stream<MapFeature> getFeatures() {
            return Stream.empty();
        }

        @Override
        public Stream<MapCategory> getCategories() {
            return Stream.empty();
        }

        @Override
        public Stream<MapIcon> getIcons() {
            return Stream.empty();
        }
    }
}
