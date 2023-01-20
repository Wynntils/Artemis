/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.territories;

import com.wynntils.core.components.Models;
import com.wynntils.handlers.scoreboard.ScoreboardPart;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import com.wynntils.handlers.scoreboard.SegmentMatcher;
import java.util.Set;

public class GuildAttackScoreboardPart implements ScoreboardPart {
    static final SegmentMatcher GUILD_ATTACK_MATCHER = SegmentMatcher.fromPattern("Upcoming Attacks:");

    @Override
    public Set<SegmentMatcher> getSegmentMatchers() {
        return Set.of(GUILD_ATTACK_MATCHER);
    }

    @Override
    public void onSegmentChange(ScoreboardSegment newValue, SegmentMatcher segmentMatcher) {
        Models.GuildAttackTimer.processChanges(newValue);
    }

    @Override
    public void onSegmentRemove(ScoreboardSegment segment, SegmentMatcher segmentMatcher) {
        Models.GuildAttackTimer.resetTimers();
    }

    @Override
    public void reset() {
        Models.GuildAttackTimer.resetTimers();
    }
}
