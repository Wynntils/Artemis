/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.wynntils.core.components.Services;
import com.wynntils.services.mapdata.attributes.DefaultMapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapAttributes;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapVisibility;
import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.services.mapdata.type.MapFeature;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This will create a special type of MapAttributes that are a record with fixed values,
 * which are guarenteed to exist. It does this by extending the lookup for the
 * attribute first to the category hierarchy for the given feature, and
 * finally by going to the default value for each attribute.
 */
public final class MapAttributesResolver {
    private final MapFeature feature;

    private MapAttributesResolver(MapFeature feature) {
        this.feature = feature;
    }

    public static ResolvedMapAttributes resolve(MapFeature feature) {
        MapAttributesResolver resolver = new MapAttributesResolver(feature);

        return new ResolvedMapAttributes(
                resolver.getAttribute(MapAttributes::getLabel),
                resolver.getAttribute(MapAttributes::getIconId),
                resolver.getAttribute(MapAttributes::getPriority),
                resolver.getAttribute(MapAttributes::getLevel),
                resolver.getResolvedMapVisibility(MapAttributes::getLabelVisibility),
                resolver.getAttribute(MapAttributes::getLabelColor),
                resolver.getAttribute(MapAttributes::getLabelShadow),
                resolver.getResolvedMapVisibility(MapAttributes::getIconVisibility),
                resolver.getAttribute(MapAttributes::getIconColor),
                resolver.getAttribute(MapAttributes::getIconDecoration));
    }

    private <T> T getAttribute(Function<MapAttributes, Optional<T>> attributeGetter) {
        // Check if the feature has overridden this attribute
        Optional<T> featureAttribute = getFromFeature(attributeGetter);
        if (featureAttribute.isPresent()) {
            return featureAttribute.get();
        }

        // Then try to get it from the category
        for (String id = getCategoryId(); id != null; id = getParentCategoryId(id)) {
            Stream<T> attributes = getAttributesForCategoryId(attributeGetter, id);

            Optional<T> attribute = attributes.findFirst();
            if (attribute.isPresent()) {
                return attribute.get();
            }
        }

        // Otherwise return the fallback default value
        return attributeGetter.apply(DefaultMapAttributes.INSTANCE).get();
    }

    private ResolvedMapVisibility getResolvedMapVisibility(
            Function<MapAttributes, Optional<MapVisibility>> attributeGetter) {
        return new ResolvedMapVisibility(
                getVisibilityValue(MapVisibility::getMin, attributeGetter),
                getVisibilityValue(MapVisibility::getMax, attributeGetter),
                getVisibilityValue(MapVisibility::getFade, attributeGetter));
    }

    private float getVisibilityValue(
            Function<MapVisibility, Optional<Float>> valueGetter,
            Function<MapAttributes, Optional<MapVisibility>> attributeGetter) {
        // Check if the feature has overridden this attribute
        Optional<MapVisibility> featureVisibility = getFromFeature(attributeGetter);
        if (featureVisibility.isPresent()) {
            // We got the attribute, but do we got the value?
            Optional<Float> featureValue = valueGetter.apply(featureVisibility.get());
            if (featureValue.isPresent()) {
                return featureValue.get();
            }
        }

        // Otherwise try to get it from the category
        for (String id = getCategoryId(); id != null; id = getParentCategoryId(id)) {
            Stream<MapVisibility> attributes = getAttributesForCategoryId(attributeGetter, id);

            // Then check each visibility in turn for the value we're looking for
            Optional<Float> attribute = attributes
                    .map(valueGetter)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            if (attribute.isPresent()) {
                return attribute.get();
            }
        }

        // Otherwise return the fallback default value
        return valueGetter
                .apply(attributeGetter.apply(DefaultMapAttributes.INSTANCE).get())
                .get();
    }

    private String getCategoryId() {
        return feature.getCategoryId();
    }

    private <T> Optional<T> getFromFeature(Function<MapAttributes, Optional<T>> attributeGetter) {
        Optional<MapAttributes> attributes = feature.getAttributes();
        if (attributes.isEmpty()) return Optional.empty();

        return attributeGetter.apply(attributes.get());
    }

    private <T> Stream<T> getAttributesForCategoryId(
            Function<MapAttributes, Optional<T>> attributeGetter, String categoryId) {
        // Find all provided MapAttributes for this category level
        Stream<MapAttributes> allAttributes = Services.MapData.getCategoryDefinitions(categoryId)
                .map(MapCategory::getAttributes)
                .filter(Optional::isPresent)
                .map(Optional::get);

        // Mulitple providers might provide MapAttributes to the same category, but not
        // all of them might provide the attribute we're actually looking for, so
        // check all (in the arbitrary order that Services.MapData gave them to us).
        return allAttributes.map(attributeGetter).filter(Optional::isPresent).map(Optional::get);
    }

    private String getParentCategoryId(String categoryId) {
        int index = categoryId.lastIndexOf(':');
        if (index == -1) return null;
        return categoryId.substring(0, index);
    }
}
