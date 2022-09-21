/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.event;

import com.wynntils.wynn.model.scoreboard.quests.QuestInfo;
import net.minecraftforge.eventbus.api.Event;

public class TrackedQuestUpdateEvent extends Event {
    private final QuestInfo questInfo;

    public TrackedQuestUpdateEvent(QuestInfo questInfo) {
        this.questInfo = questInfo;
    }

    public QuestInfo getQuestInfo() {
        return questInfo;
    }
}
