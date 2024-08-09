/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.RootCommandNode;
import com.wynntils.core.events.MixinHelper;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.AddEntityEvent;
import com.wynntils.mc.event.AdvancementUpdateEvent;
import com.wynntils.mc.event.ChatPacketReceivedEvent;
import com.wynntils.mc.event.ChatSentEvent;
import com.wynntils.mc.event.ChunkReceivedEvent;
import com.wynntils.mc.event.CommandSentEvent;
import com.wynntils.mc.event.CommandsAddedEvent;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.ContainerSetSlotEvent;
import com.wynntils.mc.event.LocalSoundEvent;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.mc.event.MobEffectEvent;
import com.wynntils.mc.event.ParticleAddedEvent;
import com.wynntils.mc.event.PlayerInfoEvent;
import com.wynntils.mc.event.PlayerInfoFooterChangedEvent;
import com.wynntils.mc.event.PlayerTeleportEvent;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.mc.event.ScoreboardEvent;
import com.wynntils.mc.event.ScoreboardSetDisplayObjectiveEvent;
import com.wynntils.mc.event.ScoreboardSetObjectiveEvent;
import com.wynntils.mc.event.SetEntityDataEvent;
import com.wynntils.mc.event.SetEntityPassengersEvent;
import com.wynntils.mc.event.SetPlayerTeamEvent;
import com.wynntils.mc.event.SetSpawnEvent;
import com.wynntils.mc.event.SetXpEvent;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.event.TeleportEntityEvent;
import com.wynntils.mc.event.TitleSetTextEvent;
import com.wynntils.mc.mixin.accessors.ClientboundSetPlayerTeamPacketAccessor;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {
    @Shadow
    private CommandDispatcher<SharedSuggestionProvider> commands;

    @Shadow
    private RegistryAccess.Frozen registryAccess;

    @Shadow
    @Final
    private FeatureFlagSet enabledFeatures;

    @Shadow
    protected abstract ParseResults<SharedSuggestionProvider> parseCommand(String command);

    protected ClientPacketListenerMixin(
            Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }

    @Unique
    private static boolean isRenderThread() {
        return McUtils.mc().isSameThread();
    }

    @Inject(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void onChatPre(String string, CallbackInfo ci) {
        ChatSentEvent event = new ChatSentEvent(string);
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendCommand(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void onSignedCommandPre(String string, CallbackInfo ci) {
        CommandSentEvent event = new CommandSentEvent(string, true);
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendUnsignedCommand(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private void onUnsignedCommandPre(String command, CallbackInfoReturnable<Boolean> cir) {
        CommandSentEvent event = new CommandSentEvent(command, false);
        MixinHelper.post(event);
        if (event.isCanceled()) {
            // Return true here, to signal to MC that we handled the command.
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(
            method = "handleCommands(Lnet/minecraft/network/protocol/game/ClientboundCommandsPacket;)V",
            at = @At("RETURN"))
    private void handleCommandsPost(ClientboundCommandsPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        // We need to read the root from the CommandDispatcher, not the packet,
        // due to interop with other mods
        RootCommandNode<SharedSuggestionProvider> root = this.commands.getRoot();
        CommandsAddedEvent event =
                new CommandsAddedEvent(root, CommandBuildContext.simple(this.registryAccess, this.enabledFeatures));
        MixinHelper.post(event);

        if (event.getRoot() != root) {
            // If we changed the root, replace the CommandDispatcher
            this.commands = new CommandDispatcher<>(event.getRoot());
        }
    }

    @Inject(
            method = "handlePlayerInfoUpdate(Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;)V",
            at = @At("RETURN"))
    private void handlePlayerInfoUpdatePost(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (!MixinHelper.onWynncraft()) return;

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.newEntries()) {
            GameProfile profile = entry.profile();
            if (profile == null) continue;
            MixinHelper.post(new PlayerInfoEvent.PlayerLogInEvent(profile.getId(), profile.getName()));
        }

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
            for (ClientboundPlayerInfoUpdatePacket.Action action : packet.actions()) {
                if (action == ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) {
                    if (entry.displayName() == null) continue;
                    MixinHelper.post(
                            new PlayerInfoEvent.PlayerDisplayNameChangeEvent(entry.profileId(), entry.displayName()));
                }
            }
        }
    }

    @Inject(
            method = "handlePlayerInfoRemove(Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoRemovePacket;)V",
            at = @At("RETURN"))
    private void handlePlayerInfoRemovePost(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (!MixinHelper.onWynncraft()) return;

        for (UUID uuid : packet.profileIds()) {
            MixinHelper.post(new PlayerInfoEvent.PlayerLogOutEvent(uuid));
        }
    }

    @Inject(
            method = "handleTabListCustomisation(Lnet/minecraft/network/protocol/game/ClientboundTabListPacket;)V",
            at = @At("RETURN"))
    private void handleTabListCustomisationPost(ClientboundTabListPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new PlayerInfoFooterChangedEvent(StyledText.fromComponent(packet.footer())));
    }

    @Inject(
            method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;)V",
            at = @At("HEAD"))
    private void handleMovePlayerPost(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (!packet.getRelativeArguments().isEmpty()) return;

        MixinHelper.post(new PlayerTeleportEvent(new Vec3(packet.getX(), packet.getY(), packet.getZ())));
    }

    @Inject(
            method = "handleOpenScreen(Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleOpenScreenPre(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MenuEvent.MenuOpenedEvent.Pre event =
                new MenuEvent.MenuOpenedEvent.Pre(packet.getType(), packet.getTitle(), packet.getContainerId());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleOpenScreen(Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;)V",
            at = @At("RETURN"))
    private void handleOpenScreenPost(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MenuEvent.MenuOpenedEvent event =
                new MenuEvent.MenuOpenedEvent.Post(packet.getType(), packet.getTitle(), packet.getContainerId());
        MixinHelper.post(event);
    }

    @Inject(
            method = "handleContainerClose(Lnet/minecraft/network/protocol/game/ClientboundContainerClosePacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleContainerClosePre(ClientboundContainerClosePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MenuEvent.MenuClosedEvent event = new MenuEvent.MenuClosedEvent(packet.getContainerId());
        MixinHelper.post(event);
        if (event.isCanceled()) {
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

        ContainerSetContentEvent.Pre event = new ContainerSetContentEvent.Pre(
                packet.getItems(), packet.getCarriedItem(), packet.getContainerId(), packet.getStateId());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }

        if (!packet.getItems().equals(event.getItems())) {
            if (packet.getContainerId() == 0) {
                McUtils.player()
                        .inventoryMenu
                        .initializeContents(packet.getStateId(), packet.getItems(), packet.getCarriedItem());
            } else if (packet.getContainerId() == McUtils.containerMenu().containerId) {
                McUtils.player()
                        .containerMenu
                        .initializeContents(packet.getStateId(), packet.getItems(), packet.getCarriedItem());
            }

            ci.cancel();
        }
    }

    @Inject(
            method =
                    "handleContainerContent(Lnet/minecraft/network/protocol/game/ClientboundContainerSetContentPacket;)V",
            at = @At("RETURN"))
    private void handleContainerContentPost(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new ContainerSetContentEvent.Post(
                packet.getItems(), packet.getCarriedItem(), packet.getContainerId(), packet.getStateId()));
    }

    @Inject(
            method = "handleContainerSetSlot(Lnet/minecraft/network/protocol/game/ClientboundContainerSetSlotPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleContainerSetSlotPre(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        ContainerSetSlotEvent.Pre event = new ContainerSetSlotEvent.Pre(
                packet.getContainerId(), packet.getStateId(), packet.getSlot(), packet.getItem());
        MixinHelper.post(event);

        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleContainerSetSlot(Lnet/minecraft/network/protocol/game/ClientboundContainerSetSlotPacket;)V",
            at = @At("RETURN"))
    private void handleContainerSetSlotPost(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new ContainerSetSlotEvent.Post(
                packet.getContainerId(), packet.getStateId(), packet.getSlot(), packet.getItem()));
    }

    @Inject(
            method = "handleSetPlayerTeamPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPlayerTeamPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleSetPlayerTeamPacketPre(ClientboundSetPlayerTeamPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        SetPlayerTeamEvent event =
                new SetPlayerTeamEvent(((ClientboundSetPlayerTeamPacketAccessor) packet).getMethod(), packet.getName());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleSetExperience(Lnet/minecraft/network/protocol/game/ClientboundSetExperiencePacket;)V",
            at = @At("RETURN"))
    private void handleSetExperiencePost(ClientboundSetExperiencePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new SetXpEvent(
                packet.getExperienceProgress(), packet.getTotalExperience(), packet.getExperienceLevel()));
    }

    @Inject(
            method =
                    "handleSetEntityPassengersPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleSetEntityPassengersPacketPre(ClientboundSetPassengersPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        SetEntityPassengersEvent event = new SetEntityPassengersEvent(packet.getVehicle());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleSetSpawn(Lnet/minecraft/network/protocol/game/ClientboundSetDefaultSpawnPositionPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleSetSpawnPre(ClientboundSetDefaultSpawnPositionPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        SetSpawnEvent event = new SetSpawnEvent(packet.getPos());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();

            // Signal loading complete to the loading screen,
            // or else we are stuck in an "infinite" loading state
            if (McUtils.mc().screen instanceof ReceivingLevelScreen receivingLevelScreen) {
                receivingLevelScreen.onClose();
            }
        }
    }

    @Inject(
            method = "setTitleText(Lnet/minecraft/network/protocol/game/ClientboundSetTitleTextPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void setTitleTextPre(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        TitleSetTextEvent event = new TitleSetTextEvent(packet.text());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "setSubtitleText(Lnet/minecraft/network/protocol/game/ClientboundSetSubtitleTextPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void setSubtitleTextPre(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        SubtitleSetTextEvent event = new SubtitleSetTextEvent(packet.text());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handlePlayerChat(Lnet/minecraft/network/protocol/game/ClientboundPlayerChatPacket;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/chat/ChatListener;handlePlayerChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/chat/ChatType$Bound;)V"),
            cancellable = true)
    private void handlePlayerChat(
            ClientboundPlayerChatPacket packet,
            CallbackInfo ci,
            @Local PlayerChatMessage playerChatMessage,
            @Local PlayerInfo playerInfo) {
        if (!isRenderThread()) return;

        // Currently, Wynncraft does not have any Player chat messages so this code
        // is not really used
        ChatPacketReceivedEvent event = new ChatPacketReceivedEvent.Player(packet.unsignedContent());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
            return;
        }

        if (event.isMessageChanged()) {
            this.minecraft
                    .getChatListener()
                    .handlePlayerChatMessage(playerChatMessage, playerInfo.getProfile(), packet.chatType());
            ci.cancel();
        }
    }

    @Inject(
            method = "handleSystemChat(Lnet/minecraft/network/protocol/game/ClientboundSystemChatPacket;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"),
            cancellable = true)
    private void handleSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        Component message = packet.content();
        ChatPacketReceivedEvent event = packet.overlay()
                ? new ChatPacketReceivedEvent.GameInfo(message)
                : new ChatPacketReceivedEvent.System(message);
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
            return;
        }

        if (event.isMessageChanged()) {
            this.minecraft.getChatListener().handleSystemMessage(event.getMessage(), packet.overlay());
            ci.cancel();
        }
    }

    @Inject(
            method = "handleAddObjective(Lnet/minecraft/network/protocol/game/ClientboundSetObjectivePacket;)V",
            at = @At("RETURN"))
    private void handleAddObjective(ClientboundSetObjectivePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        ScoreboardSetObjectiveEvent event = new ScoreboardSetObjectiveEvent(
                packet.getObjectiveName(), packet.getDisplayName(), packet.getRenderType(), packet.getMethod());
        MixinHelper.post(event);
    }

    @Inject(
            method = "handleSetScore(Lnet/minecraft/network/protocol/game/ClientboundSetScorePacket;)V",
            at = @At("RETURN"))
    private void handleSetScore(ClientboundSetScorePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        ScoreboardEvent event =
                new ScoreboardEvent.Set(StyledText.fromString(packet.owner()), packet.objectiveName(), packet.score());
        MixinHelper.post(event);
    }

    @Inject(
            method = "handleResetScore(Lnet/minecraft/network/protocol/game/ClientboundResetScorePacket;)V",
            at = @At("RETURN"))
    private void handleResetScore(ClientboundResetScorePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        ScoreboardEvent event =
                new ScoreboardEvent.Reset(StyledText.fromString(packet.owner()), packet.objectiveName());
        MixinHelper.post(event);
    }

    @Inject(
            method =
                    "handleSetDisplayObjective(Lnet/minecraft/network/protocol/game/ClientboundSetDisplayObjectivePacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        ScoreboardSetDisplayObjectiveEvent event =
                new ScoreboardSetDisplayObjectiveEvent(packet.getSlot(), packet.getObjectiveName());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method =
                    "handleUpdateAdvancementsPacket(Lnet/minecraft/network/protocol/game/ClientboundUpdateAdvancementsPacket;)V",
            at = @At("RETURN"))
    private void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new AdvancementUpdateEvent(
                packet.shouldReset(), packet.getAdded(), packet.getRemoved(), packet.getProgress()));
    }

    @Inject(
            method = "handleUpdateMobEffect(Lnet/minecraft/network/protocol/game/ClientboundUpdateMobEffectPacket;)V",
            at = @At("RETURN"))
    private void handleUpdateMobEffectPost(ClientboundUpdateMobEffectPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (!MixinHelper.onWynncraft()) return;

        MixinHelper.post(new MobEffectEvent.Update(
                McUtils.mc().level.getEntity(packet.getEntityId()),
                packet.getEffect().value(),
                packet.getEffectAmplifier(),
                packet.getEffectDurationTicks()));
    }

    @Inject(
            method = "handleRemoveMobEffect(Lnet/minecraft/network/protocol/game/ClientboundRemoveMobEffectPacket;)V",
            at = @At("RETURN"))
    private void handleRemoveMobEffectPost(ClientboundRemoveMobEffectPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new MobEffectEvent.Remove(
                packet.getEntity(McUtils.mc().level), packet.effect().value()));
    }

    @Inject(
            method = "handleAddEntity(Lnet/minecraft/network/protocol/game/ClientboundAddEntityPacket;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientPacketListener;postAddEntitySoundInstance(Lnet/minecraft/world/entity/Entity;)V",
                            shift = At.Shift.AFTER))
    private void handleAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci, @Local Entity entity) {
        if (!isRenderThread()) return;

        // This mixin is added after the last actual instruction, where the local variable entity
        // still exists.

        MixinHelper.post(new AddEntityEvent(packet, entity));
    }

    @Inject(
            method = "handleTeleportEntity(Lnet/minecraft/network/protocol/game/ClientboundTeleportEntityPacket;)V",
            at = @At("RETURN"))
    private void handleTeleportEntity(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        Entity entity = McUtils.mc().level.getEntity(packet.getId());
        if (entity == null) return;

        Vec3 position = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        MixinHelper.post(new TeleportEntityEvent(entity, position));
    }

    @ModifyArg(
            method = "handleSetEntityData(Lnet/minecraft/network/protocol/game/ClientboundSetEntityDataPacket;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/network/syncher/SynchedEntityData;assignValues(Ljava/util/List;)V"),
            index = 0)
    private List<SynchedEntityData.DataValue<?>> handleSetEntityDataPre(
            List<SynchedEntityData.DataValue<?>> packedItems,
            @Local(argsOnly = true) ClientboundSetEntityDataPacket packet) {
        if (!isRenderThread()) return packedItems;

        SetEntityDataEvent event = new SetEntityDataEvent(packet);
        MixinHelper.post(event);
        return event.getPackedItems();
    }

    @Inject(
            method = "handleRemoveEntities(Lnet/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket;)V",
            at = @At("RETURN"))
    private void handleRemoveEntities(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new RemoveEntitiesEvent(packet));
    }

    @Inject(
            method = "handleSoundEvent(Lnet/minecraft/network/protocol/game/ClientboundSoundPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleSoundEventPre(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        LocalSoundEvent.Client event =
                new LocalSoundEvent.Client(packet.getSound().value(), packet.getSource());

        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleParticleEvent(Lnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket;)V",
            at = @At("HEAD"))
    private void handleParticles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new ParticleAddedEvent(packet));
    }

    @Inject(
            method =
                    "handleLevelChunkWithLight(Lnet/minecraft/network/protocol/game/ClientboundLevelChunkWithLightPacket;)V",
            at = @At("RETURN"))
    private void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(
                new ChunkReceivedEvent(packet.getX(), packet.getZ(), packet.getChunkData(), packet.getLightData()));
    }
}
