/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.wynntils.core.components.Services;
import com.wynntils.services.mapdata.attributes.DefaultMapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapAreaAttributes;
import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapLocationAttributes;
import com.wynntils.services.mapdata.attributes.type.MapPathAttributes;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapAreaAttributes;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapLocationAttributes;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapPathAttributes;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapVisibility;
import com.wynntils.services.mapdata.type.MapArea;
import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.services.mapdata.type.MapFeature;
import com.wynntils.services.mapdata.type.MapLocation;
import com.wynntils.services.mapdata.type.MapPath;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This class will create a special type of MapAttributes that are a record with fixed values,
 * which are guaranteed to exist. It does this by extending the lookup for the
 * attribute first to the category hierarchy for the given feature, and
 * finally by going to the default value for each attribute.
 */
public class MapAttributesResolver<F extends MapFeature, A extends MapAttributes> {
    private final F feature;
    private final A defaultAttributes;
    private final Function<F, Optional<A>> featureAttributesGetter;
    private final Function<MapCategory, Optional<A>> categoryAttributesGetter;

    public MapAttributesResolver(
            F feature,
            A defaultAttributes,
            Function<F, Optional<A>> featureAttributesGetter,
            Function<MapCategory, Optional<A>> categoryAttributesGetter) {
        this.feature = feature;
        this.defaultAttributes = defaultAttributes;
        this.featureAttributesGetter = featureAttributesGetter;
        this.categoryAttributesGetter = categoryAttributesGetter;
    }

    public static ResolvedMapLocationAttributes resolve(MapLocation location) {
        MapAttributesResolver<MapLocation, MapLocationAttributes> resolver = new MapAttributesResolver<>(
                location,
                DefaultMapAttributes.LOCATION,
                MapLocation::getAttributes,
                MapCategory::getLocationAttributes);

        return new ResolvedMapLocationAttributes(
                resolver.getAttribute(MapLocationAttributes::getLabel),
                resolver.getAttribute(MapLocationAttributes::getIconId),
                resolver.getAttribute(MapLocationAttributes::getPriority),
                resolver.getAttribute(MapLocationAttributes::getLevel),
                resolver.getResolvedMapVisibility(MapLocationAttributes::getLabelVisibility),
                resolver.getAttribute(MapLocationAttributes::getLabelColor),
                resolver.getAttribute(MapLocationAttributes::getLabelShadow),
                resolver.getResolvedMapVisibility(MapLocationAttributes::getIconVisibility),
                resolver.getAttribute(MapLocationAttributes::getIconColor),
                resolver.getAttribute(MapLocationAttributes::getIconDecoration));
    }

    public static ResolvedMapAreaAttributes resolve(MapArea area) {
        MapAttributesResolver<MapArea, MapAreaAttributes> resolver = new MapAttributesResolver<>(
                area, DefaultMapAttributes.AREA, MapArea::getAttributes, MapCategory::getAreaAttributes);

        return new ResolvedMapAreaAttributes(
                resolver.getAttribute(MapAreaAttributes::getLabel),
                resolver.getAttribute(MapAreaAttributes::getPriority),
                resolver.getAttribute(MapAreaAttributes::getLevel));
    }

    public static ResolvedMapPathAttributes resolve(MapPath path) {
        MapAttributesResolver<MapPath, MapPathAttributes> resolver = new MapAttributesResolver<>(
                path, DefaultMapAttributes.PATH, MapPath::getAttributes, MapCategory::getPathAttributes);

        return new ResolvedMapPathAttributes(
                resolver.getAttribute(MapPathAttributes::getLabel),
                resolver.getAttribute(MapPathAttributes::getPriority),
                resolver.getAttribute(MapPathAttributes::getLevel));
    }

    private <T> T getAttribute(Function<A, Optional<T>> attributeGetter) {
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
        return attributeGetter.apply(defaultAttributes).get();
    }

    private ResolvedMapVisibility getResolvedMapVisibility(Function<A, Optional<MapVisibility>> attributeGetter) {
        return new ResolvedMapVisibility(
                getVisibilityValue(MapVisibility::getMin, attributeGetter),
                getVisibilityValue(MapVisibility::getMax, attributeGetter),
                getVisibilityValue(MapVisibility::getFade, attributeGetter));
    }

    private float getVisibilityValue(
            Function<MapVisibility, Optional<Float>> valueGetter,
            Function<A, Optional<MapVisibility>> attributeGetter) {
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
        return valueGetter.apply(attributeGetter.apply(defaultAttributes).get()).get();
    }

    private String getCategoryId() {
        return feature.getCategoryId();
    }

    private <T> Optional<T> getFromFeature(Function<A, Optional<T>> attributeGetter) {
        Optional<A> attributes = featureAttributesGetter.apply(feature);
        if (attributes.isEmpty()) return Optional.empty();

        return attributeGetter.apply(attributes.get());
    }

    private <T> Stream<T> getAttributesForCategoryId(Function<A, Optional<T>> attributeGetter, String categoryId) {
        // Find all provided MapAttributes for this category level
        Stream<A> allAttributes = Services.MapData.getCategoryDefinitions(categoryId)
                .map(categoryAttributesGetter)
                .filter(Optional::isPresent)
                .map(Optional::get);

        // Multiple providers might provide MapAttributes to the same category, but not
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
