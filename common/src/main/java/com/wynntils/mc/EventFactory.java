/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.mc.event.ConnectionEvent.ConnectedEvent;
import com.wynntils.mc.event.ConnectionEvent.DisconnectedEvent;
import com.wynntils.mc.event.GameMenuInitEvent;
import com.wynntils.mc.event.InventoryRenderEvent;
import com.wynntils.mc.event.MenuEvent.MenuClosedEvent;
import com.wynntils.mc.event.MenuEvent.MenuOpenedEvent;
import com.wynntils.mc.event.PacketEvent.PacketReceivedEvent;
import com.wynntils.mc.event.PacketEvent.PacketSentEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerDisplayNameChangeEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerLogInEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerLogOutEvent;
import com.wynntils.mc.event.PlayerInfoFooterChangedEvent;
import com.wynntils.mc.event.PlayerTeleportEvent;
import com.wynntils.mc.event.ResourcePackEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.mc.event.TitleScreenInitEvent;
import com.wynntils.utils.Utils;
import java.net.InetAddress;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.Position;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket.PlayerUpdate;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

/** Creates events from mixins and platform dependent hooks */
public class EventFactory {
    private static void post(Event event) {
        Utils.getEventBus().post(event);
    }

    public static void onScreenCreated(Screen screen, Consumer<AbstractWidget> addButton) {
        if (screen instanceof TitleScreen titleScreen) {
            post(new TitleScreenInitEvent(titleScreen, addButton));
        } else if (screen instanceof PauseScreen gameMenuScreen) {
            post(new GameMenuInitEvent(gameMenuScreen, addButton));
        }
    }

    public static void onScreenOpened(Screen screen) {
        post(new ScreenOpenedEvent(screen));
    }

    public static void onInventoryRender(
            Screen screen,
            PoseStack poseStack,
            int mouseX,
            int mouseY,
            float partialTicks,
            Slot hoveredSlot) {
        post(
                new InventoryRenderEvent(
                        screen, poseStack, mouseX, mouseY, partialTicks, hoveredSlot));
    }

    public static void onPlayerInfoPacket(ClientboundPlayerInfoPacket packet) {
        Action action = packet.getAction();
        List<PlayerUpdate> entries = packet.getEntries();

        if (action == Action.UPDATE_DISPLAY_NAME) {
            for (PlayerUpdate entry : entries) {
                GameProfile profile = entry.getProfile();
                if (entry.getDisplayName() == null) continue;
                post(new PlayerDisplayNameChangeEvent(profile.getId(), entry.getDisplayName()));
            }
        } else if (action == Action.ADD_PLAYER) {
            for (PlayerUpdate entry : entries) {
                GameProfile profile = entry.getProfile();
                post(new PlayerLogInEvent(profile.getId(), profile.getName()));
            }
        } else if (action == Action.REMOVE_PLAYER) {
            for (PlayerUpdate entry : entries) {
                GameProfile profile = entry.getProfile();
                post(new PlayerLogOutEvent(profile.getId()));
            }
        }
    }

    public static void onTooltipRender(Screen screen, PoseStack poseStack, int mouseX, int mouseY) {
        // TODO: Not implemented yet
    }

    public static void onTabListCustomisation(ClientboundTabListPacket packet) {
        String footer = packet.getFooter().getString();
        post(new PlayerInfoFooterChangedEvent(footer));
    }

    public static void onDisconnect() {
        post(new DisconnectedEvent());
    }

    public static void onConnect(String host, int port) {
        post(new ConnectedEvent(host, port));
    }

    public static void onResourcePack(ClientboundResourcePackPacket packet) {
        post(new ResourcePackEvent());
    }

    public static void onPacketSent(Packet<?> packet) {
        post(new PacketSentEvent(packet));
    }

    public static void onPacketReceived(Packet<?> packet) {
        post(new PacketReceivedEvent(packet));
    }

    public static void onPlayerMove(ClientboundPlayerPositionPacket packet) {
        if (!packet.getRelativeArguments().isEmpty()) return;

        Position newPosition = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        post(new PlayerTeleportEvent(newPosition));
    }

    public static void onOpenScreen(ClientboundOpenScreenPacket packet) {
        ResourceLocation menuType = Registry.MENU.getKey(packet.getType());

        post(new MenuOpenedEvent(menuType, packet.getTitle()));
    }

    public static void onContainerClose(ClientboundContainerClosePacket packet) {
        post(new MenuClosedEvent());
    }
}
