/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.wynntils.core.events.EventThread;
import net.minecraftforge.eventbus.api.Event;

/** Fired on connection to a server */
public abstract class ConnectionEvent extends Event {
    @EventThread(EventThread.Type.RENDER)
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

    @EventThread(EventThread.Type.RENDER)
    public static class DisconnectedEvent extends ConnectionEvent {}

    @EventThread(EventThread.Type.RENDER)
    public static class UnexpectedDisconnectedEvent extends DisconnectedEvent {}
}
