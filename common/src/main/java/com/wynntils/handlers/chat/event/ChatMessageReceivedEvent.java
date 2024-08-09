/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.chat.event;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.type.MessageType;
import com.wynntils.handlers.chat.type.RecipientType;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class ChatMessageReceivedEvent extends Event implements ICancellableEvent {
    // These are used to keep the original message so different features don't have to fight over it.
    private final StyledText originalStyledText;

    private StyledText styledText;

    private final MessageType messageType;
    private final RecipientType recipientType;

    public ChatMessageReceivedEvent(StyledText styledText, MessageType messageType, RecipientType recipientType) {
        this.originalStyledText = styledText;
        this.styledText = styledText;
        this.messageType = messageType;
        this.recipientType = recipientType;
    }

    public void setMessage(StyledText styledText) {
        this.styledText = styledText;
    }

    public StyledText getOriginalStyledText() {
        return originalStyledText;
    }

    public StyledText getStyledText() {
        return styledText;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public RecipientType getRecipientType() {
        return recipientType;
    }
}
