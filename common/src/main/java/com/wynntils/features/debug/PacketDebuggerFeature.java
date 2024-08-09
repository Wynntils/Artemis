/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.PacketEvent.PacketReceivedEvent;
import com.wynntils.mc.event.PacketEvent.PacketSentEvent;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@StartDisabled
@ConfigCategory(Category.DEBUG)
public class PacketDebuggerFeature extends Feature {
    /* These packets just spam the log; ignore them. */
    private static final List<Class<? extends Packet<?>>> IGNORE_LIST = List.of(
            // General
            ServerboundKeepAlivePacket.class,
            ClientboundKeepAlivePacket.class,
            ClientboundSetTimePacket.class,
            ClientboundUpdateAdvancementsPacket.class,
            ClientboundUpdateAttributesPacket.class,
            ClientboundLevelParticlesPacket.class,
            ClientboundPlayerInfoUpdatePacket.class,
            ClientboundSetEquipmentPacket.class,
            // Chunks
            ClientboundForgetLevelChunkPacket.class,
            ClientboundLightUpdatePacket.class,
            ClientboundSetChunkCacheCenterPacket.class,
            ClientboundLevelChunkWithLightPacket.class,
            ClientboundSectionBlocksUpdatePacket.class,
            ClientboundBlockUpdatePacket.class,
            // Entities
            ClientboundAddEntityPacket.class,
            ClientboundMoveEntityPacket.Pos.class,
            ClientboundMoveEntityPacket.PosRot.class,
            ClientboundMoveEntityPacket.Rot.class,
            ClientboundRotateHeadPacket.class,
            ClientboundSetEntityDataPacket.class,
            ClientboundSetEntityMotionPacket.class,
            ClientboundTeleportEntityPacket.class,
            // Client movement
            ServerboundMovePlayerPacket.Pos.class,
            ServerboundMovePlayerPacket.PosRot.class,
            ServerboundMovePlayerPacket.Rot.class);

    private static final List<Class<? extends Packet<?>>> CONTAINER_PACKETS = List.of(
            // S2C
            ClientboundContainerSetContentPacket.class,
            ClientboundContainerSetSlotPacket.class,
            ClientboundContainerSetDataPacket.class,
            ClientboundContainerSetSlotPacket.class,

            // C2S
            ServerboundContainerClickPacket.class,
            ServerboundContainerClickPacket.class,
            ServerboundContainerButtonClickPacket.class,
            ServerboundContainerClosePacket.class);

    private static final Class<? extends Packet<?>> PARTICLE_PACKET_CLASS = ClientboundLevelParticlesPacket.class;

    private static final List<Class<? extends Packet<?>>> ENTITY_PACKETS = List.of(
            ClientboundAddEntityPacket.class,
            ClientboundRemoveEntitiesPacket.class,
            ClientboundMoveEntityPacket.Pos.class,
            ClientboundMoveEntityPacket.PosRot.class,
            ClientboundMoveEntityPacket.Rot.class,
            ClientboundRotateHeadPacket.class,
            ClientboundSetEntityDataPacket.class,
            ClientboundSetEntityMotionPacket.class,
            ClientboundTeleportEntityPacket.class);

    @Persisted
    private final Config<PacketFilterType> packetFilterType = new Config<>(PacketFilterType.FILTERED);

    private String describePacket(Packet<?> packet) {
        return ReflectionToStringBuilder.toString(packet, ToStringStyle.SHORT_PREFIX_STYLE)
                .replaceFirst("net\\.minecraft\\.network\\.protocol\\..*\\.", "");
    }

    @SubscribeEvent
    public void onPacketSent(PacketSentEvent<?> e) {
        Packet<?> packet = e.getPacket();
        if (packetFilterType.get().isPacketExcluded(packet.getClass())) return;

        WynntilsMod.info("SENT packet: " + describePacket(packet));
    }

    @SubscribeEvent
    public void onPacketReceived(PacketReceivedEvent<?> e) {
        Packet<?> packet = e.getPacket();
        if (packetFilterType.get().isPacketExcluded(packet.getClass())) return;

        WynntilsMod.info("RECV packet: " + describePacket(packet));
    }

    private enum PacketFilterType {
        ALL(packetClass -> false),
        FILTERED(IGNORE_LIST::contains),
        CONTAINER_ONLY(packetClass -> !CONTAINER_PACKETS.contains(packetClass)),
        PARTICLE_ONLY(packetClass -> !PARTICLE_PACKET_CLASS.equals(packetClass)),
        ENTITY_ONLY(packetClass -> !ENTITY_PACKETS.contains(packetClass));

        private final Predicate<Class<? extends Packet>> filterPredicate;

        PacketFilterType(Predicate<Class<? extends Packet>> filterPredicate) {
            this.filterPredicate = filterPredicate;
        }

        // True if a packet is filtered out
        public boolean isPacketExcluded(Class<? extends Packet> packetClass) {
            return filterPredicate.test(packetClass);
        }
    }
}
