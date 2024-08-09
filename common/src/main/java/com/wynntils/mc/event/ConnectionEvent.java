/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.neoforged.bus.api.Event;

/** Fired on connection to a server */
public abstract class ConnectionEvent extends Event {
    public static class ConnectedEvent extends ConnectionEvent {
        private final String host;
        private final int port;

        public ConnectedEvent(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return "ConnectedEvent{" + "host='" + host + '\'' + ", port=" + port + '}';
        }
    }

    public static class DisconnectedEvent extends ConnectionEvent {}
}
