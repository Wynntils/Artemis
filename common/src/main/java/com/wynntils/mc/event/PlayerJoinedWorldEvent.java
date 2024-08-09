/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class PlayerJoinedWorldEvent extends Event {
    private final Player player;

    public PlayerJoinedWorldEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
