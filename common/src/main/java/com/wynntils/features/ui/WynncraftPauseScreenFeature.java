/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.PauseMenuInitEvent;
import com.wynntils.screens.base.WynntilsMenuScreenBase;
import com.wynntils.screens.maps.GuildMapScreen;
import com.wynntils.screens.wynntilsmenu.WynntilsMenuScreen;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class WynncraftPauseScreenFeature extends Feature {
    // From PauseScreen
    private static final String ADVANCEMENTS = "gui.advancements";
    private static final String STATS = "gui.stats";
    private static final String SEND_FEEDBACK = "menu.sendFeedback";
    private static final String REPORT_BUGS = "menu.reportBugs";

    @SubscribeEvent
    public void onPauseScreenInitEvent(PauseMenuInitEvent event) {
        PauseScreen pauseScreen = event.getPauseScreen();
        List<Renderable> renderables = new ArrayList<>(pauseScreen.renderables);

        replaceButtonFunction(
                renderables,
                ADVANCEMENTS,
                Component.translatable("feature.wynntils.wynncraftPauseScreen.territoryMap.name")
                        .withStyle(ChatFormatting.DARK_AQUA),
                (button) -> McUtils.mc().setScreen(GuildMapScreen.create()));

        replaceButtonFunction(
                renderables,
                STATS,
                Component.translatable("feature.wynntils.wynncraftPauseScreen.wynntilsMenuButton.name"),
                (button) -> WynntilsMenuScreenBase.openBook(WynntilsMenuScreen.create()));

        replaceButtonFunction(
                renderables,
                SEND_FEEDBACK,
                Component.translatable("feature.wynntils.wynncraftPauseScreen.classSelectionButton.name"),
                (button) -> {
                    McUtils.mc().setScreen(null);
                    McUtils.mc().mouseHandler.grabMouse();
                    Handlers.Command.sendCommandImmediately("class");
                });

        replaceButtonFunction(
                renderables,
                REPORT_BUGS,
                Component.translatable("feature.wynntils.wynncraftPauseScreen.hubButton.name"),
                (button) -> {
                    McUtils.mc().setScreen(null);
                    McUtils.mc().mouseHandler.grabMouse();
                    Handlers.Command.sendCommandImmediately("hub");
                });

        event.getPauseScreen().clearWidgets();

        for (Renderable renderable : renderables) {
            if (renderable instanceof AbstractWidget widget) {
                event.getPauseScreen().addRenderableWidget(widget);
            }
        }
    }

    private void replaceButtonFunction(
            List<Renderable> widgets, String translationKey, Component component, Button.OnPress onPress) {
        Button oldButton = widgets.stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> isButton(button, translationKey))
                .findFirst()
                .orElse(null);

        if (oldButton == null) return;

        Button newButton = new Button.Builder(component, onPress)
                .pos(oldButton.getX(), oldButton.getY())
                .size(oldButton.getWidth(), oldButton.getHeight())
                .build();

        widgets.set(widgets.indexOf(oldButton), newButton);
    }

    private boolean isButton(Button button, String translationKey) {
        return button.visible
                && button.getMessage().getContents() instanceof TranslatableContents translatableContents
                && translatableContents.getKey().equals(translationKey);
    }
}
