/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.mod;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.mod.event.WynncraftConnectionEvent;
import com.wynntils.mc.event.ConnectionEvent.ConnectedEvent;
import com.wynntils.mc.event.ConnectionEvent.DisconnectedEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class ConnectionManager extends Manager {
    private static final Pattern WYNNCRAFT_SERVER_PATTERN =
            Pattern.compile("^(?:(.*)\\.)?wynncraft\\.(?:com|net|org)$");

    private boolean isConnected = false;

    public ConnectionManager() {
        super(List.of());
    }

    public boolean onServer() {
        return isConnected;
    }

    // ScreenOpenedEvent.Pre is used, because it is always posted
    @SubscribeEvent
    public void onScreenOpened(ScreenOpenedEvent.Pre e) {
        if (e.getScreen() instanceof DisconnectedScreen) {
            disconnect();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDisconnected(DisconnectedEvent e) {
        if (!isConnected) return;
        disconnect();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onConnected(ConnectedEvent e) {
        if (isConnected) {
            WynntilsMod.error("Got connected event while already connected to server: " + e);
            disconnect();
            // fall through to see if the new server is Wynncraft
        }

        String host = e.getHost().toLowerCase(Locale.ROOT);
        Matcher matcher = WYNNCRAFT_SERVER_PATTERN.matcher(host);
        if (matcher.matches()) {
            String hostName = matcher.group(1);
            hostName = hostName == null ? "play" : hostName.toLowerCase(Locale.ROOT);
            connect(hostName);
        }
    }

    private void connect(String hostName) {
        isConnected = true;
        WynntilsMod.postEvent(new WynncraftConnectionEvent.Connected(hostName));
    }

    private void disconnect() {
        isConnected = false;
        WynntilsMod.postEvent(new WynncraftConnectionEvent.Disconnected());
    }
}
