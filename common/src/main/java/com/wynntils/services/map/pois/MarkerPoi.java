/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.map.pois;

import com.wynntils.services.map.type.DisplayPriority;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.type.AbstractTexture;

public class MarkerPoi extends StaticIconPoi {
    private final String name;
    private final AbstractTexture texture;

    public MarkerPoi(PoiLocation location, String name, AbstractTexture texture) {
        super(location);
        this.name = name;
        this.texture = texture;
    }

    @Override
    public AbstractTexture getIcon() {
        return texture;
    }

    @Override
    protected float getMinZoomForRender() {
        return -1f;
    }

    @Override
    public DisplayPriority getDisplayPriority() {
        return DisplayPriority.NORMAL;
    }

    @Override
    public String getName() {
        return name;
    }
}
