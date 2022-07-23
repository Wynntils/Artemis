/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.utils;

import com.wynntils.core.webapi.profiles.item.IdentificationModifier;
import com.wynntils.core.webapi.profiles.item.IdentificationProfile;
import com.wynntils.core.webapi.profiles.item.ItemProfile;
import com.wynntils.features.user.tooltips.ItemStatInfoFeature;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.Utils;
import com.wynntils.wc.objects.ItemIdentificationContainer;
import com.wynntils.wc.objects.SpellType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;

public class WynnItemUtils {
    private static final Pattern ITEM_IDENTIFICATION_PATTERN =
            Pattern.compile("(^\\+?(?<Value>-?\\d+)(?: to \\+?(?<UpperValue>-?\\d+))?(?<Suffix>%|/\\ds|"
                    + " tier)?(?<Stars>\\*{0,3}) (?<ID>[a-zA-Z 0-9]+))");

    /**
     * Parse the item ID lore line from a given item, and convert it into an ItemIdentificationContainer
     * Returns null if the given lore line is not a valid ID
     *
     * @param lore the ID lore line component
     * @param item the ItemProfile of the given item
     * @return the parsed ItemIdentificationContainer, or null if invalid lore line
     */
    public static ItemIdentificationContainer identificationFromLore(Component lore, ItemProfile item) {
        String unformattedLoreLine = WynnUtils.normalizeBadString(lore.getString());
        Matcher identificationMatcher = ITEM_IDENTIFICATION_PATTERN.matcher(unformattedLoreLine);
        if (!identificationMatcher.find()) return null; // not a valid id line

        String idName = identificationMatcher.group("ID");
        boolean isRaw = identificationMatcher.group("Suffix") == null;
        int starCount = identificationMatcher.group("Stars").length();
        int value = Integer.parseInt(identificationMatcher.group("Value"));

        String shortIdName;
        SpellType spell = SpellType.fromName(idName);
        if (spell != null) {
            shortIdName = spell.getShortIdName(isRaw);
        } else {
            shortIdName = IdentificationProfile.getAsShortName(idName, isRaw);
        }

        return identificationFromValue(lore, item, idName, shortIdName, value, starCount);
    }

    /**
     * Creates an ItemIdentificationContainer from the given item, ID names, ID value, and star count
     * Returns null if the given ID is not valid
     *
     * @param lore the ID lore line component - can be null if ID isn't being created from lore
     * @param item the ItemProfile of the given item
     * @param idName the in-game name of the given ID
     * @param shortIdName the internal wynntils name of the given ID
     * @param value the raw value of the given ID
     * @param starCount the number of stars on the given ID
     * @return the parsed ItemIdentificationContainer, or null if the ID is invalid
     */
    public static ItemIdentificationContainer identificationFromValue(
            Component lore, ItemProfile item, String idName, String shortIdName, int value, int starCount) {
        boolean isInverted = IdentificationOrderer.INSTANCE.isInverted(shortIdName);
        IdentificationProfile idProfile = item.getStatuses().get(shortIdName);
        IdentificationModifier type =
                idProfile != null ? idProfile.getType() : IdentificationProfile.getTypeFromName(shortIdName);
        if (type == null) return null; // not a valid id

        MutableComponent percentLine = new TextComponent("");
        MutableComponent rangeLine;
        MutableComponent rerollLine;

        MutableComponent statInfo = new TextComponent((value > 0 ? "+" : "") + value + type.getInGame(shortIdName));
        statInfo.setStyle(Style.EMPTY.withColor(isInverted ^ (value > 0) ? ChatFormatting.GREEN : ChatFormatting.RED));

        percentLine.append(statInfo);

        if (ItemStatInfoFeature.showStars)
            percentLine.append(new TextComponent("***".substring(3 - starCount)).withStyle(ChatFormatting.DARK_GREEN));

        percentLine.append(new TextComponent(" " + idName).withStyle(ChatFormatting.GRAY));

        boolean isNew = idProfile == null || idProfile.isInvalidValue(value);

        if (isNew) percentLine.append(new TextComponent(" [NEW]").withStyle(ChatFormatting.GOLD));

        rangeLine = percentLine.copy();
        rerollLine = percentLine.copy();

        float percentage = -1;
        if (!isNew && !idProfile.hasConstantValue()) {
            // calculate percent/range/reroll chances, append to lines
            int min = idProfile.getMin();
            int max = idProfile.getMax();

            if (isInverted) {
                percentage = MathUtils.inverseLerp(max, min, value) * 100;
            } else {
                percentage = MathUtils.inverseLerp(min, max, value) * 100;
            }

            IdentificationProfile.ReidentificationChances chances = idProfile.getChances(value, isInverted, starCount);

            percentLine.append(WynnItemUtils.getPercentageTextComponent(percentage));

            rangeLine.append(WynnItemUtils.getRangeTextComponent(min, max));

            rerollLine.append(WynnItemUtils.getRerollChancesComponent(
                    idProfile.getPerfectChance(), chances.increase(), chances.decrease()));
        }

        // lore might be null if this ID is not being created from a lore line
        if (lore == null) lore = percentLine;

        // create container
        return new ItemIdentificationContainer(
                item,
                idProfile,
                type,
                shortIdName,
                value,
                starCount,
                percentage,
                lore,
                percentLine,
                rangeLine,
                rerollLine);
    }

