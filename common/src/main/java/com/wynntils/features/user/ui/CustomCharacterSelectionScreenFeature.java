/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.ui;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.Category;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.screens.characterselector.CharacterSelectorScreen;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class CustomCharacterSelectionScreenFeature extends UserFeature {
    @Config
    public boolean onlyOpenOnce = false;

    private boolean openedInThisCharacterSelectionState = false;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onScreenOpen(ScreenOpenedEvent.Post event) {
        if ((onlyOpenOnce && openedInThisCharacterSelectionState)
                || Models.WorldState.getCurrentState() != WorldState.CHARACTER_SELECTION) return;

        if (!ComponentUtils.getCoded(event.getScreen().getTitle()).equals("§8§lSelect a Character")) {
            return;
        }

        openedInThisCharacterSelectionState = true;

        McUtils.mc().setScreen(CharacterSelectorScreen.create());
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.CHARACTER_SELECTION) {
            openedInThisCharacterSelectionState = false;
        }
    }
}
