/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.Feature;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.ClientsideMessageEvent;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.type.CodedString;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.wynn.LocationUtils;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.CHAT)
public class ChatCoordinatesFeature extends Feature {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatReceived(ChatMessageReceivedEvent e) {
        if (!Models.WorldState.onWorld()) return;

        Component message = e.getMessage();

        e.setMessage(insertCoordinateComponents(message));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientsideMessage(ClientsideMessageEvent e) {
        if (!Models.WorldState.onWorld()) return;

        Component message = e.getComponent();

        e.setMessage(insertCoordinateComponents(message));
    }

    private static Component insertCoordinateComponents(Component message) {
        // no coordinate clickables to insert
        if (!LocationUtils.strictCoordinateMatcher(ComponentUtils.getCoded(message))
                .find()) return message;

        List<MutableComponent> components =
                message.getSiblings().stream().map(Component::copy).collect(Collectors.toList());
        components.add(0, message.plainCopy().withStyle(message.getStyle()));

        MutableComponent temp = Component.literal("");

        for (Component comp : components) {
            Matcher m = LocationUtils.strictCoordinateMatcher((ComponentUtils.getCoded(comp)));
            if (!m.find()) {
                Component newComponent = comp.copy();
                temp.append(newComponent);
                continue;
            }

            do {
                CodedString text = ComponentUtils.getCoded(comp);
                Style style = comp.getStyle();

                Optional<Location> location = LocationUtils.parseFromString(m.group());
                if (location.isEmpty()) { // couldn't decode, skip
                    comp = comp.copy();
                    continue;
                }

                MutableComponent preText = Component.literal(text.str().substring(0, m.start()));
                preText.withStyle(style);
                temp.append(preText);

                // create hover-able text component for the item
                Component compassComponent = ComponentUtils.createLocationComponent(location.get());
                temp.append(compassComponent);

                comp = Component.literal(ComponentUtils.getLastPartCodes(ComponentUtils.getCoded(preText))
                                + text.str().substring(m.end()))
                        .withStyle(style);
                m = LocationUtils.strictCoordinateMatcher(
                        ComponentUtils.getCoded(comp)); // recreate matcher for new substring
            } while (m.find()); // search for multiple items in the same message

            temp.append(comp); // leftover text after item(s)
        }

        return temp;
    }
}
