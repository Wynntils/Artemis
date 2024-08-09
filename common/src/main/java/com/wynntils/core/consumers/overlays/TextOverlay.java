/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.consumers.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.ErrorOr;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * An overlay, which main purpose is to display function templates.
 */
public abstract class TextOverlay extends DynamicOverlay {
    @Persisted(i18nKey = "overlay.wynntils.textOverlay.textShadow")
    public final Config<TextShadow> textShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted(i18nKey = "overlay.wynntils.textOverlay.fontScale")
    public final Config<Float> fontScale = new Config<>(1.0f);

    @Persisted(i18nKey = "overlay.wynntils.textOverlay.enabledTemplate")
    public final Config<String> enabledTemplate = new Config<>("");

    private StyledText[] cachedLines = new StyledText[0];

    protected TextOverlay(OverlayPosition position, float width, float height) {
        super(position, width, height, 1);
    }

    protected TextOverlay(OverlayPosition position, OverlaySize size) {
        super(position, size, 1);
    }

    protected TextOverlay(
            OverlayPosition position,
            OverlaySize size,
            HorizontalAlignment horizontalAlignmentOverride,
            VerticalAlignment verticalAlignmentOverride) {
        super(position, size, horizontalAlignmentOverride, verticalAlignmentOverride, 1);
    }

    protected TextOverlay(
            OverlayPosition position,
            OverlaySize size,
            HorizontalAlignment horizontalAlignmentOverride,
            VerticalAlignment verticalAlignmentOverride,
            int id) {
        super(position, size, horizontalAlignmentOverride, verticalAlignmentOverride, id);
    }

    protected TextOverlay(int id) {
        super(id);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        if (!isRendered()) return;

        renderTemplate(poseStack, bufferSource, cachedLines, getTextScale());
    }

    @Override
    public void renderPreview(
            PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        if (!Models.WorldState.onWorld()) return;

        renderTemplate(poseStack, bufferSource, calculateTemplateValue(getPreviewTemplate()), getTextScale());
    }

    private void renderTemplate(
            PoseStack poseStack, MultiBufferSource bufferSource, StyledText[] lines, float textScale) {
        float renderX = this.getRenderX();
        float renderY = this.getRenderY();
        for (StyledText line : lines) {
            BufferedFontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            bufferSource,
                            line,
                            renderX,
                            renderX + this.getWidth(),
                            renderY,
                            renderY + this.getHeight(),
                            0,
                            this.getRenderColor(),
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment(),
                            this.textShadow.get(),
                            textScale);

            renderY += FontRenderer.getInstance().getFont().lineHeight;
        }
    }

    @Override
    public void tick() {
        if (!Models.WorldState.onWorld()) return;
        cachedLines = calculateTemplateValue(getTemplate());
    }

    protected StyledText[] calculateTemplateValue(String template) {
        return Managers.Function.doFormatLines(template);
    }

    protected CustomColor getRenderColor() {
        return CommonColors.WHITE;
    }

    private float getTextScale() {
        return fontScale.get();
    }

    protected abstract String getTemplate();

    protected abstract String getPreviewTemplate();

    @Override
    protected void onConfigUpdate(Config<?> config) {}

    public final boolean isRendered() {
        // If the enabled template is empty,
        // the overlay is rendered when the player is in the world.
        if (enabledTemplate.get().isEmpty()) return isRenderedDefault();

        // If the enabled template is not empty,
        // the overlay is rendered when the template is true.
        ErrorOr<Boolean> enabledOrError = Managers.Function.tryGetRawValueOfType(enabledTemplate.get(), Boolean.class);
        return !enabledOrError.hasError() && enabledOrError.getValue();
    }

    /**
     * Returns whether the overlay is rendered with the default (empty) template.
     *
     * @return whether the overlay is rendered with the default (empty) template
     */
    public boolean isRenderedDefault() {
        return Models.WorldState.onWorld();
    }
}
