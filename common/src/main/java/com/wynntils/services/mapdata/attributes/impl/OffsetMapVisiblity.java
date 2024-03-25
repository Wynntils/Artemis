/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.impl;

import com.wynntils.services.mapdata.attributes.type.MapVisibility;

public class OffsetMapVisiblity implements MapVisibility {
    private final int offset;
    private final int visibleZoomStep;

    public OffsetMapVisiblity(int offset, int visibleZoomStep) {
        this.offset = offset;
        this.visibleZoomStep = visibleZoomStep;
    }

    public float getVisibility(int zoomStep) {
        // visibleZoomStep - offset is the minimum zoom step for the feature to start to become visible
        // visibleZoomStep is the maximum zoom step for the feature to be visible
        // if the zoom step is between the two values, linearly interpolate the visibility
        if (zoomStep < visibleZoomStep - offset) {
            return 0f;
        } else if (zoomStep >= visibleZoomStep) {
            return 1f;
        } else if (offset == 0) {
            // zoomStep < visibleZoomStep, offset == 0, this is feature invisible
            return 0f;
        } else {
            return (float) (zoomStep - (visibleZoomStep - offset - 1)) / offset;
        }
    }
}
