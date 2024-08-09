/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.mod.event;

import net.neoforged.bus.api.Event;

public abstract class WynncraftConnectionEvent extends Event {
    public static final class Connected extends WynncraftConnectionEvent {
        private final String host;

        public Connected(String host) {
            this.host = host;
        }

        public String getHost() {
            return host;
        }
    }

    public static final class Disconnected extends WynncraftConnectionEvent {}
}
