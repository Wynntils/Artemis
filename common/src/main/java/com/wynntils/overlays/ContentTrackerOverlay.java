/*
 * Copyright © Wynntils 2022-2024.
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
import com.wynntils.handlers.scoreboard.event.ScoreboardSegmentAdditionEvent;
import com.wynntils.models.activities.ActivityTrackerScoreboardPart;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class ContentTrackerOverlay extends Overlay {
    public static final String PREVIEW_TASK =
            """
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer \
                    tempus purus in lacus pulvinar dictum. Quisque suscipit erat \
                    pellentesque egestas volutpat. \
                    """;

    @Persisted
    public final Config<Boolean> disableTrackerOnScoreboard = new Config<>(true);

    @Persisted
    public final Config<TextShadow> textShadow = new Config<>(TextShadow.OUTLINE);

    private static final List<CustomColor> TEXT_COLORS =
            List.of(CommonColors.GREEN, CommonColors.ORANGE, CommonColors.WHITE);

    private final List<TextRenderTask> toRender = createRenderTaskList();
    private final List<TextRenderTask> toRenderPreview = createRenderTaskList();

    public ContentTrackerOverlay() {
        super(
                new OverlayPosition(
                        5,
                        -5,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.RIGHT,
                        OverlayPosition.AnchorSection.TOP_RIGHT),
                new OverlaySize(300, 50),
                HorizontalAlignment.LEFT,
                VerticalAlignment.MIDDLE);

        toRenderPreview
                .get(0)
                .setText(I18n.get("feature.wynntils.contentTrackerOverlay.overlay.contentTracker.title") + " Quest:");
        toRenderPreview
                .get(1)
                .setText(I18n.get("feature.wynntils.contentTrackerOverlay.overlay.contentTracker.testQuestName") + ":");
        toRenderPreview.get(2).setText(PREVIEW_TASK);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScoreboardSegmentChange(ScoreboardSegmentAdditionEvent event) {
        if (disableTrackerOnScoreboard.get()
                && event.getSegment().getScoreboardPart() instanceof ActivityTrackerScoreboardPart) {
            event.setCanceled(true);
        }
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        updateTextRenderSettings(toRender);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        if (!Models.Activity.isTracking()) {
            return;
        }

        toRender.get(0)
                .setText(I18n.get("feature.wynntils.contentTrackerOverlay.overlay.contentTracker.title") + " "
                        + Models.Activity.getTrackedType().getDisplayName() + ":");
        toRender.get(1).setText(Models.Activity.getTrackedName());
        toRender.get(2).setText(Models.Activity.getTrackedTask());

        BufferedFontRenderer.getInstance()
                .renderTextsWithAlignment(
                        poseStack,
                        bufferSource,
                        this.getRenderX(),
                        this.getRenderY(),
                        toRender,
                        this.getWidth(),
                        this.getHeight(),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment());
    }

    @Override
    public void renderPreview(
            PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        updateTextRenderSettings(toRenderPreview); // we have to force update every time

        BufferedFontRenderer.getInstance()
                .renderTextsWithAlignment(
                        poseStack,
                        bufferSource,
                        this.getRenderX(),
                        this.getRenderY(),
                        toRenderPreview,
                        this.getWidth(),
                        this.getHeight(),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment());
    }

    private List<TextRenderTask> createRenderTaskList() {
        List<TextRenderTask> renderTaskList = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            renderTaskList.add(new TextRenderTask(
                    StyledText.EMPTY,
                    TextRenderSetting.DEFAULT
                            .withMaxWidth(this.getWidth())
                            .withCustomColor(TEXT_COLORS.get(i))
                            .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                            .withTextShadow(this.textShadow.get())));
        }
        return renderTaskList;
    }

    private void updateTextRenderSettings(List<TextRenderTask> renderTasks) {
        for (int i = 0; i < 3; i++) {
            renderTasks
                    .get(i)
                    .setSetting(TextRenderSetting.DEFAULT
                            .withMaxWidth(this.getWidth())
                            .withCustomColor(TEXT_COLORS.get(i))
                            .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                            .withTextShadow(this.textShadow.get()));
        }
    }
}
