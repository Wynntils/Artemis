/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Services;
import com.wynntils.services.mapdata.attributes.type.DerivedMapVisibility;
import com.wynntils.services.mapdata.attributes.type.FullMapVisibility;
import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.services.mapdata.type.MapFeature;
import java.util.Optional;
import java.util.function.Function;

public class FullFeatureAttributes extends DerivedAttributes {
    private final MapFeature feature;
    private final MapAttributes attributes;

    public FullFeatureAttributes(MapFeature feature) {
        this.feature = feature;
        this.attributes = feature.getAttributes();
    }

    @Override
    protected <T> T getAttribute(Function<MapAttributes, T> getter) {
        // Check if the feature has overridden this attribute
        if (attributes != null) {
            T attribute = getter.apply(attributes);
            if (attribute != null && !(attribute instanceof Integer i && i == 0)) {
                return attribute;
            }
        }

        // Otherwise get it from the category
        MapAttributes categoryAttributes = Services.MapData.getFullCategoryAttributes(feature.getCategoryId());
        if (categoryAttributes == null) return null;

        return getter.apply(categoryAttributes);
    }

    @Override
    public Optional<MapVisibility> getLabelVisibility() {
        return Optional.of(getVisibilityAttribute(MapAttributes::getLabelVisibility));
    }

    @Override
    public Optional<MapVisibility> getIconVisibility() {
        return Optional.of(getVisibilityAttribute(MapAttributes::getIconVisibility));
    }

    private <T extends MapVisibility> FullMapVisibility getVisibilityAttribute(
            Function<MapAttributes, Optional<T>> getter) {
        DerivedMapVisibility derivedFeatureVisibility = DerivedMapVisibility.of(MapVisibility.ALWAYS);

        // Check if the feature has overridden this attribute
        if (attributes != null) {
            Optional<T> attribute = getter.apply(attributes);

            if (attribute.isEmpty()) {
                // No attribute defined; this should not happen
                WynntilsMod.warn("No attribute defined for visibility when getting feature visibility: "
                        + feature.getFeatureId() + " (" + feature.getCategoryId() + ")");
                return MapVisibility.ALWAYS;
            }

            if (attribute.get() instanceof FullMapVisibility visibility) {
                // Feature defines its own visibility; return it
                return visibility;
            } else if (attribute.get() instanceof DerivedMapVisibility derivedVisibility) {
                // Feature defines a derived visibility; we'll need to apply it to the category visibility
                derivedFeatureVisibility = derivedVisibility;
            } else {
                WynntilsMod.warn("Unhandled visibility type #2: " + attribute.getClass());
                return MapVisibility.ALWAYS;
            }
        }

        // Otherwise get it from the category
        MapAttributes categoryAttributes = Services.MapData.getFullCategoryAttributes(feature.getCategoryId());
        if (categoryAttributes == null) {
            // No category attributes; return the default visibility with the derived feature visibility applied
            return MapVisibility.ALWAYS.applyDerived(derivedFeatureVisibility);
        }

        Optional<T> categoryVisibility = getter.apply(categoryAttributes);

        if (categoryVisibility.isEmpty()) {
            // No attribute defined; this should not happen
            WynntilsMod.warn(
                    "No attribute defined for visibility when getting category visibility: " + feature.getCategoryId());
            return MapVisibility.ALWAYS;
        }

        if (categoryVisibility.get() instanceof FullMapVisibility fullCategoryVisibility) {
            return fullCategoryVisibility.applyDerived(derivedFeatureVisibility);
        } else if (categoryVisibility.get() instanceof DerivedMapVisibility derivedCategoryVisibility) {
            return MapVisibility.ALWAYS.applyDerived(derivedCategoryVisibility).applyDerived(derivedFeatureVisibility);
        } else {
            WynntilsMod.warn("Unhandled visibility type #3: " + categoryVisibility.getClass());
            return MapVisibility.ALWAYS;
        }
    }
}
