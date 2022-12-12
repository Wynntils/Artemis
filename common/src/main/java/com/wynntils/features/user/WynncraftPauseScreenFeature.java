/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.features.UserFeature;
import com.wynntils.gui.screens.WynntilsMenuScreen;
import com.wynntils.gui.screens.maps.GuildMapScreen;
import com.wynntils.mc.event.PauseMenuInitEvent;
import com.wynntils.mc.utils.McUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.GridWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WynncraftPauseScreenFeature extends UserFeature {
    @SubscribeEvent
    public void onPauseScreenInitEvent(PauseMenuInitEvent event) {
        PauseScreen pauseScreen = event.getPauseScreen();

        Optional<Renderable> grid = pauseScreen.renderables.stream()
                .filter(x -> x instanceof GridWidget)
                .findFirst();
        if (grid.isEmpty()) return;

        List<Button> renderables = new ArrayList<>();

        for (AbstractWidget child : ((GridWidget) grid.get()).getContainedChildren()) {
            if (child instanceof Button) {
                renderables.add((Button) child);
            }
        }

        Button territoryMap = replaceButtonFunction(
                renderables.get(1),
                Component.translatable("feature.wynntils.wynncraftPauseScreen.territoryMap.name")
                        .withStyle(ChatFormatting.DARK_AQUA),
                (button) -> McUtils.mc().setScreen(new GuildMapScreen()));
        renderables.set(1, territoryMap);

        Button wynntilsMenu = replaceButtonFunction(
                renderables.get(2),
                Component.translatable("feature.wynntils.wynncraftPauseScreen.wynntilsMenuButton.name"),
                (button) -> McUtils.mc().setScreen(new WynntilsMenuScreen()));
        renderables.set(2, wynntilsMenu);

        Button classSelection = replaceButtonFunction(
                renderables.get(3),
                Component.translatable("feature.wynntils.wynncraftPauseScreen.classSelectionButton.name"),
                (button) -> {
                    McUtils.mc().setScreen(null);
                    McUtils.mc().mouseHandler.grabMouse();
                    McUtils.sendCommand("class");
                });

        renderables.set(3, classSelection);

        Button hub = replaceButtonFunction(
                renderables.get(4),
                Component.translatable("feature.wynntils.wynncraftPauseScreen.hubButton.name"),
                (button) -> {
                    McUtils.mc().setScreen(null);
                    McUtils.mc().mouseHandler.grabMouse();
                    McUtils.sendCommand("hub");
                });

        renderables.set(4, hub);

        // FIXME: Use the grid widget MC uses when we replace the buttons
        event.getPauseScreen().clearWidgets();

        for (AbstractWidget renderable : renderables) {
            event.getAddButton().accept(renderable);
        }
    }

    private Button replaceButtonFunction(Button widget, Component component, Button.OnPress onPress) {
        return new Button.Builder(component, onPress)
                .pos(widget.getX(), widget.getY())
                .size(widget.getWidth(), widget.getHeight())
                .build();
    }
}
