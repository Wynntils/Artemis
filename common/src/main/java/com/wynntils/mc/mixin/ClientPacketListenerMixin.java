/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.wynntils.mc.EventFactory;
import com.wynntils.mc.event.ChatPacketReceivedEvent;
import com.wynntils.mc.event.CommandsPacketEvent;
import com.wynntils.mc.mixin.accessors.ClientboundCommandsPacketAccessor;
import com.wynntils.mc.utils.McUtils;
import java.util.UUID;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    public abstract PlayerInfo getPlayerInfo(UUID uniqueId);

    private static boolean isRenderThread() {
        return (McUtils.mc().isSameThread());
    }

    @Inject(
            method = "handleCommands(Lnet/minecraft/network/protocol/game/ClientboundCommandsPacket;)V",
            at = @At("HEAD"))
    private void handleCommandsPre(ClientboundCommandsPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        CommandsPacketEvent event = EventFactory.onCommandsPacket(packet.getRoot());
        ((ClientboundCommandsPacketAccessor) packet).setRoot(event.getRoot());
    }

    @Inject(
            method = "handlePlayerInfo(Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoPacket;)V",
            at = @At("RETURN"))
    private void handlePlayerInfoPost(ClientboundPlayerInfoPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        EventFactory.onPlayerInfoPacket(packet);
    }

    @Inject(
            method = "handleTabListCustomisation(Lnet/minecraft/network/protocol/game/ClientboundTabListPacket;)V",
            at = @At("RETURN"))
    private void handleTabListCustomisationPost(ClientboundTabListPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        EventFactory.onTabListCustomisation(packet);
    }

    @Inject(
            method = "handleResourcePack(Lnet/minecraft/network/protocol/game/ClientboundResourcePackPacket;)V",
            at = @At("RETURN"))
    private void handleResourcePackPost(ClientboundResourcePackPacket packet, CallbackInfo ci) {
        EventFactory.onResourcePack();
    }

    @Inject(
            method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;)V",
            at = @At("RETURN"))
    private void handleMovePlayerPost(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        EventFactory.onPlayerMove(packet);
    }

    @Inject(
            method = "handleOpenScreen(Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleOpenScreenPre(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (EventFactory.onOpenScreen(packet).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleContainerClose(Lnet/minecraft/network/protocol/game/ClientboundContainerClosePacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleContainerClosePre(ClientboundContainerClosePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (EventFactory.onClientboundContainerClosePacket(packet.getContainerId())
                .isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method =
                    "handleContainerContent(Lnet/minecraft/network/protocol/game/ClientboundContainerSetContentPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleContainerContentPre(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (EventFactory.onContainerSetContent(packet).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        EventFactory.onContainerSetSlot(packet);
    }

    @Inject(
            method = "handleSetPlayerTeamPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPlayerTeamPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleSetPlayerTeamPacketPre(ClientboundSetPlayerTeamPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (EventFactory.onSetPlayerTeam(packet).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method =
                    "handleSetEntityPassengersPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleSetEntityPassengersPacketPre(ClientboundSetPassengersPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (EventFactory.onSetEntityPassengers(packet).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSetSpawn", at = @At("HEAD"), cancellable = true)
    private void handleSetSpawnPre(ClientboundSetDefaultSpawnPositionPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (EventFactory.onSetSpawn(packet.getPos()).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "setTitleText(Lnet/minecraft/network/protocol/game/ClientboundSetTitleTextPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void setTitleTextPre(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (EventFactory.onTitleSetText(packet).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "setSubtitleText(Lnet/minecraft/network/protocol/game/ClientboundSetSubtitleTextPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void setSubtitleTextPre(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (EventFactory.onSubtitleSetText(packet).isCanceled()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "handleChat(Lnet/minecraft/network/protocol/game/ClientboundChatPacket;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/Gui;handleChat(Lnet/minecraft/network/chat/ChatType;Lnet/minecraft/network/chat/Component;Ljava/util/UUID;)V"))
    private void redirectHandleChat(Gui gui, ChatType chatType, Component message, UUID uuid) {
        if (!isRenderThread()) return;
        ChatPacketReceivedEvent result = EventFactory.onChatReceived(chatType, message);
        if (result.isCanceled()) return;

        gui.handleChat(chatType, result.getMessage(), uuid);
    }

    @Inject(method = "handleSetScore", at = @At("HEAD"), cancellable = true)
    private void handleSetScore(ClientboundSetScorePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (EventFactory.onSetScore(packet).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onDisconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void onDisconnectPre(Component reason, CallbackInfo ci) {
        // Unexpected disconnect
        EventFactory.onDisconnect();
    }

    @Inject(method = "handleAddPlayer", at = @At("HEAD"))
    private void handleAddPlayer(ClientboundAddPlayerPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        EventFactory.onPlayerJoinedWorld(packet, this.getPlayerInfo(packet.getPlayerId()));
    }
}
