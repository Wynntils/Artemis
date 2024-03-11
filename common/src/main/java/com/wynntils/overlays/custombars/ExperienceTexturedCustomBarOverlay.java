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
import com.wynntils.utils.render.type.ObjectivesTextures;
import net.minecraft.client.renderer.MultiBufferSource;

public class ExperienceTexturedCustomBarOverlay extends CustomBarOverlayBase {
    @Persisted(i18nKey = "overlay.wynntils.objectivesTexture")
    public final Config<ObjectivesTextures> objectivesTexture = new Config<>(ObjectivesTextures.A);

    public ExperienceTexturedCustomBarOverlay(int id) {
        super(id, new OverlaySize(150, 30));
    }

    @Override
    public Texture getTexture() {
        return Texture.EXPERIENCE_BAR;
    }

    @Override
    protected float getTextureHeight() {
        return 5;
    }

    @Override
    protected void renderBar(
            PoseStack poseStack, MultiBufferSource bufferSource, float renderY, float barHeight, float progress) {
        BufferedRenderUtils.drawProgressBar(
                poseStack,
                bufferSource,
                getTexture(),
                getRenderX(),
                renderY,
                getRenderX() + getWidth(),
                renderY + barHeight,
                0,
                objectivesTexture.get().getTextureY1(),
                Texture.EXPERIENCE_BAR.width(),
                objectivesTexture.get().getTextureY2(),
                progress);
    }

    @Override
    protected BarOverlayTemplatePair getActualPreviewTemplate() {
        return new BarOverlayTemplatePair("{capped_xp}", "capped_xp");
    }
}
