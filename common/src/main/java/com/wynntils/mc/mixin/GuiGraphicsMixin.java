/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.ItemCountOverlayRenderEvent;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.mc.event.TooltipRenderEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    // Note: Call site 1 of 3 of ItemTooltipRenderEvent. Check the event class for more info.
    //       This mixin works on Fabric, and on Forge as well.
    @WrapOperation(
            method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
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
            @Local(argsOnly = true) ItemStack itemStack) {
        ItemTooltipRenderEvent.Pre event = new ItemTooltipRenderEvent.Pre(
                (GuiGraphics) (Object) this,
                itemStack,
                Screen.getTooltipFromItem(McUtils.mc(), itemStack),
                mouseX,
                mouseY);
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

    @Inject(
            method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("RETURN"))
    private void renderTooltipPost(Font font, ItemStack itemStack, int mouseX, int mouseY, CallbackInfo ci) {
        MixinHelper.post(new ItemTooltipRenderEvent.Post((GuiGraphics) (Object) this, itemStack, mouseX, mouseY));
    }

    @WrapOperation(
            method =
                    "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"))
    private Vector2ic renderTooltipInternalPositioning(
            ClientTooltipPositioner instance,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY,
            int tooltipWidth,
            int tooltipHeight,
            Operation<Vector2ic> operation) {
        TooltipRenderEvent event = new TooltipRenderEvent();
        MixinHelper.post(event);

        if (event.getPositioner() != null) {
            return event.getPositioner()
                    .positionTooltip(screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight);
        }

        return operation.call(instance, screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight);
    }

    @ModifyVariable(
            method =
                    "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private String renderItemDecorations(
            String text,
            Font font,
            ItemStack itemStack,
            int x,
            int y,
            String ignored,
            @Share("wynntilsCountOverlayColor") LocalIntRef wynntilsCountOverlayColor) {
        if (!MixinHelper.onWynncraft()) {
            wynntilsCountOverlayColor.set(0xFFFFFF);
            return text;
        }

        String count = (itemStack.getCount() == 1) ? "" : String.valueOf(itemStack.getCount());
        String countString = (text == null) ? count : text;

        ItemCountOverlayRenderEvent event = new ItemCountOverlayRenderEvent(itemStack, countString, 0xFFFFFF);
        MixinHelper.post(event);
        wynntilsCountOverlayColor.set(event.getCountColor());

        return event.getCountString();
    }

    @WrapOperation(
            method =
                    "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"))
    private int changeCountOverlayColor(
            GuiGraphics guiGraphics,
            Font font,
            String text,
            int x,
            int y,
            int color,
            boolean dropShadow,
            Operation<Integer> original,
            @Share("wynntilsCountOverlayColor") LocalIntRef wynntilsCountOverlayColor) {
        return original.call(guiGraphics, font, text, x, y, wynntilsCountOverlayColor.get(), dropShadow);
    }
}
