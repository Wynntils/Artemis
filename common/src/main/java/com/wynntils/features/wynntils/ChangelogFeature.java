/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.wynntils;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.screens.changelog.ChangelogScreen;
import com.wynntils.utils.mc.McUtils;
import java.util.Map;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.WYNNTILS)
public class ChangelogFeature extends Feature {
    // If we don't know the last version, assume we just downloaded the mod, so don't show the changelog
    @Persisted
    public final Storage<String> lastShownVersion = new Storage<>(WynntilsMod.getVersion());

    @Persisted
    public final Config<Boolean> autoClassMenu = new Config<>(false);

    private boolean waitForScreen = false;
    private String changelogData = "";

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (!event.isFirstJoinWorld()) return;
        if (WynntilsMod.getVersion().equals(lastShownVersion.get())) return;

        ApiResponse response = Services.WynntilsAccount.callApi(
                UrlId.API_ATHENA_UPDATE_CHANGELOG,
                Map.of("old_version", lastShownVersion.get(), "new_version", WynntilsMod.getVersion()));

        response.handleJsonObject(
                jsonObject -> {
                    if (!jsonObject.has("changelog")) return;

                    String changelog = jsonObject.get("changelog").getAsString();

                    lastShownVersion.store(WynntilsMod.getVersion());

                    if (autoClassMenu.get()) {
                        Handlers.Command.sendCommandImmediately("class");
                        waitForScreen = true;
                        changelogData = changelog;
                    } else {
                        Managers.TickScheduler.scheduleNextTick(
                                () -> McUtils.mc().setScreen(ChangelogScreen.create(changelog)));
                    }
                },
                throwable -> WynntilsMod.warn("Could not get update changelog: ", throwable));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenOpenedPost(ScreenOpenedEvent.Post event) {
        if (!waitForScreen) return;

        event.setCanceled(true);
        waitForScreen = false;
        McUtils.mc().setScreen(ChangelogScreen.create(changelogData));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenOpenedPre(ScreenOpenedEvent.Pre event) {
        if (!(McUtils.mc().screen instanceof ChangelogScreen)) return;

        event.setCanceled(true);
    }
}
