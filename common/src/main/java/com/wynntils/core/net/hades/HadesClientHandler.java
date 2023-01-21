/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net.hades;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.net.hades.event.HadesEvent;
import com.wynntils.core.net.hades.objects.HadesUser;
import com.wynntils.features.user.HadesFeature;
import com.wynntils.hades.objects.HadesConnection;
import com.wynntils.hades.protocol.interfaces.adapters.IHadesClientAdapter;
import com.wynntils.hades.protocol.packets.client.HCPacketAuthenticate;
import com.wynntils.hades.protocol.packets.server.HSPacketAuthenticationResponse;
import com.wynntils.hades.protocol.packets.server.HSPacketClearMutual;
import com.wynntils.hades.protocol.packets.server.HSPacketDisconnect;
import com.wynntils.hades.protocol.packets.server.HSPacketDiscordLobbyServer;
import com.wynntils.hades.protocol.packets.server.HSPacketPong;
import com.wynntils.hades.protocol.packets.server.HSPacketUpdateMutual;
import com.wynntils.utils.mc.McUtils;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class HadesClientHandler implements IHadesClientAdapter {
    private final HadesConnection hadesConnection;

    public HadesClientHandler(HadesConnection hadesConnection) {
        this.hadesConnection = hadesConnection;
    }

    @Override
    public void onConnect() {
        if (!Managers.WynntilsAccount.isLoggedIn()) {
            hadesConnection.disconnect();

            if (Managers.WorldState.onServer()) {
                McUtils.sendMessageToClient(
                        Component.literal("Could not connect to HadesServer because you are not logged in on Athena.")
                                .withStyle(ChatFormatting.RED));
            }

            throw new IllegalStateException("Tried to auth to HadesServer without being logged in on Athena.");
        }

        hadesConnection.sendPacketAndFlush(new HCPacketAuthenticate(Managers.WynntilsAccount.getToken()));
    }

    @Override
    public void onDisconnect() {
        WynntilsMod.postEvent(new HadesEvent.Disconnected());

        if (Managers.WorldState.onServer()) {
            McUtils.sendMessageToClient(
                    Component.literal("Disconnected from HadesServer").withStyle(ChatFormatting.RED));
        }

        WynntilsMod.info("Disconnected from HadesServer.");

        Models.HadesUser.getHadesUserMap().clear();
    }

    @Override
    public void handleAuthenticationResponse(HSPacketAuthenticationResponse packet) {
        Component userComponent = Component.empty();

        switch (packet.getResponse()) {
            case SUCCESS -> {
                WynntilsMod.info("Successfully connected to HadesServer: " + packet.getMessage());
                userComponent = Component.literal("Successfully connected to HadesServer")
                        .withStyle(ChatFormatting.GREEN);
                Managers.TickScheduler.scheduleNextTick(() -> WynntilsMod.postEvent(new HadesEvent.Authenticated()));
            }
            case INVALID_TOKEN -> {
                WynntilsMod.error("Got invalid token when trying to connect to HadesServer: " + packet.getMessage());
                userComponent = Component.literal("Got invalid token when connecting HadesServer")
                        .withStyle(ChatFormatting.RED);
            }
            case ERROR -> {
                WynntilsMod.error("Got an error trying to connect to HadesServer: " + packet.getMessage());
                userComponent = Component.literal("Got error when connecting HadesServer")
                        .withStyle(ChatFormatting.RED);
            }
        }

        if (Managers.WorldState.onServer()) {
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
        if (!HadesFeature.INSTANCE.getOtherPlayerInfo) return;

        Optional<HadesUser> userOptional = Models.HadesUser.getUser(packet.getUser());
        if (userOptional.isPresent()) {
            userOptional.get().updateFromPacket(packet);
        } else {
            Models.HadesUser.putUser(packet.getUser(), new HadesUser(packet));
        }
    }

    @Override
    public void handleDiscordLobbyServer(HSPacketDiscordLobbyServer packet) {
        // noop for now
    }

    @Override
    public void handleClearMutual(HSPacketClearMutual packet) {
        Models.HadesUser.removeUser(packet.getUser());
    }

    @Override
    public void handleDisconnect(HSPacketDisconnect packet) {
        WynntilsMod.info("Disconnected from HadesServer. Reason: " + packet.getReason());

        if (Managers.WorldState.onServer()) {
            McUtils.sendMessageToClient(Component.literal("[Wynntils/Artemis] Disconnected from HadesServer.")
                    .withStyle(ChatFormatting.YELLOW));
        }

        Models.HadesUser.getHadesUserMap().clear();
    }
}
