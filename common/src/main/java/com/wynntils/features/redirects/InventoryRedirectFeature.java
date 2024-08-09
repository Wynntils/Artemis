/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.redirects;

import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.notifications.MessageContainer;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.REDIRECTS)
public class InventoryRedirectFeature extends Feature {
    private static final Pattern INGREDIENT_POUCH_PICKUP_PATTERN = Pattern.compile("^§a\\+\\d+ §7.+§a to pouch$");
    private static final Pattern EMERALD_POUCH_PICKUP_PATTERN = Pattern.compile("§a\\+(\\d+)§7 Emeralds? §ato pouch");
    private static final Pattern POTION_STACK_PATTERN = Pattern.compile("§a\\+(\\d+)§7 potion §acharges?");

    private long lastEmeraldPouchPickup = 0;
    private MessageContainer emeraldPouchMessage = null;

    @Persisted
    public final Config<Boolean> redirectIngredientPouch = new Config<>(true);

    @Persisted
    public final Config<Boolean> redirectEmeraldPouch = new Config<>(true);

    @Persisted
    public final Config<Boolean> redirectPotionStack = new Config<>(true);

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        lastEmeraldPouchPickup = 0;
        emeraldPouchMessage = null;
    }

    @SubscribeEvent
    public void onSubtitleSetText(SubtitleSetTextEvent event) {
        if (!redirectEmeraldPouch.get() && !redirectIngredientPouch.get() && !redirectPotionStack.get()) {
            return;
        }

        Component component = event.getComponent();
        StyledText styledText = StyledText.fromComponent(component);

        if (redirectIngredientPouch.get()) {
            if (styledText.getMatcher(INGREDIENT_POUCH_PICKUP_PATTERN).matches()) {
                event.setCanceled(true);
                Managers.Notification.queueMessage(styledText);
                return;
            }
        }

        if (redirectEmeraldPouch.get()) {
            Matcher matcher = styledText.getMatcher(EMERALD_POUCH_PICKUP_PATTERN);
            if (matcher.matches()) {
                event.setCanceled(true);

                // If the last emerald pickup event was less than 3 seconds ago, assume Wynn has relayed us an "updated"
                // emerald title
                // Edit the first message it gave us with the new amount
                // editMessage doesn't return the new MessageContainer, so we can just keep re-using the first one
                if (lastEmeraldPouchPickup > System.currentTimeMillis() - 3000 && emeraldPouchMessage != null) {
                    emeraldPouchMessage = Managers.Notification.editMessage(emeraldPouchMessage, styledText);
                } else {
                    emeraldPouchMessage = Managers.Notification.queueMessage(styledText);
                }

                lastEmeraldPouchPickup = System.currentTimeMillis();

                return;
            }
        }

        if (redirectPotionStack.get()) {
            Matcher matcher = styledText.getMatcher(POTION_STACK_PATTERN);
            if (matcher.matches()) {
                event.setCanceled(true);
                String potionCount = matcher.group(1);
                StyledText potionMessage = StyledText.fromString(String.format("§a+%s Potion Charges", potionCount));
                Managers.Notification.queueMessage(potionMessage);

                return;
            }
        }
    }
}
