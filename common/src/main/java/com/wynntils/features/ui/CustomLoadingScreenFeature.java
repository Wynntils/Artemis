/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.LoadingProgressEvent;
import com.wynntils.mc.event.LocalSoundEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.mc.event.ServerResourcePackEvent;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.event.TickAlwaysEvent;
import com.wynntils.mc.event.TitleSetTextEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.screens.characterselector.LoadingScreen;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class CustomLoadingScreenFeature extends Feature {
    private LoadingScreen loadingScreen;
    private ConnectScreen baseConnectScreen;
    // Minecraft does some of its connection logic in ConnectScreen which is strange
    // We need to be able to tell our custom loading screen to work with it in the background

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenOpenPre(ScreenOpenedEvent.Pre event) {
        if (event.getScreen() instanceof ConnectScreen cs) {
            baseConnectScreen = cs;
        } else if (!(event.getScreen() instanceof LoadingScreen)) {
            // Ensures baseConnectScreen is cleared after the initial handshake occurs
            // Only our LoadingScreen and ConnectScreen should be able to work with baseConnectScreen
            baseConnectScreen = null;
        }

        if (loadingScreen == null) return;

        if (event.getScreen() instanceof ProgressScreen) {
            event.setCanceled(true);
            loadingScreen.setMessage(I18n.get("feature.wynntils.customLoadingScreen.working"));
        }
        if (event.getScreen() instanceof ReceivingLevelScreen) {
            event.setCanceled(true);
            loadingScreen.setMessage(I18n.get("feature.wynntils.customLoadingScreen.receivingTerrain"));
        }
    }

    @SubscribeEvent
    public void onLoadingProgress(LoadingProgressEvent event) {
        if (loadingScreen == null) {
            loadingScreen = LoadingScreen.create();
            loadingScreen.setMessage(event.getMessage());
            McUtils.mc().setScreen(loadingScreen);
        }

        loadingScreen.setMessage(event.getMessage());
    }

    @SubscribeEvent
    public void onResourcePack(ServerResourcePackEvent.Load e) {
        if (loadingScreen == null) return;

        loadingScreen.setMessage(I18n.get("feature.wynntils.customLoadingScreen.resourcePack"));
    }

    @SubscribeEvent
    public void onTitleSetText(TitleSetTextEvent e) {
        if (loadingScreen == null) return;

        loadingScreen.setTitle(e.getComponent().getString());
    }

    @SubscribeEvent
    public void onSubtitleSetText(SubtitleSetTextEvent e) {
        if (loadingScreen == null) return;

        loadingScreen.setSubtitle(e.getComponent().getString());
    }

    @SubscribeEvent
    public void onPlayerSound(LocalSoundEvent.Client event) {
        if (loadingScreen == null) return;

        // Silence all player sounds while loading (falling and equip sounds)
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerSound(LocalSoundEvent.Player event) {
        if (loadingScreen == null) return;

        // Silence all player sounds while loading (falling and equip sounds)
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerSound(LocalSoundEvent.LocalEntity event) {
        if (loadingScreen == null) return;

        // Silence all player sounds while loading (falling and equip sounds)
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        switch (event.getNewState()) {
            case CONNECTING -> {
                loadingScreen = LoadingScreen.create();
                loadingScreen.setMessage(I18n.get("feature.wynntils.customLoadingScreen.connecting"));
                McUtils.mc().setScreen(loadingScreen);
            }
            case INTERIM -> {
                if (loadingScreen == null) return;

                loadingScreen.setMessage(I18n.get("feature.wynntils.customLoadingScreen.joiningWorld"));
            }
            case WORLD, HUB, NOT_CONNECTED -> {
                if (loadingScreen == null) return;

                loadingScreen = null;
                McUtils.mc().setScreen(null);
            }
        }
    }

    @SubscribeEvent
    public void onTickAlways(TickAlwaysEvent e) {
        // Minecraft does connection logic work every tick, do not remove this behaviour when cancelling ConnectScreens
        if (baseConnectScreen == null) return;
        baseConnectScreen.tick();
    }
}
