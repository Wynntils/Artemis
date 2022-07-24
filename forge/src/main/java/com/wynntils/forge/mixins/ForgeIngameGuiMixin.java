/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.forge.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.mc.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeIngameGui.class)
public abstract class ForgeIngameGuiMixin {

    // This is kinda hacky, but we can't shadow minecraft here,
    // so we have to use the instance.

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void onRenderGuiPre(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        EventFactory.onRenderGuiPre(
                poseStack, partialTick, Minecraft.getInstance().getWindow());
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    public void onRenderGuiPost(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        EventFactory.onRenderGuiPost(
                poseStack, partialTick, Minecraft.getInstance().getWindow());
    }

    @Inject(method = "renderFood", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void onRenderFoodPre(int width, int height, PoseStack poseStack, CallbackInfo ci) {
        if (EventFactory.onRenderFoodPre(poseStack, Minecraft.getInstance().getWindow())
                .isCanceled()) {
            ci.cancel();
        }
    }
}
