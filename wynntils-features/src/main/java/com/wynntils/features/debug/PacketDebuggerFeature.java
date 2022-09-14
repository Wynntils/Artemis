/*
 * Copyright © Wynntils 2021-2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.features.DebugFeature;
import com.wynntils.mc.event.PacketEvent.PacketReceivedEvent;
import com.wynntils.mc.event.PacketEvent.PacketSentEvent;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PacketDebuggerFeature extends DebugFeature {
    private static final boolean DEBUG_PACKETS = false;

    /* These packets just spam the log; ignore them. */
    private static final List<Class<? extends Packet<?>>> IGNORE_LIST = Arrays.asList(
            // General
            ServerboundKeepAlivePacket.class,
            ClientboundKeepAlivePacket.class,
            ClientboundSetTimePacket.class,
            ClientboundUpdateAdvancementsPacket.class,
            ClientboundUpdateAttributesPacket.class,
            ClientboundLevelParticlesPacket.class,
            ClientboundPlayerInfoPacket.class,
            ClientboundSetEquipmentPacket.class,
            // Chunks
            ClientboundForgetLevelChunkPacket.class,
            ClientboundLightUpdatePacket.class,
            ClientboundSetChunkCacheCenterPacket.class,
            ClientboundLevelChunkWithLightPacket.class,
            // Entities
            ClientboundAddEntityPacket.class,
            ClientboundAddMobPacket.class,
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

    private String describePacket(Packet<?> packet) {
        return ReflectionToStringBuilder.toString(packet, ToStringStyle.SHORT_PREFIX_STYLE)
                .replaceFirst("net\\.minecraft\\.network\\.protocol\\..*\\.", "");
    }

    @SubscribeEvent
    public void onPacketSent(PacketSentEvent<?> e) {
        if (!DEBUG_PACKETS) return;

        Packet<?> packet = e.getPacket();
        if (IGNORE_LIST.contains(packet.getClass())) return;

        WynntilsMod.info("SENT packet: " + describePacket(packet));
    }

    @SubscribeEvent
    public void onPacketReceived(PacketReceivedEvent<?> e) {
        if (!DEBUG_PACKETS) return;

        Packet<?> packet = e.getPacket();
        if (IGNORE_LIST.contains(packet.getClass())) return;

        WynntilsMod.info("RECV packet: " + describePacket(packet));
    }
}
