/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.wrappedscreen;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handler;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.wrappedscreen.event.WrappedScreenOpenEvent;
import com.wynntils.handlers.wrappedscreen.type.WrappedScreenInfo;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.SubscribeEvent;

public class WrappedScreenHandler extends Handler {
    private final Set<WrappedScreenHolder> wrappedScreenHolders = new HashSet<>();

    private WrappedScreenHolder<?> currentWrappedScreenHolder;
    private Screen currentWrappedScreen;

    @SubscribeEvent
    public void onMenuOpened(MenuEvent.MenuOpenedEvent.Post event) {
        if (!(McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen)) return;

        StyledText titleStyledText = StyledText.fromComponent(abstractContainerScreen.getTitle());

        // If we opened a new screen, reset the current wrapped screen
        resetWrappedScreen(false);

        Optional<WrappedScreenHolder> holderForScreenOpt = wrappedScreenHolders.stream()
                .filter(holder -> titleStyledText.matches(holder.getReplacedScreenTitlePattern()))
                .findFirst();

        if (holderForScreenOpt.isEmpty()) return;

        WrappedScreenHolder holder = holderForScreenOpt.get();

        // Post an event to allow consumers to allow opening the wrapped screen
        WrappedScreenOpenEvent openEvent = new WrappedScreenOpenEvent(holder.getWrappedScreenClass());
        WynntilsMod.postEvent(openEvent);

        // If the event was not accepted, don't open the screen
        if (!openEvent.shouldOpenScreen()) return;

        // Set up and open the wrapped screen
        currentWrappedScreenHolder = holder;
        WynntilsMod.registerEventListener(holder);

        currentWrappedScreen = holder.createWrappedScreen(new WrappedScreenInfo(
                abstractContainerScreen, McUtils.containerMenu(), McUtils.containerMenu().containerId));

        holder.setWrappedScreen(currentWrappedScreen);
        McUtils.mc().setScreen(currentWrappedScreen);
    }

    @SubscribeEvent
    public void onScreenClose(ScreenClosedEvent event) {
        resetWrappedScreen(true);
    }

    public void registerWrappedScreen(WrappedScreenHolder holder) {
        wrappedScreenHolders.add(holder);
    }

    private void resetWrappedScreen(boolean closeContainer) {
        if (currentWrappedScreen == null) return;

        // If the container was overridden, we don't need to close it
        if (closeContainer) {
            ContainerUtils.closeContainer(((WrappedScreen) currentWrappedScreen)
                    .getWrappedScreenInfo()
                    .containerId());
        }

        currentWrappedScreen = null;

        currentWrappedScreenHolder.reset();
        WynntilsMod.unregisterEventListener(currentWrappedScreenHolder);
        currentWrappedScreenHolder = null;
    }
}
