/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.actionbar;

public abstract class ActionBarSegment {
    protected final String segmentText;

    protected ActionBarSegment(String segmentText) {
        this.segmentText = segmentText;
    }

    public String getSegmentText() {
        return segmentText;
    }
}
