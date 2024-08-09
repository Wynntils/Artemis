/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.RenderLevelEvent;
import com.wynntils.mc.event.RenderTileLevelLastEvent;
import com.wynntils.mc.extension.EntityExtension;
import com.wynntils.utils.colors.CustomColor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            at = @At("TAIL"),
            method =
                    "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V")
    private void renderLevelPost(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f viewMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        // No PoseStack is provided here, as it'd be just an empty stack.
        MixinHelper.post(
                new RenderLevelEvent.Post(this.minecraft.levelRenderer, deltaTracker, projectionMatrix, camera));
    }

    @Inject(
            at = @At("HEAD"),
            method =
                    "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V")
    private void renderLevelPre(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f viewMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        MixinHelper.post(
                new RenderLevelEvent.Pre(this.minecraft.levelRenderer, deltaTracker, projectionMatrix, camera));
    }

    @ModifyExpressionValue(
            method =
                    "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int modifyOutlineColor(int original, @Local Entity entity) {
        EntityExtension entityExt = (EntityExtension) entity;

        if (entityExt.getGlowColor() != CustomColor.NONE) {
            return entityExt.getGlowColorInt();
        }

        return original;
    }

    @Inject(
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
                            ordinal = 2),
            method =
                    "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V")
    private void renderTilePost(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f viewMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci,
            @Local PoseStack poseStack) {
        MixinHelper.post(new RenderTileLevelLastEvent(
                this.minecraft.levelRenderer, poseStack, deltaTracker, projectionMatrix, camera));
    }

    @Inject(
            at = @At("HEAD"),
            method =
                    "renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            cancellable = true)
    public void renderEntity(
            Entity entity,
            double camX,
            double camY,
            double camZ,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CallbackInfo ci) {
        if (!((EntityExtension) entity).isRendered()) {
            ci.cancel();
        }
    }
}
