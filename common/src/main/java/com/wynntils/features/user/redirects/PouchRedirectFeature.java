/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.redirects;

import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.objects.MessageContainer;
import com.wynntils.features.user.overlays.GameUpdateOverlayFeature;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.utils.ComponentUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(category = "Game Update Overlay Redirects")
public class PouchRedirectFeature extends UserFeature {
    private static final Pattern INGREDIENT_POUCH_PICKUP_PATTERN = Pattern.compile("^§a\\+\\d+ §7.+§a to pouch$");
    private static final Pattern EMERALD_POUCH_PICKUP_PATTERN = Pattern.compile("§a\\+(\\d+)§7 Emeralds? §ato pouch");

    private static long lastEmeraldPouchPickup = 0;
    private static MessageContainer emeraldPouchMessage = null;

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
        String stringWithFormattingCodes = ComponentUtils.getFormatted(component);

        if (redirectIngredientPouch) {
            if (INGREDIENT_POUCH_PICKUP_PATTERN
                    .matcher(stringWithFormattingCodes)
                    .matches()) {
                event.setCanceled(true);
                GameUpdateOverlayFeature.queueMessage(stringWithFormattingCodes);
                return;
            }
        }

        if (redirectEmeraldPouch) {
            Matcher matcher = EMERALD_POUCH_PICKUP_PATTERN.matcher(stringWithFormattingCodes);
            if (matcher.matches()) {
                event.setCanceled(true);

                // If the last emerald pickup event was less than 3 seconds ago, assume Wynn has relayed us an "updated"
                // emerald title
                // Edit the first message it gave us with the new amount
                // editMessage doesn't return the new MessageContainer, so we can just keep re-using the first one
                if (lastEmeraldPouchPickup > System.currentTimeMillis() - 3000 && emeraldPouchMessage != null) {
                    GameUpdateOverlayFeature.editMessage(emeraldPouchMessage, stringWithFormattingCodes);
                } else {
                    emeraldPouchMessage = GameUpdateOverlayFeature.queueMessage(stringWithFormattingCodes);
                }

                lastEmeraldPouchPickup = System.currentTimeMillis();
            }
        }
    }
}
