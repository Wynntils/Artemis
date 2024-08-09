/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

// Fired when a message is sent to the local chat.
public class ClientsideMessageEvent extends Event implements ICancellableEvent {
    private final StyledText originalStyledText;

    private StyledText styledText;

    public ClientsideMessageEvent(Component component) {
        this.originalStyledText = StyledText.fromComponent(component);
        this.styledText = originalStyledText;
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
}
