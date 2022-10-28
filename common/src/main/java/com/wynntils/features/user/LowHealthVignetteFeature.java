/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.managers.Model;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.mc.event.ClientTickEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.objects.CustomColor;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.MathUtils;
import com.wynntils.wynn.model.ActionBarModel;
import java.util.List;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LowHealthVignetteFeature extends UserFeature {
    private static final float INTENSITY = 0.3f;

    @Config
    public int lowHealthPercentage = 25;

    @Config
    public float animationSpeed = 0.6f;

    @Config
    public HealthVignetteEffect healthVignetteEffect = HealthVignetteEffect.Pulse;

    @Config
    public CustomColor color = new CustomColor(255, 0, 0);

    private float animation = 10f;
    private float value = INTENSITY;
    private boolean shouldRender = false;

    @Override
    public List<Class<? extends Model>> getModelDependencies() {
        return List.of(ActionBarModel.class);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderGui(RenderEvent.Post event) {
        if (!shouldRender || event.getType() != RenderEvent.ElementType.GUI) return;

        Window window = McUtils.window();

        float[] colorArray = color.asFloatArray();
        RenderSystem.setShaderColor(colorArray[0], colorArray[1], colorArray[2], value);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        RenderUtils.drawTexturedRect(
                event.getPoseStack(),
                Texture.VIGNETTE.resource(),
                0,
                0,
                0,
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight(),
                0,
                0,
                Texture.VIGNETTE.width(),
                Texture.VIGNETTE.height(),
                Texture.VIGNETTE.width(),
                Texture.VIGNETTE.height());

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Start event) {
        float healthPercent = (float) ActionBarModel.getCurrentHealth() / ActionBarModel.getMaxHealth();
        float threshold = lowHealthPercentage / 100f;
        shouldRender = false;

        if (healthPercent > threshold) return;
        shouldRender = true;

        switch (healthVignetteEffect) {
            case Pulse -> {
                animation = (animation + animationSpeed) % 40;
                value = threshold - healthPercent * INTENSITY + 0.01f * Math.abs(20 - animation);
            }
            case Growing -> value = MathUtils.map(healthPercent, 0, threshold, INTENSITY, 0.1f);
            case Static -> value = INTENSITY;
        }
    }

    public enum HealthVignetteEffect {
        Pulse,
        Growing,
        Static
    }
}
