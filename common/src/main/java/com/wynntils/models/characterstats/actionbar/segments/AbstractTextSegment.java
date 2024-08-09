/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.segments;

import com.wynntils.handlers.actionbar.ActionBarSegment;

/**
 * A common base class for health and mana text segments in the action bar.
 */
public abstract class AbstractTextSegment extends ActionBarSegment {
    protected AbstractTextSegment(String segmentText) {
        super(segmentText);
    }
}
