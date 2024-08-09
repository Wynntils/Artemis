/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.screens.characterselector.CharacterSelectorScreen;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class CustomCharacterSelectionScreenFeature extends Feature {
    private static final StyledText CHARACTER_SELECTION_TITLE = StyledText.fromString("§8§lSelect a Character");

    @Persisted
    public final Config<Boolean> onlyOpenOnce = new Config<>(false);

    private boolean openedInThisCharacterSelectionState = false;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onScreenOpen(ScreenOpenedEvent.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> abstractContainerScreen)) return;

        if ((onlyOpenOnce.get() && openedInThisCharacterSelectionState)
                || Models.WorldState.getCurrentState() != WorldState.CHARACTER_SELECTION) {
            return;
        }

        if (!StyledText.fromComponent(event.getScreen().getTitle()).equals(CHARACTER_SELECTION_TITLE)) {
            return;
        }

        openedInThisCharacterSelectionState = true;

        McUtils.mc().setScreen(CharacterSelectorScreen.create(abstractContainerScreen));
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.CHARACTER_SELECTION) {
            openedInThisCharacterSelectionState = false;
        }
    }
}
