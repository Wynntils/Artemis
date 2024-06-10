/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.territories.event;

import net.minecraftforge.eventbus.api.Event;

/**
 * This event is fired when new territory data is received from the API or the advancements.
 */
public abstract class TerritoriesUpdatedEvent extends Event {
    public static class Api extends TerritoriesUpdatedEvent {}

    public static class Advancements extends TerritoriesUpdatedEvent {}
}
