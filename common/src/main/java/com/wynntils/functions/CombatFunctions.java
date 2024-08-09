/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.arguments.FunctionArguments;
import java.util.List;

public class CombatFunctions {
    public static class AreaDamagePerSecondFunction extends Function<Long> {
        @Override
        public Long getValue(FunctionArguments arguments) {
            return Models.Damage.getAreaDamagePerSecond();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("adps");
        }
    }

    public static class AreaDamageAverageFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Models.Damage.getAverageAreaDamagePerSecond(
                    arguments.getArgument("seconds").getIntegerValue());
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.OptionalArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("seconds", Integer.class, 10)));
        }

        @Override
        protected List<String> getAliases() {
            return List.of("adavg");
        }
    }

    public static class BlocksAboveGroundFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getBlocksAboveGround();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("agl", "above_ground_level");
        }
    }

    public static class LastSpellNameFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return arguments.getArgument("burst").getBooleanValue()
                    ? Models.Spell.getLastBurstSpellName()
                    : Models.Spell.getLastSpellName();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.OptionalArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("burst", Boolean.class, false)));
        }

        @Override
        protected List<String> getAliases() {
            return List.of("recast_name");
        }
    }

    public static class LastSpellRepeatCountFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return arguments.getArgument("burst").getBooleanValue()
                    ? Models.Spell.getRepeatedBurstSpellCount()
                    : Models.Spell.getRepeatedSpellCount();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.OptionalArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("burst", Boolean.class, false)));
        }

        @Override
        protected List<String> getAliases() {
            return List.of("recast_count");
        }
    }

    public static class TicksSinceLastSpellFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return arguments.getArgument("burst").getBooleanValue()
                    ? Models.Spell.getTicksSinceCastBurst()
                    : Models.Spell.getTicksSinceCast();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.OptionalArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("burst", Boolean.class, false)));
        }

        @Override
        protected List<String> getAliases() {
            return List.of("recast_ticks");
        }
    }
}
