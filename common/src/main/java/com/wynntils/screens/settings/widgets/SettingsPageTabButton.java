/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.wynntils.utils.render.Texture;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

public class SettingsPageTabButton extends GeneralSettingsTabButton {
    public SettingsPageTabButton(
            int x, int y, int width, int height, Consumer<Integer> onClick, List<Component> tooltip, boolean nextPage) {
        super(
                x,
                y,
                width,
                height,
                onClick,
                tooltip,
                Texture.TAG_RED,
                nextPage ? Texture.NEXT : Texture.PREVIOUS,
                OffsetDirection.UP);
    }
}
