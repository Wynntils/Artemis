/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.redirects;

import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import java.util.regex.Pattern;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.REDIRECTS)
public class AbilityRefreshRedirectFeature extends Feature {
    private static final Pattern REFRESH_PATTERN = Pattern.compile("\\[⬤\\] (.+) has been refreshed!");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ChatMessageReceivedEvent event) {
        if (event.getOriginalStyledText().matches(REFRESH_PATTERN, PartStyle.StyleType.NONE)) {
            event.setCanceled(true);
            Managers.Notification.queueMessage(event.getOriginalStyledText());
        }
    }
}
