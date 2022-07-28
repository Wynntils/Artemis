/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.mc.EventFactory;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Final
    @Shadow
    private List<GuiEventListener> children;

    @Final
    @Shadow
    private List<NarratableEntry> narratables;

    @Final
    @Shadow
    private List<Widget> renderables;

    // Making this public is required for the mixin, use this with caution anywhere else
    public <T extends GuiEventListener & Widget & NarratableEntry> T addRenderableWidget(T widget) {
        renderables.add(widget);
        return addWidget(widget);
    }

    // Making this public is required for the mixin, use this with caution anywhere else
    public <T extends GuiEventListener & NarratableEntry> T addWidget(T listener) {
        children.add(listener);
        narratables.add(listener);
        return listener;
    }

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("RETURN"))
    private void initPost(Minecraft client, int width, int height, CallbackInfo info) {
        Screen screen = (Screen) (Object) this;

        EventFactory.onScreenCreated(screen, this::addRenderableWidget);
    }

    @Redirect(
            method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/Screen;renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;Ljava/util/Optional;II)V"))
    private void renderTooltipPre(
            Screen instance,
            PoseStack poseStack,
            List<Component> tooltips,
            Optional<TooltipComponent> visualTooltipComponent,
            int mouseX,
            int mouseY,
            PoseStack poseStack2,
            ItemStack itemStack,
            int mouseX2,
            int mouseY2) {
        ItemTooltipRenderEvent.Pre e =
                EventFactory.onItemTooltipRenderPre(poseStack, itemStack, tooltips, mouseX, mouseY);
        if (e.isCanceled()) return;
        instance.renderTooltip(
                e.getPoseStack(), e.getTooltips(), e.getItemStack().getTooltipImage(), e.getMouseX(), e.getMouseY());
    }

    @Inject(
            method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("RETURN"))
    private void renderTooltipPost(PoseStack poseStack, ItemStack itemStack, int mouseX, int mouseY, CallbackInfo ci) {
        EventFactory.onItemTooltipRenderPost(poseStack, itemStack, mouseX, mouseY);
    }
}
