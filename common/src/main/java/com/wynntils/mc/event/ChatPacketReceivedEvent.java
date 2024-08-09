/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ChatPacketReceivedEvent extends Event implements ICancellableEvent {
    private Component message;
    private boolean messageChanged;

    protected ChatPacketReceivedEvent(Component message) {
        this.message = message;
    }

    public Component getMessage() {
        return message;
    }

    public void setMessage(Component message) {
        this.message = message;
        this.messageChanged = true;
    }

    public boolean isMessageChanged() {
        return messageChanged;
    }

    public static final class GameInfo extends ChatPacketReceivedEvent {
        public GameInfo(Component message) {
            super(message);
        }
    }

    public static final class System extends ChatPacketReceivedEvent {
        public System(Component message) {
            super(message);
        }
    }

    public static final class Player extends ChatPacketReceivedEvent {
        public Player(Component message) {
            super(message);
        }
    }
}
