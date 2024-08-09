/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.ClientsideMessageEvent;
import com.wynntils.utils.mc.StyledTextUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.IterationDecision;
import com.wynntils.utils.wynn.LocationUtils;
import java.util.Optional;
import java.util.regex.Matcher;
import net.minecraft.network.chat.Style;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.CHAT)
public class ChatCoordinatesFeature extends Feature {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChatReceived(ChatMessageReceivedEvent e) {
        if (!Models.WorldState.onWorld()) return;

        StyledText styledText = e.getStyledText();

        StyledText modified = getStyledTextWithCoordinatesInserted(styledText);

        // No changes were made, there were no coordinates.
        if (styledText.equals(modified)) return;

        e.setMessage(modified);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientsideMessage(ClientsideMessageEvent e) {
        if (!Models.WorldState.onWorld()) return;

        StyledText styledText = e.getStyledText();

        StyledText modified = getStyledTextWithCoordinatesInserted(styledText);

        // No changes were made, there were no coordinates.
        if (styledText.equals(modified)) return;

        e.setMessage(modified);
    }

    private static StyledText getStyledTextWithCoordinatesInserted(StyledText styledText) {
        return styledText.iterate((part, changes) -> {
            StyledTextPart partToReplace = part;
            Matcher matcher =
                    LocationUtils.strictCoordinateMatcher(partToReplace.getString(null, PartStyle.StyleType.NONE));

            while (matcher.find()) {
                Optional<Location> location = LocationUtils.parseFromString(matcher.group(1));

                if (location.isEmpty()) {
                    continue;
                }

                String match = partToReplace.getString(null, PartStyle.StyleType.NONE);

                String firstPart = match.substring(0, matcher.start(1));
                String lastPart = match.substring(matcher.end(1));

                if (firstPart.endsWith("[") && lastPart.startsWith("]")) {
                    firstPart = firstPart.substring(0, firstPart.length() - 1);
                    lastPart = lastPart.substring(1);
                }

                PartStyle partStyle = partToReplace.getPartStyle();

                StyledTextPart first = new StyledTextPart(firstPart, partStyle.getStyle(), null, Style.EMPTY);
                StyledTextPart coordinate = StyledTextUtils.createLocationPart(location.get());
                StyledTextPart last = new StyledTextPart(lastPart, partStyle.getStyle(), null, Style.EMPTY);

                changes.remove(partToReplace);
                changes.add(first);
                changes.add(coordinate);
                changes.add(last);

                partToReplace = last;
                matcher = LocationUtils.strictCoordinateMatcher(lastPart);
            }

            return IterationDecision.CONTINUE;
        });
    }
}
