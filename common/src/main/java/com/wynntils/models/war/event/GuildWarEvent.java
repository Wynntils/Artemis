/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.war.event;

import com.wynntils.models.war.type.WarBattleInfo;
import net.neoforged.bus.api.Event;

public abstract class GuildWarEvent extends Event {
    protected final WarBattleInfo warBattleInfo;

    protected GuildWarEvent(WarBattleInfo warBattleInfo) {
        this.warBattleInfo = warBattleInfo;
    }

    public WarBattleInfo getWarBattleInfo() {
        return warBattleInfo;
    }

    /**
     * Event fired when a guild war starts, when the tower starts attacking.
     */
    public static class Started extends GuildWarEvent {
        public Started(WarBattleInfo warBattleInfo) {
            super(warBattleInfo);
        }
    }

    /**
     * Event fired when a guild war ends, when the tower stops attacking.
     * Note that leaving/dying/capturing territories all call this event.
     */
    public static class Ended extends GuildWarEvent {
        public Ended(WarBattleInfo warBattleInfo) {
            super(warBattleInfo);
        }
    }
}
