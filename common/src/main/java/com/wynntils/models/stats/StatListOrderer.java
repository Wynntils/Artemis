/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.stats;

import com.wynntils.models.stats.builders.MiscStatKind;
import com.wynntils.models.stats.type.DamageStatType;
import com.wynntils.models.stats.type.DefenceStatType;
import com.wynntils.models.stats.type.MiscStatType;
import com.wynntils.models.stats.type.SkillStatType;
import com.wynntils.models.stats.type.SpellStatType;
import com.wynntils.models.stats.type.StatListDelimiter;
import com.wynntils.models.stats.type.StatListOrdering;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.ListUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StatListOrderer {
    // Legacy order was defined by Athena as a fixed list.
    // This was missing several stat types;
    // I have tried filling them in into "logical" places.
    // NOTE: This list was changed to use Artemis keys,
    //       as a hotfix to support Item API V3.
    //       This will be removed once the new encoding is implemented.
    private static final List<String> LEGACY_ORDER = List.of(
            "MISC_ATTACK_SPEED",
            "DAMAGE_MAIN_ATTACK_ALL_RAW",
            "DAMAGE_MAIN_ATTACK_ALL_PERCENT",
            "DAMAGE_MAIN_ATTACK_NEUTRAL_RAW",
            "DAMAGE_MAIN_ATTACK_NEUTRAL_PERCENT",
            "DAMAGE_MAIN_ATTACK_EARTH_RAW",
            "DAMAGE_MAIN_ATTACK_EARTH_PERCENT",
            "DAMAGE_MAIN_ATTACK_THUNDER_RAW",
            "DAMAGE_MAIN_ATTACK_THUNDER_PERCENT",
            "DAMAGE_MAIN_ATTACK_WATER_RAW",
            "DAMAGE_MAIN_ATTACK_WATER_PERCENT",
            "DAMAGE_MAIN_ATTACK_FIRE_RAW",
            "DAMAGE_MAIN_ATTACK_FIRE_PERCENT",
            "DAMAGE_MAIN_ATTACK_AIR_RAW",
            "DAMAGE_MAIN_ATTACK_AIR_PERCENT",
            "DAMAGE_MAIN_ATTACK_RAINBOW_RAW",
            "DAMAGE_MAIN_ATTACK_RAINBOW_PERCENT",
            "DAMAGE_SPELL_ALL_RAW",
            "DAMAGE_SPELL_ALL_PERCENT",
            "DAMAGE_SPELL_NEUTRAL_RAW",
            "DAMAGE_SPELL_NEUTRAL_PERCENT",
            "DAMAGE_SPELL_EARTH_RAW",
            "DAMAGE_SPELL_EARTH_PERCENT",
            "DAMAGE_SPELL_THUNDER_RAW",
            "DAMAGE_SPELL_THUNDER_PERCENT",
            "DAMAGE_SPELL_WATER_RAW",
            "DAMAGE_SPELL_WATER_PERCENT",
            "DAMAGE_SPELL_FIRE_RAW",
            "DAMAGE_SPELL_FIRE_PERCENT",
            "DAMAGE_SPELL_AIR_RAW",
            "DAMAGE_SPELL_AIR_PERCENT",
            "DAMAGE_SPELL_RAINBOW_RAW",
            "DAMAGE_SPELL_RAINBOW_PERCENT",
            "", // delimiter
            "MISC_HEALTH",
            "MISC_HEALTH_REGEN_RAW",
            "MISC_HEALTH_REGEN_PERCENT",
            "MISC_LIFE_STEAL",
            "MISC_MANA_REGEN",
            "MISC_MANA_STEAL",
            "", // delimiter
            "DAMAGE_ANY_ALL_RAW",
            "DAMAGE_ANY_ALL_PERCENT",
            "DAMAGE_ANY_NEUTRAL_RAW",
            "DAMAGE_ANY_NEUTRAL_PERCENT",
            "DAMAGE_ANY_EARTH_RAW",
            "DAMAGE_ANY_EARTH_PERCENT",
            "DAMAGE_ANY_THUNDER_RAW",
            "DAMAGE_ANY_THUNDER_PERCENT",
            "DAMAGE_ANY_WATER_RAW",
            "DAMAGE_ANY_WATER_PERCENT",
            "DAMAGE_ANY_FIRE_RAW",
            "DAMAGE_ANY_FIRE_PERCENT",
            "DAMAGE_ANY_AIR_RAW",
            "DAMAGE_ANY_AIR_PERCENT",
            "DAMAGE_ANY_RAINBOW_RAW",
            "DAMAGE_ANY_RAINBOW_PERCENT",
            "", // delimiter
            "DEFENCE_EARTH",
            "DEFENCE_THUNDER",
            "DEFENCE_WATER",
            "DEFENCE_FIRE",
            "DEFENCE_AIR",
            "", // delimiter
            "MISC_EXPLODING",
            "MISC_POISON",
            "MISC_THORNS",
            "MISC_REFLECTION",
            "", // delimiter
            "MISC_WALK_SPEED",
            "MISC_SPRINT",
            "MISC_SPRINT_REGEN",
            "MISC_JUMP_HEIGHT",
            "", // delimiter
            "MISC_SOUL_POINT_REGEN",
            "MISC_LOOT_BONUS",
            "MISC_LOOT_QUALITY",
            "MISC_STEALING",
            "MISC_XP_BONUS",
            "MISC_GATHER_XP_BONUS",
            "MISC_GATHER_SPEED",
            "", // delimiter
            "SPELL_FIRST_SPELL_COST_RAW",
            "SPELL_FIRST_SPELL_COST_PERCENT",
            "SPELL_SECOND_SPELL_COST_RAW",
            "SPELL_SECOND_SPELL_COST_PERCENT",
            "SPELL_THIRD_SPELL_COST_RAW",
            "SPELL_THIRD_SPELL_COST_PERCENT",
            "SPELL_FOURTH_SPELL_COST_RAW",
            "SPELL_FOURTH_SPELL_COST_PERCENT",
            // These were added in 2.0.3 and is not present in Legacy
            "MISC_HEALING_EFFICIENCY",
            "MISC_KNOCKBACK",
            "MISC_SLOW_ENEMY",
            "MISC_WEAKEN_ENEMY");

    private static final List<MiscStatKind> WYNNCRAFT_MISC_ORDER_1 = List.of(
            MiscStatKind.KNOCKBACK,
            MiscStatKind.HEALTH_REGEN_PERCENT,
            MiscStatKind.MANA_REGEN,
            MiscStatKind.LIFE_STEAL,
            MiscStatKind.MANA_STEAL,
            MiscStatKind.XP_BONUS,
            MiscStatKind.LOOT_BONUS,
            MiscStatKind.REFLECTION,
            MiscStatKind.THORNS,
            MiscStatKind.EXPLODING,
            MiscStatKind.WALK_SPEED,
            MiscStatKind.ATTACK_SPEED,
            MiscStatKind.POISON,
            MiscStatKind.HEALTH,
            MiscStatKind.STEALING,
            MiscStatKind.HEALTH_REGEN_RAW,
            MiscStatKind.HEALING_EFFICIENCY,
            MiscStatKind.SLOW_ENEMY,
            MiscStatKind.WEAKEN_ENEMY);
    private static final List<MiscStatKind> WYNNCRAFT_MISC_ORDER_2 =
            List.of(MiscStatKind.SPRINT, MiscStatKind.SPRINT_REGEN);
    private static final List<MiscStatKind> WYNNCRAFT_MISC_ORDER_3 = List.of(
            MiscStatKind.JUMP_HEIGHT,
            MiscStatKind.GATHER_XP_BONUS,
            MiscStatKind.GATHER_SPEED,
            MiscStatKind.LOOT_QUALITY);

    public static Map<StatListOrdering, List<StatType>> createOrderingMap(
            List<SkillStatType> skillStats,
            List<MiscStatType> miscStats,
            List<DefenceStatType> defenceStats,
            List<DamageStatType> damageStats,
            List<SpellStatType> spellStats) {
        return Map.of(
                StatListOrdering.DEFAULT,
                createDefaultOrdering(skillStats, miscStats, defenceStats, damageStats, spellStats),
                StatListOrdering.WYNNCRAFT,
                createWynncraftOrdering(skillStats, miscStats, defenceStats, damageStats, spellStats),
                StatListOrdering.LEGACY,
                createLegacyOrdering(skillStats, miscStats, defenceStats, damageStats, spellStats));
    }

    private static List<StatType> createDefaultOrdering(
            List<SkillStatType> skillStats,
            List<MiscStatType> miscStats,
            List<DefenceStatType> defenceStats,
            List<DamageStatType> damageStats,
            List<SpellStatType> spellStats) {
        List<StatType> defaultOrdering = new ArrayList<>();

        // Default ordering is a lightly curated version of the Wynncraft vanilla ordering
        defaultOrdering.addAll(skillStats);
        defaultOrdering.add(new StatListDelimiter());
        defaultOrdering.addAll(miscStats);
        defaultOrdering.add(new StatListDelimiter());
        defaultOrdering.addAll(defenceStats);
        defaultOrdering.add(new StatListDelimiter());
        defaultOrdering.addAll(damageStats);
        defaultOrdering.add(new StatListDelimiter());
        defaultOrdering.addAll(spellStats);
        return defaultOrdering;
    }

    private static List<StatType> createWynncraftOrdering(
            List<SkillStatType> skillStats,
            List<MiscStatType> miscStats,
            List<DefenceStatType> defenceStats,
            List<DamageStatType> damageStats,
            List<SpellStatType> spellStats) {
        List<StatType> wynncraftOrdering = new ArrayList<>();

        // Wynncraft order seem to have grown a bit haphazardly
        wynncraftOrdering.addAll(skillStats);
        wynncraftOrdering.add(new StatListDelimiter());
        addMiscStats(wynncraftOrdering, miscStats, WYNNCRAFT_MISC_ORDER_1);
        wynncraftOrdering.add(new StatListDelimiter());
        wynncraftOrdering.addAll(damageStats);
        wynncraftOrdering.add(new StatListDelimiter());
        wynncraftOrdering.addAll(defenceStats);
        wynncraftOrdering.add(new StatListDelimiter());
        addMiscStats(wynncraftOrdering, miscStats, WYNNCRAFT_MISC_ORDER_2);
        wynncraftOrdering.add(new StatListDelimiter());
        // Spell stats are swapped in Wynncraft, so in this case they have raw before percent
        List<SpellStatType> swappedSpellStats = new ArrayList<>(spellStats);
        ListUtils.swapPairwise(swappedSpellStats);
        wynncraftOrdering.addAll(swappedSpellStats);
        wynncraftOrdering.add(new StatListDelimiter());
        addMiscStats(wynncraftOrdering, miscStats, WYNNCRAFT_MISC_ORDER_3);

        return wynncraftOrdering;
    }

    private static void addMiscStats(
            List<StatType> targetList, List<MiscStatType> miscStats, List<MiscStatKind> miscOrder) {
        for (MiscStatKind kind : miscOrder) {
            StatType statType = getMiscStat(kind, miscStats);
            targetList.add(statType);
        }
    }

    private static StatType getMiscStat(MiscStatKind kind, List<MiscStatType> miscStats) {
        for (MiscStatType stat : miscStats) {
            if (stat.getKind() == kind) {
                return stat;
            }
        }
        return null;
    }

    private static List<StatType> createLegacyOrdering(
            List<SkillStatType> skillStats,
            List<MiscStatType> miscStats,
            List<DefenceStatType> defenceStats,
            List<DamageStatType> damageStats,
            List<SpellStatType> spellStats) {
        List<StatType> allStats = new ArrayList<>();
        // We add skill stats in a special way
        // The legacy order is defined by Athena, and is missing all skill stats
        // We can add them here, since they are guranteed to be fixed stats,
        // so it doesn't break the encoding order
        allStats.addAll(miscStats);
        allStats.addAll(defenceStats);
        allStats.addAll(damageStats);
        allStats.addAll(spellStats);

        List<StatType> legacyOrdering = new ArrayList<>();

        // We add skill stats separately, as they are not present in the legacy order
        legacyOrdering.addAll(skillStats);

        // Legacy ordering is determined by a hard-coded list in Athena, which is
        // by LEGACY_ORDER
        for (String keyName : LEGACY_ORDER) {
            if (keyName.isEmpty()) {
                legacyOrdering.add(new StatListDelimiter());
            } else {
                allStats.stream()
                        .filter(statType -> statType.getKey().equals(keyName))
                        .findFirst()
                        .ifPresent(legacyOrdering::add);
            }
        }
        return legacyOrdering;
    }
}
