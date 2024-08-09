/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.EntityNameTagRenderEvent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Shadow
    @Final
    protected EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    public abstract Font getFont();

    @Inject(
            method =
                    "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at =
                    @At(
                            value = "INVOKE_ASSIGN",
                            target = "Lnet/minecraft/client/Options;getBackgroundOpacity(F)F",
                            shift = At.Shift.AFTER),
            cancellable = true)
    private void onNameTagRenderPre(
            T entity,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick,
            CallbackInfo ci,
            @Local(ordinal = 1) LocalFloatRef backgroundOpacity) {
        EntityNameTagRenderEvent event = new EntityNameTagRenderEvent(
                entity,
                displayName,
                poseStack,
                bufferSource,
                packedLight,
                this.entityRenderDispatcher,
                this.getFont(),
                backgroundOpacity.get());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            // We inject in the middle of the method, to have locals, but we need to manually pop the matrix stack now
            poseStack.popPose();
            ci.cancel();
            return;
        }

        backgroundOpacity.set(event.getBackgroundOpacity());
    }
}
