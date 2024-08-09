/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.google.common.base.Preconditions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class PlayerInteractEvent extends PlayerEvent {
    private final InteractionHand hand;
    private InteractionResult cancellationResult = InteractionResult.PASS;

    private PlayerInteractEvent(Player player, InteractionHand hand) {
        super(player);
        this.hand = hand;
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public ItemStack getItemStack() {
        return this.getPlayer().getItemInHand(this.hand);
    }

    public Level getWorld() {
        return this.getPlayer().getCommandSenderWorld();
    }

    public InteractionResult getCancellationResult() {
        return this.cancellationResult;
    }

    public void setCancellationResult(InteractionResult result) {
        this.cancellationResult = result;
    }

    public static class RightClickBlock extends PlayerInteractEvent implements ICancellableEvent {
        private final BlockPos pos;
        private final BlockHitResult hitVec;

        public RightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitVec) {
            super(player, hand);
            this.pos = Preconditions.checkNotNull(pos, "Null position in PlayerInteractEvent!");
            this.hitVec = hitVec;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public BlockHitResult getHitVec() {
            return hitVec;
        }
    }

    public static class Interact extends PlayerInteractEvent implements ICancellableEvent {
        private final Entity target;

        public Interact(Player player, InteractionHand hand, Entity target) {
            super(player, hand);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    public static class InteractAt extends Interact implements ICancellableEvent {
        private final EntityHitResult entityHitResult;

        public InteractAt(Player player, InteractionHand hand, Entity target, EntityHitResult entityHitResult) {
            super(player, hand, target);
            this.entityHitResult = entityHitResult;
        }

        public EntityHitResult getEntityHitResult() {
            return entityHitResult;
        }
    }
}
