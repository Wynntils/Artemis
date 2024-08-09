/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.wynntilsmenu.widgets;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.Texture;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public record WynntilsMenuButton(
        Texture buttonTexture, boolean dynamicTexture, Runnable clickAction, List<Component> tooltipList) {
    public WynntilsMenuButton(
            Texture buttonTexture, boolean dynamicTexture, Screen openedScreen, List<Component> tooltipList) {
        this(buttonTexture, dynamicTexture, () -> McUtils.mc().setScreen(openedScreen), tooltipList);
    }
}
