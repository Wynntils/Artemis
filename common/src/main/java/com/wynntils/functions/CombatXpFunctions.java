/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions;

import com.wynntils.core.components.Models;
import com.wynntils.core.functions.Function;
import com.wynntils.core.functions.arguments.FunctionArguments;
import com.wynntils.utils.StringUtils;
import java.util.List;

public class CombatXpFunctions {
    public static class XpPerMinuteRawFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return (int) (Models.CombatXp.getRawXpGainInLastMinute().stream()
                    .mapToDouble(Float::doubleValue)
                    .sum());
        }

        @Override
        public List<String> getAliases() {
            return List.of("xpm_raw");
        }
    }

    public static class XpPerMinuteFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return StringUtils.integerToShortString((int) (Models.CombatXp.getRawXpGainInLastMinute().stream()
                    .mapToDouble(Float::doubleValue)
                    .sum()));
        }

        @Override
        public List<String> getAliases() {
            return List.of("xpm");
        }
    }

    public static class XpPercentagePerMinuteFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Models.CombatXp.getPercentageXpGainInLastMinute().stream()
                    .mapToDouble(Float::doubleValue)
                    .sum();
        }

        @Override
        public List<String> getAliases() {
            return List.of("xppm");
        }
    }

    public static class LevelFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.CombatXp.getCombatLevel().current();
        }

        @Override
        public List<String> getAliases() {
            return List.of("lvl");
        }
    }

    public static class XpFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return StringUtils.integerToShortString(Models.CombatXp.getXp().current());
        }
    }

    public static class XpRawFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.CombatXp.getXp().current();
        }
    }

    public static class XpReqFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return StringUtils.integerToShortString(Models.CombatXp.getXp().max());
        }
    }

    public static class XpReqRawFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.CombatXp.getXp().max();
        }
    }

    public static class XpPctFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Models.CombatXp.getXp().getPercentage();
        }
    }
}
