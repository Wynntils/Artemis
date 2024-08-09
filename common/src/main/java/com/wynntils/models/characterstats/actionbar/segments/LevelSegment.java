/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.segments;

import com.wynntils.handlers.actionbar.ActionBarSegment;

public class LevelSegment extends ActionBarSegment {
    private final int level;

    public LevelSegment(String segmentText, int level) {
        super(segmentText);
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return "LevelSegment{" + "level=" + level + ", segmentText='" + segmentText + '\'' + '}';
    }
}
