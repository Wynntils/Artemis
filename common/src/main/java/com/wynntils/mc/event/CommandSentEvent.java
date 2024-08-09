/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class CommandSentEvent extends Event implements ICancellableEvent {
    private final String command;
    private final boolean signed;

    public CommandSentEvent(String command, boolean signed) {
        this.command = command;
        this.signed = signed;
    }

    public String getCommand() {
        return command;
    }

    public boolean isSigned() {
        return signed;
    }
}
