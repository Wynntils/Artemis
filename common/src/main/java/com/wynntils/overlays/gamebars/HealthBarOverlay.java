/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays.gamebars;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.handlers.bossbar.TrackedBar;
import com.wynntils.handlers.bossbar.type.BossBarProgress;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HealthTexture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.CappedValue;

public class HealthBarOverlay extends OverflowableBarOverlay {
    @RegisterConfig(i18nKey = "overlay.wynntils.healthBar.healthTexture")
    public final Config<HealthTexture> healthTexture = new Config<>(HealthTexture.A);

    public HealthBarOverlay() {
        this(
                new OverlayPosition(
                        -29,
                        -52,
                        VerticalAlignment.BOTTOM,
                        HorizontalAlignment.CENTER,
                        OverlayPosition.AnchorSection.BOTTOM_MIDDLE),
                new OverlaySize(81, 21));
    }

    protected HealthBarOverlay(OverlayPosition overlayPosition, OverlaySize overlaySize) {
        super(overlayPosition, overlaySize, CommonColors.RED);
    }

    @Override
    public float textureHeight() {
        return healthTexture.get().getHeight();
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
    protected void onConfigUpdate(ConfigHolder<?> configHolder) {
        Models.CharacterStats.hideHealth(!this.shouldDisplayOriginal.get());
    }

    @Override
    public BossBarProgress progress() {
        CappedValue health = Models.CharacterStats.getHealth();
        return new BossBarProgress(health, (float) health.getProgress());
    }

    @Override
    protected Class<? extends TrackedBar> getTrackedBarClass() {
        return null;
    }

    @Override
    protected Texture getTexture() {
        return Texture.HEALTH_BAR;
    }

    @Override
    protected Texture getOverflowTexture() {
        return Texture.HEALTH_BAR_OVERFLOW;
    }

    @Override
    protected int getTextureY1() {
        return healthTexture.get().getTextureY1();
    }

    @Override
    protected int getTextureY2() {
        return healthTexture.get().getTextureY2();
    }
}
