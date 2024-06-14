/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes;

import com.wynntils.services.mapdata.attributes.type.MapAreaAttributes;
import com.wynntils.services.mapdata.attributes.type.MapDecoration;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.attributes.type.MapLocationAttributes;
import com.wynntils.services.mapdata.attributes.type.MapPathAttributes;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;
import java.util.Optional;

/**
 * These are the fallback attributes used by FullFeatureAttribute if no other attributes
 * are defined. These are guaranteed to be non-empty.
 */
public final class DefaultMapAttributes {
    public static final MapLocationAttributes LOCATION = new MapLocationAttributes() {
        @Override
        public Optional<String> getLabel() {
            return Optional.of("");
        }

        @Override
        public Optional<String> getIconId() {
            return Optional.of(MapIcon.NO_ICON_ID);
        }

        @Override
        public Optional<Integer> getPriority() {
            return Optional.of(500);
        }

        @Override
        public Optional<Integer> getLevel() {
            return Optional.of(0);
        }

        @Override
        public Optional<MapVisibility> getLabelVisibility() {
            return Optional.of(FixedMapVisibility.LABEL_ALWAYS);
        }

        @Override
        public Optional<CustomColor> getLabelColor() {
            return Optional.of(CommonColors.WHITE);
        }

        @Override
        public Optional<TextShadow> getLabelShadow() {
            return Optional.of(TextShadow.OUTLINE);
        }

        @Override
        public Optional<MapVisibility> getIconVisibility() {
            return Optional.of(FixedMapVisibility.ICON_ALWAYS);
        }

        @Override
        public Optional<CustomColor> getIconColor() {
            return Optional.of(CommonColors.WHITE);
        }

        @Override
        public Optional<MapDecoration> getIconDecoration() {
            return Optional.of(MapDecoration.NONE);
        }
    };

    public static final MapAreaAttributes AREA = new MapAreaAttributes() {
        @Override
        public Optional<String> getLabel() {
            return Optional.of("");
        }

        @Override
        public Optional<Integer> getPriority() {
            return Optional.of(500);
        }

        @Override
        public Optional<Integer> getLevel() {
            return Optional.of(0);
        }
    };

    public static final MapPathAttributes PATH = new MapPathAttributes() {
        @Override
        public Optional<String> getLabel() {
            return Optional.of("");
        }

        @Override
        public Optional<Integer> getPriority() {
            return Optional.of(500);
        }

        @Override
        public Optional<Integer> getLevel() {
            return Optional.of(0);
        }
    };
}
