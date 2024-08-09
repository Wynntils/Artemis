/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.google.gson.JsonElement;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.models.abilitytree.type.AbilityTreeInfo;
import com.wynntils.models.items.items.gui.AbilityTreeItem;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

// NOTE: This feature was intented to be used on fully reset ability trees.
//       Although support for parsing any tree is present, I would still recommend using a fresh tree to avoid any
//       issues.
@StartDisabled
@ConfigCategory(Category.DEBUG)
public class AbilityTreeDataDumpFeature extends Feature {
    private static final File SAVE_FOLDER = WynntilsMod.getModStorageDir("debug");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInventoryClick(ContainerClickEvent event) {
        if (!(McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen)) return;
        if (!KeyboardUtils.isShiftDown()) return;

        Optional<AbilityTreeItem> abilityTreeItem = Models.Item.asWynnItem(event.getItemStack(), AbilityTreeItem.class);

        if (abilityTreeItem.isEmpty()) return;

        event.setCanceled(true);
        McUtils.player().closeContainer();

        // Wait for the container to close
        Managers.TickScheduler.scheduleNextTick(
                () -> Models.AbilityTree.ABILITY_TREE_CONTAINER_QUERIES.dumpAbilityTree(this::saveToDisk));
    }

    private void saveToDisk(AbilityTreeInfo abilityTreeInfo) {
        // Save the dump to a file
        JsonElement element = Managers.Json.GSON.toJsonTree(abilityTreeInfo);

        String fileName = Models.Character.getClassType().getName().toLowerCase(Locale.ROOT) + "_ablities.json";
        File jsonFile = new File(SAVE_FOLDER, fileName);
        Managers.Json.savePreciousJson(jsonFile, element.getAsJsonObject());

        McUtils.sendMessageToClient(Component.literal("Saved ability tree dump to " + jsonFile.getAbsolutePath()));
    }
}
