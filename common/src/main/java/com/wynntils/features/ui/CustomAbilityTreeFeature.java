/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.Feature;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.items.items.gui.AbilityTreeItem;
import com.wynntils.screens.abilities.CustomAbilityTreeScreen;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.Optional;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class CustomAbilityTreeFeature extends Feature {
    @SubscribeEvent
    public void onInventoryClick(ContainerClickEvent event) {
        if (!(McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen)) return;
        if (!KeyboardUtils.isShiftDown()) return;

        Optional<AbilityTreeItem> abilityTreeItem = Models.Item.asWynnItem(event.getItemStack(), AbilityTreeItem.class);
        if (abilityTreeItem.isEmpty()) return;

        if (Models.Character.getClassType() == ClassType.NONE) {
            McUtils.sendMessageToClient(Component.translatable("feature.wynntils.customAbilityTree.noClassData"));
            return;
        }

        if (!Models.AbilityTree.isLoaded()) {
            McUtils.sendMessageToClient(Component.translatable("feature.wynntils.customAbilityTree.noAbilityTreeData"));
            return;
        }

        event.setCanceled(true);

        Models.AbilityTree.ABILITY_TREE_CONTAINER_QUERIES.updateParsedAbilityTree();

        McUtils.mc().setScreen(new CustomAbilityTreeScreen());
    }
}
