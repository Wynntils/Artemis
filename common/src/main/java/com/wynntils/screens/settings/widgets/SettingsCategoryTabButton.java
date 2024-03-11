/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.wynntils.core.persisted.config.Category;
import com.wynntils.utils.render.Texture;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

public class SettingsCategoryTabButton extends GeneralSettingsTabButton {
    private Category category;

    public SettingsCategoryTabButton(
            int x,
            int y,
            int width,
            int height,
            Consumer<Integer> onClick,
            List<Component> tooltip,
            Category category,
            boolean selectedCategory) {
        super(x, y, width, height, onClick, tooltip, Texture.TAG_RED, category.getCategoryIcon(), OffsetDirection.UP);
        this.category = category;
        this.selectedTab = selectedCategory;

        setSelectedCategory(selectedCategory);
    }

    public SettingsCategoryTabButton(
            int x,
            int y,
            int width,
            int height,
            Consumer<Integer> onClick,
            List<Component> tooltip,
            boolean selectedCategory) {
        super(x, y, width, height, onClick, tooltip, Texture.TAG_RED, Texture.ALL_CONFIG_ICON, OffsetDirection.UP);
        this.category = null;
        this.selectedTab = selectedCategory;

        setSelectedCategory(selectedCategory);
    }

    public void setSelectedCategory(boolean selectedCategory) {
        this.selectedTab = selectedCategory;

        // When selected use a different texture to show the category has been selected
        if (this.selectedTab) {
            tagTexture = Texture.TAG_RED_SELECTED;
        } else {
            tagTexture = Texture.TAG_RED;
        }
    }

    public Category getCategory() {
        return category;
    }
}
