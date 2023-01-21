/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.inventory;

import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureCategory;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.mc.event.MouseScrollEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(stability = FeatureInfo.Stability.STABLE, category = FeatureCategory.INVENTORY)
public class AbilityTreeScrollFeature extends UserFeature {
    private static final int abilityTreePreviousSlot = 57;
    private static final int abilityTreeNextSlot = 59;

    @Config
    public boolean invertScroll = false;

    @Override
    public List<Model> getModelDependencies() {
        return List.of(Models.Container);
    }

    @SubscribeEvent
    public void onInteract(MouseScrollEvent event) {
        Screen screen = McUtils.mc().screen;

        if (!(screen instanceof AbstractContainerScreen<?> gui)) return;
        if (!Models.Container.isAbilityTreeScreen(gui)) return;

        boolean up = event.isScrollingUp() ^ invertScroll;
        int slot = up ? abilityTreePreviousSlot : abilityTreeNextSlot;

        ContainerUtils.clickOnSlot(
                slot,
                gui.getMenu().containerId,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                gui.getMenu().getItems());
    }
}
