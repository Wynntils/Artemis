/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.entity.player.Player;

public class PlayerEvent extends LivingEvent {
    private final Player entityPlayer;

    public PlayerEvent(Player player) {
        super(player);
        this.entityPlayer = player;
    }

    public Player getPlayer() {
        return this.entityPlayer;
    }
}
