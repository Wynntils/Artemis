/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.war.event;

import net.neoforged.bus.api.Event;

public abstract class GuildWarTowerEffectEvent extends Event {
    public static class AuraSpawned extends GuildWarTowerEffectEvent {}

    public static class VolleySpawned extends GuildWarTowerEffectEvent {}
}
