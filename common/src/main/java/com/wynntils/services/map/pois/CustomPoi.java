/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.map.pois;

import com.wynntils.services.map.type.DisplayPriority;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.Texture;
import java.util.Objects;

public class CustomPoi extends StaticIconPoi {
    private final String name;
    private final CustomColor color;
    private final Texture icon;
    private final Visibility visibility;

    public CustomPoi(PoiLocation location, String name, CustomColor color, Texture icon, Visibility visibility) {
        super(location);

        this.name = name;
        this.color = color;
        this.icon = icon;
        this.visibility = visibility;
    }

    @Override
    public Texture getIcon() {
        return icon;
    }

    @Override
    public float getMinZoomForRender() {
        return switch (getVisibility()) {
            case ALWAYS -> -1;
            case HIDDEN -> Integer.MAX_VALUE;
            case DEFAULT -> 0.28f; // This is unused now
        };
    }

    @Override
    public String getName() {
        return name;
    }

    public CustomColor getColor() {
        return color;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public CustomColor getIconColor() {
        return this.getColor();
    }

    @Override
    public DisplayPriority getDisplayPriority() {
        return DisplayPriority.LOW;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        CustomPoi customPoi = (CustomPoi) other;
        return location.equals(customPoi.location)
                && visibility == customPoi.visibility
                && name.equals(customPoi.name)
                && color.equals(customPoi.color)
                && icon == customPoi.icon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, name, color, icon, visibility);
    }

    public enum Visibility {
        DEFAULT("screens.wynntils.poiCreation.visibility.default"),
        ALWAYS("screens.wynntils.poiCreation.visibility.alwaysVisible"),
        HIDDEN("screens.wynntils.poiCreation.visibility.hidden");

        private final String translationKey;

        Visibility(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }
}
