/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.features.overlays.sizes.GuiScaledOverlaySize;
import com.wynntils.core.features.overlays.sizes.OverlaySize;
import com.wynntils.core.features.properties.FeatureCategory;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.managers.Model;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.gui.render.VerticalAlignment;
import com.wynntils.mc.event.CustomBarAddEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.objects.CommonColors;
import com.wynntils.mc.objects.CustomColor;
import com.wynntils.wynn.event.ActionBarMessageUpdateEvent;
import com.wynntils.wynn.model.ActionBarModel;
import com.wynntils.wynn.model.bossbar.BossBarModel;
import com.wynntils.wynn.objects.HealthTexture;
import com.wynntils.wynn.objects.ManaTexture;
import com.wynntils.wynn.utils.WynnUtils;
import java.util.List;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(category = FeatureCategory.OVERLAYS)
public class CustomBarsOverlayFeature extends UserFeature {

    @Override
    public List<Class<? extends Model>> getModelDependencies() {
        return List.of(ActionBarModel.class, BossBarModel.class);
    }

    @SubscribeEvent
    public void onActionBarManaUpdate(ActionBarMessageUpdateEvent.ManaText event) {
        if (!manaBarOverlay.isEnabled() || manaBarOverlay.shouldDisplayOriginal) return;

        event.setMessage("");
    }

    @SubscribeEvent
    public void onActionBarHealthUpdate(ActionBarMessageUpdateEvent.HealthText event) {
        if (!healthBarOverlay.isEnabled() || healthBarOverlay.shouldDisplayOriginal) return;

        event.setMessage("");
    }

    @SubscribeEvent
    public void onBossBarAdd(CustomBarAddEvent event) {
        BaseBarOverlay overlay =
                switch (event.getType()) {
                    case BLOODPOOL -> bloodPoolBarOverlay;
                    case MANABANK -> manaBankBarOverlay;
                    case AWAKENED -> awakenedProgressBarOverlay;
                    case FOCUS -> focusBarOverlay;
                    case CORRUPTED -> corruptedBarOverlay;
                };

        if (overlay.isEnabled() && !overlay.shouldDisplayOriginal) {
            event.setCanceled(true);
        }
    }

    @OverlayInfo(renderType = RenderEvent.ElementType.HealthBar, renderAt = OverlayInfo.RenderState.Replace)
    private final HealthBarOverlay healthBarOverlay = new HealthBarOverlay();

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final BloodPoolBarOverlay bloodPoolBarOverlay = new BloodPoolBarOverlay();

    @OverlayInfo(renderType = RenderEvent.ElementType.FoodBar, renderAt = OverlayInfo.RenderState.Replace)
    private final ManaBarOverlay manaBarOverlay = new ManaBarOverlay();

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final ManaBankBarOverlay manaBankBarOverlay = new ManaBankBarOverlay();

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final FocusBarOverlay focusBarOverlay = new FocusBarOverlay();

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final AwakenedProgressBarOverlay awakenedProgressBarOverlay = new AwakenedProgressBarOverlay();

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final CorruptedBarOverlay corruptedBarOverlay = new CorruptedBarOverlay();

    public abstract static class BaseBarOverlay extends Overlay {
        @Config(key = "feature.wynntils.customBarsOverlay.overlay.baseBar.textShadow")
        public FontRenderer.TextShadow textShadow = FontRenderer.TextShadow.OUTLINE;

        @Config(key = "feature.wynntils.customBarsOverlay.overlay.baseBar.flip")
        public boolean flip = false;

        @Config(key = "feature.wynntils.customBarsOverlay.overlay.baseBar.shouldDisplayOriginal")
        public boolean shouldDisplayOriginal = false;

        // hacky override of custom color
        @Config(key = "feature.wynntils.customBarsOverlay.overlay.baseBar.textColor")
        public CustomColor textColor = CustomColor.NONE;

        protected BaseBarOverlay(OverlayPosition position, OverlaySize size) {
            super(position, size);
        }

        protected float textureHeight() {
            return Texture.UNIVERSAL_BAR.height() / 2f;
        }

        protected abstract BossBarModel.BarProgress progress();

        protected abstract String icon();

        protected abstract boolean isActive();

        @Override
        public void render(PoseStack poseStack, float partialTicks, Window window) {
            if (!WynnUtils.onWorld() || !isActive()) return;

            float barHeight = textureHeight() * (this.getWidth() / 81);
            float renderY = getModifiedRenderY(barHeight + 10);

            BossBarModel.BarProgress barProgress = progress();

            String text = String.format("%s %s %s", barProgress.current(), icon(), barProgress.max());
            renderText(poseStack, renderY, text);

            float progress = (flip ? -1 : 1) * barProgress.progress();
            renderBar(poseStack, renderY + 10, barHeight, progress);
        }

        protected float getModifiedRenderY(float renderedHeight) {

            return switch (this.getRenderVerticalAlignment()) {
                case Top -> this.getRenderY();
                case Middle -> this.getRenderY() + (this.getHeight() - renderedHeight) / 2;
                case Bottom -> this.getRenderY() + this.getHeight() - renderedHeight;
            };
        }

