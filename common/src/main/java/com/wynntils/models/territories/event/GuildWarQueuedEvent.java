/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.territories.event;

import com.wynntils.models.territories.TerritoryAttackTimer;
import net.neoforged.bus.api.Event;

public class GuildWarQueuedEvent extends Event {
    private final TerritoryAttackTimer attackTimer;

    public GuildWarQueuedEvent(TerritoryAttackTimer attackTimer) {
        this.attackTimer = attackTimer;
    }

    public TerritoryAttackTimer getAttackTimer() {
        return attackTimer;
    }
}
