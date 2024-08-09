/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.fabric.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class FabricAbstractContainerScreenMixin {
    // Note: Call site 2 of 3 of ItemTooltipRenderEvent. Check the event class for more info.
    //       See ForgeGuiGraphicsMixin#renderTooltipPre for the Forge mixin.
    @WrapOperation(
            method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V"))
    private void renderTooltipPre(
            GuiGraphics instance,
            Font font,
            List<Component> tooltipLines,
            Optional<TooltipComponent> visualTooltipComponent,
            int mouseX,
            int mouseY,
            Operation<Void> operation,
            @Local ItemStack itemStack) {
        ItemTooltipRenderEvent.Pre event =
                new ItemTooltipRenderEvent.Pre(instance, itemStack, tooltipLines, mouseX, mouseY);
        MixinHelper.post(event);
        if (event.isCanceled()) return;

        operation.call(
                instance,
                font,
                event.getTooltips(),
                event.getItemStack().getTooltipImage(),
                event.getMouseX(),
                event.getMouseY());
    }

    // See ForgeGuiGraphics#renderTooltipPost for the Forge mixin.
    @Inject(
            method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V",
                            shift = At.Shift.AFTER))
    private void renderTooltipPost(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci, @Local ItemStack itemStack) {
        MixinHelper.post(new ItemTooltipRenderEvent.Post(guiGraphics, itemStack, x, y));
    }

    // See the ForgeAbstractContainerScreenMixin#renderSlotPreCount for the Forge mixin.
    @Inject(
            method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void renderSlotPreCount(GuiGraphics guiGraphics, Slot slot, CallbackInfo info) {
        MixinHelper.post(new SlotRenderEvent.CountPre(guiGraphics, (Screen) (Object) this, slot));
    }
}
