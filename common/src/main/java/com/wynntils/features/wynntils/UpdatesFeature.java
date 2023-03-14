/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.wynntils;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.ConfigInfo;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.net.athena.UpdateManager;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.WYNNTILS)
public class UpdatesFeature extends UserFeature {
    @ConfigInfo
    public final Config<Boolean> updateReminder = new Config<>(true);

    @ConfigInfo
    public final Config<Boolean> autoUpdate = new Config<>(false);

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (!event.isFirstJoinWorld()) return;

        CompletableFuture.runAsync(() -> Managers.Update.getLatestBuild()
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

                        CompletableFuture<UpdateManager.UpdateResult> completableFuture = Managers.Update.tryUpdate();

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

        McUtils.sendMessageToClient(Component.literal("[Wynntils/Artemis]: ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.translatable(
                                "feature.wynntils.updates.reminder", WynntilsMod.getVersion(), newVersion)
                        .append(clickable)
                        .append(Component.literal("\n"))
                        .append(Component.translatable("feature.wynntils.updates.reminder.alpha"))
                        .withStyle(ChatFormatting.GREEN)));
    }
}
