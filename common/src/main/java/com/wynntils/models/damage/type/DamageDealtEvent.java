/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.damage.type;

import com.wynntils.models.stats.type.DamageType;
import java.util.Map;
import net.neoforged.bus.api.Event;

/**
 * This event is sent out whenever Wynntils register that a mob has received damage.
 * We cannot tell which mob it is, but we know the damage is caused by melee or
 * spell attack from the current player.
 */
public final class DamageDealtEvent extends Event {
    private final Map<DamageType, Long> damages;

    public DamageDealtEvent(Map<DamageType, Long> damages) {
        this.damages = damages;
    }

    public Map<DamageType, Long> getDamages() {
        return damages;
    }
}
