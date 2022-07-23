/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.wynntils.core.WynntilsMod;
import com.wynntils.mc.event.BossHealthUpdateEvent;
import com.wynntils.mc.event.ChatReceivedEvent;
import com.wynntils.mc.event.ChatSentEvent;
import com.wynntils.mc.event.ClientTickEvent;
import com.wynntils.mc.event.ConnectionEvent.ConnectedEvent;
import com.wynntils.mc.event.ConnectionEvent.DisconnectedEvent;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.ContainerCloseEvent;
import com.wynntils.mc.event.ContainerRenderEvent;
import com.wynntils.mc.event.DisplayResizeEvent;
import com.wynntils.mc.event.DropHeldItemEvent;
import com.wynntils.mc.event.HotbarSlotRenderEvent;
import com.wynntils.mc.event.InventoryKeyPressEvent;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.mc.event.ItemsReceivedEvent;
import com.wynntils.mc.event.KeyInputEvent;
import com.wynntils.mc.event.MenuEvent.MenuClosedEvent;
import com.wynntils.mc.event.MenuEvent.MenuOpenedEvent;
import com.wynntils.mc.event.PacketEvent.PacketReceivedEvent;
import com.wynntils.mc.event.PacketEvent.PacketSentEvent;
import com.wynntils.mc.event.PauseMenuInitEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerDisplayNameChangeEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerLogInEvent;
import com.wynntils.mc.event.PlayerInfoEvent.PlayerLogOutEvent;
import com.wynntils.mc.event.PlayerInfoFooterChangedEvent;
import com.wynntils.mc.event.PlayerInteractEvent;
import com.wynntils.mc.event.PlayerTeleportEvent;
import com.wynntils.mc.event.RemovePlayerFromTeamEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.event.RenderLevelLastEvent;
import com.wynntils.mc.event.ResourcePackEvent;
import com.wynntils.mc.event.ScoreboardSetScoreEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.mc.event.SetPlayerTeamEvent;
import com.wynntils.mc.event.SetSlotEvent;
import com.wynntils.mc.event.SetSpawnEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.event.TitleScreenInitEvent;
import com.wynntils.mc.event.TitleSetTextEvent;
import com.wynntils.mc.event.WebSetupEvent;
import com.wynntils.mc.mixin.accessors.ClientboundSetPlayerTeamPacketAccessor;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket.PlayerUpdate;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.eventbus.api.Event;

/** Creates events from mixins and platform dependent hooks */
public class EventFactory {
    private static <T extends Event> T post(T event) {
        WynntilsMod.getEventBus().post(event);
        return event;
    }

    // region Wynntils Events
    public static void onWebSetup() {
        post(new WebSetupEvent());
    }
    // endregion

    // region Render Events
    public static void onRenderLast(
            LevelRenderer context,
            PoseStack poseStack,
            float partialTick,
            Matrix4f projectionMatrix,
            long finishTimeNano) {
        post(new RenderLevelLastEvent(context, poseStack, partialTick, projectionMatrix, finishTimeNano));
    }

    public static void onRenderGuiPre(PoseStack poseStack, float partialTicks, Window window) {
        post(new RenderEvent.Pre(poseStack, partialTicks, window, RenderEvent.ElementType.GUI));
    }

    public static void onRenderGuiPost(PoseStack poseStack, float partialTicks, Window window) {
        post(new RenderEvent.Post(poseStack, partialTicks, window, RenderEvent.ElementType.GUI));
    }

    public static RenderEvent.Pre onRenderCrosshairPre(PoseStack poseStack, Window window) {
        return post(new RenderEvent.Pre(poseStack, 0, window, RenderEvent.ElementType.Crosshair));
    }

    public static void onContainerRender(
            Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partialTicks, Slot hoveredSlot) {
        post(new ContainerRenderEvent(screen, poseStack, mouseX, mouseY, partialTicks, hoveredSlot));
    }

    public static ItemTooltipRenderEvent.Pre onItemTooltipRenderPre(
            PoseStack poseStack, ItemStack stack, List<Component> tooltips, int mouseX, int mouseY) {
        return post(new ItemTooltipRenderEvent.Pre(poseStack, stack, tooltips, mouseX, mouseY));
    }

    public static void onItemTooltipRenderPost(PoseStack poseStack, ItemStack stack, int mouseX, int mouseY) {
        post(new ItemTooltipRenderEvent.Post(poseStack, stack, mouseX, mouseY));
    }

    public static void onSlotRenderPre(Screen screen, Slot slot) {
        post(new SlotRenderEvent.Pre(screen, slot));
    }

    public static void onSlotRenderPost(Screen screen, Slot slot) {
        post(new SlotRenderEvent.Post(screen, slot));
    }

    public static void onHotbarSlotRenderPre(ItemStack stack, int x, int y) {
        post(new HotbarSlotRenderEvent.Pre(stack, x, y));
    }

    public static void onHotbarSlotRenderPost(ItemStack stack, int x, int y) {
        post(new HotbarSlotRenderEvent.Post(stack, x, y));
    }
    // endregion

    // region Screen Events
    public static void onScreenCreated(Screen screen, Consumer<AbstractWidget> addButton) {
        if (screen instanceof TitleScreen titleScreen) {
            post(new TitleScreenInitEvent(titleScreen, addButton));
        } else if (screen instanceof PauseScreen pauseMenuScreen) {
            post(new PauseMenuInitEvent(pauseMenuScreen, addButton));
        }
    }

