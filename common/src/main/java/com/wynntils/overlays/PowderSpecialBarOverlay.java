/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.characterstats.type.PowderSpecialInfo;
import com.wynntils.models.elements.type.Powder;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.wynn.ItemUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;

public class PowderSpecialBarOverlay extends Overlay {
    @Persisted
    public final Config<TextShadow> textShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> flip = new Config<>(false);

    @Persisted
    public final Config<Boolean> onlyIfWeaponHeld = new Config<>(true);

    @Persisted
    public final Config<Boolean> hideIfNoCharge = new Config<>(true);

    public PowderSpecialBarOverlay() {
        super(
                new OverlayPosition(
                        -30,
                        150,
                        VerticalAlignment.BOTTOM,
                        HorizontalAlignment.CENTER,
                        OverlayPosition.AnchorSection.BOTTOM_MIDDLE),
                new OverlaySize(81, 21));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        PowderSpecialInfo powderSpecialInfo = Models.CharacterStats.getPowderSpecialInfo();
        if (this.onlyIfWeaponHeld.get()
                && !ItemUtils.isWeapon(McUtils.inventory().getSelected())) return;
        if (this.hideIfNoCharge.get()
                && (powderSpecialInfo == PowderSpecialInfo.EMPTY || powderSpecialInfo.charge() == 0f)) return;

        renderWithSpecificSpecial(
                poseStack, bufferSource, powderSpecialInfo.charge() * 100f, powderSpecialInfo.powder());
    }

    @Override
    public void renderPreview(
            PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        renderWithSpecificSpecial(poseStack, bufferSource, 40, Powder.THUNDER);
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {}

    private void renderWithSpecificSpecial(
            PoseStack poseStack, MultiBufferSource bufferSource, float powderSpecialCharge, Powder powderSpecialType) {
        Texture universalBarTexture = Texture.UNIVERSAL_BAR;

        final float renderedHeight = universalBarTexture.height() / 2f * (this.getWidth() / 81);

        float renderY =
                switch (this.getRenderVerticalAlignment()) {
                    case TOP -> this.getRenderY();
                    case MIDDLE -> this.getRenderY() + (this.getHeight() - renderedHeight) / 2;
                    case BOTTOM -> this.getRenderY() + this.getHeight() - renderedHeight;
                };

        CustomColor color;
        StyledText text;
        if (powderSpecialType == null) {
            color = CommonColors.GRAY;
            text = StyledText.fromString("Unknown");
        } else {
            color = powderSpecialType.getColor();
            text = StyledText.fromString(
                    powderSpecialType.getColoredSymbol().getString() + " " + (int) powderSpecialCharge + "%");
        }

        BufferedFontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        bufferSource,
                        text,
                        this.getRenderX(),
                        this.getRenderX() + this.getWidth(),
                        renderY,
                        0,
                        color,
                        this.getRenderHorizontalAlignment(),
                        this.textShadow.get());

        BufferedRenderUtils.drawColoredProgressBar(
                poseStack,
                bufferSource,
                universalBarTexture,
                color,
                this.getRenderX(),
                renderY + 10,
                this.getRenderX() + this.getWidth(),
                renderY + 10 + renderedHeight,
                0,
                0,
                universalBarTexture.width(),
                universalBarTexture.height(),
                (this.flip.get() ? -1f : 1f) * powderSpecialCharge / 100f);
    }
}
