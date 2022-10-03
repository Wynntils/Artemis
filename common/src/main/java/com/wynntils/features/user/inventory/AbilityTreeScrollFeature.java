/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.inventory;

import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureCategory;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.mc.event.MouseScrollEvent;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wynn.utils.ContainerUtils;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(stability = FeatureInfo.Stability.STABLE, category = FeatureCategory.INVENTORY)
public class AbilityTreeScrollFeature extends UserFeature {

    private static final Pattern ABILITY_TREE_PATTERN = Pattern.compile("(?:Warrior|Shaman|Mage|Assassin|Archer) Abilities");
    private static final int abilityTreePreviousSlot = 57;
    private static final int abilityTreeNextSlot = 59;

    @Config
    public boolean invertScroll = false;

    @SubscribeEvent
    public void onInteract(MouseScrollEvent event) {
        Screen screen = McUtils.mc().screen;

        if (!(screen instanceof AbstractContainerScreen<?> gui)) return;
        if (!ABILITY_TREE_PATTERN.matcher(gui.getTitle().getString()).matches()) return;

        boolean up = event.isScrollingUp() ^ invertScroll;
        int slot = up ? abilityTreePreviousSlot : abilityTreeNextSlot;

        ContainerUtils.clickOnSlot(
                slot,
                gui.getMenu().containerId,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                gui.getMenu().getItems());
    }
}