    public static void onScreenOpened(Screen screen) {
        post(new ScreenOpenedEvent(screen));
    }

    public static void onOpenScreen(ClientboundOpenScreenPacket packet) {
        post(new MenuOpenedEvent(packet.getType(), packet.getTitle()));
    }
    // endregion

    // region Container Events
    public static void onClientboundContainerClosePacket(ClientboundContainerClosePacket packet) {
        post(new MenuClosedEvent());
    }

    public static ContainerCloseEvent.Pre onCloseContainerPre() {
        return post(new ContainerCloseEvent.Pre());
    }

    public static void onCloseContainerPost() {
        post(new ContainerCloseEvent.Post());
    }

    public static void onItemsReceived(List<ItemStack> items, AbstractContainerMenu container) {
        post(new ItemsReceivedEvent(container, items));
    }

    public static SetSlotEvent onSetSlot(Container container, int slot, ItemStack item) {
        return post(new SetSlotEvent(container, slot, item));
    }

    public static InventoryKeyPressEvent onInventoryKeyPress(
            int keyCode, int scanCode, int modifiers, Slot hoveredSlot) {
        return post(new InventoryKeyPressEvent(keyCode, scanCode, modifiers, hoveredSlot));
    }

    public static ContainerClickEvent onContainerClickEvent(
            int containerId, int slotNum, ItemStack itemStack, ClickType clickType, int buttonNum) {
        return post(new ContainerClickEvent(containerId, slotNum, itemStack, clickType, buttonNum));
    }
    // endregion

    // region Player Input Events
    public static void onPlayerMove(ClientboundPlayerPositionPacket packet) {
        if (!packet.getRelativeArguments().isEmpty()) return;

        Position newPosition = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        post(new PlayerTeleportEvent(newPosition));
    }

    public static KeyInputEvent onKeyInput(int key, int scanCode, int action, int modifiers) {
        return post(new KeyInputEvent(key, scanCode, action, modifiers));
    }

    public static void onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitVec) {
        PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, hand, pos, hitVec);
        post(event);
    }

    public static DropHeldItemEvent onDropPre(boolean fullStack) {
        return post(new DropHeldItemEvent(fullStack));
    }
    // endregion

    // region Chat Events
    public static ChatSentEvent onChatSent(String message) {
        return post(new ChatSentEvent(message));
    }

    public static ChatReceivedEvent onChatReceived(ChatType type, Component message) {
        return post(new ChatReceivedEvent(type, message));
    }
    // endregion

    // region Server Events
    public static void onDisconnect() {
        post(new DisconnectedEvent());
    }

    public static void onConnect(String host, int port) {
        post(new ConnectedEvent(host, port));
    }

    public static void onResourcePack(ClientboundResourcePackPacket packet) {
        post(new ResourcePackEvent());
    }

    public static SetPlayerTeamEvent onSetPlayerTeam(ClientboundSetPlayerTeamPacket packet) {
        return post(new SetPlayerTeamEvent(
                ((ClientboundSetPlayerTeamPacketAccessor) packet).getMethod(), packet.getName()));
    }

    public static RemovePlayerFromTeamEvent onRemovePlayerFromTeam(String username, PlayerTeam playerTeam) {
        return post(new RemovePlayerFromTeamEvent(username, playerTeam));
    }

    public static BossHealthUpdateEvent onBossHealthUpdate(
            ClientboundBossEventPacket packet, Map<UUID, LerpingBossEvent> bossEvents) {
        return post(new BossHealthUpdateEvent(packet, bossEvents));
    }

    public static SetSpawnEvent onSetSpawn(BlockPos spawnPos) {
        return post(new SetSpawnEvent(spawnPos));
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

    public static void onTabListCustomisation(ClientboundTabListPacket packet) {
        String footer = packet.getFooter().getString();
        post(new PlayerInfoFooterChangedEvent(footer));
    }

    public static void onSetScore(ClientboundSetScorePacket packet) {
        post(new ScoreboardSetScoreEvent(
                packet.getOwner(), packet.getObjectiveName(), packet.getScore(), packet.getMethod()));
    }
    // endregion

    // region Packet Events
    public static <T extends Packet<?>> PacketSentEvent<T> onPacketSent(T packet) {
        return post(new PacketSentEvent<>(packet));
    }

    public static <T extends Packet<?>> PacketReceivedEvent<T> onPacketReceived(T packet) {
        return post(new PacketReceivedEvent<>(packet));
    }
    // endregion

    // region Game Events
    public static void onTickStart() {
        post(new ClientTickEvent(ClientTickEvent.Phase.START));
    }

    public static void onTickEnd() {
        post(new ClientTickEvent(ClientTickEvent.Phase.END));
    }

    public static void onResizeDisplayPost() {
        post(new DisplayResizeEvent());
    }

    // endregion

    // region Title Events
    public static Event onTitleSetText(ClientboundSetTitleTextPacket packet) {
        return post(new TitleSetTextEvent(packet.getText()));
    }

    public static Event onSubtitleSetText(ClientboundSetSubtitleTextPacket packet) {
        return post(new SubtitleSetTextEvent(packet.getText()));
    }

    // endregion
}
