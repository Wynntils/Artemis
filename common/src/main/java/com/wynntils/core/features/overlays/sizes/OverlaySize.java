/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.features.overlays.sizes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class OverlaySize {
    protected static final Pattern SIZE_REGEX = Pattern.compile("OverlaySize\\{width=(.+),height=(.+)}");

    protected float width;
    protected float height;

    // For GSON
    public OverlaySize() {}

    public OverlaySize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public OverlaySize(String string) {
        Matcher matcher = SIZE_REGEX.matcher(string.replaceAll(" ", ""));

        if (!matcher.matches()) {
            throw new RuntimeException("Failed to parse OverlaySize");
        }

        try {
            this.width = Float.parseFloat(matcher.group(1));
            this.height = Float.parseFloat(matcher.group(2));
        } catch (NumberFormatException exception) {
            throw new RuntimeException("Failed to parse OverlaySize");
        }
    }

    public abstract float getWidth();

    public abstract float getHeight();

    public abstract float getRenderedWidth();

    public abstract float getRenderedHeight();

    public void setWidth(float newWidth) {
        this.width = newWidth;
    }

    public void setHeight(float newHeight) {
        this.height = newHeight;
    }

    @Override
    public String toString() {
        return "OverlaySize{" + "width=" + width + ", height=" + height + '}';
    }
}
