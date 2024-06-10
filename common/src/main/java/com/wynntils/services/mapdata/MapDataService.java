/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Service;
import com.wynntils.core.components.Services;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapAttributes;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapVisibility;
import com.wynntils.services.mapdata.providers.MapDataProvider;
import com.wynntils.services.mapdata.providers.builtin.BuiltInProvider;
import com.wynntils.services.mapdata.providers.builtin.CategoriesProvider;
import com.wynntils.services.mapdata.providers.builtin.CombatListProvider;
import com.wynntils.services.mapdata.providers.builtin.LootChestsProvider;
import com.wynntils.services.mapdata.providers.builtin.MapIconsProvider;
import com.wynntils.services.mapdata.providers.builtin.PlaceListProvider;
import com.wynntils.services.mapdata.providers.builtin.PlayerProvider;
import com.wynntils.services.mapdata.providers.builtin.ServiceListProvider;
import com.wynntils.services.mapdata.providers.builtin.TerritoryProvider;
import com.wynntils.services.mapdata.providers.builtin.WaypointsProvider;
import com.wynntils.services.mapdata.providers.json.JsonProvider;
import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.services.mapdata.type.MapDataProvidedType;
import com.wynntils.services.mapdata.type.MapFeature;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MapDataService extends Service {
    private static final CategoriesProvider CATEGORIES_PROVIDER = new CategoriesProvider();
    private static final MapIconsProvider MAP_ICONS_PROVIDER = new MapIconsProvider();
    private static final ServiceListProvider SERVICE_LIST_PROVIDER = new ServiceListProvider();
    private static final CombatListProvider COMBAT_LIST_PROVIDER = new CombatListProvider();
    private static final PlaceListProvider PLACE_LIST_PROVIDER = new PlaceListProvider();
    private static final PlayerProvider PLAYER_PROVIDER = new PlayerProvider();
    private static final TerritoryProvider TERRITORY_PROVIDER = new TerritoryProvider();

    public static final WaypointsProvider WAYPOINTS_PROVIDER = new WaypointsProvider();
    public static final LootChestsProvider LOOT_CHESTS_PROVIDER = new LootChestsProvider();

    private static final MapDataProvider ONLINE_PLACEHOLDER_PROVIDER = new PlaceholderProvider();
    // FIXME: i18n
    private static final String NAMELESS_CATEGORY = "Category '%s'";

    // Used for referencing the map data service before it is fully initialized in Services
    private final LinkedList<String> providerOrder = new LinkedList<>();
    private final Map<String, MapDataProvider> allProviders = new HashMap<>();
    private final Map<MapFeature, ResolvedMapAttributes> resolvedAttributesCache = new HashMap<>();
    private final Map<String, Optional<MapIcon>> iconCache = new HashMap<>();

    public MapDataService() {
        super(List.of());

        createBuiltInProviders();
    }

    @Override
    public void reloadData() {
        getProviders().forEach(MapDataProvider::reloadData);
    }

    public Stream<MapFeature> getFeatures() {
        return getProviders().flatMap(MapDataProvider::getFeatures);
    }

    public Stream<MapFeature> getFeaturesForCategory(String categoryId) {
        return getFeatures().filter(f -> f.getCategoryId().startsWith(categoryId));
    }

    // region Lookup features and resolve attributes

    public ResolvedMapAttributes resolveMapAttributes(MapFeature feature) {
        return resolvedAttributesCache.computeIfAbsent(feature, k -> MapAttributesResolver.resolve(feature));
    }

    public Stream<MapCategory> getCategoryDefinitions(String categoryId) {
        return getProviders().flatMap(MapDataProvider::getCategories).filter(p -> p.getCategoryId()
                .equals(categoryId));
    }

    public String getCategoryName(String categoryId) {
        return getCategoryDefinitions(categoryId)
                .map(MapCategory::getName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(NAMELESS_CATEGORY.formatted(categoryId));
    }

    public Optional<MapIcon> getIcon(String iconId) {
        if (iconId.equals(MapIcon.NO_ICON_ID)) return Optional.empty();

        return iconCache.computeIfAbsent(iconId, k -> {
            Stream<MapIcon> allIcons = getProviders().flatMap(MapDataProvider::getIcons);
            return allIcons.filter(i -> i.getIconId().equals(iconId)).findFirst();
        });
    }

    public Optional<MapIcon> getIcon(MapFeature feature) {
        return getIcon(resolveMapAttributes(feature).iconId());
    }

    public MapIcon getIconOrFallback(String iconId) {
        return getIcon(iconId)
                .orElse(Services.MapData.getIcon(MapIconsProvider.FALLBACK_ICON_ID)
                        .get());
    }

    public MapIcon getIconOrFallback(MapFeature feature) {
        return getIcon(feature)
                .orElse(Services.MapData.getIcon(MapIconsProvider.FALLBACK_ICON_ID)
                        .get());
    }

    // endregion

    // region Providers

    // per-account, per-character or shared
    // can be added just from disk, or downloaded from an url
    public void createBundledProvider(String id, String filename) {
        String completeId = "bundled:" + id;
        JsonProvider provider = JsonProvider.loadBundledResource(completeId, filename);
        registerProvider(completeId, provider);
    }

    public void createLocalProvider(String id, String filename) {
        String completeId = "local:" + id;
        JsonProvider provider = JsonProvider.loadLocalFile(completeId, new File(filename));
        registerProvider(completeId, provider);
    }

    public void createOnlineProvider(String id, String url) {
        String completeId = "online:" + id;
        JsonProvider.loadOnlineResource(completeId, url, this::registerProvider);
        // Register a dummy provider; this will be replaced once loading has finished
        registerProvider(completeId, ONLINE_PLACEHOLDER_PROVIDER);
    }

    public void prioritizeProvider(String providerId) {
        // This functionality should be replaced with a general reordering of
        // providers from a GUI
        if (providerOrder.remove(providerId)) {
            // If it existed, put it back first
            providerOrder.addFirst(providerId);
        }
    }

    private void createBuiltInProviders() {
        // Metadata
        registerBuiltInProvider(CATEGORIES_PROVIDER);
        registerBuiltInProvider(MAP_ICONS_PROVIDER);

        // Locations
        registerBuiltInProvider(SERVICE_LIST_PROVIDER);
        registerBuiltInProvider(COMBAT_LIST_PROVIDER);
        registerBuiltInProvider(PLACE_LIST_PROVIDER);
        registerBuiltInProvider(PLAYER_PROVIDER);
        registerBuiltInProvider(TERRITORY_PROVIDER);
        registerBuiltInProvider(WAYPOINTS_PROVIDER);
        registerBuiltInProvider(LOOT_CHESTS_PROVIDER);
    }

    private void registerBuiltInProvider(BuiltInProvider provider) {
        registerProvider("built-in:" + provider.getProviderId(), provider);
        WynntilsMod.registerEventListener(provider);
    }

    private void registerProvider(String providerId, MapDataProvider provider) {
        if (provider == null) {
            WynntilsMod.warn("Provider missing for '" + providerId + "'");
            return;
        }
        if (!allProviders.containsKey(providerId)) {
            // It is not previously known, so add it first
            providerOrder.addFirst(providerId);
        }
        // Add or update the provider
        allProviders.put(providerId, provider);
        provider.onChange(this::onProviderChange);

        // Invalidate caches
        invalidateAllCaches();
    }

    private void onProviderChange(MapDataProvidedType mapDataProvidedType) {
        if (mapDataProvidedType instanceof MapFeature mapFeature) {
            resolvedAttributesCache.remove(mapFeature);
        } else if (mapDataProvidedType instanceof MapIcon mapIcon) {
            iconCache.remove(mapIcon.getIconId());
        } else if (mapDataProvidedType instanceof MapCategory mapCategory) {
            // If this happens, we need to redo everything
            invalidateAllCaches();
        }
    }

    private void invalidateAllCaches() {
        resolvedAttributesCache.clear();
        iconCache.clear();
    }

    private Stream<MapDataProvider> getProviders() {
        return providerOrder.stream().map(allProviders::get);
    }

    // endregion

    /** This method requires a MapVisibility with all values non-empty to work correctly. */
    public float calculateVisibility(ResolvedMapVisibility mapVisibility, float zoomLevel) {
        float min = mapVisibility.min();
        float max = mapVisibility.max();
        float fade = mapVisibility.fade();

        // Having max larger than min is a way to represent "never show"
        if (max < min) {
            return 0f;
        }

        float startFadeIn = min - fade;
        float stopFadeIn = min + fade;
        float startFadeOut = max - fade;
        float stopFadeOut = max + fade;

        // If min or max is at the extremes, do not apply fading.
        // This is a way of representing "always show".
        if (min <= 1) {
            startFadeIn = 0;
            stopFadeIn = 0;
        }
        if (max >= 100) {
            startFadeOut = 101;
            stopFadeOut = 101;
        }

        if (zoomLevel < startFadeIn) {
            return 0;
        }
        if (zoomLevel < stopFadeIn) {
            // The visibility should be linearly interpolated between 0 and 1 for values
            // between startFadeIn and stopFadeIn.
            return (zoomLevel - startFadeIn) / (fade * 2);
        }

        if (zoomLevel < startFadeOut) {
            return 1;
        }

        if (zoomLevel < stopFadeOut) {
            // The visibility should be linearly interpolated between 1 and 0 for values
            // between startFadeIn and stopFadeIn.
            return 1 - (zoomLevel - startFadeOut) / (fade * 2);
        }

        return 0;
    }

    private static final class PlaceholderProvider implements MapDataProvider {
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

        @Override
        public void onChange(Consumer<MapDataProvidedType> callback) {}

        @Override
        public void reloadData() {}
    }
}
