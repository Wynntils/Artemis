/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.model;

import com.wynntils.core.WynntilsMod;
import com.wynntils.mc.event.ConnectionEvent.ConnectedEvent;
import com.wynntils.mc.event.ConnectionEvent.DisconnectedEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerDisplayNameChangeEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerLogOutEvent;
import com.wynntils.mc.event.PlayerInfoFooterChangedEvent;
import com.wynntils.mc.event.PlayerTeleportEvent;
import com.wynntils.mc.event.ResourcePackEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.wc.Model;
import com.wynntils.wc.event.WorldStateEvent;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WorldState implements Model {
    private static final Pattern WORLD_NAME = Pattern.compile("^§f  §lGlobal \\[(.*)\\]$");
    private static final Pattern HUB_NAME = Pattern.compile("^\n§6§l play.wynncraft.com \n$");
    private static final Position CHARACTER_SELECTION_POSITION = new Vec3(-1337.5, 16.2, -1120.5);
    private static final String WYNNCRAFT_SERVER_SUFFIX = ".wynncraft.com";
    private static final String WYNNCRAFT_BETA_PREFIX = "beta.";

    private String currentTabListFooter = "";
    private String currentWorldName = "";
    private UUID currentWorldId = null;
    private boolean onBetaServer;

    private State currentState = State.NOT_CONNECTED;

    public boolean onServer() {
        return currentState != State.NOT_CONNECTED;
    }

    public boolean onWorld() {
        return currentState == State.WORLD;
    }

    public boolean isInStream() {
        return currentWorldName.equals("-");
    }

    public boolean isOnBetaServer() {
        return onBetaServer;
    }

    public State getCurrentState() {
        return currentState;
    }

    private void setState(State newState, String newWorldName, UUID newUuid) {
        if (newState == currentState && newWorldName.equals(currentWorldName)) return;

        State oldState = currentState;
        // Switch state before sending event
        currentState = newState;
        currentWorldName = newWorldName;
        currentWorldId = newUuid;
        WynntilsMod.getEventBus().post(new WorldStateEvent(newState, oldState, newWorldName));
    }

    @SubscribeEvent
    public void screenOpened(ScreenOpenedEvent e) {
        if (!onServer()) return;

        if (e.getScreen() instanceof DisconnectedScreen) {
            setState(State.NOT_CONNECTED, "", null);
        }
    }

    @SubscribeEvent
    public void disconnected(DisconnectedEvent e) {
        if (!onServer()) return;

        setState(State.NOT_CONNECTED, "", null);
    }

    @SubscribeEvent
    public void connecting(ConnectedEvent e) {
        if (onServer()) {
            WynntilsMod.error("Got connected event while already connected to server: " + e);
            currentState = State.NOT_CONNECTED;
            currentWorldName = "";
        }

        String host = e.getHost().toLowerCase(Locale.ROOT);
        if (host.endsWith(WYNNCRAFT_SERVER_SUFFIX)) {
            onBetaServer = host.startsWith(WYNNCRAFT_BETA_PREFIX);
            setState(State.CONNECTING, "", null);
            currentTabListFooter = "";
        }
    }

    @SubscribeEvent
    public void remove(PlayerLogOutEvent e) {
        if (!onServer()) return;

        if (e.getId().equals(currentWorldId) && currentWorldName.length() > 0) {
            setState(State.INTERIM, "", null);
        }
    }

    @SubscribeEvent
    public void onResourcePack(ResourcePackEvent e) {
        if (!onServer()) return;

        setState(State.INTERIM, "", null);
    }

    @SubscribeEvent
    public void onTeleport(PlayerTeleportEvent e) {
        if (!onServer()) return;

        if (e.getNewPosition().equals(CHARACTER_SELECTION_POSITION)) {
            setState(State.CHARACTER_SELECTION, "", null);
        }
    }

    @SubscribeEvent
    public void onTabListFooter(PlayerInfoFooterChangedEvent e) {
        if (!onServer()) return;

        String footer = e.getFooter();
        if (footer.equals(currentTabListFooter)) return;

        currentTabListFooter = footer;

        if (footer.length() > 0) {
            if (HUB_NAME.matcher(footer).find()) {
                setState(State.HUB, "", null);
            }
        }
    }

    @SubscribeEvent
    public void update(PlayerDisplayNameChangeEvent e) {
        if (!onServer()) return;

        Component displayName = e.getDisplayName();
        String name = ComponentUtils.getFormatted(displayName);
        Matcher m = WORLD_NAME.matcher(name);
        if (m.find()) {
            String worldName = m.group(1);
            setState(State.WORLD, worldName, e.getId());
        }
    }

    public enum State {
        NOT_CONNECTED,
        CONNECTING,
        INTERIM,
        HUB,
        CHARACTER_SELECTION,
        WORLD
    }
}
