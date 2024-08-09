/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.neoforged.bus.api.Event;

public class ChunkReceivedEvent extends Event {
    private final int chunkX;
    private final int chunkZ;
    private final ClientboundLevelChunkPacketData chunkData;
    private final ClientboundLightUpdatePacketData lightData;

    public ChunkReceivedEvent(
            int chunkX,
            int chunkZ,
            ClientboundLevelChunkPacketData chunkData,
            ClientboundLightUpdatePacketData lightData) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkData = chunkData;
        this.lightData = lightData;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public ClientboundLevelChunkPacketData getChunkData() {
        return chunkData;
    }

    public ClientboundLightUpdatePacketData getLightData() {
        return lightData;
    }
}
