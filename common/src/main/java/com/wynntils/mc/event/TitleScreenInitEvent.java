/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.bus.api.Event;

/** Fired on the first initialization of {@link TitleScreen} */
public abstract class TitleScreenInitEvent extends Event {
    private final TitleScreen titleScreen;

    protected TitleScreenInitEvent(TitleScreen titleScreen) {
        this.titleScreen = titleScreen;
    }

    public TitleScreen getTitleScreen() {
        return titleScreen;
    }

    public static final class Pre extends TitleScreenInitEvent {
        public Pre(TitleScreen titleScreen) {
            super(titleScreen);
        }
    }

    public static final class Post extends TitleScreenInitEvent {
        public Post(TitleScreen titleScreen) {
            super(titleScreen);
        }
    }
}
