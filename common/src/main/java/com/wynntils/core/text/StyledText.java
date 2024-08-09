/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.text;

import com.google.common.collect.Iterables;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.type.IterationDecision;
import com.wynntils.utils.type.Pair;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class StyledText implements Iterable<StyledTextPart> {
    // High surrogate characters for the positive and negative space characters
    // These can be used to trim unicode spacers from StyledTexts
    private static final char POSITIVE_SPACE_HIGH_SURROGATE = '\uDB00';
    private static final char NEGATIVE_SPACE_HIGH_SURROGATE = '\uDAFF';

    public static final StyledText EMPTY = new StyledText(List.of(), List.of(), List.of());

    private final List<StyledTextPart> parts;

    private final List<ClickEvent> clickEvents;
    private final List<HoverEvent> hoverEvents;

    /**
     * Note: All callers of this constructor should ensure that the event lists are collected from the parts.
     *       Additionally, they should ensure that the events are distinct.
     */
    private StyledText(List<StyledTextPart> parts, List<ClickEvent> clickEvents, List<HoverEvent> hoverEvents) {
        this.parts = parts.stream()
                .filter(styledTextPart -> !styledTextPart.isEmpty())
                .map(styledTextPart -> new StyledTextPart(styledTextPart, this))
                .collect(Collectors.toList());
        this.clickEvents = Collections.unmodifiableList(clickEvents);
        this.hoverEvents = Collections.unmodifiableList(hoverEvents);
    }

    public static StyledText fromComponent(Component component) {
        List<StyledTextPart> parts = new ArrayList<>();

        // Walk the component tree using DFS
        // Component#visit behaves weirdly, so we do it manually
        // Save the style of the parent component so we can inherit it
        Deque<Pair<Component, Style>> deque = new LinkedList<>();

        deque.add(new Pair<>(component, Style.EMPTY));

        while (!deque.isEmpty()) {
            Pair<Component, Style> currentPair = deque.pop();
            Component current = currentPair.key();
            Style parentStyle = currentPair.value();

            // We use getContents here to get this and only this component's string.
            String componentString =
                    MutableComponent.create(current.getContents()).getString();

            List<StyledTextPart> styledTextParts =
                    StyledTextPart.fromCodedString(componentString, current.getStyle(), null, parentStyle);

            // Only actual styles are inherited, string formatting codes are not
            Style styleToFollowForChildren = current.getStyle().applyTo(parentStyle);

            List<Pair<Component, Style>> siblingPairs = current.getSiblings().stream()
                    .map(sibling -> new Pair<>(sibling, styleToFollowForChildren))
                    .collect(Collectors.toList());

            Collections.reverse(siblingPairs);
            siblingPairs.forEach(deque::addFirst);

            // Disallow empty parts
            parts.addAll(
                    styledTextParts.stream().filter(part -> !part.isEmpty()).toList());
        }

        return fromParts(parts);
    }

    public static StyledText fromString(String codedString) {
        return new StyledText(
                StyledTextPart.fromCodedString(codedString, Style.EMPTY, null, Style.EMPTY), List.of(), List.of());
    }

    public static StyledText fromModifiedString(String codedString, StyledText styledText) {
        List<HoverEvent> hoverEvents = List.copyOf(styledText.hoverEvents);
        List<ClickEvent> clickEvents = List.copyOf(styledText.clickEvents);

        return new StyledText(
                StyledTextPart.fromCodedString(codedString, Style.EMPTY, styledText, Style.EMPTY),
                clickEvents,
                hoverEvents);
    }

    public static StyledText fromUnformattedString(String unformattedString) {
        StyledTextPart part = new StyledTextPart(unformattedString, Style.EMPTY, null, Style.EMPTY);
        return new StyledText(List.of(part), List.of(), List.of());
    }

    public static StyledText fromPart(StyledTextPart part) {
        return fromParts(List.of(part));
    }

    public static StyledText fromParts(List<StyledTextPart> parts) {
        // Collect the events
        List<ClickEvent> clickEvents = new ArrayList<>();
        List<HoverEvent> hoverEvents = new ArrayList<>();

        for (StyledTextPart part : parts) {
            ClickEvent clickEvent = part.getPartStyle().getClickEvent();
            if (clickEvent != null && !clickEvents.contains(clickEvent)) {
                clickEvents.add(clickEvent);
            }

            HoverEvent hoverEvent = part.getPartStyle().getHoverEvent();
            if (hoverEvent != null && !hoverEvents.contains(hoverEvent)) {
                hoverEvents.add(hoverEvent);
            }
        }

        return new StyledText(parts, clickEvents, hoverEvents);
    }

    // We don't want to expose the actual string to the outside world
    // If you need to do an operation with this string, implement it as a method
    public String getString(PartStyle.StyleType type) {
        StringBuilder builder = new StringBuilder();

        PartStyle previousStyle = null;
        for (StyledTextPart part : parts) {
            builder.append(part.getString(previousStyle, type));
            previousStyle = part.getPartStyle();
        }

        return builder.toString();
    }

    /**
     * @return The string representation of this {@link StyledText} with default formatting codes.
     */
    public String getString() {
        return getString(PartStyle.StyleType.DEFAULT);
    }

    public String getStringWithoutFormatting() {
        return getString(PartStyle.StyleType.NONE);
    }

    public MutableComponent getComponent() {
        if (parts.isEmpty()) {
            return Component.empty();
        }

        MutableComponent component = Component.empty();

        for (StyledTextPart part : parts) {
            component.append(part.getComponent());
        }

        return component;
    }

    public int length() {
        return parts.stream().mapToInt(StyledTextPart::length).sum();
    }

    public int length(PartStyle.StyleType styleType) {
        return getString(styleType).length();
    }

    public static StyledText join(StyledText styledTextSeparator, StyledText... texts) {
        List<StyledTextPart> parts = new ArrayList<>();

        final int length = texts.length;
        for (int i = 0; i < length; i++) {
            StyledText text = texts[i];
            parts.addAll(text.parts);

            if (i != length - 1) {
                parts.addAll(styledTextSeparator.parts);
            }
        }

        return fromParts(parts);
    }

    public static StyledText join(StyledText styledTextSeparator, Iterable<StyledText> texts) {
        return join(styledTextSeparator, Iterables.toArray(texts, StyledText.class));
    }

    public static StyledText join(String codedStringSeparator, StyledText... texts) {
        return join(StyledText.fromString(codedStringSeparator), texts);
    }

    public static StyledText join(String codedStringSeparator, Iterable<StyledText> texts) {
        return join(StyledText.fromString(codedStringSeparator), Iterables.toArray(texts, StyledText.class));
    }

    public static StyledText concat(StyledText... texts) {
        return fromParts(Arrays.stream(texts)
                .map(text -> text.parts)
                .flatMap(List::stream)
                .toList());
    }

    public static StyledText concat(Iterable<StyledText> texts) {
        return concat(Iterables.toArray(texts, StyledText.class));
    }

    public StyledText getNormalized() {
        return fromParts(parts.stream().map(StyledTextPart::asNormalized).collect(Collectors.toList()));
    }

    /**
     * Strips all of Wynncraft's unicode alignment characters from this {@link StyledText}.
     * @return the stripped {@link StyledText}
     */
    public StyledText stripAlignment() {
        return iterate((part, functionParts) -> {
            String text = part.getString(null, PartStyle.StyleType.NONE);

            // If the text contains the positive or negative space characters,
            // then we need to strip them, and the following character
            if (text.contains(String.valueOf(POSITIVE_SPACE_HIGH_SURROGATE))
                    || text.contains(String.valueOf(NEGATIVE_SPACE_HIGH_SURROGATE))) {
                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < text.length(); i++) {
                    char ch = text.charAt(i);

                    if (Character.isHighSurrogate(ch)
                            && (ch == POSITIVE_SPACE_HIGH_SURROGATE || ch == NEGATIVE_SPACE_HIGH_SURROGATE)) {
                        // Skip the surrogate pair (high and low surrogate)
                        if (i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                            i++; // Skip the low surrogate
                        }
                    } else {
                        builder.append(ch);
                    }
                }

                functionParts.set(
                        0,
                        new StyledTextPart(
                                builder.toString(), part.getPartStyle().getStyle(), null, Style.EMPTY));
            }

            return IterationDecision.CONTINUE;
        });
    }

    public StyledText trim() {
        if (parts.isEmpty()) {
            return this;
        }

        List<StyledTextPart> newParts = new ArrayList<>(parts);
        newParts.set(0, newParts.getFirst().stripLeading());

        int lastIndex = newParts.size() - 1;
        newParts.set(lastIndex, newParts.get(lastIndex).stripTrailing());

        return fromParts(newParts);
    }

    public boolean isEmpty() {
        return parts.isEmpty();
    }

    public boolean isBlank() {
        return parts.stream().allMatch(StyledTextPart::isBlank);
    }

    public boolean contains(String codedString) {
        return contains(codedString, PartStyle.StyleType.DEFAULT);
    }

    public boolean contains(StyledText styledText) {
        return contains(styledText.getString(PartStyle.StyleType.DEFAULT), PartStyle.StyleType.DEFAULT);
    }

    public boolean contains(String codedString, PartStyle.StyleType styleType) {
        return getString(styleType).contains(codedString);
    }

    public boolean contains(StyledText styledText, PartStyle.StyleType styleType) {
        return contains(styledText.getString(styleType), styleType);
    }

    public boolean startsWith(String codedString) {
        return startsWith(codedString, PartStyle.StyleType.DEFAULT);
    }

    public boolean startsWith(StyledText styledText) {
        return startsWith(styledText.getString(PartStyle.StyleType.DEFAULT), PartStyle.StyleType.DEFAULT);
    }

    public boolean startsWith(String codedString, PartStyle.StyleType styleType) {
        return getString(styleType).startsWith(codedString);
    }

    public boolean startsWith(StyledText styledText, PartStyle.StyleType styleType) {
        return startsWith(styledText.getString(styleType), styleType);
    }

    public boolean endsWith(String codedString) {
        return endsWith(codedString, PartStyle.StyleType.DEFAULT);
    }

    public boolean endsWith(StyledText styledText) {
        return endsWith(styledText.getString(PartStyle.StyleType.DEFAULT), PartStyle.StyleType.DEFAULT);
    }

    public boolean endsWith(String codedString, PartStyle.StyleType styleType) {
        return getString(styleType).endsWith(codedString);
    }

    public boolean endsWith(StyledText styledText, PartStyle.StyleType styleType) {
        return endsWith(styledText.getString(styleType), styleType);
    }

    public Matcher getMatcher(Pattern pattern) {
        return getMatcher(pattern, PartStyle.StyleType.DEFAULT);
    }

    public Matcher getMatcher(Pattern pattern, PartStyle.StyleType styleType) {
        return pattern.matcher(getString(styleType));
    }

    public boolean matches(Pattern pattern) {
        return matches(pattern, PartStyle.StyleType.DEFAULT);
    }

    public boolean matches(Pattern pattern, PartStyle.StyleType styleType) {
        return pattern.matcher(getString(styleType)).matches();
    }

    public boolean find(Pattern pattern) {
        return find(pattern, PartStyle.StyleType.DEFAULT);
    }

    public boolean find(Pattern pattern, PartStyle.StyleType styleType) {
        return pattern.matcher(getString(styleType)).find();
    }

    public StyledText append(StyledText styledText) {
        return concat(this, styledText);
    }

    public StyledText append(String codedString) {
        return append(StyledText.fromString(codedString));
    }

    public StyledText appendPart(StyledTextPart part) {
        List<StyledTextPart> newParts = new ArrayList<>(parts);
        newParts.add(part);
        return fromParts(newParts);
    }

    public StyledText prepend(StyledText styledText) {
        return concat(styledText, this);
    }

    public StyledText prepend(String codedString) {
        return prepend(StyledText.fromString(codedString));
    }

    public StyledText prependPart(StyledTextPart part) {
        List<StyledTextPart> newParts = new ArrayList<>(parts);
        newParts.addFirst(part);
        return fromParts(newParts);
    }

    /**
     * Splits this {@link StyledText} into multiple {@link StyledText}s at the given index.
     * <p> Note that {@link PartStyle.StyleType.NONE} is used when splitting.
     * @param regex the regex to split at
     * @return the split {@link StyledText}s
     */
    public StyledText[] split(String regex) {
        // If this is an empty text, return an array with a single empty text
        if (parts.isEmpty()) {
            return new StyledText[] {StyledText.EMPTY};
        }

        final Pattern pattern = Pattern.compile(regex);

        List<StyledText> splitTexts = new ArrayList<>();
        List<StyledTextPart> splitParts = new ArrayList<>();

        for (int i = 0; i < parts.size(); i++) {
            StyledTextPart part = parts.get(i);
            String partString = part.getString(null, PartStyle.StyleType.NONE);

            // Avoid empty parts at the end of the list, but keep them otherwise
            int maxSplit = i == parts.size() - 1 ? 0 : -1;

            List<String> stringParts =
                    Arrays.stream(pattern.split(partString, maxSplit)).toList();

            Matcher matcher = pattern.matcher(partString);
            if (matcher.find()) {
                // If the pattern matches, then we have a split
                // Create the new styled texts

                // Each part is a new text, but the first and last one connect to the previous/next text
                for (int j = 0; j < stringParts.size(); j++) {
                    String stringPart = stringParts.get(j);
                    splitParts.add(
                            new StyledTextPart(stringPart, part.getPartStyle().getStyle(), null, Style.EMPTY));

                    // If this is the last part, then we might need to add other parts
                    if (j != stringParts.size() - 1) {
                        splitTexts.add(fromParts(splitParts));
                        splitParts.clear();
                    }
                }

                continue;
            }

            // If the pattern doesn't match, then we can just add the part to the list
            splitParts.add(part);
        }

        // Add the last text
        if (!splitParts.isEmpty()) {
            splitTexts.add(fromParts(splitParts));
        }

        return splitTexts.toArray(StyledText[]::new);
    }

    public StyledText substring(int beginIndex) {
        return substring(beginIndex, length(), PartStyle.StyleType.NONE);
    }

    public StyledText substring(int beginIndex, PartStyle.StyleType styleType) {
        return substring(beginIndex, length(styleType), styleType);
    }

    public StyledText substring(int beginIndex, int endIndex) {
        return substring(beginIndex, endIndex, PartStyle.StyleType.NONE);
    }

    /**
     * Returns a new {@link StyledText} that is a substring of this {@link StyledText}.
     *
     * @param beginIndex the beginning index, inclusive
     * @param endIndex   the ending index, exclusive
     * @param styleType  the style type to use when calculating indexes
     * @return the new {@link StyledText}
     * @throws IndexOutOfBoundsException if the indexes are out of bounds
     * @throws IllegalArgumentException  if the substring splits a formatting code
     */
    public StyledText substring(int beginIndex, int endIndex, PartStyle.StyleType styleType) {
        if (endIndex < beginIndex) {
            throw new IndexOutOfBoundsException("endIndex must be greater than beginIndex");
        }
        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException("beginIndex must be greater than or equal to 0");
        }
        if (endIndex > length(styleType)) {
            throw new IndexOutOfBoundsException("endIndex must be less than or equal to length(styleType)");
        }

        List<StyledTextPart> includedParts = new ArrayList<>();

        int currentIndex = 0;
        PartStyle previousPartStyle = null;

        for (StyledTextPart part : parts) {
            if (currentIndex >= beginIndex && currentIndex + part.length() < endIndex) {
                // 1. This full part is included

                includedParts.add(part);
            } else if (MathUtils.rangesIntersect(
                    currentIndex, currentIndex + part.length(), beginIndex, endIndex - 1)) {
                // 2. This part is partially included

                int startIndexInPart = Math.max(0, beginIndex - currentIndex);
                int endIndexInPart = Math.min(part.length(), endIndex - currentIndex);

                String fullString = part.getString(previousPartStyle, styleType);

                String beforeSubstring = fullString.substring(0, startIndexInPart);
                String includedSubstring = fullString.substring(startIndexInPart, endIndexInPart);

                // If the substring splits a formatting code, then we need to throw an exception
                if (beforeSubstring.endsWith(String.valueOf(ChatFormatting.PREFIX_CODE))
                        || includedSubstring.endsWith(String.valueOf(ChatFormatting.PREFIX_CODE))) {
                    throw new IllegalArgumentException("The substring splits a formatting code.");
                }

                // Reparse the string to drop the formatting codes and keep them as part styles
                includedParts.addAll(StyledTextPart.fromCodedString(
                        includedSubstring, part.getPartStyle().getStyle(), null, Style.EMPTY));
            }

            currentIndex += part.getString(previousPartStyle, styleType).length();

            previousPartStyle = part.getPartStyle();
        }

        return fromParts(includedParts);
    }

    public StyledText[] partition(int... indexes) {
        return partition(PartStyle.StyleType.NONE, indexes);
    }

    /**
     * Splits this {@link StyledText} into multiple {@link StyledText}s at the given indexes.
     * @param styleType the style type to use when calculating indexes
     * @param indexes the indexes to split at, in ascending order
     * @return the split {@link StyledText}s as an array
     * @throws IllegalArgumentException if the indexes are not in ascending order or an index splits a formatting code
     */
    public StyledText[] partition(PartStyle.StyleType styleType, int... indexes) {
        if (indexes.length == 0) {
            return new StyledText[] {this};
        }

        List<StyledText> splitTexts = new ArrayList<>();

        int currentIndex = 0;

        for (int index : indexes) {
            if (index < currentIndex) {
                throw new IllegalArgumentException("Indexes must be in ascending order");
            }

            splitTexts.add(substring(currentIndex, index, styleType));
            currentIndex = index;
        }

        splitTexts.add(substring(currentIndex, styleType));

        return splitTexts.toArray(StyledText[]::new);
    }

    /**
     * Replaces the first occurrence of the given regex with the given replacement.
     * <p> Note that {@link PartStyle.StyleType.NONE} is used when matching and replacing.
     * @param regex the regex to replace
     * @param replacement the replacement
     * @return the new {@link StyledText}
     */
    public StyledText replaceFirst(String regex, String replacement) {
        final Pattern pattern = Pattern.compile(regex);

        List<StyledTextPart> newParts = new ArrayList<>();

        for (StyledTextPart part : parts) {
            String partString = part.getString(null, PartStyle.StyleType.NONE);

            Matcher matcher = pattern.matcher(partString);

            if (matcher.find()) {
                String replacedString = matcher.replaceFirst(replacement);

                newParts.add(
                        new StyledTextPart(replacedString, part.getPartStyle().getStyle(), null, Style.EMPTY));

                newParts.addAll(parts.subList(parts.indexOf(part) + 1, parts.size()));
                break;
            }

            newParts.add(part);
        }

        return fromParts(newParts);
    }

    /**
     * Replaces all occurrences of the given regex with the given replacement.
     * <p> Note that {@link PartStyle.StyleType.NONE} is used when matching and replacing.
     * @param regex the regex to replace
     * @param replacement the replacement
     * @return the new {@link StyledText}
     */
    public StyledText replaceAll(String regex, String replacement) {
        final Pattern pattern = Pattern.compile(regex);

        List<StyledTextPart> newParts = new ArrayList<>();

        for (StyledTextPart part : parts) {
            String partString = part.getString(null, PartStyle.StyleType.NONE);

            Matcher matcher = pattern.matcher(partString);

            if (matcher.find()) {
                String replacedString = matcher.replaceAll(replacement);

                newParts.add(
                        new StyledTextPart(replacedString, part.getPartStyle().getStyle(), null, Style.EMPTY));
            } else {
                newParts.add(part);
            }
        }

        return fromParts(newParts);
    }

    /**
     * Returns the parts of this {@link StyledText} as a {@link StyledText} array.
     * @return the array
     */
    public StyledText[] getPartsAsTextArray() {
        return parts.stream().map(StyledText::fromPart).toArray(StyledText[]::new);
    }

    public StyledText iterate(BiFunction<StyledTextPart, List<StyledTextPart>, IterationDecision> function) {
        List<StyledTextPart> newParts = new ArrayList<>();

        for (int i = 0; i < parts.size(); i++) {
            StyledTextPart part = parts.get(i);
            List<StyledTextPart> functionParts = new ArrayList<>();
            functionParts.add(part);
            IterationDecision decision = function.apply(part, functionParts);

            newParts.addAll(functionParts);

            if (decision == IterationDecision.BREAK) {
                // Add the rest of the parts
                newParts.addAll(parts.subList(i + 1, parts.size()));
                break;
            }
        }

        return fromParts(newParts);
    }

    public StyledText iterateBackwards(BiFunction<StyledTextPart, List<StyledTextPart>, IterationDecision> function) {
        List<StyledTextPart> newParts = new ArrayList<>();

        for (int i = parts.size() - 1; i >= 0; i--) {
            StyledTextPart part = parts.get(i);
            List<StyledTextPart> functionParts = new ArrayList<>();
            functionParts.add(part);
            IterationDecision decision = function.apply(part, functionParts);

            newParts.addAll(0, functionParts);

            if (decision == IterationDecision.BREAK) {
                // Add the rest of the parts
                newParts.addAll(0, parts.subList(0, i));
                break;
            }
        }

        return fromParts(newParts);
    }

    public StyledText withoutFormatting() {
        return iterate((part, functionParts) -> {
            functionParts.set(
                    0,
                    new StyledTextPart(part.getString(null, PartStyle.StyleType.NONE), Style.EMPTY, null, Style.EMPTY));
            return IterationDecision.CONTINUE;
        });
    }

    public boolean equalsString(String string) {
        return equalsString(string, PartStyle.StyleType.DEFAULT);
    }

    public boolean equalsString(String string, PartStyle.StyleType styleType) {
        return getString(styleType).equals(string);
    }

    public StyledTextPart getFirstPart() {
        if (parts.isEmpty()) {
            return null;
        }

        return parts.getFirst();
    }

    public StyledTextPart getLastPart() {
        if (parts.isEmpty()) {
            return null;
        }

        return parts.getLast();
    }

    public int getPartCount() {
        return parts.size();
    }

    /**
     * Returns the first part of this {@link StyledText} that matches the given event.
     * @param clickEvent the event to find
     * @return the 1-based index, or -1
     */
    int getClickEventIndex(ClickEvent clickEvent) {
        // Check if the event is already in the list
        for (int i = 0; i < clickEvents.size(); i++) {
            ClickEvent event = clickEvents.get(i);
            if (event.equals(clickEvent)) {
                return i + 1;
            }
        }

        return -1;
    }

    ClickEvent getClickEvent(int index) {
        return Iterables.get(clickEvents, index - 1, null);
    }

    /**
     * Returns the first part of this {@link StyledText} that matches the given event.
     * @param hoverEvent the event to find
     * @return the 1-based index, or -1
     */
    int getHoverEventIndex(HoverEvent hoverEvent) {
        // Check if the event is already in the list
        for (int i = 0; i < hoverEvents.size(); i++) {
            HoverEvent event = hoverEvents.get(i);
            if (event.equals(hoverEvent)) {
                return i + 1;
            }
        }

        return -1;
    }

    HoverEvent getHoverEvent(int index) {
        return Iterables.get(hoverEvents, index - 1, null);
    }

    private StyledTextPart getPartBefore(StyledTextPart part) {
        int index = parts.indexOf(part);
        if (index == 0) {
            return null;
        }

        return parts.get(index - 1);
    }

    @Override
    public Iterator<StyledTextPart> iterator() {
        return parts.iterator();
    }

    @Override
    public String toString() {
        return "StyledText{'" + getString(PartStyle.StyleType.INCLUDE_EVENTS) + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StyledText that = (StyledText) o;
        return Objects.deepEquals(parts, that.parts)
                && Objects.deepEquals(clickEvents, that.clickEvents)
                && Objects.deepEquals(hoverEvents, that.hoverEvents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parts, clickEvents, hoverEvents);
    }

    public static class StyledTextSerializer implements JsonSerializer<StyledText>, JsonDeserializer<StyledText> {
        @Override
        public StyledText deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return StyledText.fromString(json.getAsString());
        }

        @Override
        public JsonElement serialize(StyledText src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.getString());
        }
    }
}