        @Override
        protected void onConfigUpdate(ConfigHolder configHolder) {}

        protected void renderBar(PoseStack poseStack, float renderY, float renderHeight, float progress) {
            Texture universalBarTexture = Texture.UNIVERSAL_BAR;

            RenderUtils.drawColoredProgressBar(
                    poseStack,
                    universalBarTexture,
                    this.textColor,
                    this.getRenderX(),
                    renderY,
                    this.getRenderX() + this.getWidth(),
                    renderY + renderHeight,
                    0,
                    0,
                    universalBarTexture.width(),
                    universalBarTexture.height(),
                    progress);
        }

        protected void renderText(PoseStack poseStack, float renderY, String text) {
            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            text,
                            this.getRenderX(),
                            this.getRenderX() + this.getWidth(),
                            renderY,
                            0,
                            this.textColor,
                            this.getRenderHorizontalAlignment(),
                            this.textShadow);
        }
    }

    public static class HealthBarOverlay extends BaseBarOverlay {
        @Config(key = "feature.wynntils.customBarsOverlay.overlay.healthBar.healthTexture")
        public HealthTexture healthTexture = HealthTexture.a;

        protected HealthBarOverlay() {
            this(
                    new OverlayPosition(
                            -30,
                            -52,
                            VerticalAlignment.Bottom,
                            HorizontalAlignment.Center,
                            OverlayPosition.AnchorSection.BottomMiddle),
                    new GuiScaledOverlaySize(81, 21));
        }

        protected HealthBarOverlay(OverlayPosition overlayPosition, GuiScaledOverlaySize guiScaledOverlaySize) {
            super(overlayPosition, guiScaledOverlaySize);
            textColor = CommonColors.RED;
        }

        @Override
        public float textureHeight() {
            return healthTexture.getHeight();
        }

        @Override
        public String icon() {
            return "❤";
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public BossBarModel.BarProgress progress() {
            int current = ActionBarModel.getCurrentHealth();
            int max = ActionBarModel.getMaxHealth();
            return new BossBarModel.BarProgress(current, max, current / (float) max);
        }

        protected void renderBar(PoseStack poseStack, float renderY, float renderHeight, float progress) {
            if (progress > 1) { // overflowing health
                CustomColor yellow = CommonColors.YELLOW;

                float x1 = this.getRenderX();
                float x2 = this.getRenderX() + this.getWidth();
                int textureY1 = healthTexture.getTextureY1();
                int textureY2 = healthTexture.getTextureY2();

                int half = (textureY1 + textureY2) / 2 + (textureY2 - textureY1) % 2;
                RenderUtils.drawProgressBarBackground(poseStack, Texture.HEALTH_BAR, x1, renderY, x2, renderY + renderHeight, 0, textureY1, 81, half);
                RenderUtils.drawProgressBarForeground(
                        poseStack,
                        Texture.HEALTH_BAR,
                        x1,
                        renderY,
                        x2,
                        renderY + renderHeight,
                        0,
                        half,
                        81,
                        textureY2 + (textureY2 - textureY1) % 2,
                        1f/progress);
                RenderUtils.drawProgressBarForeground(
                        poseStack,
                        Texture.HEALTH_BAR_OVERFLOW,
                        x1,
                        renderY,
                        x2,
                        renderY + renderHeight,
                        0,
                        half,
                        81,
                        textureY2 + (textureY2 - textureY1) % 2,
                        1f/progress - 1);

                return;
            }

            RenderUtils.drawProgressBar(
                    poseStack,
                    Texture.HEALTH_BAR,
                    this.getRenderX(),
                    renderY,
                    this.getRenderX() + this.getWidth(),
                    renderY + renderHeight,
                    0,
                    healthTexture.getTextureY1(),
                    81,
                    healthTexture.getTextureY2(),
                    progress);
        }
    }

    public static class BloodPoolBarOverlay extends HealthBarOverlay {
        protected BloodPoolBarOverlay() {
            super(
                    new OverlayPosition(
                            -30,
                            -150,
                            VerticalAlignment.Bottom,
                            HorizontalAlignment.Center,
                            OverlayPosition.AnchorSection.BottomMiddle),
                    new GuiScaledOverlaySize(81, 21));
        }

        @Override
        public String icon() {
            return "⚕";
        }

        @Override
        public BossBarModel.BarProgress progress() {
            return BossBarModel.bloodPoolBar.getBarProgress();
        }

        @Override
        public boolean isActive() {
            return BossBarModel.bloodPoolBar.isActive();
        }
    }

    public static class ManaBarOverlay extends BaseBarOverlay {
        @Config(key = "feature.wynntils.customBarsOverlay.overlay.manaBar.manaTexture")
        public ManaTexture manaTexture = ManaTexture.a;

        protected ManaBarOverlay() {
            this(
                    new OverlayPosition(
                            -30,
                            52,
                            VerticalAlignment.Bottom,
                            HorizontalAlignment.Center,
                            OverlayPosition.AnchorSection.BottomMiddle),
                    new GuiScaledOverlaySize(81, 21));
        }

        protected ManaBarOverlay(OverlayPosition overlayPosition, GuiScaledOverlaySize guiScaledOverlaySize) {
            super(overlayPosition, guiScaledOverlaySize);
            textColor = CommonColors.LIGHT_BLUE;
        }

        @Override
        public float textureHeight() {
            return manaTexture.getHeight();
        }

        @Override
        public BossBarModel.BarProgress progress() {
            int current = ActionBarModel.getCurrentMana();
            int max = ActionBarModel.getMaxMana();
            return new BossBarModel.BarProgress(current, max, current / (float) max);
        }

        @Override
        public String icon() {
            return "✺";
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        protected void renderBar(PoseStack poseStack, float renderY, float renderHeight, float progress) {
            if (progress > 1) { // overflowing mana
                float x1 = this.getRenderX();
                float x2 = this.getRenderX() + this.getWidth();
                int textureY1 = manaTexture.getTextureY1();
                int textureY2 = manaTexture.getTextureY2();

                int half = (textureY1 + textureY2) / 2 + (textureY2 - textureY1) % 2;
                RenderUtils.drawProgressBarBackground(poseStack, Texture.MANA_BAR, x1, renderY, x2, renderY + renderHeight, 0, textureY1, 81, half);
                RenderUtils.drawProgressBarForeground(
                        poseStack,
                        Texture.MANA_BAR,
                        x1,
                        renderY,
                        x2,
                        renderY + renderHeight,
                        0,
                        half,
                        81,
                        textureY2 + (textureY2 - textureY1) % 2,
                        1f/progress);
                RenderUtils.drawProgressBarForeground(
                        poseStack,
                        Texture.MANA_BAR_OVERFLOW,
                        x1,
                        renderY,
                        x2,
                        renderY + renderHeight,
                        0,
                        half,
                        81,
                        textureY2 + (textureY2 - textureY1) % 2,
                        1f/progress - 1);

                return;
            }

            RenderUtils.drawProgressBar(
                    poseStack,
                    Texture.MANA_BAR,
                    this.getRenderX(),
                    renderY,
                    this.getRenderX() + this.getWidth(),
                    renderY + renderHeight,
                    0,
                    manaTexture.getTextureY1(),
                    81,
                    manaTexture.getTextureY2(),
                    progress);
        }
    }

    public static class ManaBankBarOverlay extends ManaBarOverlay {
        protected ManaBankBarOverlay() {
            super(
                    new OverlayPosition(
                            -30,
                            -150,
                            VerticalAlignment.Bottom,
                            HorizontalAlignment.Center,
                            OverlayPosition.AnchorSection.BottomMiddle),
                    new GuiScaledOverlaySize(81, 21));
        }

        @Override
        public String icon() {
            return "☄";
        }

        @Override
        public BossBarModel.BarProgress progress() {
            return BossBarModel.manaBankBar.getBarProgress();
        }

        @Override
        public boolean isActive() {
            return BossBarModel.manaBankBar.isActive();
        }
    }

    public static class AwakenedProgressBarOverlay extends BaseBarOverlay {

        protected AwakenedProgressBarOverlay() {
            super(
                    new OverlayPosition(
                            -70,
                            -150,
                            VerticalAlignment.Bottom,
                            HorizontalAlignment.Center,
                            OverlayPosition.AnchorSection.BottomMiddle),
                    new GuiScaledOverlaySize(81, 21));
            textColor = CommonColors.WHITE;
        }

        @Override
        public BossBarModel.BarProgress progress() {
            return BossBarModel.awakenedBar.getBarProgress();
        }

        @Override
        public String icon() {
            return "۞";
        }

        @Override
        public boolean isActive() {
            return BossBarModel.awakenedBar.isActive();
        }
    }

    public static class FocusBarOverlay extends BaseBarOverlay {
        protected FocusBarOverlay() {
            super(
                    new OverlayPosition(
                            -30,
                            -150,
                            VerticalAlignment.Bottom,
                            HorizontalAlignment.Center,
                            OverlayPosition.AnchorSection.BottomMiddle),
                    new GuiScaledOverlaySize(81, 21));
            textColor = CommonColors.YELLOW;
        }

        @Override
        public BossBarModel.BarProgress progress() {
            return BossBarModel.focusBar.getBarProgress();
        }

        @Override
        public String icon() {
            return "➶";
        }

        @Override
        public boolean isActive() {
            return BossBarModel.focusBar.isActive();
        }
    }

    public static class CorruptedBarOverlay extends BaseBarOverlay {

        protected CorruptedBarOverlay() {
            super(
                    new OverlayPosition(
                            -70,
                            -150,
                            VerticalAlignment.Bottom,
                            HorizontalAlignment.Center,
                            OverlayPosition.AnchorSection.BottomMiddle),
                    new GuiScaledOverlaySize(81, 21));
            textColor = CommonColors.PURPLE;
        }

        @Override
        public BossBarModel.BarProgress progress() {
            return BossBarModel.corruptedBar.getBarProgress();
        }

        @Override
        public String icon() {
            return "☠";
        }

        @Override
        public boolean isActive() {
            return BossBarModel.corruptedBar.isActive();
        }
    }
}
