/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gearinfo.parsing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.models.concepts.Powder;
import com.wynntils.models.concepts.Skill;
import com.wynntils.models.gearinfo.GearCalculator;
import com.wynntils.models.gearinfo.type.GearInfo;
import com.wynntils.models.gearinfo.type.GearTier;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.wynn.WynnItemMatchers;
import com.wynntils.utils.wynn.WynnUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class GearParser {
    // Test suite: https://regexr.com/776qt
    public static final Pattern IDENTIFICATION_STAT_PATTERN = Pattern.compile(
            "^§[ac]([-+]\\d+)(?:§r§[24] to §r§[ac](-?\\d+))?(%| tier|\\/[35]s)?(?:§r§2(\\*{1,3}))? ?§r§7 ?(.*)$");

    // FIXME: Clean up this class!
    private static final Pattern ID_NEW_PATTERN =
            Pattern.compile("^§([ac])([-+]\\d+)(%|/3s|/5s| tier)?(?:§r§2(\\*{1,3}))? ?§r§7 ?(.*)$");

    // Test suite: https://regexr.com/778gh
    private static final Pattern TIER_AND_REROLL_PATTERN = Pattern.compile(
            "^(§fNormal|§eUnique|§dRare|§bLegendary|§cFabled|§5Mythic|§aSet) (Raid Reward|Item)(?: \\[(\\d+)\\])?$");

    // Test suite: https://regexr.com/778gk
    private static final Pattern POWDER_PATTERN =
            Pattern.compile("^§7\\[(\\d+)/(\\d+)\\] Powder Slots(?: \\[§r§(.*)§r§7\\])?$");

    public static GearParseResult parseItemStack(ItemStack itemStack) {
        List<StatActualValue> identifications = new ArrayList<>();
        List<Powder> powders = new ArrayList<>();
        int rerolls = 0;
        GearTier tier = null;

        // Parse lore for identifications, powders and rerolls
        List<Component> lore = ComponentUtils.stripDuplicateBlank(LoreUtils.getTooltipLines(itemStack));
        lore.remove(0); // remove item name

        for (Component loreLine : lore) {
            String unformattedLoreLine = WynnUtils.normalizeBadString(loreLine.getString());
            String coded = ComponentUtils.getCoded(loreLine);
            String normalizedCoded = WynnUtils.normalizeBadString(coded);

            // Look for powder
            Matcher powderMatcher = POWDER_PATTERN.matcher(normalizedCoded);
            if (powderMatcher.matches()) {
                int usedSlots = Integer.parseInt(powderMatcher.group(1));
                String codedPowders = powderMatcher.group(3);
                if (codedPowders == null) continue;

                String powderString = codedPowders.replaceAll("[^✹✦❋❉✤]", "");
                if (powderString.length() != usedSlots) {
                    WynntilsMod.warn("Mismatch between powder slot count " + usedSlots + " and actual powder symbols: "
                            + codedPowders + " for " + itemStack.getHoverName().getString());
                    // Fall through and use codedPowfers nevertheless
                }

                codedPowders.chars().forEach(ch -> {
                    Powder powder = Powder.getFromSymbol(Character.toString(ch));
                    powders.add(powder);
                });

                continue;
            }

            // Look for tier and rerolls
            Matcher tiearMatcher = TIER_AND_REROLL_PATTERN.matcher(normalizedCoded);
            if (tiearMatcher.matches()) {
                String tierString = tiearMatcher.group(1);
                tier = GearTier.fromFormattedString(tierString);

                String rerollCountString = tiearMatcher.group(3);
                rerolls = rerollCountString != null ? Integer.parseInt(rerollCountString) : 0;

                continue;
            }

            // Look for identifications
            Matcher statMatcher = IDENTIFICATION_STAT_PATTERN.matcher(normalizedCoded);
            if (statMatcher.matches()) {
                int value = Integer.parseInt(statMatcher.group(1));
                // group 2 is only present for unidentified gears, as the to-part of the range
                String unit = statMatcher.group(3);
                String starString = statMatcher.group(4);
                String statDisplayName = statMatcher.group(5);

                StatType type = Models.Stat.fromDisplayName(statDisplayName, unit);
                if (type == null && Skill.isSkill(statDisplayName)) {
                    // Skill bonuses looks like stats when parsing, ignore them
                    continue;
                }
                if (type.showAsInverted()) {
                    // Spell Cost stats are shown as negative, but we store them as positive
                    value = -value;
                }

                int stars = starString == null ? 0 : starString.length();

                identifications.add(new StatActualValue(type, value, stars));
            }
        }

        return new GearParseResult(tier, identifications, powders, rerolls);
    }

    public static GearParseResult parseInternalRolls(GearInfo gearInfo, JsonObject itemData) {
        List<StatActualValue> identifications = new ArrayList<>();

        if (itemData.has("identifications")) {
            JsonArray ids = itemData.getAsJsonArray("identifications");
            for (int i = 0; i < ids.size(); i++) {
                JsonObject idInfo = ids.get(i).getAsJsonObject();
                String id = idInfo.get("type").getAsString();
                int internalRoll = idInfo.get("percent").getAsInt();

                // Lore line is: {type: "<loretype>", percent: <internal roll>}
                // <internal roll> is an integer between 30 and 130

                // First convert loretype (e.g. DAMAGEBONUS) to our StatTypes
                StatType statType = Models.Stat.fromInternalRollId(id);
                if (statType == null) {
                    // This can happen for skill point bonus, which used to be variable...
                    Skill skill = Skill.fromApiId(id.replace("POINTS", ""));
                    if (skill != null) continue;

                    WynntilsMod.warn("Remote player's " + gearInfo.name() + " contains unknown stat type " + id);
                    continue;
                }

                // Then convert the internal roll
                StatActualValue actualValue = getStatActualValue(gearInfo, statType, internalRoll);
                if (actualValue == null) continue;

                identifications.add(actualValue);
            }
        }

        List<Powder> powders = new ArrayList<>();

        if (itemData.has("powders")) {
            JsonArray powderData = itemData.getAsJsonArray("powders");
            for (int i = 0; i < powderData.size(); i++) {
                String type = powderData.get(i).getAsJsonObject().get("type").getAsString();
                Powder powder = Powder.valueOf(type.toUpperCase(Locale.ROOT));

                powders.add(powder);
            }
        }

        int rerolls = itemData.has("identification_rolls")
                ? itemData.get("identification_rolls").getAsInt()
                : 0;

        return new GearParseResult(gearInfo.tier(), identifications, powders, rerolls);
    }

    private static StatActualValue getStatActualValue(GearInfo gearInfo, StatType statType, int internalRoll) {
        StatPossibleValues possibleValue = gearInfo.getPossibleValues(statType);
        if (possibleValue == null) {
            WynntilsMod.warn("Remote player's " + gearInfo.name() + " claims to have " + statType);
            return null;
        }
        int value = Math.round(possibleValue.baseValue() * (internalRoll / 100f));
        if (value == 0) {
            // If we get to 0, use 1 or -1 instead
            value = (int) Math.signum(possibleValue.baseValue());
        }

        // Negative values can never show stars
        int stars = (value > 0) ? GearCalculator.getStarsFromInternalRoll(internalRoll) : 0;

        return new StatActualValue(statType, value, stars);
    }

    public static CraftedGearItem getCraftedGearItem(ItemStack itemStack) {
        CappedValue durability = WynnItemMatchers.getDurability(itemStack);

        List<StatActualValue> identifications = new ArrayList<>();
        List<Powder> powders = List.of();

        // Parse lore for identifications and powders
        List<Component> lore = ComponentUtils.stripDuplicateBlank(itemStack.getTooltipLines(null, TooltipFlag.NORMAL));
        if (lore.size() <= 1) {
            // We should always have the item name as the first line, unless some other mod interacts badly...
            return null;
        }
        lore.remove(0); // remove item name

        for (Component loreLine : lore) {
            // FIXME: This is partially shared with GearAnnotator
            String unformattedLoreLine = WynnUtils.normalizeBadString(loreLine.getString());

            // Look for Powder
            if (unformattedLoreLine.contains("] Powder Slots")) {
                List<Powder> foundPowders = new ArrayList<>();
                unformattedLoreLine.chars().forEach(ch -> {
                    Powder powder = Powder.getFromSymbol(Character.toString(ch));
                    foundPowders.add(powder);
                });

                powders = foundPowders;
                continue;
            }

            // Look for identifications
            String formatId = ComponentUtils.getCoded(loreLine);
            Matcher statMatcher = ID_NEW_PATTERN.matcher(formatId);
            if (statMatcher.matches()) {
                int value = Integer.parseInt(statMatcher.group(2));
                String unit = statMatcher.group(3);
                String statDisplayName = statMatcher.group(5);

                StatType type = Models.Stat.fromDisplayName(statDisplayName, unit);
                if (type == null && Skill.isSkill(statDisplayName)) {
                    // Skill point buff looks like stats when parsing
                    continue;
                }

                // FIXME: crafted gear do not have stars, ever
                // Instead, they have <current value><unit>/<max value><unit>
                // Also, "fixed" stats can become changing here...
                // Also, the order of stats is completely arbitrary
                // So we need a better design to fit this

                identifications.add(new StatActualValue(type, value, -1));
            }
        }

        // FIXME: Missing requirements and damages
        return new CraftedGearItem(List.of(), List.of(), identifications, powders, durability);
    }
}
