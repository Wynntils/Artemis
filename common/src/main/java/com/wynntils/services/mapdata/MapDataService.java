/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.wynntils.core.components.Service;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapAttributes;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapVisibility;
import com.wynntils.services.mapdata.providers.MapDataProvider;
import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.services.mapdata.type.MapFeature;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class MapDataService extends Service {
    // FIXME: i18n
    private static final String NAMELESS_CATEGORY = "Category '%s'";

    private final MapDataProviders providers = new MapDataProviders();

    private final Map<MapFeature, ResolvedMapAttributes> resolvedAttributesCache = new HashMap<>();

    public MapDataService() {
        super(List.of());
    }

    public Stream<MapFeature> getFeatures() {
        return providers.getProviders().flatMap(MapDataProvider::getFeatures);
    }

    public Stream<Poi> getFeaturesAsPois() {
        return getFeatures().map(feature -> new MapFeaturePoiWrapper(feature, resolveMapAttributes(feature)));
    }

    // region Lookup and extend data from providers

    public ResolvedMapAttributes resolveMapAttributes(MapFeature feature) {
        return resolvedAttributesCache.computeIfAbsent(feature, k -> MapAttributesResolver.resolve(feature));
    }

    public Stream<MapCategory> getCategoryDefinitions(String categoryId) {
        return providers.getProviders().flatMap(MapDataProvider::getCategories).filter(p -> p.getCategoryId()
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

        Stream<MapIcon> allIcons = providers.getProviders().flatMap(MapDataProvider::getIcons);
        return allIcons.filter(i -> i.getIconId().equals(iconId)).findFirst();
    }

    // endregion

    /** This method requires a MapVisibility with all values non-empty to work correctly. */
    public float calculateVisibility(ResolvedMapVisibility mapVisibility, float zoomLevel) {
        float min = mapVisibility.min();
        float max = mapVisibility.max();
        float fade = mapVisibility.fade();

        float startFadeIn = min - fade;
        float stopFadeIn = min + fade;
        float startFadeOut = max - fade;
        float stopFadeOut = max + fade;

        // If min or max is at the extremes, do not apply fading
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
}
