/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.utils.scoreboard;

public interface ScoreboardHandler {
    void onSegmentChange(Segment newValue, ScoreboardManager.SegmentType segmentType);

    void onSegmentRemove(Segment segment, ScoreboardManager.SegmentType segmentType);
}
