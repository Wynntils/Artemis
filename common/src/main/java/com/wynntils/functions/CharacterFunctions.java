/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.arguments.FunctionArguments;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.CappedValue;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.player.LocalPlayer;

public class CharacterFunctions {
    public static class CappedManaFunction extends Function<CappedValue> {
        @Override
        public CappedValue getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getMana();
        }
    }

    public static class CappedHealthFunction extends Function<CappedValue> {
        @Override
        public CappedValue getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getHealth();
        }
    }

    public static class SprintFunction extends Function<CappedValue> {
        @Override
        public CappedValue getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getSprint();
        }
    }

    public static class BpsFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            LocalPlayer player = McUtils.player();
            double dX = player.getX() - player.xOld;
            double dZ = player.getZ() - player.zOld;
            double dY = player.getY() - player.yOld;
            return Math.sqrt((dX * dX) + (dZ * dZ) + (dY * dY)) * 20;
        }
    }

    public static class BpsXzFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            LocalPlayer player = McUtils.player();
            double dX = player.getX() - player.xOld;
            double dZ = player.getZ() - player.zOld;
            return Math.sqrt((dX * dX) + (dZ * dZ)) * 20;
        }
    }

    public static class StatusEffectsFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            List<String> statusEffectsList = Models.StatusEffect.getStatusEffects().stream()
                    .map(statusEffect -> statusEffect.asString().getString())
                    .toList();

            return String.join("\n", statusEffectsList);
        }
    }

    public static class StatusEffectActiveFunction extends Function<Boolean> {
        @Override
        public Boolean getValue(FunctionArguments arguments) {
            String query = arguments.getArgument("query").getStringValue();
            return Models.StatusEffect.getStatusEffects().stream()
                    .anyMatch(statusEffect ->
                            statusEffect.getName().getStringWithoutFormatting().equals(query));
        }

        @Override
        protected List<String> getAliases() {
            return List.of("contains_effect");
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("query", String.class, null)));
        }
    }

    public static class ClassFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            Boolean showReskinnedName =
                    arguments.getArgument("showReskinnedName").getBooleanValue();

            String name = showReskinnedName
                    ? Models.Character.getActualName()
                    : Models.Character.getClassType().getActualName(false);

            if (arguments.getArgument("uppercase").getBooleanValue()) {
                return name.toUpperCase(Locale.ROOT);
            }

            return name;
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.OptionalArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("uppercase", Boolean.class, false),
                    new FunctionArguments.Argument<>("showReskinnedName", Boolean.class, true)));
        }
    }

    public static class ManaFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getMana().current();
        }
    }

    public static class ManaMaxFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getMana().max();
        }
    }

    public static class HealthFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getHealth().current();
        }
    }

    public static class HealthMaxFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getHealth().max();
        }
    }

    public static class HealthPctFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getHealth().getPercentage();
        }
    }

    public static class ManaPctFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Models.CharacterStats.getMana().getPercentage();
        }
    }

    public static class IdFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return Models.Character.getId();
        }
    }
}