    /**
     * Create a list of ItemIdentificationContainer corresponding to the given ItemProfile, formatted for item guide items
     *
     * @param item the profile of the item
     * @return a list of appropriately formatted ItemIdentificationContainer
     */
    public static List<ItemIdentificationContainer> identificationsFromProfile(ItemProfile item) {
        List<ItemIdentificationContainer> ids = new ArrayList<>();

        for (Map.Entry<String, IdentificationProfile> entry : item.getStatuses().entrySet()) {
            IdentificationProfile idProfile = entry.getValue();
            IdentificationModifier type = idProfile.getType();
            String idName = entry.getKey();
            MutableComponent line;

            boolean inverted = IdentificationOrderer.INSTANCE.isInverted(idName);
            if (idProfile.hasConstantValue()) {
                int value = idProfile.getBaseValue();
                line = new TextComponent((value > 0 ? "+" : "") + value + type.getInGame(idName));
                line.setStyle(
                        Style.EMPTY.withColor(inverted ^ (value > 0) ? ChatFormatting.GREEN : ChatFormatting.RED));
            } else {
                int min = idProfile.getMin();
                int max = idProfile.getMax();
                ChatFormatting mainColor = inverted ^ (min > 0) ? ChatFormatting.GREEN : ChatFormatting.RED;
                ChatFormatting textColor = inverted ^ (min > 0) ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED;
                line = new TextComponent((min > 0 ? "+" : "") + min).withStyle(mainColor);
                line.append(new TextComponent(" to ").withStyle(textColor));
                line.append(
                        new TextComponent((max > 0 ? "+" : "") + max + type.getInGame(idName)).withStyle(mainColor));
            }

            line.append(new TextComponent(" " + IdentificationProfile.getAsLongName(idName))
                    .withStyle(ChatFormatting.GRAY));

            ItemIdentificationContainer id =
                    new ItemIdentificationContainer(item, idProfile, type, idName, 0, 0, -1, line, line, line, line);
            ids.add(id);
        }

        return ids;
    }

    /**
     * Create the colored percentage component for an item ID
     *
     * @param percentage the percent roll of the ID
     * @return the styled percentage text component
     */
    public static MutableComponent getPercentageTextComponent(float percentage) {
        Style color = Style.EMPTY
                .withColor(
                        ItemStatInfoFeature.colorLerp
                                ? getPercentageColor(percentage)
                                : getFlatPercentageColor(percentage))
                .withItalic(false);
        return new TextComponent(String.format(Utils.getGameLocale(), " [%.1f%%]", percentage)).withStyle(color);
    }

