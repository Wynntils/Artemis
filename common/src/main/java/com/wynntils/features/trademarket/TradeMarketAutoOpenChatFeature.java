/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.trademarket;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.ChatScreen;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.TRADEMARKET)
public class TradeMarketAutoOpenChatFeature extends Feature {
    // Type the price in emeralds or type 'cancel' to cancel:
    // Type the amount you wish to buy or type 'cancel' to cancel:
    // Type the item name or type 'cancel' to cancel:
    private static final Pattern TYPE_TO_CHAT_PATTERN = Pattern.compile(
            "^§5\uE00A\uE002 \n\uE001 Type the item name or type 'cancel' to \n\uE001 cancel:\n\uE001\\s+$");

    private boolean openChatWhenContainerClosed = false;

    @SubscribeEvent
    public void onChatMessageReceive(ChatMessageReceivedEvent event) {
        if (!Models.WorldState.onWorld()) return;

        if (event.getOriginalStyledText().stripAlignment().matches(TYPE_TO_CHAT_PATTERN)) {
            openChatWhenContainerClosed = true;
        }
    }

    @SubscribeEvent
    public void onScreenClose(ScreenClosedEvent event) {
        if (!openChatWhenContainerClosed) return;

        openChatWhenContainerClosed = false;
        McUtils.mc().setScreen(new ChatScreen(""));
    }

    @SubscribeEvent
    public void onScreenOpen(ScreenOpenedEvent.Post event) {
        openChatWhenContainerClosed = false;
    }
}
