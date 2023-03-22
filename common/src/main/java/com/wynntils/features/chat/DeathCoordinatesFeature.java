/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.Feature;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.CHAT)
public class DeathCoordinatesFeature extends Feature {
    private static final String WYNN_DEATH_MESSAGE = "§r §4§lYou have died...";
    private Vec3 lastLocation;

    @SubscribeEvent
    public void onChatReceived(ChatMessageReceivedEvent e) {
        if (!Models.WorldState.onWorld() || lastLocation == null) return;
        if (!e.getCodedMessage().contains(WYNN_DEATH_MESSAGE)) return;

        MutableComponent deathMessage = Component.translatable("feature.wynntils.deathCoordinates.diedAt")
                .withStyle(ChatFormatting.DARK_RED);
        MutableComponent coordMessage = Component.translatable(
                        "feature.wynntils.deathCoordinates.coordinates",
                        (int) lastLocation.x(),
                        (int) lastLocation.y(),
                        (int) lastLocation.z())
                .withStyle(ChatFormatting.DARK_RED)
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(s -> s.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/compass at " + (int) lastLocation.x() + " " + (int) lastLocation.y() + " "
                                + (int) lastLocation.z())))
                .withStyle(s -> s.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("feature.wynntils.deathCoordindates.clickToCompass"))));

        e.setCanceled(true);
        McUtils.player().sendSystemMessage(deathMessage.append(coordMessage));
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        if (McUtils.player() == null) return;
        lastLocation = McUtils.player().position();
    }
}
