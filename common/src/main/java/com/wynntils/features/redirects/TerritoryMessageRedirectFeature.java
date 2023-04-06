/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.redirects;

import com.wynntils.core.components.Managers;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.Feature;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;

@ConfigCategory(Category.REDIRECTS)
public class TerritoryMessageRedirectFeature extends Feature {
    private static final Pattern TERRITORY_MESSAGE_PATTERN = Pattern.compile("§7\\[You are now (\\S+) (.+)\\]");

    // Handles the subtitle text event.
    @SubscribeEvent
    public void onSubtitleSetText(SubtitleSetTextEvent event) {
        StyledText styledText = StyledText.fromComponent(event.getComponent());
        Matcher matcher = styledText.getMatcher(TERRITORY_MESSAGE_PATTERN, PartStyle.StyleType.FULL);
        if (!matcher.matches()) return;

        event.setCanceled(true);

        String rawDirection = matcher.group(1);
        String rawTerritoryName = matcher.group(2);
        String directionalArrow;

        switch (rawDirection) {
            case "entering" -> directionalArrow = "→";
            case "leaving" -> directionalArrow = "←";
            default -> {
                return;
            }
        }

        // Want to account for weird stuff like "the Forgery" and make it "The Forgery"
        // for the sake of our brief message (looks odd otherwise).
        String territoryName = StringUtils.capitalize(rawTerritoryName);

        StyledText enteringMessage = StyledText.fromString(String.format("§7%s %s", directionalArrow, territoryName));
        Managers.Notification.queueMessage(enteringMessage);
    }

    // Handles the chat log message event, we don't want a duplicate so just cancel the event and rely on the subtitle
    // text event.
    @SubscribeEvent
    public void onChat(ChatMessageReceivedEvent event) {
        if (event.getOriginalCodedMessage()
                .getMatcher(TERRITORY_MESSAGE_PATTERN, PartStyle.StyleType.FULL)
                .matches()) {
            event.setCanceled(true);
        }
    }
}
