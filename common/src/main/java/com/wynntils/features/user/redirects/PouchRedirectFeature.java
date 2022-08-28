/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.redirects;

import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureCategory;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.notifications.MessageContainer;
import com.wynntils.core.notifications.NotificationManager;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.utils.ComponentUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(category = FeatureCategory.REDIRECTS)
public class PouchRedirectFeature extends UserFeature {
    private static final Pattern INGREDIENT_POUCH_PICKUP_PATTERN = Pattern.compile("^§a\\+\\d+ §7.+§a to pouch$");
    private static final Pattern EMERALD_POUCH_PICKUP_PATTERN = Pattern.compile("§a\\+(\\d+)§7 Emeralds? §ato pouch");

    private long lastEmeraldPouchPickup = 0;
    private MessageContainer emeraldPouchMessage = null;

    @Config
    public boolean redirectIngredientPouch = true;

    @Config
    public boolean redirectEmeraldPouch = true;

    @SubscribeEvent
    public void onSubtitleSetText(SubtitleSetTextEvent event) {
        if (!redirectEmeraldPouch && !redirectIngredientPouch) {
            return;
        }

        Component component = event.getComponent();
        String codedString = ComponentUtils.getCoded(component);

        if (redirectIngredientPouch) {
            if (INGREDIENT_POUCH_PICKUP_PATTERN.matcher(codedString).matches()) {
                event.setCanceled(true);
                NotificationManager.queueMessage(codedString);
                return;
            }
        }

        if (redirectEmeraldPouch) {
            Matcher matcher = EMERALD_POUCH_PICKUP_PATTERN.matcher(codedString);
            if (matcher.matches()) {
                event.setCanceled(true);

                // If the last emerald pickup event was less than 3 seconds ago, assume Wynn has relayed us an "updated"
                // emerald title
                // Edit the first message it gave us with the new amount
                // editMessage doesn't return the new MessageContainer, so we can just keep re-using the first one
                if (lastEmeraldPouchPickup > System.currentTimeMillis() - 3000 && emeraldPouchMessage != null) {
                    NotificationManager.editMessage(emeraldPouchMessage, codedString);
                } else {
                    emeraldPouchMessage = NotificationManager.queueMessage(codedString);
                }

                lastEmeraldPouchPickup = System.currentTimeMillis();
            }
        }
    }
}
