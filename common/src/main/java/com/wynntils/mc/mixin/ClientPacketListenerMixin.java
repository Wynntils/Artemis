/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.wynntils.mc.EventFactory;
import com.wynntils.mc.event.ChatReceivedEvent;
import com.wynntils.mc.utils.McUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(
            method = "handlePlayerInfo(Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoPacket;)V",
            at = @At("RETURN"))
    private void handlePlayerInfoPost(ClientboundPlayerInfoPacket packet, CallbackInfo ci) {
        EventFactory.onPlayerInfoPacket(packet);
    }

    @Inject(
            method = "handleTabListCustomisation(Lnet/minecraft/network/protocol/game/ClientboundTabListPacket;)V",
            at = @At("RETURN"))
    private void handleTabListCustomisationPost(ClientboundTabListPacket packet, CallbackInfo ci) {
        EventFactory.onTabListCustomisation(packet);
    }

    @Inject(
            method = "handleResourcePack(Lnet/minecraft/network/protocol/game/ClientboundResourcePackPacket;)V",
            at = @At("RETURN"))
    private void handleResourcePackPost(ClientboundResourcePackPacket packet, CallbackInfo ci) {
        EventFactory.onResourcePack(packet);
    }

    @Inject(
            method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;)V",
            at = @At("RETURN"))
    private void handleMovePlayerPost(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        EventFactory.onPlayerMove(packet);
    }

    @Inject(
            method = "handleOpenScreen(Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;)V",
            at = @At("RETURN"))
    private void handleOpenScreenPost(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        EventFactory.onOpenScreen(packet);
    }

    @Inject(
            method = "handleContainerClose(Lnet/minecraft/network/protocol/game/ClientboundContainerClosePacket;)V",
            at = @At("RETURN"))
    private void handleContainerClosePost(ClientboundContainerClosePacket packet, CallbackInfo ci) {
        EventFactory.onClientboundContainerClosePacket(packet);
    }

    @Inject(
            method = "handleSetPlayerTeamPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPlayerTeamPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleSetPlayerTeamPacketPre(ClientboundSetPlayerTeamPacket packet, CallbackInfo ci) {
        if (EventFactory.onSetPlayerTeam(packet).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSetSpawn", at = @At("HEAD"), cancellable = true)
    private void handleSetSpawnPre(ClientboundSetDefaultSpawnPositionPacket packet, CallbackInfo ci) {
        if (EventFactory.onSetSpawn(packet.getPos()).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method =
                    "handleContainerContent(Lnet/minecraft/network/protocol/game/ClientboundContainerSetContentPacket;)V",
            at = @At("RETURN"))
    private void handleContainerContentPost(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        List<ItemStack> items = new ArrayList<>(packet.getItems());
        items.add(packet.getCarriedItem());

        if (packet.getContainerId() == 0) {
            EventFactory.onItemsReceived(items, McUtils.inventoryMenu());
        } else if (packet.getContainerId() == McUtils.containerMenu().containerId) {
            EventFactory.onItemsReceived(items, McUtils.containerMenu());
        }
    }

    @Inject(
            method = "handleContainerSetSlot(Lnet/minecraft/network/protocol/game/ClientboundContainerSetSlotPacket;)V",
            at = @At("RETURN"))
    private void handleContainerSetSlotPost(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (packet.getContainerId() == -1 || packet.getContainerId() == McUtils.containerMenu().containerId) {
            EventFactory.onItemsReceived(Collections.singletonList(packet.getItem()), McUtils.containerMenu());
        } else if (packet.getContainerId() == -2
                || (packet.getContainerId() == 0 && InventoryMenu.isHotbarSlot(packet.getSlot()))) {
            EventFactory.onItemsReceived(Collections.singletonList(packet.getItem()), McUtils.inventoryMenu());
        }
    }

    @Inject(
            method = "setTitleText(Lnet/minecraft/network/protocol/game/ClientboundSetTitleTextPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void setTitleTextPre(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        if (EventFactory.onTitleSetText(packet).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "setSubtitleText(Lnet/minecraft/network/protocol/game/ClientboundSetSubtitleTextPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void setSubtitleTextPre(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
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
        ChatReceivedEvent result = EventFactory.onChatReceived(chatType, message);
        if (result.isCanceled()) return;

        gui.handleChat(chatType, result.getMessage(), uuid);
    }

    @Inject(method = "handleSetScore", at = @At("RETURN"))
    private void handleSetScore(ClientboundSetScorePacket packet, CallbackInfo ci) {
        EventFactory.onSetScore(packet);
    }
}
