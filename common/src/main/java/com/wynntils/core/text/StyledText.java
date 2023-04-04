/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.text;

import com.wynntils.utils.wynn.WynnUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class StyledText {
    public static final StyledText EMPTY = new StyledText("");

    private final String str;

    public StyledText(String str) {
        this.str = str;
    }

    public static StyledText of(String s) {
        return new StyledText(s);
    }

    public static StyledText fromComponentIgnoringComponentStylesAndJustUsingFormattingCodes(Component component) {
        return StyledText.of(component.getString());
    }

    public String getInternalCodedStringRepresentation() {
        return str;
    }

    public String withoutFormatting() {
        return ChatFormatting.stripFormatting(str);
    }

    public MutableComponent asSingleLiteralComponentWithCodedString() {
        return Component.literal(str);
    }

    public StyledText getNormalized() {
        return new StyledText(WynnUtils.normalizeBadString(str));
    }

    public StyledText trim() {
        return StyledText.of(str.trim());
    }

    public boolean isEmpty() {
        return str.isEmpty();
    }

    public boolean isBlank() {
        return str.isBlank();
    }

    public Matcher match(Pattern pattern) {
        return pattern.matcher(str);
    }

    public boolean contains(StyledText string) {
        return str.contains(string.str);
    }

    public boolean contains(String string) {
        return str.contains(string);
    }

    public boolean startsWith(StyledText prefix) {
        return str.startsWith(prefix.str);
    }

    public boolean startsWith(String prefix) {
        return str.startsWith(prefix);
    }

    public boolean endsWith(StyledText suffix) {
        return str.endsWith(suffix.str);
    }

    public boolean endsWith(String suffix) {
        return str.endsWith(suffix);
    }

    public static StyledText join(List<StyledText> strings, String delimiter) {
        return StyledText.of(
                String.join(delimiter, strings.stream().map(s -> s.str).toList()));
    }

    public StyledText[] split(String regex) {
        return Arrays.stream(str.split(regex)).map(StyledText::of).toArray(StyledText[]::new);
    }

    public static StyledText join(StyledText[] strings, String delimiter) {
        return StyledText.of(
                String.join(delimiter, Arrays.stream(strings).map(s -> s.str).toList()));
    }

    public static StyledText concat(StyledText... str) {
        return StyledText.of(Arrays.stream(str).map(s -> s.str).collect(Collectors.joining()));
    }

    public StyledText prepend(StyledText prefix) {
        return prefix.append(str);
    }

    public StyledText prepend(String prefix) {
        return StyledText.of(prefix + str);
    }

    public StyledText append(StyledText suffix) {
        return StyledText.of(str + suffix.str);
    }

    public StyledText append(String suffix) {
        return StyledText.of(str + suffix);
    }

    @Override
    public String toString() {
        return str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StyledText that = (StyledText) o;
        return Objects.equals(str, that.str);
    }

    @Override
    public int hashCode() {
        return Objects.hash(str);
    }
}
