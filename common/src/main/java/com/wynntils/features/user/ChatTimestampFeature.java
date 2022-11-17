/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.chat.ChatModel;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.managers.Model;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wynn.event.ChatMessageReceivedEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ChatTimestampFeature extends UserFeature {
    @Config
    public String formatPattern = "HH:mm:ss";

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);

    @Override
    public List<Class<? extends Model>> getModelDependencies() {
        return List.of(ChatModel.class);
    }

    @Override
    protected void onConfigUpdate(ConfigHolder configHolder) {
        // Try to set the new format string and if it fails revert to the default
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(formatPattern);

            formatter = fmt;
        } catch (Exception e) {
            formatter = null;

            McUtils.sendMessageToClient(new TranslatableComponent("feature.wynntils.chatTimestamp.invalidFormatMsg")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ChatMessageReceivedEvent event) {
        if (formatter == null) return;

        Component message = event.getMessage();

        LocalDateTime date = LocalDateTime.now();
        MutableComponent timestamp = new TextComponent("§8[§7" + date.format(formatter) + "§8]§r ");

        timestamp.append(message);

        event.setMessage(timestamp);
    }
}
