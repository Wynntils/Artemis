/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.mc;

import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.utils.mc.type.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

public final class StyledTextUtils {
    private static final Pattern COORDINATE_PATTERN =
            Pattern.compile(".*\\[(-?\\d+)(?:.\\d+)?, ?(-?\\d+)(?:.\\d+)?, ?(-?\\d+)(?:.\\d+)?\\].*");

    public static StyledTextPart createLocationPart(Location location) {
        String locationString = "[%d, %d, %d]".formatted(location.x, location.y, location.z);
        Style style = Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withUnderlined(true);

        style = style.withClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/compass at " + location.x + " " + location.y + " " + location.z));
        style = style.withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT, Component.translatable("utils.wynntils.component.clickToSetCompass")));

        return new StyledTextPart(locationString, style, null, Style.EMPTY);
    }

    public static Optional<Location> extractLocation(StyledText text) {
        Matcher matcher = text.getMatcher(COORDINATE_PATTERN, PartStyle.StyleType.NONE);
        if (!matcher.matches()) return Optional.empty();

        return Optional.of(new Location(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))));
    }

    public static StyledText joinLines(List<StyledText> lines) {
        String description = String.join(
                        " ", lines.stream().map(StyledText::getString).toList())
                .replaceAll("\\s+", " ")
                .trim();
        return StyledText.fromString(description);
    }

    /**
     * This method is used by {@link com.wynntils.handlers.chat.ChatHandler} to split multi-line messages.
     * Multi-line messages use new lines not just to split the multi-line message, but also to format the message.
     * If a part is only a new line, we know it's a new message, but if the new line is in the middle of a part,
     * we know it's a new line in the same message, which we don't want to split.
     * @param styledText The styled text to split
     * @return A list of styled texts, each representing a line
     */
    public static List<StyledText> splitInLines(StyledText styledText) {
        List<StyledText> newLines = new ArrayList<>();

        List<StyledTextPart> parts = new ArrayList<>();
        for (StyledTextPart part : styledText) {
            String partString = part.getString(null, PartStyle.StyleType.NONE);

            if (partString.equals("\n")) {
                newLines.add(StyledText.fromParts(parts));
                parts.clear();
            } else {
                parts.add(part);
            }
        }

        if (!parts.isEmpty()) {
            newLines.add(StyledText.fromParts(parts));
        }

        return newLines;
    }

    /**
     * Removes all newlines from the given styled text.
     */
    public static StyledText joinAllLines(StyledText styledText) {
        return styledText.replaceAll("\n", "");
    }
}
