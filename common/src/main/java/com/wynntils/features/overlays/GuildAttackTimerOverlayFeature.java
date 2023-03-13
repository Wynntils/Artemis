/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.config.ConfigInfo;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.features.overlays.sizes.GuiScaledOverlaySize;
import com.wynntils.handlers.scoreboard.event.ScoreboardSegmentAdditionEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.models.territories.TerritoryAttackTimer;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Comparator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.OVERLAYS)
public class GuildAttackTimerOverlayFeature extends UserFeature {
    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final TerritoryAttackTimerOverlay territoryAttackTimerOverlay = new TerritoryAttackTimerOverlay();

    @ConfigInfo
    public Config<Boolean> disableAttackTimersOnScoreboard = new Config<>(true);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScoreboardSegmentChange(ScoreboardSegmentAdditionEvent event) {
        if (disableAttackTimersOnScoreboard) {
            if (Models.GuildAttackTimer.isGuildAttackSegment(event.getSegment())
                    && territoryAttackTimerOverlay.isEnabled()) {
                event.setCanceled(true);
            }
        }
    }

    public static class TerritoryAttackTimerOverlay extends Overlay {
        @ConfigInfo
        public Config<TextShadow> textShadow = new Config<>(TextShadow.OUTLINE);

        private TextRenderSetting textRenderSetting;

        protected TerritoryAttackTimerOverlay() {
            super(
                    new OverlayPosition(
                            165,
                            -5,
                            VerticalAlignment.Top,
                            HorizontalAlignment.Right,
                            OverlayPosition.AnchorSection.TopRight),
                    new GuiScaledOverlaySize(200, 110));

            updateTextRenderSetting();
        }

        @Override
        public void render(
                PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTicks, Window window) {
            BufferedFontRenderer.getInstance()
                    .renderTextsWithAlignment(
                            poseStack,
                            bufferSource,
                            this.getRenderX(),
                            this.getRenderY(),
                            Models.GuildAttackTimer.getAttackTimers().stream()
                                    .sorted(Comparator.comparing(TerritoryAttackTimer::asSeconds))
                                    .map(territoryAttackTimer ->
                                            new TextRenderTask(territoryAttackTimer.asString(), textRenderSetting))
                                    .toList(),
                            this.getWidth(),
                            this.getHeight(),
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment());
        }

        @Override
        public void renderPreview(
                PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTicks, Window window) {
            BufferedFontRenderer.getInstance()
                    .renderTextWithAlignment(
                            poseStack,
                            bufferSource,
                            this.getRenderX(),
                            this.getRenderY(),
                            new TextRenderTask(
                                    ChatFormatting.GRAY + "Detlas" + ChatFormatting.YELLOW + " (High)"
                                            + ChatFormatting.AQUA + " 02:31",
                                    textRenderSetting),
                            this.getWidth(),
                            this.getHeight(),
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment());
        }

        @Override
        protected void onConfigUpdate(ConfigHolder configHolder) {
            updateTextRenderSetting();
        }

        private void updateTextRenderSetting() {
            textRenderSetting = TextRenderSetting.DEFAULT
                    .withMaxWidth(this.getWidth())
                    .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                    .withTextShadow(textShadow);
        }
    }
}
