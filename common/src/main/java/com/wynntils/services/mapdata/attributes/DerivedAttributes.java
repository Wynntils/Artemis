/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes;

import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapDecoration;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.type.RangedValue;
import java.util.function.Function;

public abstract class DerivedAttributes implements MapAttributes {
    protected abstract <T> T getAttribute(Function<MapAttributes, T> getter);

    @Override
    public String getLabel() {
        return getAttribute(MapAttributes::getLabel);
    }

    @Override
    public String getIconId() {
        return getAttribute(MapAttributes::getIconId);
    }

    @Override
    public int getPriority() {
        Integer integer = getAttribute(MapAttributes::getPriority);
        return integer == null ? 0 : integer;
    }

    @Override
    public RangedValue getLevelRange() {
        return getAttribute(MapAttributes::getLevelRange);
    }

    @Override
    public MapVisibility getLabelVisibility() {
        return getAttribute(MapAttributes::getLabelVisibility);
    }

    @Override
    public CustomColor getLabelColor() {
        return getAttribute(MapAttributes::getLabelColor);
    }

    @Override
    public TextShadow getLabelShadow() {
        return getAttribute(MapAttributes::getLabelShadow);
    }

    @Override
    public MapVisibility getIconVisibility() {
        return getAttribute(MapAttributes::getIconVisibility);
    }

    @Override
    public CustomColor getIconColor() {
        return getAttribute(MapAttributes::getIconColor);
    }

    @Override
    public MapDecoration getIconDecoration() {
        return getAttribute(MapAttributes::getIconDecoration);
    }
}
