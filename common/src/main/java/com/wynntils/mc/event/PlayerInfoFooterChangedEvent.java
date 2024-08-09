/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.wynntils.core.text.StyledText;
import net.neoforged.bus.api.Event;

/** Fires on change to footer of scoreboard */
public class PlayerInfoFooterChangedEvent extends Event {
    private final StyledText footer;

    public StyledText getFooter() {
        return footer;
    }

    public PlayerInfoFooterChangedEvent(StyledText footer) {
        this.footer = footer;
    }
}
