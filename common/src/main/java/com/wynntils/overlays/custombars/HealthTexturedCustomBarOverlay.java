/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays.custombars;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HealthTexture;
import net.minecraft.client.renderer.MultiBufferSource;

public class HealthTexturedCustomBarOverlay extends CustomBarOverlayBase {
    @Persisted(i18nKey = "overlay.wynntils.healthBar.healthTexture")
    public final Config<HealthTexture> healthTexture = new Config<>(HealthTexture.A);

    public HealthTexturedCustomBarOverlay(int id) {
        super(id, new OverlaySize(81, 21));
    }

    @Override
    public Texture getTexture() {
        return Texture.HEALTH_BAR;
    }

    @Override
    public float getTextureHeight() {
        return healthTexture.get().getHeight();
    }

    @Override
    protected void renderBar(
            PoseStack poseStack, MultiBufferSource bufferSource, float renderY, float renderHeight, float progress) {
        BufferedRenderUtils.drawProgressBar(
                poseStack,
                bufferSource,
                Texture.HEALTH_BAR,
                getRenderX(),
                renderY,
                getRenderX() + getWidth(),
                renderY + renderHeight,
                0,
                healthTexture.get().getTextureY1(),
                Texture.HEALTH_BAR.width(),
                healthTexture.get().getTextureY2(),
                progress);
    }

    @Override
    protected BarOverlayTemplatePair getActualPreviewTemplate() {
        return new BarOverlayTemplatePair("432/1500", "capped(432; 1500)");
    }
}
