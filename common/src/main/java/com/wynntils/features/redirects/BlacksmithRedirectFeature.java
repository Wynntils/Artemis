/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.redirects;

import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.gear.type.GearTier;
import java.util.EnumMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.REDIRECTS)
public class BlacksmithRedirectFeature extends Feature {
    private static final Pattern BLACKSMITH_MESSAGE_PATTERN = Pattern.compile(
            "§5Blacksmith: §dYou (.+): (.+) for a total of §e(\\d+)§d (emeralds|scrap). It was a pleasure doing business with you.");
    private static final Pattern ITEM_PATTERN = Pattern.compile("§([fedacb53])([A-Z][a-zA-Z-'\\s]+)");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChat(ChatMessageReceivedEvent event) {
        Matcher messageMatcher = event.getOriginalStyledText().getMatcher(BLACKSMITH_MESSAGE_PATTERN);
        if (!messageMatcher.matches()) return;
        event.setCanceled(true);

        EnumMap<GearTier, Integer> totalItems = new EnumMap<>(GearTier.class);

        // How many emeralds/scrap we got from the transaction.
        String paymentString = messageMatcher.group(3);

        // Full message to send to the user.
        StyledText sendableMessage;

        // Should we use item, or items?
        String itemPluralizer;

        // Tracks count of sold or scrapped items
        // Sold item count is 1 + the count of ',' and "and" in the message.
        int totalItemInteger = 1;
        String itemPart = messageMatcher.group(2);
        for (char c : itemPart.toCharArray()) {
            if (c == ',') {
                totalItemInteger++;
            }
        }
        if (itemPart.contains("and")) {
            totalItemInteger++;
        }

        // This is for selling items for emeralds.
        if (messageMatcher.group(1).equals("sold me")) {
            // Tally up the items that we sold.
            for (Component sibling :
                    event.getOriginalStyledText().getComponent().getSiblings()) {
                // Retrieve the color code of the item, and then match it to the item tier.
                Matcher itemMatcher =
                        StyledText.fromComponent(sibling).getMatcher(ITEM_PATTERN); // Second group contains the items.

                if (!itemMatcher.matches()) {
                    continue;
                }

                char itemColorCode = itemMatcher.group(1).charAt(0);

                ChatFormatting itemColor = ChatFormatting.getByCode(
                        itemColorCode); // find the color code to find ChatFormatting and ascertain the tier.
                GearTier tierToIncrease = GearTier.fromChatFormatting(itemColor);

                if (tierToIncrease == null) continue;

                totalItems.put(tierToIncrease, totalItems.getOrDefault(tierToIncrease, 0) + 1);
            }

            itemPluralizer = totalItemInteger == 1 ? "item" : "items";

            // Build up the string that outlines how many items were sold in what tier (0/0/0/0/0/0/0/0).
            StringBuilder countByTier = new StringBuilder();
            for (GearTier tier : GearTier.values()) {
                countByTier.append('/');
                countByTier.append(tier.getChatFormatting().toString());
                countByTier.append(totalItems.getOrDefault(tier, 0));
                countByTier.append(ChatFormatting.LIGHT_PURPLE);
            }

            countByTier.setCharAt(0, '(');
            countByTier.append(")");

            String countByTierString = countByTier.toString();

            // Sold 1 (1/0/0/0/0/0/0/0) item for 4e.
            sendableMessage = StyledText.fromString(String.format(
                    "§dSold %d %s %s for §a%s%s§d.",
                    totalItemInteger,
                    itemPluralizer,
                    countByTierString,
                    paymentString,
                    EmeraldUnits.EMERALD.getSymbol()));
        }
        // Scrapping items for scrap.
        else {
            itemPluralizer = totalItemInteger == 1 ? "item" : "items";

            sendableMessage = StyledText.fromString(String.format(
                    "§dScrapped %d %s for §a%s scrap§d.", totalItemInteger, itemPluralizer, paymentString));
        }

        // Finally, we send the message.
        Managers.Notification.queueMessage(sendableMessage);
    }
}
