/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.stats.builders;

import com.google.common.base.CaseFormat;
import com.wynntils.models.stats.type.AttackType;
import com.wynntils.models.stats.type.DamageStatType;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.models.stats.type.StatUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DamageStatBuilder extends StatBuilder<DamageStatType> {
    public static List<DamageStatType> createStats() {
        List<DamageStatType> statList = new ArrayList<>();

        DamageStatBuilder builder = new DamageStatBuilder();
        builder.buildStats(statList::add);
        return statList;
    }

    @Override
    public void buildStats(Consumer<DamageStatType> callback) {
        for (AttackType attackType : AttackType.values()) {
            for (DamageType damageType : DamageType.values()) {
                DamageStatType percentType = buildDamageStat(attackType, damageType, StatUnit.PERCENT);
                callback.accept(percentType);

                DamageStatType rawType = buildDamageStat(attackType, damageType, StatUnit.RAW);
                callback.accept(rawType);
            }
        }
    }

    private static DamageStatType buildDamageStat(AttackType attackType, DamageType damageType, StatUnit unit) {
        String apiName = buildApiName(attackType, damageType, unit);
        return new DamageStatType(
                buildKey(attackType, damageType, unit),
                buildDisplayName(attackType, damageType),
                apiName,
                buildLoreName(apiName),
                unit);
    }

    private static String buildApiName(AttackType attackType, DamageType damageType, StatUnit unit) {
        return CaseFormat.UPPER_CAMEL.to(
                CaseFormat.LOWER_CAMEL,
                attackType.getApiName() + damageType.getApiName() + "DamageBonus"
                        + (unit == StatUnit.RAW ? "Raw" : ""));
    }

    private static String buildKey(AttackType attackType, DamageType damageType, StatUnit unit) {
        return "DAMAGE_" + attackType.name() + "_" + damageType.name() + "_" + unit.name();
    }

    private static String buildDisplayName(AttackType attackType, DamageType damageType) {
        return damageType.getDisplayName() + attackType.getDisplayName() + "Damage";
    }

    private static String buildLoreName(String apiName) {
        return switch (apiName) {
                // A few damage stats do not follow normal rules
            case "spellDamageBonus" -> "SPELLDAMAGE";
            case "spellDamageBonusRaw" -> "SPELLDAMAGERAW";
            case "mainAttackDamageBonus" -> "DAMAGEBONUS";
            case "mainAttackDamageBonusRaw" -> "DAMAGEBONUSRAW";

            default -> CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, apiName);
        };
    }
}
