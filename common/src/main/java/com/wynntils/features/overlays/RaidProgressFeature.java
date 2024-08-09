/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.annotations.OverlayInfo;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.models.raid.event.RaidEndedEvent;
import com.wynntils.models.raid.event.RaidNewBestTimeEvent;
import com.wynntils.overlays.RaidProgressOverlay;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.OVERLAYS)
public class RaidProgressFeature extends Feature {
    private static final String TIME_FORMAT = "%02d:%02d.%03d\n";

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final RaidProgressOverlay raidProgressOverlay = new RaidProgressOverlay();

    @Persisted
    public final Config<Boolean> printTimes = new Config<>(true);

    @Persisted
    public final Config<Boolean> playSoundOnBest = new Config<>(true);

    @SubscribeEvent
    public void onRaidCompleted(RaidEndedEvent.Completed event) {
        if (!printTimes.get()) return;

        if (event.getRoomTimes().size() != Models.Raid.ROOM_TIMERS_COUNT) {
            WynntilsMod.error("Unexpected room count on raid completion: "
                    + event.getRoomTimes().size());
            return;
        }

        MutableComponent raidComponents = Component.literal("");

        raidComponents.append(Component.literal(event.getRaid().getName())
                .withStyle(ChatFormatting.GOLD)
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.UNDERLINE));

        raidComponents.append(Component.literal("\n\n"));

        for (int i = 0; i < Models.Raid.MAX_CHALLENGES; i++) {
            raidComponents.append(
                    Component.literal("Challenge " + (i + 1) + ": ").withStyle(ChatFormatting.LIGHT_PURPLE));
            raidComponents.append(formatTime(event.getRoomTimes().get(i)));
        }

        raidComponents.append(Component.literal("\n"));

        raidComponents.append(Component.literal("Boss: ").withStyle(ChatFormatting.DARK_RED));
        raidComponents.append(formatTime(event.getRoomTimes().get(3)));
        raidComponents.append(Component.literal("\n"));

        raidComponents.append(Component.literal("Intermission: ").withStyle(ChatFormatting.DARK_GRAY));
        raidComponents.append(formatTime(event.getRoomTimes().get(4)));

        raidComponents.append(Component.literal("Total: ").withStyle(ChatFormatting.DARK_PURPLE));
        raidComponents.append(formatTime(event.getRaidTime()));

        McUtils.sendMessageToClient(raidComponents);
    }

    @SubscribeEvent
    public void onRaidPersonalBest(RaidNewBestTimeEvent event) {
        if (!printTimes.get()) return;

        long minutes = (event.getTime() / 1000) / 60;
        long seconds = (event.getTime() / 1000) % 60;
        long milliseconds = event.getTime() % 1000;

        McUtils.sendMessageToClient(Component.translatable(
                        "feature.wynntils.raidProgress.personalBest",
                        event.getRaid().getName(),
                        minutes,
                        seconds,
                        milliseconds)
                .withStyle(ChatFormatting.GOLD)
                .withStyle(ChatFormatting.BOLD));

        if (playSoundOnBest.get()) {
            McUtils.playSoundAmbient(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST);
        }
    }

    private String formatTime(long time) {
        long minutes = (time / 1000) / 60;
        long seconds = (time / 1000) % 60;
        long milliseconds = time % 1000;

        return String.format(TIME_FORMAT, minutes, seconds, milliseconds);
    }
}
