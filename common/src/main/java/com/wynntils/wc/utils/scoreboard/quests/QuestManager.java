/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.utils.scoreboard.quests;

import com.wynntils.core.WynntilsMod;
import com.wynntils.wc.utils.scoreboard.ScoreboardHandler;
import com.wynntils.wc.utils.scoreboard.ScoreboardManager;
import com.wynntils.wc.utils.scoreboard.Segment;
import java.util.List;
import net.minecraft.ChatFormatting;

public class QuestManager implements ScoreboardHandler {
    private static QuestInfo currentQuest = null;

    public static QuestInfo getCurrentQuest() {
        return currentQuest;
    }

    @Override
    public void onSegmentChange(Segment newValue, ScoreboardManager.SegmentType segmentType) {
        List<String> content = newValue.getContent();

        if (content.isEmpty()) {
            WynntilsMod.error("QuestManager: content was empty.");
        }

        StringBuilder questName = new StringBuilder();
        StringBuilder description = new StringBuilder();

        for (String line : content) {
            if (line.startsWith("§e")) {
                questName.append(ChatFormatting.stripFormatting(line)).append(" ");
            } else {
                description.append(ChatFormatting.stripFormatting(line)).append(" ");
            }
        }

        currentQuest = new QuestInfo(
                questName.toString().trim(), description.toString().trim());
    }

    @Override
    public void onSegmentRemove(Segment segment, ScoreboardManager.SegmentType segmentType) {
        resetCurrentQuest();
    }

    public static void resetCurrentQuest() {
        currentQuest = null;
    }
}
