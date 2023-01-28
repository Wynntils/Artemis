/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.stats;

import com.wynntils.models.stats.builders.MiscStatKind;
import com.wynntils.models.stats.type.DamageStatType;
import com.wynntils.models.stats.type.DefenceStatType;
import com.wynntils.models.stats.type.MiscStatType;
import com.wynntils.models.stats.type.SpellStatType;
import com.wynntils.models.stats.type.StatListDelimiter;
import com.wynntils.models.stats.type.StatListOrdering;
import com.wynntils.models.stats.type.StatType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StatListOrderer {
    // Legacy order was defined by Athena as a fixed list. This was missing several stat types; I have
    // tried filling them in into "logical" places
    private static final List<String> LEGACY_ORDER = List.of(
            "attackSpeedBonus",
            "mainAttackDamageBonusRaw",
            "mainAttackDamageBonus",
            "mainAttackNeutralDamageBonusRaw",
            "mainAttackNeutralDamageBonus",
            "mainAttackElementalDamageBonusRaw",
            "mainAttackElementalDamageBonus",
            "mainAttackEarthDamageBonusRaw",
            "mainAttackEarthDamageBonus",
            "mainAttackThunderDamageBonusRaw",
            "mainAttackThunderDamageBonus",
            "mainAttackWaterDamageBonusRaw",
            "mainAttackWaterDamageBonus",
            "mainAttackFireDamageBonusRaw",
            "mainAttackFireDamageBonus",
            "mainAttackAirDamageBonusRaw",
            "mainAttackAirDamageBonus",
            "spellDamageBonusRaw",
            "spellDamageBonus",
            "spellNeutralDamageBonusRaw",
            "spellNeutralDamageBonus",
            "spellElementalDamageBonusRaw",
            "spellElementalDamageBonus",
            "spellEarthDamageBonusRaw",
            "spellEarthDamageBonus",
            "spellThunderDamageBonusRaw",
            "spellThunderDamageBonus",
            "spellWaterDamageBonusRaw",
            "spellWaterDamageBonus",
            "spellFireDamageBonusRaw",
            "spellFireDamageBonus",
            "spellAirDamageBonusRaw",
            "spellAirDamageBonus",
            "", // delimiter
            "healthBonus",
            "healthRegenRaw",
            "healthRegen",
            "lifeSteal",
            "manaRegen",
            "manaSteal",
            "", // delimiter
            "damageBonusRaw",
            "damageBonus",
            "neutralDamageBonusRaw",
            "neutralDamageBonus",
            "elementalDamageBonusRaw",
            "elementalDamageBonus",
            "earthDamageBonusRaw",
            "earthDamageBonus",
            "thunderDamageBonusRaw",
            "thunderDamageBonus",
            "waterDamageBonusRaw",
            "waterDamageBonus",
            "fireDamageBonusRaw",
            "fireDamageBonus",
            "airDamageBonusRaw",
            "airDamageBonus",
            "", // delimiter
            "bonusEarthDefense",
            "bonusThunderDefense",
            "bonusWaterDefense",
            "bonusFireDefense",
            "bonusAirDefense",
            "", // delimiter
            "exploding",
            "poison",
            "thorns",
            "reflection",
            "", // delimiter
            "speed",
            "sprint",
            "sprintRegen",
            "jumpHeight",
            "", // delimiter
            "soulPoints",
            "lootBonus",
            "lootQuality",
            "emeraldStealing",
            "xpBonus",
            "gatherXpBonus",
            "gatherSpeed",
            "", // delimiter
            "spellCostRaw1",
            "spellCostPct1",
            "spellCostRaw2",
            "spellCostPct2",
            "spellCostRaw3",
            "spellCostPct3",
            "spellCostRaw4",
            "spellCostPct4");

    private static final List<MiscStatKind> WYNNCRAFT_MISC_ORDER_1 = List.of(
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
            MiscStatKind.SOUL_POINT_REGEN,
            MiscStatKind.STEALING,
            MiscStatKind.HEALTH_REGEN_RAW);
    private static final List<MiscStatKind> WYNNCRAFT_MISC_ORDER_2 =
            List.of(MiscStatKind.SPRINT, MiscStatKind.SPRINT_REGEN);
    private static final List<MiscStatKind> WYNNCRAFT_MISC_ORDER_3 = List.of(
            MiscStatKind.JUMP_HEIGHT,
            MiscStatKind.GATHER_XP_BONUS,
            MiscStatKind.GATHER_SPEED,
            MiscStatKind.LOOT_QUALITY);

    public static Map<StatListOrdering, List<StatType>> createOrderingMap(
            List<MiscStatType> miscStats,
            List<DefenceStatType> defenceStats,
            List<DamageStatType> damageStats,
            List<SpellStatType> spellStats) {

        return Map.of(
                StatListOrdering.DEFAULT,
                createDefaultOrdering(miscStats, defenceStats, damageStats, spellStats),
                StatListOrdering.WYNNCRAFT,
                createWynncraftOrdering(miscStats, defenceStats, damageStats, spellStats),
                StatListOrdering.LEGACY,
                createLegacyOrdering(miscStats, defenceStats, damageStats, spellStats));
    }

    private static List<StatType> createDefaultOrdering(
            List<MiscStatType> miscStats,
            List<DefenceStatType> defenceStats,
            List<DamageStatType> damageStats,
            List<SpellStatType> spellStats) {
        List<StatType> defaultOrdering = new ArrayList<>();

        // Default ordering is a lightly curated version of the Wynncraft vanilla ordering
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
            List<MiscStatType> miscStats,
            List<DefenceStatType> defenceStats,
            List<DamageStatType> damageStats,
            List<SpellStatType> spellStats) {
        List<StatType> wynncraftOrdering = new ArrayList<>();

        // Wynncraft order seem to have grown a bit haphazardly
        addMiscStats(wynncraftOrdering, miscStats, WYNNCRAFT_MISC_ORDER_1);
        wynncraftOrdering.add(new StatListDelimiter());
        wynncraftOrdering.addAll(damageStats);
        wynncraftOrdering.add(new StatListDelimiter());
        wynncraftOrdering.addAll(defenceStats);
        wynncraftOrdering.add(new StatListDelimiter());
        addMiscStats(wynncraftOrdering, miscStats, WYNNCRAFT_MISC_ORDER_2);
        wynncraftOrdering.add(new StatListDelimiter());
        wynncraftOrdering.addAll(spellStats);
        wynncraftOrdering.add(new StatListDelimiter());
        addMiscStats(wynncraftOrdering, miscStats, WYNNCRAFT_MISC_ORDER_3);

        return wynncraftOrdering;
    }

    private static void addMiscStats(
            List<StatType> targetList, List<MiscStatType> miscStats, List<MiscStatKind> miscOrder) {
        for (MiscStatKind kind : miscOrder) {
            StatType stat = getMiscStat(kind, miscStats);
            targetList.add(stat);
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
            List<MiscStatType> miscStats,
            List<DefenceStatType> defenceStats,
            List<DamageStatType> damageStats,
            List<SpellStatType> spellStats) {
        List<StatType> allStats = new ArrayList<>();
        allStats.addAll(miscStats);
        allStats.addAll(defenceStats);
        allStats.addAll(damageStats);
        allStats.addAll(spellStats);

        List<StatType> legacyOrdering = new ArrayList<>();

        // Legacy ordering is determined by a hard-coded list in Athena, which is
        // by LEGACY_ORDER
        for (String apiName : LEGACY_ORDER) {
            if (apiName.isEmpty()) {
                legacyOrdering.add(new StatListDelimiter());
            } else {
                legacyOrdering.addAll(fromApiName(apiName, allStats));
            }
        }
        return legacyOrdering;
    }

    private static List<StatType> fromApiName(String apiName, List<StatType> statTypeRegistry) {
        List<StatType> stats = new ArrayList<>();
        // We might have many stats matching the same name (for spell cost stats)
        // FIXME: do something about this?
        for (StatType stat : statTypeRegistry) {
            if (stat.getApiName().equals(apiName)) {
                stats.add(stat);
            }
        }
        return stats;
    }
}
