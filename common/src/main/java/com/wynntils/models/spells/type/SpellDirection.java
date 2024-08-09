/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.spells.type;

import com.wynntils.utils.mc.McUtils;
import java.util.Arrays;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;

public enum SpellDirection {
    RIGHT(() -> McUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(
            InteractionHand.MAIN_HAND,
            id,
            McUtils.player().getXRot(),
            McUtils.player().getYRot()))),
    LEFT(() -> McUtils.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND)));

    private final Runnable sendPacketRunnable;

    public static final SpellDirection[] NO_SPELL = new SpellDirection[0];

    SpellDirection(Runnable sendPacketRunnable) {
        this.sendPacketRunnable = sendPacketRunnable;
    }

    public Runnable getSendPacketRunnable() {
        return sendPacketRunnable;
    }

    public static SpellDirection[] invertArray(SpellDirection[] initial) {
        return Arrays.stream(initial).map((x) -> (x == RIGHT) ? LEFT : RIGHT).toArray(SpellDirection[]::new);
    }
}
