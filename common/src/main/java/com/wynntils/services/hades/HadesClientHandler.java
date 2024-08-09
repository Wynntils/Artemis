/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.hades;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Services;
import com.wynntils.features.players.HadesFeature;
import com.wynntils.hades.objects.HadesConnection;
import com.wynntils.hades.protocol.interfaces.adapters.IHadesClientAdapter;
import com.wynntils.hades.protocol.packets.client.HCPacketAuthenticate;
import com.wynntils.hades.protocol.packets.server.HSPacketAuthenticationResponse;
import com.wynntils.hades.protocol.packets.server.HSPacketClearMutual;
import com.wynntils.hades.protocol.packets.server.HSPacketDisconnect;
import com.wynntils.hades.protocol.packets.server.HSPacketDiscordLobbyServer;
import com.wynntils.hades.protocol.packets.server.HSPacketPong;
import com.wynntils.hades.protocol.packets.server.HSPacketUpdateMutual;
import com.wynntils.services.hades.event.HadesEvent;
import com.wynntils.services.hades.event.HadesUserAddedEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class HadesClientHandler implements IHadesClientAdapter {
    private final HadesConnection hadesConnection;
    private final HadesUserRegistry userRegistry;

    public HadesClientHandler(HadesConnection hadesConnection, HadesUserRegistry userRegistry) {
        this.hadesConnection = hadesConnection;
        this.userRegistry = userRegistry;

        // FIXME: This is needed for patching class loading issue with Forge EventBus:
        //        https://github.com/MinecraftForge/EventBus/issues/44
        Thread.currentThread().setContextClassLoader(WynntilsMod.class.getClassLoader());
    }

    @Override
    public void onConnect() {
        if (!Services.WynntilsAccount.isLoggedIn()) {
            hadesConnection.disconnect();

            if (Managers.Connection.onServer()) {
                McUtils.sendErrorToClient(
                        "Could not connect to the remote player server because you are not logged in on Athena.");
            }

            throw new IllegalStateException(
                    "Tried to auth to the remote player server without being logged in on Athena.");
        }

        hadesConnection.sendPacketAndFlush(new HCPacketAuthenticate(Services.WynntilsAccount.getToken()));
    }

    @Override
    public void onDisconnect() {
        WynntilsMod.postEvent(new HadesEvent.Disconnected());

        if (Managers.Connection.onServer()) {
            McUtils.sendErrorToClient("Disconnected from the remote player server.");
        }

        WynntilsMod.info("Disconnected from the remote player server.");

        userRegistry.getHadesUserMap().clear();
    }

    @Override
    public void handleAuthenticationResponse(HSPacketAuthenticationResponse packet) {
        Component userComponent = Component.empty();

        switch (packet.getResponse()) {
            case SUCCESS -> {
                WynntilsMod.info("Successfully connected to the remote player server: " + packet.getMessage());
                userComponent = Component.literal("Successfully connected to the remote player server.")
                        .withStyle(ChatFormatting.GREEN);

                WynntilsMod.postEvent(new HadesEvent.Authenticated());
            }
            case INVALID_TOKEN -> {
                WynntilsMod.error(
                        "Got invalid token when trying to connect to the remote player server: " + packet.getMessage());
                userComponent = Component.literal("Got invalid token when connecting the remote player server.")
                        .withStyle(ChatFormatting.RED);
            }
            case ERROR -> {
                WynntilsMod.error("Got an error trying to connect to the remote player server: " + packet.getMessage());
                userComponent = Component.literal("Got error when connecting the remote player server.")
                        .withStyle(ChatFormatting.RED);
            }
        }

        if (Managers.Connection.onServer()) {
            McUtils.sendMessageToClient(userComponent);
        }
    }

    @Override
    public void handlePing(HSPacketPong packet) {
        // noop at the moment
        // todo eventually calculate ping
    }

    @Override
    public void handleUpdateMutual(HSPacketUpdateMutual packet) {
        if (!Managers.Feature.getFeatureInstance(HadesFeature.class)
                .getOtherPlayerInfo
                .get()) return;

        Optional<HadesUser> userOptional = userRegistry.getUser(packet.getUser());
        if (userOptional.isPresent()) {
            userOptional.get().updateFromPacket(packet);
        } else {
            HadesUser hadesUser = new HadesUser(packet);
            userRegistry.putUser(packet.getUser(), hadesUser);
            WynntilsMod.postEventOnMainThread(new HadesUserAddedEvent(hadesUser));
        }
    }

    @Override
    public void handleDiscordLobbyServer(HSPacketDiscordLobbyServer packet) {
        // noop for now
    }

    @Override
    public void handleClearMutual(HSPacketClearMutual packet) {
        userRegistry.removeUser(packet.getUser());
    }

    @Override
    public void handleDisconnect(HSPacketDisconnect packet) {
        WynntilsMod.info("Disconnected from the remote player server. Reason: " + packet.getReason());

        if (Managers.Connection.onServer()) {
            McUtils.sendMessageToClient(Component.literal("[Wynntils] Disconnected from the remote player server.")
                    .withStyle(ChatFormatting.YELLOW));
        }

        userRegistry.getHadesUserMap().clear();
    }
}
