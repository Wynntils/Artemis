/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.hades;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Service;
import com.wynntils.core.components.Services;
import com.wynntils.features.players.HadesFeature;
import com.wynntils.hades.objects.HadesConnection;
import com.wynntils.hades.protocol.builders.HadesNetworkBuilder;
import com.wynntils.hades.protocol.enums.PacketAction;
import com.wynntils.hades.protocol.enums.PacketDirection;
import com.wynntils.hades.protocol.enums.SocialType;
import com.wynntils.hades.protocol.packets.client.HCPacketPing;
import com.wynntils.hades.protocol.packets.client.HCPacketSocialUpdate;
import com.wynntils.hades.protocol.packets.client.HCPacketUpdateStatus;
import com.wynntils.hades.protocol.packets.client.HCPacketUpdateWorld;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.models.players.event.HadesRelationsUpdateEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.services.athena.event.AthenaLoginEvent;
import com.wynntils.services.hades.event.HadesEvent;
import com.wynntils.services.hades.type.PlayerStatus;
import com.wynntils.services.map.pois.PlayerMainMapPoi;
import com.wynntils.services.map.pois.PlayerMiniMapPoi;
import com.wynntils.utils.mc.McUtils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;

public final class HadesService extends Service {
    private static final int TICKS_PER_UPDATE = 2;
    private static final int MS_PER_PING = 1000;

    private final HadesUserRegistry userRegistry = new HadesUserRegistry();

    private HadesConnection hadesConnection;
    private int tickCountUntilUpdate = 0;
    private PlayerStatus lastSentStatus;
    private ScheduledExecutorService pingScheduler;

    public HadesService() {
        super(List.of());
    }

    public Stream<PlayerMainMapPoi> getPlayerPois(boolean renderRemotePartyPlayers, boolean renderRemoteFriendPlayers) {
        return getHadesUsers()
                .filter(hadesUser -> (hadesUser.isPartyMember() && renderRemotePartyPlayers)
                        || (hadesUser.isMutualFriend() && renderRemoteFriendPlayers))
                .map(PlayerMainMapPoi::new);
    }

    public Stream<PlayerMiniMapPoi> getMiniPlayerPois(
            boolean renderRemotePartyPlayers, boolean renderRemoteFriendPlayers) {
        return getHadesUsers()
                .filter(hadesUser -> (hadesUser.isPartyMember() && renderRemotePartyPlayers)
                        || (hadesUser.isMutualFriend() && renderRemoteFriendPlayers))
                .map(PlayerMiniMapPoi::new);
    }

    public Stream<HadesUser> getHadesUsers() {
        return userRegistry.getHadesUserMap().values().stream();
    }

    private void login() {
        // Try to log in to Hades, if we're not already connected
        if (!isConnected()) {
            tryCreateConnection();
        }
    }

    private void tryCreateConnection() {
        try {
            hadesConnection = new HadesNetworkBuilder()
                    .setAddress(InetAddress.getByName("io.wynntils.com"), 9000)
                    .setDirection(PacketDirection.SERVER)
                    .setCompressionThreshold(256)
                    .setHandlerFactory(a -> new HadesClientHandler(a, userRegistry))
                    .buildClient();

            tickCountUntilUpdate = 0;
            lastSentStatus = null;
        } catch (UnknownHostException e) {
            WynntilsMod.error("Could not resolve Hades host address.", e);
        }
    }

    public void tryDisconnect() {
        if (hadesConnection != null && hadesConnection.isOpen()) {
            hadesConnection.disconnect();
        }
    }

