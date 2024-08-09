/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.segments;

import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.models.characterstats.type.PowderSpecialInfo;

public class PowderSpecialSegment extends ActionBarSegment {
    private final PowderSpecialInfo powderSpecialInfo;

    public PowderSpecialSegment(String segmentText, PowderSpecialInfo powderSpecialInfo) {
        super(segmentText);
        this.powderSpecialInfo = powderSpecialInfo;
    }

    public PowderSpecialInfo getPowderSpecialInfo() {
        return powderSpecialInfo;
    }

    @Override
    public String toString() {
        return "PowderSpecialSegment{" + "powderSpecialInfo="
                + powderSpecialInfo + ", segmentText='"
                + segmentText + '\'' + '}';
    }
}
