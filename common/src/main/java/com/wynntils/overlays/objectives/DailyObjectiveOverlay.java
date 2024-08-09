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
import com.wynntils.models.objectives.DailyObjectiveScoreboardPart;
import com.wynntils.models.objectives.WynnObjective;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class DailyObjectiveOverlay extends ObjectiveOverlayBase {
    @Persisted
    public final Config<Boolean> disableObjectiveTrackingOnScoreboard = new Config<>(true);

    @Persisted(i18nKey = "feature.wynntils.objectivesOverlay.overlay.objectiveOverlayBase.textColor")
    public final Config<CustomColor> textColor = new Config<>(CommonColors.GREEN);

    public DailyObjectiveOverlay() {
        super(
                new OverlayPosition(
                        -35,
                        -5,
                        VerticalAlignment.BOTTOM,
                        HorizontalAlignment.RIGHT,
                        OverlayPosition.AnchorSection.BOTTOM_RIGHT),
                new OverlaySize(150, 100),
                HorizontalAlignment.LEFT,
                VerticalAlignment.BOTTOM);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScoreboardSegmentChange(ScoreboardSegmentAdditionEvent event) {
        if (disableObjectiveTrackingOnScoreboard.get()
                && event.getSegment().getScoreboardPart() instanceof DailyObjectiveScoreboardPart) {
            event.setCanceled(true);
            return;
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        List<WynnObjective> objectives = Models.Objectives.getPersonalObjectives();

        if (objectives.isEmpty()) return;

        final int barHeight = this.enableProgressBar.get() ? 5 : 0;
        final int barWidth = 182;
        final float actualBarHeight = barHeight * (this.getWidth() / barWidth);
        final float renderedHeightWithoutTextHeight = SPACE_BETWEEN + actualBarHeight;

        int tempHeight = 0;
        for (WynnObjective objective : objectives) {
            tempHeight += FontRenderer.getInstance()
                    .calculateRenderHeight(objective.asObjectiveString(), (int) this.getWidth());
        }

        final float fullHeight = renderedHeightWithoutTextHeight * objectives.size() - SPACE_BETWEEN + tempHeight;

        float offsetY =
                switch (this.getRenderVerticalAlignment()) {
                    case TOP -> 0;
                    case MIDDLE -> (this.getHeight() - fullHeight) / 2f;
                    case BOTTOM -> this.getHeight() - fullHeight;
                };

        for (WynnObjective objective : objectives) {
            if (this.hideOnInactivity.get()) {
                final int maxInactivityMs = 3000;
                if (objective.getUpdatedAt() + maxInactivityMs < System.currentTimeMillis()) {
                    continue;
                }
            }

            float renderY = offsetY + this.getRenderY();

            final String text = objective.asObjectiveString();
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

            final float textHeight = FontRenderer.getInstance().calculateRenderHeight(text, (int) this.getWidth());

            if (textHeight > 9) {
                renderY += textHeight - 9;
            }

            if (this.enableProgressBar.get()) {
                BufferedRenderUtils.drawProgressBar(
                        poseStack,
                        bufferSource,
                        Texture.EXPERIENCE_BAR,
                        this.getRenderX(),
                        renderY + SPACE_BETWEEN,
                        this.getRenderX() + this.getWidth(),
                        renderY + SPACE_BETWEEN + actualBarHeight,
                        0,
                        objectivesTexture.get().getTextureY1(),
                        barWidth,
                        objectivesTexture.get().getTextureY2(),
                        objective.getProgress());
            }

            offsetY += renderedHeightWithoutTextHeight + textHeight;
        }
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {}
}
