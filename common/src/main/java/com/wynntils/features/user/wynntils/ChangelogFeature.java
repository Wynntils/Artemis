/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.wynntils;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.screens.changelog.ChangelogScreen;
import com.wynntils.utils.mc.McUtils;
import java.util.Map;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.WYNNTILS)
public class ChangelogFeature extends UserFeature {
    // "v0.0.2-alpha.2" is the first version with a changelog on GitHub
    @Config(visible = false)
    public String lastShownVersion = "v0.0.2-alpha.2";

    @Config
    public boolean autoClassMenu = false;

    private boolean waitForScreen = false;
    private String changelogData = "";

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (!event.isFirstJoinWorld()) return;
        if (WynntilsMod.getVersion().equals(lastShownVersion)) return;

        ApiResponse response = Managers.Net.callApi(
                UrlId.API_ATHENA_UPDATE_CHANGELOG,
                Map.of("old_version", lastShownVersion, "new_version", WynntilsMod.getVersion()));

        response.handleJsonObject(
                jsonObject -> {
                    if (!jsonObject.has("changelog")) return;

                    String changelog = jsonObject.get("changelog").getAsString();

                    lastShownVersion = WynntilsMod.getVersion();
                    Managers.Config.saveConfig();

                    if (autoClassMenu) {
                        McUtils.sendCommand("class");
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
