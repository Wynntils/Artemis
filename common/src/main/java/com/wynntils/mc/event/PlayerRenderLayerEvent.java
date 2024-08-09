/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class PlayerRenderLayerEvent extends Event implements ICancellableEvent {
    private final Player player;

    protected PlayerRenderLayerEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public static class Armor extends PlayerRenderLayerEvent {
        private final EquipmentSlot slot;

        public Armor(Player player, EquipmentSlot slot) {
            super(player);
            this.slot = slot;
        }

        public EquipmentSlot getSlot() {
            return slot;
        }
    }

    public static class Cape extends PlayerRenderLayerEvent {
        public Cape(Player player) {
            super(player);
        }
    }

    public static class Elytra extends PlayerRenderLayerEvent {
        public Elytra(Player player) {
            super(player);
        }
    }
}
