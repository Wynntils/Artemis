/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.quests;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.handlers.scoreboard.ScoreboardPart;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import com.wynntils.handlers.scoreboard.SegmentMatcher;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.wynn.WynnUtils;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;

public class QuestScoreboardPart implements ScoreboardPart {
    static final SegmentMatcher QUEST_MATCHER = SegmentMatcher.fromPattern("Tracked Quest:");

    @Override
    public Set<SegmentMatcher> getSegmentMatchers() {
        return Set.of(QUEST_MATCHER);
    }

    @Override
    public void onSegmentChange(ScoreboardSegment newValue, SegmentMatcher segmentMatcher) {
        List<String> content = newValue.getContent();

        if (content.isEmpty()) {
            WynntilsMod.error("QuestHandler: content was empty.");
        }

        StringBuilder questName = new StringBuilder();
        StringBuilder nextTask = new StringBuilder();

        for (String line : content) {
            if (line.startsWith("§e")) {
                questName.append(ComponentUtils.stripFormatting(line)).append(" ");
            } else {
                nextTask.append(line.replaceAll(ChatFormatting.WHITE.toString(), ChatFormatting.AQUA.toString())
                                .replaceAll(ChatFormatting.GRAY.toString(), ChatFormatting.RESET.toString()))
                        .append(" ");
            }
        }

        String fixedName = WynnUtils.normalizeBadString(questName.toString().trim());
        String fixedNextTask = WynnUtils.normalizeBadString(nextTask.toString().trim());
        Managers.Quest.updateTrackedQuestFromScoreboard(fixedName, fixedNextTask);
    }

    @Override
    public void onSegmentRemove(ScoreboardSegment segment, SegmentMatcher segmentMatcher) {
        Managers.Quest.clearTrackedQuestFromScoreBoard();
    }

    @Override
    public void reset() {
        Managers.Quest.clearTrackedQuestFromScoreBoard();
    }
}
