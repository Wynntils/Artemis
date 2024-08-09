/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ContainerRenderEvent;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.ObjectivesTextures;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class CraftingProfessionLevelProgressBarFeature extends Feature {
    @Persisted
    public final Config<ObjectivesTextures> texture = new Config<>(ObjectivesTextures.WYNN);

    @SubscribeEvent
    public void onContainerRender(ContainerRenderEvent event) {
        if (!(event.getScreen() instanceof ContainerScreen screen)) return;
        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;

        PoseStack poseStack = event.getPoseStack();

        ProfessionType profession = container.getProfessionType();
        int level = Models.Profession.getLevel(profession);
        double progress = Models.Profession.getProgress(profession);

        RenderUtils.drawProgressBar(
                poseStack,
                Texture.EXPERIENCE_BAR,
                screen.leftPos,
                screen.topPos - 6,
                screen.leftPos + screen.imageWidth,
                screen.topPos - 1,
                0,
                texture.get().getTextureY1(),
                Texture.EXPERIENCE_BAR.width(),
                texture.get().getTextureY2(),
                (float) (progress / 100d));

        final String text = "Level %d (%.2f%%)".formatted(level, progress);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(text),
                        (float) screen.width / 2,
                        screen.topPos - 8,
                        0,
                        CommonColors.GREEN,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.BOTTOM,
                        TextShadow.NORMAL);
    }
}
