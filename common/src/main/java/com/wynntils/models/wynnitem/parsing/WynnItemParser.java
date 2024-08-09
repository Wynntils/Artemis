/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.wynnitem.parsing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.elements.type.Element;
import com.wynntils.models.elements.type.Powder;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearAttackSpeed;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.gear.type.SetInfo;
import com.wynntils.models.gear.type.SetInstance;
import com.wynntils.models.stats.StatCalculator;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.models.stats.type.ShinyStat;
import com.wynntils.models.stats.type.SkillStatType;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.models.wynnitem.type.ConsumableEffect;
import com.wynntils.models.wynnitem.type.ItemEffect;
import com.wynntils.models.wynnitem.type.NamedItemEffect;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class WynnItemParser {
    public static final Pattern HEALTH_PATTERN = Pattern.compile("^§4❤ Health: ([+-]\\d+)$");

    // Test in WynnItemParser_ITEM_ATTACK_SPEED_PATTERN
    private static final Pattern ITEM_ATTACK_SPEED_PATTERN = Pattern.compile("^§7(.+) Attack Speed$");

    // Test in WynnItemParser_ITEM_DAMAGE_PATTERN
    private static final Pattern ITEM_DAMAGE_PATTERN =
            Pattern.compile("^§.(?<symbol>[✤✦❉✹❋✣]+) (?<type>.+) Damage: (?<range>(\\d+)-(\\d+))$");

    // Test in WynnItemParser_ITEM_DEFENCE_PATTERN
    private static final Pattern ITEM_DEFENCE_PATTERN =
            Pattern.compile("^§.(?<symbol>[✤✦❉✹❋]+) (?<type>.+)§7 Defence: (?<value>[+-]?\\d+)$");

    // Test in WynnItemParser_IDENTIFICATION_STAT_PATTERN
    public static final Pattern IDENTIFICATION_STAT_PATTERN = Pattern.compile(
            "^§[ac]([-+]\\d+)(?:§[24] to §[ac](-?\\d+))?(%| tier|\\/[35]s)?(?:§8\\/([-+]?\\d+)(?:%| tier|\\/[35]s)?)?(?:§2(\\*{1,3}))? ?§7 ?(.*)$");

    // Test in WynnItemParser_TIER_AND_REROLL_PATTERN
    private static final Pattern TIER_AND_REROLL_PATTERN = Pattern.compile(
            "^(§fNormal|§eUnique|§dRare|§bLegendary|§cFabled|§5Mythic|§aSet|§3Crafted) ([A-Za-z\\d _]+)(?:§8)?(?: \\[(\\d+)(?:\\/(\\d+) Durability)?\\])?$");

    // Test in WynnItemParser_POWDER_PATTERN
    private static final Pattern POWDER_PATTERN =
            Pattern.compile("^§7\\[(\\d+)/(\\d+)\\] Powder Slots(?: \\[§(.*)§7\\])?$");

    // Test in WynnItemParser_EFFECT_LINE_PATTERN
    private static final Pattern EFFECT_LINE_PATTERN = Pattern.compile("^§(.)- §7(.*): §f([+-]?\\d+)(?:§.§.)? ?(.*)$");

    // Test in WynnItemParser_MIN_LEVEL_PATTERN
    private static final Pattern MIN_LEVEL_PATTERN = Pattern.compile("^§(c✖|a✔)§7 Combat Lv. Min: (?<level>\\d+)$");

    // Test in WynnItemParser_CLASS_REQ_PATTERN
    private static final Pattern CLASS_REQ_PATTERN =
            Pattern.compile("^§(c✖|a✔)§7 Class Req: (?<name>.+)\\/(?<skinned>.+)$");

    // Test in WynnItemParser_SKILL_REQ_PATTERN
    private static final Pattern SKILL_REQ_PATTERN =
            Pattern.compile("^§(c✖|a✔)§7 (?<skill>[a-zA-Z]+) Min: (?<value>-?\\d+)$");

    // Test in WynnItemParser_MISC_REQ_PATTERN
    private static final Pattern MISC_REQ_PATTERN = Pattern.compile("^§(c✖|a✔)§7 (.+)$");

    private static final Pattern EFFECT_HEADER_PATTERN = Pattern.compile("^§(.)Effect:$");

    private static final Pattern POWDER_MARKERS = Pattern.compile("[^✹✦❋❉✤]");

    private static final Pattern SET_PATTERN = Pattern.compile("§a(.+) Set §7\\((\\d)/\\d\\)");

    public static final Pattern SET_BONUS_PATTERN = Pattern.compile("^§aSet Bonus:$");

    // Checks for items eg. "- Morph-Emerald" to determine if item is equipped from color
    private static final Pattern SET_ITEM_PATTERN = Pattern.compile("^§[a7]- §([28])(.+)");

    private static final Pattern SET_BONUS_IDENTIFICATION_PATTERN =
            Pattern.compile("§[ac]([-+]\\d+)(%| tier|/[35]s)? ?§7 ?(.*)");

    // Test in WynnItemParser_SHINY_STAT_PATTERN
    public static final Pattern SHINY_STAT_PATTERN = Pattern.compile("^§f⬡ §7([a-zA-Z ]+): §f(\\d+)$");

    // Crafted items
    // Test in WynnItemParser_CRAFTED_ITEM_NAME_PATTERN
    private static final Pattern CRAFTED_ITEM_NAME_PATTERN = Pattern.compile("^§3§o(.+)§b§o \\[(\\d+)%\\]À*$");

    public static WynnItemParseResult parseItemStack(
            ItemStack itemStack, Map<StatType, StatPossibleValues> possibleValuesMap) {
        List<StatActualValue> identifications = new ArrayList<>();
        List<NamedItemEffect> namedEffects = new ArrayList<>();
        List<ItemEffect> effects = new ArrayList<>();
        List<Powder> powders = new ArrayList<>();
        int powderSlots = 0;
        int health = 0;
        int level = 0;
        int tierCount = 0;
        int durabilityMax = 0;
        GearTier tier = null;
        String itemType = "";
        boolean setBonusStats = false;
        boolean parsingEffects = false;
        Optional<ShinyStat> shinyStat = Optional.empty();
        String effectsColorCode = "";
        boolean allRequirementsMet = true;
        SetInfo setInfo = null;
        Map<String, Boolean> activeItems = new HashMap<>();
        int setWynnCount = 0;
        Map<StatType, Integer> wynnBonuses = new HashMap<>();

        // Parse lore for identifications, powders and rerolls
        List<Component> lore = ComponentUtils.stripDuplicateBlank(LoreUtils.getTooltipLines(itemStack));
        lore.removeFirst(); // remove item name

        for (Component loreLine : lore) {
            StyledText coded = StyledText.fromComponent(loreLine);
            StyledText normalizedCoded = coded.getNormalized();

            if (setBonusStats) {
                // We should revert back to normal parsing when we encounter an empty line
                if (normalizedCoded.isEmpty()) {
                    setBonusStats = false;
                    continue;
                }

                Matcher setBonusIdentificationMatcher = normalizedCoded.getMatcher(SET_BONUS_IDENTIFICATION_PATTERN);
                if (!setBonusIdentificationMatcher.matches()) {
                    WynntilsMod.warn("Item " + itemStack.getHoverName().getString()
                            + " has unknown set bonus stat line: " + loreLine);
                    continue;
                }
                int value = Integer.parseInt(setBonusIdentificationMatcher.group(1));
                String unit = setBonusIdentificationMatcher.group(2);
                String statDisplayName = setBonusIdentificationMatcher.group(3);

                StatType statType = Models.Stat.fromDisplayName(statDisplayName, unit);
                if (statType == null) {
                    WynntilsMod.warn("Item " + itemStack.getHoverName().getString()
                            + " has unknown identified set bonus stat " + statDisplayName);
                    continue;
                }
                wynnBonuses.put(statType, value);
            }

            // Look for powder
            Matcher powderMatcher = normalizedCoded.getMatcher(POWDER_PATTERN);
            if (powderMatcher.matches()) {
                int usedSlots = Integer.parseInt(powderMatcher.group(1));
                powderSlots = Integer.parseInt(powderMatcher.group(2));
                String codedPowders = powderMatcher.group(3);
                if (codedPowders == null) continue;

                String powderString = POWDER_MARKERS.matcher(codedPowders).replaceAll("");
                if (powderString.length() != usedSlots) {
                    WynntilsMod.warn("Mismatch between powder slot count " + usedSlots + " and actual powder symbols: "
                            + codedPowders + " for " + itemStack.getHoverName().getString());
                    // Fall through and use codedPowders nevertheless
                }

                codedPowders.chars().forEach(ch -> {
                    Powder powder = Powder.getFromSymbol(Character.toString(ch));
                    if (powder != null) {
                        powders.add(powder);
                    }
                });

                continue;
            }

            // Look for tier and rerolls
            Matcher tierMatcher = normalizedCoded.getMatcher(TIER_AND_REROLL_PATTERN);
            if (tierMatcher.matches()) {
                String tierString = tierMatcher.group(1);
                tier = GearTier.fromStyledText(StyledText.fromString(tierString));
                itemType = tierMatcher.group(2);

                // This is either the rerolls (for re-identified gear), or the
                // current durability (for crafted gear)
                String tierCountString = tierMatcher.group(3);
                tierCount = tierCountString != null ? Integer.parseInt(tierCountString) : 0;

                // If we have a crafted gear, we also have a durability max
                String durabilityMaxString = tierMatcher.group(4);
                durabilityMax = durabilityMaxString != null ? Integer.parseInt(durabilityMaxString) : 0;
                continue;
            }

            Matcher healthMatcher = normalizedCoded.getMatcher(HEALTH_PATTERN);
            if (healthMatcher.matches()) {
                health = Integer.parseInt(healthMatcher.group(1));
                continue;
            }

            // Requirements
            // Combat level
            Matcher levelMatcher = normalizedCoded.getMatcher(MIN_LEVEL_PATTERN);
            if (levelMatcher.matches()) {
                level = Integer.parseInt(levelMatcher.group("level"));
                continue;
            }

            // Class
            Matcher classMatcher = normalizedCoded.getMatcher(CLASS_REQ_PATTERN);
            if (classMatcher.matches()) {
                String mark = classMatcher.group(1);
                if (mark.contains("✖")) {
                    allRequirementsMet = false;
                }
            }

            // Skills
            Matcher skillMatcher = normalizedCoded.getMatcher(SKILL_REQ_PATTERN);
            if (skillMatcher.matches()) {
                String mark = skillMatcher.group(1);
                if (mark.contains("✖")) {
                    allRequirementsMet = false;
                }
            }

            // Misc requirements
            Matcher miscMatcher = normalizedCoded.getMatcher(MISC_REQ_PATTERN);
            if (miscMatcher.matches()) {
                String mark = miscMatcher.group(1);
                if (mark.contains("✖")) {
                    allRequirementsMet = false;
                }
            }

            Matcher setMatcher = normalizedCoded.getMatcher(SET_PATTERN);
            if (setMatcher.matches()) {
                String setName = setMatcher.group(1);
                setInfo = Models.Set.getSetInfo(setName);
                setWynnCount = Integer.parseInt(setMatcher.group(2));
            }

            Matcher setItemMatcher = normalizedCoded.getMatcher(SET_ITEM_PATTERN);
            if (setItemMatcher.matches()) {
                boolean active = setItemMatcher.group(1).equals("2");
                String itemName = setItemMatcher.group(2);
                activeItems.put(itemName, active);
            }

            Matcher setBonusMatcher = normalizedCoded.getMatcher(SET_BONUS_PATTERN);
            if (setBonusMatcher.matches()) {
                // Any stat lines that follow from now on belongs to the Set Bonus
                // These are collected at the top of this loop for efficiency
                setBonusStats = true;
            }

            // Look for effects (only on consumables)
            Matcher effectHeaderMatcher = normalizedCoded.getMatcher(EFFECT_HEADER_PATTERN);
            if (effectHeaderMatcher.matches()) {
                effectsColorCode = effectHeaderMatcher.group(1);
                parsingEffects = true;
                continue;
            }
            if (parsingEffects) {
                Matcher effectMatcher = normalizedCoded.getMatcher(EFFECT_LINE_PATTERN);
                if (effectMatcher.matches()) {
                    String colorCode = effectMatcher.group(1);
                    String type = effectMatcher.group(2);
                    int value = Integer.parseInt(effectMatcher.group(3));
                    String suffix = effectMatcher.group(4);

                    // A sanity check; otherwise fall through
                    if (colorCode.equals(effectsColorCode)) {
                        // If type is "Heal", "Mana" or "Duration", keep it, otherwise
                        // replace it with the actual effect type
                        if (type.equals("Effect")) {
                            type = suffix;
                        }
                        ConsumableEffect consumableEffect = ConsumableEffect.fromString(type);
                        if (consumableEffect != null) {
                            namedEffects.add(new NamedItemEffect(consumableEffect, value));
                        } else {
                            effects.add(new ItemEffect(type, value));
                        }
                        continue;
                    }
                }

                parsingEffects = false;
                // fall through
            }

            // Look for identifications
            Matcher statMatcher = normalizedCoded.getMatcher(IDENTIFICATION_STAT_PATTERN);
            if (statMatcher.matches() && !setBonusStats) {
                int value = Integer.parseInt(statMatcher.group(1));
                // group 2 is only present for unidentified gears, as the to-part of the range
                String unit = statMatcher.group(3);
                // group 4 is only present for crafted gear, as the top value for that stat
                String starString = statMatcher.group(5);
                String statDisplayName = statMatcher.group(6);

                StatType statType = Models.Stat.fromDisplayName(statDisplayName, unit);
                if (statType == null) {
                    WynntilsMod.warn(
                            "Item " + itemStack.getHoverName() + " has unknown identified stat " + statDisplayName);
                    continue;
                }
                if (statType.calculateAsInverted()) {
                    // Spell Cost stats are shown as negative, but we store them as positive
                    value = -value;
                }

                int stars = starString == null ? 0 : starString.length();

                // Load the possible values for this stat
                // If we are parsing a crafted item, we want this to be null
                StatPossibleValues possibleValues = possibleValuesMap != null ? possibleValuesMap.get(statType) : null;

                // group 4 is only present for crafted gear, as the top value for that stat
                // parse possible values for this stat
                if (statMatcher.group(4) != null && possibleValuesMap != null) {
                    int maxValue = Integer.parseInt(statMatcher.group(4));
                    // minimum value is 10% of maximum value, rounded
                    int minValue = (int) Math.round(maxValue * 0.1);

                    // Add possible values for this stat
                    StatPossibleValues calculatedPossibleValues =
                            new StatPossibleValues(statType, RangedValue.of(minValue, maxValue), maxValue, false);
                    possibleValuesMap.put(statType, calculatedPossibleValues);
                }

                StatActualValue actualValue = Models.Stat.buildActualValue(statType, value, stars, possibleValues);
                identifications.add(actualValue);
            }

            // Look for shiny stat
            Matcher shinyStatMatcher = normalizedCoded.getMatcher(SHINY_STAT_PATTERN);
            if (shinyStatMatcher.matches() && shinyStat.isEmpty()) {
                String shinyName = shinyStatMatcher.group(1);
                int shinyValue = Integer.parseInt(shinyStatMatcher.group(2));
                shinyStat = Optional.of(new ShinyStat(Models.Shiny.getShinyStat(shinyName), shinyValue));
            }
        }

        return new WynnItemParseResult(
                tier,
                itemType,
                health,
                level,
                identifications,
                namedEffects,
                effects,
                powders,
                powderSlots,
                tierCount,
                tierCount,
                durabilityMax,
                shinyStat,
                allRequirementsMet,
                Optional.of(new SetInstance(setInfo, activeItems, setWynnCount, wynnBonuses)));
    }

    public static WynnItemParseResult parseInternalRolls(GearInfo gearInfo, JsonObject itemData) {
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

        // Shiny stats are not available from internal roll lore (on other players)
        return new WynnItemParseResult(
                gearInfo.tier(),
                "",
                0,
                0,
                identifications,
                List.of(),
                List.of(),
                powders,
                powders.size(),
                rerolls,
                0,
                0,
                Optional.empty(),
                false,
                Optional.empty());
    }

    public static CraftedItemParseResults parseCraftedItem(ItemStack itemStack) {
        List<Component> lore = ComponentUtils.stripDuplicateBlank(LoreUtils.getTooltipLines(itemStack));

        String name = "";
        int effectStrength = 0;
        GearAttackSpeed attackSpeed = null;
        List<Pair<DamageType, RangedValue>> damages = new ArrayList<>();
        List<Pair<Element, Integer>> defences = new ArrayList<>();
        // requirements
        int levelReq = 0;
        List<Pair<Skill, Integer>> skillReqs = new ArrayList<>();
        ClassType classReq = null;

        if (!lore.isEmpty()) {
            Matcher nameMatcher = StyledText.fromComponent(lore.getFirst()).getMatcher(CRAFTED_ITEM_NAME_PATTERN);
            if (nameMatcher.matches()) {
                name = nameMatcher.group(1);
                effectStrength = Integer.parseInt(nameMatcher.group(2));
            } else {
                nameMatcher = StyledText.fromComponent(itemStack.getHoverName()).getMatcher(CRAFTED_ITEM_NAME_PATTERN);
                if (nameMatcher.matches()) {
                    name = nameMatcher.group(1);
                    effectStrength = Integer.parseInt(nameMatcher.group(2));
                } else {
                    WynntilsMod.warn("Crafted item "
                            + StyledText.fromComponent(itemStack.getHoverName()).getString()
                            + " has no name in lore, or as a custom name: "
                            + lore.stream()
                                    .map(StyledText::fromComponent)
                                    .map(StyledText::getString)
                                    .collect(Collectors.joining("\n")));
                    return null;
                }
            }
        }

        boolean allRequirementsMet = true;
        for (Component loreLine : lore) {
            StyledText coded = StyledText.fromComponent(loreLine);

            Matcher attackSpeedMatcher = coded.getMatcher(ITEM_ATTACK_SPEED_PATTERN);
            if (attackSpeedMatcher.matches()) {
                String speedName = attackSpeedMatcher.group(1);
                attackSpeed = GearAttackSpeed.fromString(speedName.replaceAll(" ", "_"));
            }

            Matcher damageMatcher = coded.getMatcher(ITEM_DAMAGE_PATTERN);
            if (damageMatcher.matches()) {
                String symbol = damageMatcher.group("symbol");
                RangedValue range = RangedValue.fromString(damageMatcher.group("range"));
                damages.add(Pair.of(DamageType.fromSymbol(symbol), range));
            }

            Matcher defenceMatcher = coded.getMatcher(ITEM_DEFENCE_PATTERN);
            if (defenceMatcher.matches()) {
                String symbol = defenceMatcher.group("symbol");
                int value = Integer.parseInt(defenceMatcher.group("value"));
                defences.add(Pair.of(Element.fromSymbol(symbol), value));
            }

            // Requirements
            // Combat level
            Matcher levelMatcher = coded.getMatcher(MIN_LEVEL_PATTERN);
            if (levelMatcher.matches()) {
                levelReq = Integer.parseInt(levelMatcher.group("level"));

                String mark = levelMatcher.group(1);
                if (mark.contains("✖")) {
                    allRequirementsMet = false;
                }
            }

            // Class
            Matcher classMatcher = coded.getMatcher(CLASS_REQ_PATTERN);
            if (classMatcher.matches()) {
                String className = classMatcher.group("name");
                classReq = ClassType.fromName(className);

                String mark = classMatcher.group(1);
                if (mark.contains("✖")) {
                    allRequirementsMet = false;
                }
            }

            // Skills
            Matcher skillMatcher = coded.getMatcher(SKILL_REQ_PATTERN);
            if (skillMatcher.matches()) {
                String skillName = skillMatcher.group("skill");
                Skill skill = Skill.fromString(skillName);
                int value = Integer.parseInt(skillMatcher.group("value"));
                skillReqs.add(Pair.of(skill, value));

                String mark = skillMatcher.group(1);
                if (mark.contains("✖")) {
                    allRequirementsMet = false;
                }
            }
        }

        return new CraftedItemParseResults(
                name,
                effectStrength,
                attackSpeed,
                damages,
                defences,
                new GearRequirements(levelReq, Optional.ofNullable(classReq), skillReqs, Optional.empty()),
                allRequirementsMet);
    }

    private static StatActualValue getStatActualValue(GearInfo gearInfo, StatType statType, int internalRoll) {
        StatPossibleValues possibleValue = gearInfo.getPossibleValues(statType);
        if (possibleValue == null) {
            if (!(statType instanceof SkillStatType)) {
                // We know Wynncraft send skill stats as 100%; don't complain about that
                WynntilsMod.warn("Remote player's " + gearInfo.name() + " claims to have " + statType);
            }
            return null;
        }
        int value = StatCalculator.calculateStatValue(internalRoll, possibleValue);

        // Negative values can never show stars
        int stars = (value > 0)
                ? StatCalculator.calculateStarsFromInternalRoll(statType, possibleValue.baseValue(), internalRoll)
                : 0;

        // In this case, we actually know the exact internal roll
        return new StatActualValue(statType, value, stars, RangedValue.of(internalRoll, internalRoll));
    }
}
