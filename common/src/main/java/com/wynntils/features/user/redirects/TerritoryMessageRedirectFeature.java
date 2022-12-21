/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.redirects;

import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureCategory;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.notifications.NotificationManager;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.handlers.chat.events.ChatMessageReceivedEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;

@FeatureInfo(category = FeatureCategory.REDIRECTS)
public class TerritoryMessageRedirectFeature extends UserFeature {
    private static final Pattern TERRITORY_MESSAGE_PATTERN = Pattern.compile("§7\\[You are now (\\S+) (.+)\\]");

    // Handles the subtitle text event.
    @SubscribeEvent
    public void onSubtitleSetText(SubtitleSetTextEvent event) {
        String codedString = ComponentUtils.getCoded(event.getComponent());
        Matcher matcher = TERRITORY_MESSAGE_PATTERN.matcher(codedString);
        if (!matcher.matches()) return;

        event.setCanceled(true);

        String rawDirection = matcher.group(1);
        String rawTerritoryName = matcher.group(2);
        String directionalArrow;

        switch (rawDirection) {
            case "entering":
                directionalArrow = "→";
                break;
            case "leaving":
                directionalArrow = "←";
                break;
            default:
                return;
        }

        // Want to account for weird stuff like "the Forgery" and make it "The Forgery"
        // for the sake of our brief message (looks odd otherwise).
        String territoryName = StringUtils.capitalize(rawTerritoryName);

        String enteringMessage = String.format("§7%s %s", directionalArrow, territoryName);
        NotificationManager.queueMessage(enteringMessage);
    }

    // Handles the chat log message event, we don't want a duplicate so just cancel the event and rely on the subtitle
    // text event.
    @SubscribeEvent
    public void onChat(ChatMessageReceivedEvent event) {
        if (TERRITORY_MESSAGE_PATTERN.matcher(event.getOriginalCodedMessage()).matches()) event.setCanceled(true);
    }
}
