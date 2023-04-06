/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.Feature;
import com.wynntils.core.text.PartStyle;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.ChatSentEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.CHAT)
public class TradeMarketPriceConversionFeature extends Feature {
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("^§6Type the price in emeralds or type 'cancel' to cancel:$");
    private static final Pattern TRADE_MARKET_PATTERN = Pattern.compile("^What would you like to sell\\?$");
    private static final Pattern CANCELLED_PATTERN = Pattern.compile("^You moved and your chat input was canceled.$");

    private boolean shouldConvert = false;

    @SubscribeEvent
    public void onChatMessageReceive(ChatMessageReceivedEvent event) {
        if (event.getOriginalCodedMessage()
                .getMatcher(PRICE_PATTERN, PartStyle.StyleType.FULL)
                .matches()) {
            shouldConvert = true;
        }
        if (event.getOriginalCodedMessage()
                .getMatcher(CANCELLED_PATTERN, PartStyle.StyleType.FULL)
                .matches()) {
            shouldConvert = false;
        }
    }

    @SubscribeEvent
    public void onClientChat(ChatSentEvent event) {
        if (!shouldConvert) return;
        shouldConvert = false;

        String price = Models.Emerald.convertEmeraldPrice(event.getMessage());
        if (!price.isEmpty()) {
            event.setCanceled(true);
            McUtils.mc().getConnection().sendChat(price);
        }
    }

    @SubscribeEvent
    public void onGuiOpen(ScreenOpenedEvent.Post event) {
        if (TRADE_MARKET_PATTERN
                .matcher(event.getScreen().getTitle().getString())
                .matches()) {
            shouldConvert = false;
        }
    }
}