    @SubscribeEvent
    public void onAuth(HadesEvent.Authenticated event) {
        if (Models.WorldState.onWorld()) {
            // Send initial world data if Hades login only happened after joining the player's class
            tryResendWorldData();
        }

        WynntilsMod.info("Starting Hades Ping Scheduler Task");

        pingScheduler = Executors.newSingleThreadScheduledExecutor();
        pingScheduler.scheduleAtFixedRate(this::sendPing, 0, MS_PER_PING, TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public void onDisconnect(HadesEvent.Disconnected event) {
        if (pingScheduler == null) return;
        pingScheduler.shutdown();
        pingScheduler = null;
    }

    private void sendPing() {
        if (!isConnected()) return;

        hadesConnection.sendPacketAndFlush(new HCPacketPing(System.currentTimeMillis()));
    }

    @SubscribeEvent
    public void onFriendListUpdate(HadesRelationsUpdateEvent.FriendList event) {
        if (!isConnected()) return;
        if (!Managers.Feature.getFeatureInstance(HadesFeature.class)
                .shareWithFriends
                .get()) return;

        hadesConnection.sendPacket(new HCPacketSocialUpdate(
                event.getChangedPlayers().stream().toList(),
                event.getChangeType().getPacketAction(),
                SocialType.FRIEND));
    }

    @SubscribeEvent
    public void onPartyListUpdate(HadesRelationsUpdateEvent.PartyList event) {
        if (!isConnected()) return;
        if (!Managers.Feature.getFeatureInstance(HadesFeature.class)
                .shareWithParty
                .get()) return;

        hadesConnection.sendPacket(new HCPacketSocialUpdate(
                event.getChangedPlayers().stream().toList(),
                event.getChangeType().getPacketAction(),
                SocialType.PARTY));
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() != WorldState.NOT_CONNECTED && !isConnected()) {
            if (Services.WynntilsAccount.isLoggedIn()) {
                tryCreateConnection();
            }
        }

        userRegistry.reset();

        if (event.isFirstJoinWorld()) {
            if (!isConnected()) {
                MutableComponent failed = Component.translatable("service.wynntils.hades.failedToConnect")
                        .withStyle(ChatFormatting.GREEN);
                failed.append(Component.translatable("service.wynntils.hades.clickToConnect1")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)));
                failed.append(Component.translatable("service.wynntils.hades.clickToConnect2")
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.AQUA)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wynntils reauth"))));
                failed.append(Component.translatable("service.wynntils.hades.clickToConnect3")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)));

                McUtils.sendMessageToClient(failed);

                return;
            }
        }

        tryResendWorldData();
    }

    @SubscribeEvent
    public void onAthenaLogin(AthenaLoginEvent event) {
        if (Models.WorldState.getCurrentState() != WorldState.NOT_CONNECTED && !isConnected()) {
            if (Services.WynntilsAccount.isLoggedIn()) {
                login();
            }
        }
    }

    @SubscribeEvent
    public void onClassChange(CharacterUpdateEvent event) {
        tryResendWorldData();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isConnected()) return;
        if (!Models.WorldState.onWorld() || McUtils.player().hasEffect(MobEffects.NIGHT_VISION)) return;
        if (!Managers.Feature.getFeatureInstance(HadesFeature.class)
                        .shareWithParty
                        .get()
                && !Managers.Feature.getFeatureInstance(HadesFeature.class)
                        .shareWithGuild
                        .get()
                && !Managers.Feature.getFeatureInstance(HadesFeature.class)
                        .shareWithFriends
                        .get()) return;

        tickCountUntilUpdate--;

        if (tickCountUntilUpdate <= 0) {
            LocalPlayer player = McUtils.player();

            float pX = (float) player.getX();
            float pY = (float) player.getY();
            float pZ = (float) player.getZ();

            PlayerStatus newStatus =
                    new PlayerStatus(pX, pY, pZ, Models.CharacterStats.getHealth(), Models.CharacterStats.getMana());

            if (newStatus.equals(lastSentStatus)) {
                tickCountUntilUpdate = 1;
                return;
            }

            tickCountUntilUpdate = TICKS_PER_UPDATE;

            lastSentStatus = newStatus;

            hadesConnection.sendPacketAndFlush(new HCPacketUpdateStatus(
                    lastSentStatus.x(),
                    lastSentStatus.y(),
                    lastSentStatus.z(),
                    lastSentStatus.health().current(),
                    lastSentStatus.health().max(),
                    lastSentStatus.mana().current(),
                    lastSentStatus.mana().max()));
        }
    }

    public void tryResendWorldData() {
        if (!isConnected()) return;

        hadesConnection.sendPacket(new HCPacketUpdateWorld(
                Models.WorldState.getCurrentWorldName(),
                Models.Character.getId().hashCode()));
    }

    public void resetSocialType(SocialType socialType) {
        if (!isConnected()) return;

        hadesConnection.sendPacketAndFlush(new HCPacketSocialUpdate(List.of(), PacketAction.RESET, socialType));
    }

    public void resetHadesUsers() {
        userRegistry.getHadesUserMap().clear();
    }

    private boolean isConnected() {
        return hadesConnection != null && hadesConnection.isOpen();
    }
}
