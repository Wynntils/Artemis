/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.features.UserFeature;
import com.wynntils.mc.event.PauseMenuInitEvent;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.screens.OverlaySelectionScreen;
import com.wynntils.wc.utils.WynnUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WynncraftPauseScreenFeature extends UserFeature {

    @SubscribeEvent
    public void onPauseScreenInitEvent(PauseMenuInitEvent event) {
        if (!WynnUtils.onServer()) return;

        PauseScreen pauseScreen = event.getPauseScreen();
        List<Widget> renderables = new ArrayList<>(pauseScreen.renderables);

        Button wynntilsMenu = replaceButtonFunction(
                (Button) renderables.get(2),
                new TranslatableComponent("feature.wynntils.wynncraftPauseScreen.wynntilsMenuButton.name"),
                (button) -> {
                    // TODO: Open Wynntils menu when we add it :)
                    McUtils.mc().setScreen(new OverlaySelectionScreen());
                });
        renderables.set(2, wynntilsMenu);

        Button classSelection = replaceButtonFunction(
                (Button) renderables.get(3),
                new TranslatableComponent("feature.wynntils.wynncraftPauseScreen.classSelectionButton.name"),
                (button) -> {
                    McUtils.mc().setScreen(null);
                    McUtils.mc().mouseHandler.grabMouse();
                    McUtils.player().chat("/class");
                });

        renderables.set(3, classSelection);

        Button hub = replaceButtonFunction(
                (Button) renderables.get(4),
                new TranslatableComponent("feature.wynntils.wynncraftPauseScreen.hubButton.name"),
                (button) -> {
                    McUtils.mc().setScreen(null);
                    McUtils.mc().mouseHandler.grabMouse();
                    McUtils.player().chat("/hub");
                });

        renderables.set(4, hub);

        event.getPauseScreen().clearWidgets();

        for (Widget renderable : renderables) {
            event.getAddButton().accept((AbstractWidget) renderable);
        }
    }

    private Button replaceButtonFunction(
            Button widget, TranslatableComponent translatableComponent, Button.OnPress onPress) {
        return new Button(widget.x, widget.y, widget.getWidth(), widget.getHeight(), translatableComponent, onPress);
    }
}
