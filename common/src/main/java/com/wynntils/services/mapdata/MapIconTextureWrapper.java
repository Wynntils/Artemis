/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.utils.render.type.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

public class MapIconTextureWrapper implements AbstractTexture {
    private final MapIcon mapIcon;

    public MapIconTextureWrapper(MapIcon mapIcon) {
        this.mapIcon = mapIcon;
    }

    @Override
    public ResourceLocation resource() {
        return mapIcon.getResourceLocation();
    }

    @Override
    public int width() {
        return mapIcon.getWidth();
    }

    @Override
    public int height() {
        return mapIcon.getHeight();
    }
}
