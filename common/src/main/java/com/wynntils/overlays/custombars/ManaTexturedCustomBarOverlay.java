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
import com.wynntils.utils.render.type.ManaTexture;
import net.minecraft.client.renderer.MultiBufferSource;

public class ManaTexturedCustomBarOverlay extends CustomBarOverlayBase {
    @Persisted(i18nKey = "overlay.wynntils.manaBar.manaTexture")
    public final Config<ManaTexture> manaTexture = new Config<>(ManaTexture.A);

    public ManaTexturedCustomBarOverlay(int id) {
        super(id, new OverlaySize(81, 21));
    }

    @Override
    public Texture getTexture() {
        return Texture.MANA_BAR;
    }

    @Override
    protected float getTextureHeight() {
        return manaTexture.get().getHeight();
    }

    @Override
    protected void renderBar(
            PoseStack poseStack, MultiBufferSource bufferSource, float renderY, float renderHeight, float progress) {
        BufferedRenderUtils.drawProgressBar(
                poseStack,
                bufferSource,
                Texture.MANA_BAR,
                getRenderX(),
                renderY,
                getRenderX() + getWidth(),
                renderY + renderHeight,
                0,
                manaTexture.get().getTextureY1(),
                Texture.MANA_BAR.width(),
                manaTexture.get().getTextureY2(),
                progress);
    }

    @Override
    protected BarOverlayTemplatePair getActualPreviewTemplate() {
        return new BarOverlayTemplatePair("12/100", "capped(12; 100)");
    }
}
