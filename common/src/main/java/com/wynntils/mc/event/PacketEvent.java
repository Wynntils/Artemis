/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.wynntils.core.events.EventThread;
import net.minecraft.network.protocol.Packet;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fires on packet sending or receiving
 *
 * <p>Please do not misuse this class by looking for specific packet classes; instead create a
 * unique Event class in mc.event and add a mixin for the handler in ClientPacketListenerMixin.
 */
public abstract class PacketEvent<T extends Packet<?>> extends Event {
    private final T packet;

    protected PacketEvent(T packet) {
        this.packet = packet;
    }

    public T getPacket() {
        return packet;
    }

    @EventThread(EventThread.Type.ANY)
    public static class PacketSentEvent<T extends Packet<?>> extends PacketEvent<T> implements ICancellableEvent {
        public PacketSentEvent(T packet) {
            super(packet);
        }
    }

    @EventThread(EventThread.Type.ANY)
    public static class PacketReceivedEvent<T extends Packet<?>> extends PacketEvent<T> implements ICancellableEvent {
        public PacketReceivedEvent(T packet) {
            super(packet);
        }
    }
}
