/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.SetSlotEvent;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @WrapOperation(
            method = "set(Lnet/minecraft/world/item/ItemStack;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void onSetItem(Container container, int slot, ItemStack itemStack, Operation<Void> original) {
        MixinHelper.post(new SetSlotEvent.Pre(container, slot, itemStack));

        original.call(container, slot, itemStack);

        MixinHelper.post(new SetSlotEvent.Post(container, slot, itemStack));
    }
}
