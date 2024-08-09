/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays.objectives;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.scoreboard.event.ScoreboardSegmentAdditionEvent;
import com.wynntils.models.objectives.GuildObjectiveScoreboardPart;
import com.wynntils.models.objectives.WynnObjective;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class GuildObjectiveOverlay extends ObjectiveOverlayBase {
    @Persisted
    public final Config<Boolean> disableObjectiveTrackingOnScoreboard = new Config<>(true);

    @Persisted(i18nKey = "feature.wynntils.objectivesOverlay.overlay.objectiveOverlayBase.textColor")
    public final Config<CustomColor> textColor = new Config<>(CommonColors.LIGHT_BLUE);

    public GuildObjectiveOverlay() {
        super(
                new OverlayPosition(
                        -5.5f,
                        -5,
                        VerticalAlignment.BOTTOM,
                        HorizontalAlignment.RIGHT,
                        OverlayPosition.AnchorSection.BOTTOM_RIGHT),
                new OverlaySize(150, 30),
                HorizontalAlignment.LEFT,
                VerticalAlignment.MIDDLE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScoreboardSegmentChange(ScoreboardSegmentAdditionEvent event) {
        if (disableObjectiveTrackingOnScoreboard.get()
                && event.getSegment().getScoreboardPart() instanceof GuildObjectiveScoreboardPart) {
            event.setCanceled(true);
            return;
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        WynnObjective guildObjective = Models.Objectives.getGuildObjective();

        if (guildObjective == null) return;

        if (this.hideOnInactivity.get()) {
            final int maxInactivityMs = 3000;
            if (guildObjective.getUpdatedAt() + maxInactivityMs < System.currentTimeMillis()) {
                return;
            }
        }

        final int barHeight = this.enableProgressBar.get() ? 5 : 0;
        final int barWidth = 182;
        final float actualBarHeight = barHeight * (this.getWidth() / barWidth);
        final float renderedHeight =
                FontRenderer.getInstance().calculateRenderHeight(guildObjective.asObjectiveString(), this.getWidth())
                        + actualBarHeight;

        float renderY = this.getRenderY()
                + switch (this.getRenderVerticalAlignment()) {
                    case TOP -> 0;
                    case MIDDLE -> (this.getHeight() - renderedHeight) / 2f;
                    case BOTTOM -> this.getHeight() - renderedHeight;
                };

        final String text = guildObjective.asObjectiveString();
        BufferedFontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        bufferSource,
                        StyledText.fromString(text),
                        this.getRenderX(),
                        this.getRenderX() + this.getWidth(),
                        renderY,
                        this.getWidth(),
                        this.textColor.get(),
                        this.getRenderHorizontalAlignment(),
                        this.textShadow.get());

        float height = FontRenderer.getInstance().calculateRenderHeight(text, this.getWidth());

        if (height > 9) {
            renderY += height - 9;
        }

        if (this.enableProgressBar.get()) {
            BufferedRenderUtils.drawProgressBar(
                    poseStack,
                    bufferSource,
                    Texture.BUBBLE_BAR,
                    this.getRenderX(),
                    renderY + SPACE_BETWEEN,
                    this.getRenderX() + this.getWidth(),
                    renderY + SPACE_BETWEEN + actualBarHeight,
                    0,
                    objectivesTexture.get().getTextureY1(),
                    barWidth,
                    objectivesTexture.get().getTextureY2(),
                    guildObjective.getProgress());
        }
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {}
}
