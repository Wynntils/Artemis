/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.scoreboard.event;

import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class ScoreboardSegmentAdditionEvent extends Event implements ICancellableEvent {
    private final ScoreboardSegment segment;

    public ScoreboardSegmentAdditionEvent(ScoreboardSegment segment) {
        this.segment = segment;
    }

    public ScoreboardSegment getSegment() {
        return segment;
    }
}
