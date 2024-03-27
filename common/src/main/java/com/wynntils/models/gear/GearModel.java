/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear;

import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearInstance;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.gear.type.SetInstance;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.items.items.game.GearBoxItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.items.game.UnknownGearItem;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.models.wynnitem.parsing.CraftedItemParseResults;
import com.wynntils.models.wynnitem.parsing.WynnItemParseResult;
import com.wynntils.models.wynnitem.parsing.WynnItemParser;
import com.wynntils.utils.type.CappedValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;

/**
 * Gear and stats are complex, have lots of corner cases and suffer from a general
 * lack of comprehensible, exhaustive, correct and authoritive documentation. :-(
 *
 * Here is a collection of generally helpful links:
 *
 * 2016 Guide: https://forums.wynncraft.com/threads/how-identifications-are-calculated.128923/
 * 2019 Guide: https://forums.wynncraft.com/threads/stats-and-identifications-guide.246308/
 * The Damage Bible: https://docs.google.com/document/d/1BXdLrMWj-BakPcAWnuqvSFbwiz7oGTOMcEEdC5vCWs4
 * WynnBuilder "Wynnfo": https://hppeng-wynn.github.io/wynnfo/, especially
 * Damage Calculations: https://hppeng-wynn.github.io/wynnfo/pdfs/Damage_calculation.pdf
 *
 * A note on percent vs raw numbers and how they combine, from HeyZeer0:
 * base = base + (base * percentage1) + (base * percentage2) + rawValue
 */
public final class GearModel extends Model {
    private final GearInfoRegistry gearInfoRegistry = new GearInfoRegistry();

    private final GearChatEncoding gearChatEncoding = new GearChatEncoding();
    private final Map<GearBoxItem, List<GearInfo>> possibilitiesCache = new HashMap<>();
    private final Map<String, List<SetInstance>> activeSetsCache = new HashMap<>();

    public GearModel(SetModel setModel) {
        super(List.of(setModel));
    }

    public List<GearInfo> getPossibleGears(GearBoxItem gearBoxItem) {
        List<GearInfo> possibilities = possibilitiesCache.get(gearBoxItem);
        if (possibilities != null) return possibilities;

        List<GearInfo> possibleGear = getAllGearInfos()
                .filter(gear -> gear.type() == gearBoxItem.getGearType()
                        && gear.tier() == gearBoxItem.getGearTier()
                        && canBeGearBox(gear)
                        && gearBoxItem
                                .getLevelRange()
                                .inRange(gear.requirements().level()))
                .toList();
        possibilitiesCache.put(gearBoxItem, possibleGear);

        return possibleGear;
    }

    public boolean canBeGearBox(GearInfo gear) {
        // If an item is pre-identified, it cannot be in a gear box
        // If all the ways we can obtain this is by merchants, it cannot be in a gear box
        return !gear.metaInfo().preIdentified()
                && gear.metaInfo().obtainInfo().stream()
                        .anyMatch(o -> !o.sourceType().isMerchant());
    }

    @Override
    public void reloadData() {
        gearInfoRegistry.reloadData();
    }

    // For "real" gear items eg. from the inventory
    public GearInstance parseInstance(GearInfo gearInfo, ItemStack itemStack) {
        WynnItemParseResult result = WynnItemParser.parseItemStack(itemStack, gearInfo.getVariableStatsMap());
        if (result.tier() != gearInfo.tier()) {
            WynntilsMod.warn("Tier for " + gearInfo.name() + " is reported as " + result.tier());
        }

        return GearInstance.create(
                gearInfo, result.identifications(), result.powders(), result.rerolls(), result.shinyStat());
    }

    // For parsing gear from the gear viewer
    public GearInstance parseInstance(GearInfo gearInfo, JsonObject itemData) {
        WynnItemParseResult result = WynnItemParser.parseInternalRolls(gearInfo, itemData);

        return GearInstance.create(
                gearInfo, result.identifications(), result.powders(), result.rerolls(), result.shinyStat());
    }

    public CraftedGearItem parseCraftedGearItem(ItemStack itemStack) {
        // We pass this down to the parser, so it can populate it
        // (gears don't have to parse possible values on the fly, since the api provides them)
        Map<StatType, StatPossibleValues> possibleValuesMap = new HashMap<>();
        WynnItemParseResult result = WynnItemParser.parseItemStack(itemStack, possibleValuesMap);

        CraftedItemParseResults craftedResults = WynnItemParser.parseCraftedItem(itemStack);
        CappedValue durability = new CappedValue(result.durabilityCurrent(), result.durabilityMax());
        GearType gearType;
        // If it is crafted, and has a skin, then we cannot determine weapon type from item stack
        // Maybe it is possible to find in the string type, e.g. "Crafted Wand"
        gearType = GearType.fromString(result.itemType());
        if (gearType == null && craftedResults.requirements().classType().isPresent()) {
            // If the item is signed, we can find the class type from the requirements
            gearType = GearType.fromClassType(
                    craftedResults.requirements().classType().get());
        }

        // If we still failed to find the gear type, try to find it from the item stack
        if (gearType == null) {
            gearType = GearType.fromItemStack(itemStack);

            if (gearType == null) {
                // If we failed to find the gear type, assume it is a weapon
                gearType = GearType.WEAPON;
            }
        }

        return new CraftedGearItem(
                craftedResults.name(),
                craftedResults.effectStrength(),
                gearType,
                craftedResults.attackSpeed(),
                result.health(),
                craftedResults.damages(),
                craftedResults.defences(),
                craftedResults.requirements(),
                possibleValuesMap.values().stream().toList(),
                result.identifications(),
                result.powders(),
                result.powderSlots(),
                durability);
    }

    public UnknownGearItem parseUnknownGearItem(
            String name, GearType gearType, GearTier gearTier, ItemStack itemStack) {
        WynnItemParseResult result = WynnItemParser.parseItemStack(itemStack, null);

        // FIXME: Damages and requirements are not yet parsed
        return new UnknownGearItem(
                name,
                gearType,
                gearTier,
                result.level(),
                List.of(),
                List.of(),
                result.identifications(),
                result.powders(),
                result.rerolls());
    }

    public GearItem fromEncodedString(String encoded) {
        return gearChatEncoding.fromEncodedString(encoded);
    }

    public String toEncodedString(GearItem gearItem) {
        return gearChatEncoding.toEncodedString(gearItem);
    }

    public Matcher gearChatEncodingMatcher(String str) {
        return gearChatEncoding.gearChatEncodingMatcher(str);
    }

    public GearInfo getGearInfoFromDisplayName(String gearName) {
        return gearInfoRegistry.getFromDisplayName(gearName);
    }

    public GearInfo getGearInfoFromApiName(String apiName) {
        return gearInfoRegistry.getFromApiName(apiName);
    }

    public Stream<GearInfo> getAllGearInfos() {
        return gearInfoRegistry.getGearInfoStream();
    }
}