    /**
     * Create the colored value range component for an item ID
     *
     * @param min the minimum stat roll
     * @param max the maximum stat roll
     * @return the styled ID range text component
     */
    public static MutableComponent getRangeTextComponent(int min, int max) {
        return new TextComponent(" [")
                .append(new TextComponent(min + ", " + max).withStyle(ChatFormatting.GREEN))
                .append("]")
                .withStyle(ChatFormatting.DARK_GREEN);
    }

    /**
     * Create the colored reroll chance component for an item ID
     *
     * @param perfect the chance of a perfect roll
     * @param increase the chance of an increased roll
     * @param decrease the chance of a decreased roll
     * @return the styled reroll chance text component
     */
    public static MutableComponent getRerollChancesComponent(double perfect, double increase, double decrease) {
        return new TextComponent(String.format(Utils.getGameLocale(), " \u2605%.2f%%", perfect * 100))
                .withStyle(ChatFormatting.AQUA)
                .append(new TextComponent(String.format(Utils.getGameLocale(), " \u21E7%.1f%%", increase * 100))
                        .withStyle(ChatFormatting.GREEN))
                .append(new TextComponent(String.format(Utils.getGameLocale(), " \u21E9%.1f%%", decrease * 100))
                        .withStyle(ChatFormatting.RED));
    }

    private static final TreeMap<Float, TextColor> colorMap = new TreeMap<>() {
        {
            put(0f, TextColor.fromLegacyFormat(ChatFormatting.RED));
            put(30f, TextColor.fromLegacyFormat(ChatFormatting.YELLOW));
            put(80f, TextColor.fromLegacyFormat(ChatFormatting.GREEN));
            put(96f, TextColor.fromLegacyFormat(ChatFormatting.AQUA));
        }
    };

    private static TextColor getPercentageColor(float percentage) {
        Map.Entry<Float, TextColor> lowerEntry = colorMap.floorEntry(percentage);
        Map.Entry<Float, TextColor> higherEntry = colorMap.ceilingEntry(percentage);

        // Boundary conditions
        if (lowerEntry == null) {
            return higherEntry.getValue();
        } else if (higherEntry == null) {
            return lowerEntry.getValue();
        }

        if (Objects.equals(lowerEntry.getKey(), higherEntry.getKey())) {
            return lowerEntry.getValue();
        }

        float t = MathUtils.inverseLerp(lowerEntry.getKey(), higherEntry.getKey(), percentage);

        int lowerColor = lowerEntry.getValue().getValue();
        int higherColor = higherEntry.getValue().getValue();

        int r = (int) MathUtils.lerp((lowerColor >> 16) & 0xff, (higherColor >> 16) & 0xff, t);
        int g = (int) MathUtils.lerp((lowerColor >> 8) & 0xff, (higherColor >> 8) & 0xff, t);
        int b = (int) MathUtils.lerp(lowerColor & 0xff, higherColor & 0xff, t);

        return TextColor.fromRgb((r << 16) | (g << 8) | b);
    }

    private static TextColor getFlatPercentageColor(float percentage) {
        if (percentage < 30f) {
            return TextColor.fromLegacyFormat(ChatFormatting.RED);
        } else if (percentage < 80f) {
            return TextColor.fromLegacyFormat(ChatFormatting.YELLOW);
        } else if (percentage < 96f) {
            return TextColor.fromLegacyFormat(ChatFormatting.GREEN);
        } else {
            return TextColor.fromLegacyFormat(ChatFormatting.AQUA);
        }
    }

    public static void removeLoreTooltipLines(List<Component> tooltip) {
        List<Component> toRemove = new ArrayList<>();
        boolean lore = false;
        for (Component c : tooltip) {
            // only remove text after the item type indicator
            if (!lore && WynnItemMatchers.rarityLineMatcher(c).find()) {
                lore = true;
                continue;
            }

            if (lore) toRemove.add(c);
        }
        tooltip.removeAll(toRemove);
    }
}
