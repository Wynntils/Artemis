/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.chat.transcoder;

import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.type.Pair;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.ArrayUtils;

public final class StyledText {
    private final Component temporaryWorkaround;

    private final List<StyledTextPart> parts;

    private final List<ClickEvent> clickEvents;
    private final List<HoverEvent> hoverEvents;

    StyledText(Component component) {
        temporaryWorkaround = component;

        parts = new LinkedList<>();
        clickEvents = new LinkedList<>();
        hoverEvents = new LinkedList<>();

        // Walk the component tree using DFS
        // Save the style of the parent component so we can inherit it
        Deque<Pair<Component, Style>> deque = new LinkedList<>();

        deque.add(new Pair<>(component, Style.EMPTY));

        while (!deque.isEmpty()) {
            Pair<Component, Style> currentPair = deque.pop();
            Component current = currentPair.key();
            Style parentStyle = currentPair.value();

            String componentString =
                    MutableComponent.create(current.getContents()).getString();

            StyledTextPart styledTextPart = new StyledTextPart(componentString, current.getStyle(), this, parentStyle);

            List<Pair<Component, Style>> siblingPairs = current.getSiblings().stream()
                    .map(sibling ->
                            new Pair<>(sibling, styledTextPart.getPartStyle().getStyle()))
                    .collect(Collectors.toList());

            Collections.reverse(siblingPairs);
            siblingPairs.forEach(deque::addFirst);

            parts.add(styledTextPart);
        }
    }

    StyledText(String string) {
        parts = new LinkedList<>();
        clickEvents = new LinkedList<>();
        hoverEvents = new LinkedList<>();

        Style currentStyle = Style.EMPTY;
        StringBuilder currentString = new StringBuilder();

        boolean nextIsColoring = false;

        for (char current : string.toCharArray()) {
            if (nextIsColoring) {
                nextIsColoring = false;

                ChatFormatting formatting = ChatFormatting.getByCode(current);

                if (formatting == null) {
                    currentString.append(ChatFormatting.PREFIX_CODE);
                    currentString.append(current);
                    continue;
                }

                // We already had some text with the current style
                // Append it before modifying the style
                if (!currentString.isEmpty()) {
                    parts.add(new StyledTextPart(currentString.toString(), currentStyle, this, null));
                    currentString = new StringBuilder();
                }

                currentStyle = currentStyle.applyFormat(formatting);

                continue;
            }

            if (current == ChatFormatting.PREFIX_CODE) {
                nextIsColoring = true;
                continue;
            }

            currentString.append(current);
        }

        // Check if we have some text left
        if (!currentString.isEmpty()) {
            parts.add(new StyledTextPart(currentString.toString(), currentStyle, this, null));
        }

        // We can't know the component, just use our own representation
        temporaryWorkaround = getComponent();
    }

    private StyledText(
            List<StyledTextPart> parts,
            Component temporaryWorkaround,
            List<ClickEvent> clickEvents,
            List<HoverEvent> hoverEvents) {
        this.parts = parts;
        this.temporaryWorkaround = temporaryWorkaround;
        this.clickEvents = clickEvents;
        this.hoverEvents = hoverEvents;
    }

    public static StyledText fromComponent(Component component) {
        return new StyledText(component);
    }

    public static StyledText fromString(String string) {
        return new StyledText(string);
    }

    // We don't want to expose the actual string to the outside world
    // If you need to do an operation with this string, implement it as a method
    public String getString(PartStyle.StyleType type) {
        if (type == PartStyle.StyleType.FULL) {
            return ComponentUtils.getCoded(temporaryWorkaround);
        }

        StringBuilder builder = new StringBuilder();

        PartStyle previousStyle = null;
        for (StyledTextPart part : parts) {
            builder.append(part.getString(previousStyle, type));
            previousStyle = part.getPartStyle();
        }

        return builder.toString();
    }

    public MutableComponent getComponent() {
        if (parts.isEmpty()) {
            return Component.empty();
        }

        MutableComponent component = parts.get(0).getComponent();

        for (int i = 1; i < parts.size(); i++) {
            component.append(parts.get(i).getComponent());
        }

        return component;
    }

    public StyledText getNormalized() {
        return new StyledText(
                parts.stream().map(StyledTextPart::asNormalized).collect(Collectors.toList()),
                temporaryWorkaround,
                clickEvents,
                hoverEvents);
    }

