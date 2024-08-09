/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.neoforge.mixins;

import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.SlotRenderEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ForgeAbstractContainerScreenMixin {
    // This mixin replaces AbstractContainerScreenMixin#renderSlotPreCount.
    @Inject(
            method =
                    "renderSlotContents(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Ljava/lang/String;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"),
            remap = false)
    private void renderSlotPreCount(
            GuiGraphics guiGraphics, ItemStack itemstack, Slot slot, String countString, CallbackInfo ci) {
        MixinHelper.post(new SlotRenderEvent.CountPre(guiGraphics, (Screen) (Object) this, slot));
    }
}
