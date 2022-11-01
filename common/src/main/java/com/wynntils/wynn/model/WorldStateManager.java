/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.managers.CoreManager;
import com.wynntils.mc.event.ConnectionEvent.ConnectedEvent;
import com.wynntils.mc.event.ConnectionEvent.DisconnectedEvent;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerDisplayNameChangeEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerLogOutEvent;
import com.wynntils.mc.event.PlayerInfoFooterChangedEvent;
import com.wynntils.mc.event.PlayerTeleportEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.wynn.event.WorldStateEvent;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WorldStateManager extends CoreManager {
    private static final UUID WORLD_NAME_UUID = UUID.fromString("16ff7452-714f-3752-b3cd-c3cb2068f6af");
    private static final Pattern WORLD_NAME = Pattern.compile("^§f {2}§lGlobal \\[(.*)\\]$");
    private static final Pattern HUB_NAME = Pattern.compile("^\n§6§l play.wynncraft.com \n$");
    private static final Position CHARACTER_SELECTION_POSITION = new Vec3(-1337.5, 16.2, -1120.5);
    private static final Pattern WYNNCRAFT_SERVER_PATTERN = Pattern.compile("^(.*)\\.wynncraft\\.(?:com|net|org)$");
    private static final String WYNNCRAFT_BETA_NAME = "beta";

    private static String currentTabListFooter = "";
    private static String currentWorldName = "";
    private static boolean onBetaServer;

    private static State currentState = State.NOT_CONNECTED;

    /** Needed for all Models */
    public static void init() {}

    public static boolean onServer() {
        return currentState != State.NOT_CONNECTED;
    }

    public static boolean onWorld() {
        return currentState == State.WORLD;
    }

    public static boolean isInStream() {
        return currentWorldName.equals("-");
    }

    public static boolean isOnBetaServer() {
        return onBetaServer;
    }

    public static State getCurrentState() {
        return currentState;
    }

    private static void setState(State newState, String newWorldName) {
        if (newState == currentState && newWorldName.equals(currentWorldName)) return;

        State oldState = currentState;
        // Switch state before sending event
        currentState = newState;
        currentWorldName = newWorldName;
        WynntilsMod.postEvent(new WorldStateEvent(newState, oldState, newWorldName));
    }

    @SubscribeEvent
    public static void screenOpened(ScreenOpenedEvent e) {
        if (e.getScreen() instanceof DisconnectedScreen) {
            setState(State.NOT_CONNECTED, "");
        }
    }

    @SubscribeEvent
    public static void disconnected(DisconnectedEvent e) {
        setState(State.NOT_CONNECTED, "");
    }

    @SubscribeEvent
    public static void connecting(ConnectedEvent e) {
        if (onServer()) {
            WynntilsMod.error("Got connected event while already connected to server: " + e);
            currentState = State.NOT_CONNECTED;
            currentWorldName = "";
        }

        String host = e.getHost().toLowerCase(Locale.ROOT);
        Matcher m = WYNNCRAFT_SERVER_PATTERN.matcher(host);
        if (m.matches()) {
            onBetaServer = m.group(1).equals(WYNNCRAFT_BETA_NAME);
            setState(State.CONNECTING, "");
            currentTabListFooter = "";
        }
    }

    @SubscribeEvent
    public static void remove(PlayerLogOutEvent e) {
        if (e.getId().equals(WORLD_NAME_UUID) && !currentWorldName.isEmpty()) {
            setState(State.INTERIM, "");
        }
    }

    @SubscribeEvent
    public static void onTeleport(PlayerTeleportEvent e) {
        if (e.getNewPosition().equals(CHARACTER_SELECTION_POSITION)) {
            // We get here even if the character selection menu will not show up because of autojoin
            if (getCurrentState() != State.CHARACTER_SELECTION) {
                // Sometimes the TP comes after the character selection menu, instead of before
                // Don't lose the CHARACTER_SELECTION state if that is the case
                setState(State.INTERIM, "");
            }
        }
    }

    @SubscribeEvent
    public static void onMenuOpened(MenuEvent.MenuOpenedEvent e) {
        if (e.getMenuType() == MenuType.GENERIC_9x3
                && ComponentUtils.getCoded(e.getTitle()).equals("§8§lSelect a Character")) {
            setState(State.CHARACTER_SELECTION, "");
        }
    }

    @SubscribeEvent
    public static void onTabListFooter(PlayerInfoFooterChangedEvent e) {
        String footer = e.getFooter();
        if (footer.equals(currentTabListFooter)) return;

        currentTabListFooter = footer;

        if (!footer.isEmpty()) {
            if (HUB_NAME.matcher(footer).find()) {
                setState(State.HUB, "");
            }
        }
    }

    @SubscribeEvent
    public static void update(PlayerDisplayNameChangeEvent e) {
        if (!e.getId().equals(WORLD_NAME_UUID)) return;

        Component displayName = e.getDisplayName();
        String name = ComponentUtils.getCoded(displayName);
        Matcher m = WORLD_NAME.matcher(name);
        if (m.find()) {
            String worldName = m.group(1);
            setState(State.WORLD, worldName);
        }
    }

    public static String getCurrentWorldName() {
        return currentWorldName;
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
