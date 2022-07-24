/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.features.overlays.sizes;

public class RestrictedRangeOverlaySize extends FixedOverlaySize {
    public RestrictedRangeOverlaySize() {
        super();
        this.maxWidth = -1;
        this.maxHeight = -1;
    }

    private final float maxWidth;
    private final float maxHeight;

    public RestrictedRangeOverlaySize(float width, float height, float maxWidth, float maxHeight) {
        super(width, height);

        this.width = Math.min(maxWidth, width);
        this.height = Math.min(maxHeight, height);

        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    @Override
    public void setWidth(float newWidth) {
        this.width = Math.min(newWidth, maxWidth);
    }

    @Override
    public void setHeight(float newHeight) {
        this.height = Math.min(newHeight, maxHeight);
    }
}
