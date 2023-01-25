/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.mc.EventFactory;
import com.wynntils.mc.extension.EntityExtension;
import com.wynntils.utils.colors.CustomColor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
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

    @Inject(at = @At("TAIL"), method = "renderLevel")
    private void renderLevelPost(
            PoseStack poseStack,
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        EventFactory.onRenderLevelPost(
                this.minecraft.levelRenderer, poseStack, partialTick, projectionMatrix, finishNanoTime, camera);
    }

    @Inject(at = @At("HEAD"), method = "renderLevel")
    private void renderLevelPre(
            PoseStack poseStack,
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        EventFactory.onRenderLevelPre(
                this.minecraft.levelRenderer, poseStack, partialTick, projectionMatrix, finishNanoTime, camera);
    }

    @ModifyExpressionValue(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int modifyOutlineColor(int original, @Local Entity entity) {
        EntityExtension entityExt = (EntityExtension) entity;

        if (entityExt.getGlowColor() != CustomColor.NONE) {
            return property.getGlowColorInt();
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
            method = "renderLevel")
    private void renderTilePost(
            PoseStack poseStack,
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        EventFactory.onRenderTileLast(
                this.minecraft.levelRenderer, poseStack, partialTick, projectionMatrix, finishNanoTime, camera);
    }
}
