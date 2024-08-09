/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.wynntils;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.services.athena.UpdateService;
import com.wynntils.utils.mc.McUtils;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.WYNNTILS)
public class UpdatesFeature extends Feature {
    @Persisted
    public final Config<Boolean> updateReminder = new Config<>(true);

    @Persisted
    public final Config<Boolean> autoUpdate = new Config<>(false);

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (!event.isFirstJoinWorld()) return;

        CompletableFuture.runAsync(() -> Services.Update.getLatestBuild()
                .whenCompleteAsync((version, throwable) -> Managers.TickScheduler.scheduleNextTick(() -> {
                    if (version == null) {
                        WynntilsMod.info(
                                "Couldn't fetch latest version, not attempting update reminder or auto-update.");
                        return;
                    }

                    if (Objects.equals(version, WynntilsMod.getVersion())) {
                        WynntilsMod.info("Mod is on latest version, not attempting update reminder or auto-update.");
                        return;
                    }

                    if (updateReminder.get()) {
                        if (WynntilsMod.isDevelopmentEnvironment()) {
                            WynntilsMod.info("Tried to show update reminder, but we are in development environment.");
                            return;
                        }

                        remindToUpdateIfExists(version);
                    }

                    if (autoUpdate.get()) {
                        if (WynntilsMod.isDevelopmentEnvironment()) {
                            WynntilsMod.info("Tried to auto-update, but we are in development environment.");
                            return;
                        }

                        WynntilsMod.info("Attempting to auto-update.");

                        McUtils.sendMessageToClient(Component.translatable("feature.wynntils.updates.updating")
                                .withStyle(ChatFormatting.YELLOW));

                        CompletableFuture<UpdateService.UpdateResult> completableFuture = Services.Update.tryUpdate();

                        completableFuture.whenCompleteAsync(
                                (result, t) -> McUtils.sendMessageToClient(result.getMessage()));
                    }
                })));
    }

    private static void remindToUpdateIfExists(String newVersion) {
        MutableComponent clickable = Component.translatable("feature.wynntils.updates.reminder.clickable");
        clickable.setStyle(clickable
                .getStyle()
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wynntils update"))
                .withUnderlined(true)
                .withBold(true));

        McUtils.sendMessageToClient(Component.literal("[Wynntils]: ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.translatable(
                                "feature.wynntils.updates.reminder", WynntilsMod.getVersion(), newVersion)
                        .append(clickable)
                        .withStyle(ChatFormatting.GREEN)));
    }
}
