/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.segments;

import com.wynntils.handlers.actionbar.ActionBarSegment;

public abstract class AbstractMeterSegment extends ActionBarSegment {
    protected AbstractMeterSegment(String segmentText) {
        super(segmentText);
    }
}
