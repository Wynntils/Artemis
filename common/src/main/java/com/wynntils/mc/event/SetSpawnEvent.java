/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class SetSpawnEvent extends Event implements ICancellableEvent {
    private final BlockPos spawnPos;

    public SetSpawnEvent(BlockPos spawnPos) {
        this.spawnPos = spawnPos;
    }

    public BlockPos getSpawnPos() {
        return spawnPos;
    }
}
