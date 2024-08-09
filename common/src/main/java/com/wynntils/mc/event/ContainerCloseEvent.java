/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ContainerCloseEvent extends Event {
    public static class Pre extends ContainerCloseEvent implements ICancellableEvent {}

    public static class Post extends ContainerCloseEvent {}
}