    public Matcher getMatcher(Pattern pattern) {
        return getMatcher(pattern, PartStyle.StyleType.DEFAULT);
    }

    public Matcher getMatcher(Pattern pattern, PartStyle.StyleType styleType) {
        return pattern.matcher(getString(styleType));
    }

    /**
     * Splits the style string into two parts at the given index.
     * The index is part of the second string.
     *
     * @param index The index to split at.
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    public StyledText splitAt(int index) {
        int stringLength = 0;

        if (index < 0) {
            throw new IndexOutOfBoundsException("Index must be non-negative.");
        }

        StyledTextPart partToSplit = null;
        int indexToSplit = 0;

        for (StyledTextPart part : parts) {
            int currentLength = part.getString(null, PartStyle.StyleType.NONE).length();
            stringLength += currentLength;

            if (index < stringLength) {
                partToSplit = part;
                indexToSplit = index - (stringLength - currentLength);
                break;
            }
        }

        if (partToSplit == null) {
            throw new IndexOutOfBoundsException("Index out of bounds.");
        }

        List<StyledTextPart> newParts = new LinkedList<>(parts);

        String partString = partToSplit.getString(null, PartStyle.StyleType.NONE);

        String firstString = partString.substring(0, indexToSplit);
        String secondString = partString.substring(indexToSplit);

        StyledTextPart partBefore = getPartBefore(partToSplit);
        Style styleBefore =
                partBefore == null ? Style.EMPTY : partBefore.getPartStyle().getStyle();

        Style style = partToSplit.getPartStyle().getStyle();
        StyledTextPart firstPart = new StyledTextPart(firstString, style, this, styleBefore);
        StyledTextPart secondPart = new StyledTextPart(
                secondString, style, this, firstPart.getPartStyle().getStyle());

        int indexOfPart = parts.indexOf(partToSplit);

        newParts.add(indexOfPart, firstPart);
        newParts.add(indexOfPart + 1, secondPart);
        newParts.remove(partToSplit);

        return new StyledText(newParts, temporaryWorkaround, clickEvents, hoverEvents);
    }

    public StyledTextPart getPartFinding(Pattern pattern) {
        return getPartFinding(pattern, PartStyle.StyleType.DEFAULT);
    }

    public StyledTextPart getPartFinding(Pattern pattern, PartStyle.StyleType styleType) {
        PartStyle previousPartStyle = null;

        for (StyledTextPart part : parts) {
            if (pattern.matcher(part.getString(previousPartStyle, styleType)).find()) {
                return part;
            }

            previousPartStyle = part.getPartStyle();
        }

        return null;
    }

    public StyledTextPart getPartMatching(Pattern pattern) {
        return getPartMatching(pattern, PartStyle.StyleType.DEFAULT);
    }

    public StyledTextPart getPartMatching(Pattern pattern, PartStyle.StyleType styleType) {
        PartStyle previousPartStyle = null;

        for (StyledTextPart part : parts) {
            if (pattern.matcher(part.getString(previousPartStyle, styleType)).matches()) {
                return part;
            }

            previousPartStyle = part.getPartStyle();
        }

        return null;
    }

    public int getPartCount() {
        return parts.size();
    }

    int addClickEvent(ClickEvent clickEvent) {
        // Check if the event is already in the list
        for (int i = 0; i < clickEvents.size(); i++) {
            ClickEvent event = clickEvents.get(i);
            if (event.equals(clickEvent)) {
                return i + 1;
            }
        }

        clickEvents.add(clickEvent);

        return clickEvents.size();
    }

    int addHoverEvent(HoverEvent hoverEvent) {
        // Check if the event is already in the list
        for (int i = 0; i < hoverEvents.size(); i++) {
            HoverEvent event = hoverEvents.get(i);
            if (event.equals(hoverEvent)) {
                return i + 1;
            }
        }

        hoverEvents.add(hoverEvent);

        return hoverEvents.size();
    }

    private StyledTextPart getPartBefore(StyledTextPart part) {
        int index = parts.indexOf(part);
        if (index == 0) {
            return null;
        }

        return parts.get(index - 1);
    }

    @Override
    public String toString() {
        return "CodedString{" + "parts="
                + ArrayUtils.toString(parts) + ", clickEvents="
                + ArrayUtils.toString(clickEvents) + ", hoverEvents="
                + ArrayUtils.toString(hoverEvents) + '}';
    }
}
