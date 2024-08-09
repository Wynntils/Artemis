/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.HotbarSlotRenderEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method =
                    "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("HEAD"))
    private void renderSlotPre(
            GuiGraphics guiGraphics,
            int x,
            int y,
            DeltaTracker deltaTracker,
            Player player,
            ItemStack itemStack,
            int i,
            CallbackInfo info) {
        MixinHelper.post(new HotbarSlotRenderEvent.Pre(guiGraphics, itemStack, x, y));
    }

    @Inject(
            method =
                    "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void renderSlotCountPre(
            GuiGraphics guiGraphics,
            int x,
            int y,
            DeltaTracker deltaTracker,
            Player player,
            ItemStack itemStack,
            int i,
            CallbackInfo info) {
        MixinHelper.post(new HotbarSlotRenderEvent.CountPre(guiGraphics, itemStack, x, y));
    }

    @Inject(
            method =
                    "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("RETURN"))
    private void renderSlotPost(
            GuiGraphics guiGraphics,
            int x,
            int y,
            DeltaTracker deltaTracker,
            Player player,
            ItemStack itemStack,
            int i,
            CallbackInfo info) {
        MixinHelper.post(new HotbarSlotRenderEvent.Post(guiGraphics, itemStack, x, y));
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"))
    private void onRenderGuiPre(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // FIXME: This is a temporary fix. We should integrate overlays into Gui's LayeredDraw order
        if (McUtils.options().hideGui) return;
        MixinHelper.post(new RenderEvent.Pre(
                guiGraphics, deltaTracker, this.minecraft.getWindow(), RenderEvent.ElementType.GUI));
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("RETURN"))
    private void onRenderGuiPost(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // FIXME: This is a temporary fix. We should integrate overlays into Gui's LayeredDraw order
        if (McUtils.options().hideGui) return;
        MixinHelper.post(new RenderEvent.Post(
                guiGraphics, deltaTracker, this.minecraft.getWindow(), RenderEvent.ElementType.GUI));
    }

    @Inject(
            method = "renderVehicleHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onRenderVehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!MixinHelper.onWynncraft()) return;

        // On Wynncraft we always cancel vehicle health; it has no purpose and it interfers
        // with our foodbar event above
        ci.cancel();
    }

    @Inject(
            method = "renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onRenderCrosshairPre(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvent.Pre event = new RenderEvent.Pre(
                guiGraphics, deltaTracker, this.minecraft.getWindow(), RenderEvent.ElementType.CROSSHAIR);
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method =
                    "renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onRenderHeartsPre(
            GuiGraphics guiGraphics,
            Player player,
            int x,
            int y,
            int height,
            int offsetHeartIndex,
            float maxHealth,
            int currentHealth,
            int displayHealth,
            int absorptionAmount,
            boolean renderHighlight,
            CallbackInfo ci) {
        if (!MixinHelper.onWynncraft()) return;

        RenderEvent.Pre event = new RenderEvent.Pre(
                guiGraphics, DeltaTracker.ZERO, this.minecraft.getWindow(), RenderEvent.ElementType.HEALTH_BAR);
        MixinHelper.post(event);

        if (event.isCanceled()) {
            ci.cancel();
        }
    }
}
