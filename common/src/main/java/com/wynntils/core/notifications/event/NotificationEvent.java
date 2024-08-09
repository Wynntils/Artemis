/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.notifications.event;

import com.wynntils.core.notifications.MessageContainer;
import net.neoforged.bus.api.Event;

public class NotificationEvent extends Event {
    private final MessageContainer messageContainer;

    private NotificationEvent(MessageContainer messageContainer) {
        this.messageContainer = messageContainer;
    }

    public MessageContainer getMessageContainer() {
        return messageContainer;
    }

    public static class Queue extends NotificationEvent {
        public Queue(MessageContainer messageContainer) {
            super(messageContainer);
        }
    }

    public static class Edit extends NotificationEvent {
        public Edit(MessageContainer messageContainer) {
            super(messageContainer);
        }
    }

    public static class Remove extends NotificationEvent {
        public Remove(MessageContainer messageContainer) {
            super(messageContainer);
        }
    }
}
