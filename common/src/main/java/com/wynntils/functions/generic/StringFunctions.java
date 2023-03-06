/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions.generic;

import com.wynntils.core.functions.GenericFunction;
import com.wynntils.core.functions.arguments.FunctionArguments;
import com.wynntils.utils.StringUtils;
import java.util.List;

public class StringFunctions {
    public static class FormatFunction extends GenericFunction<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return StringUtils.integerToShortString(
                    arguments.getArgument("value").getIntegerValue());
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("value", Number.class, null)));
        }
    }

    public static class StringFunction extends GenericFunction<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return arguments.getArgument("value").getValue().toString();
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("value", Number.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("str");
        }
    }

    public static class ConcatFunction extends GenericFunction<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            List<String> values = arguments.getArgument("values").asList().getValues();

            return String.join("", values);
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.ListArgument<>("values", String.class)));
        }
    }

    public static class StringEqualsFunction extends GenericFunction<Boolean> {
        @Override
        public Boolean getValue(FunctionArguments arguments) {
            return arguments
                    .getArgument("first")
                    .getStringValue()
                    .equals(arguments.getArgument("second").getStringValue());
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("first", String.class, null),
                    new FunctionArguments.Argument<>("second", String.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("eq_str");
        }
    }

    public static class ParseIntegerFunction extends GenericFunction<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            try {
                return Integer.parseInt(arguments.getArgument("value").getStringValue());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("value", String.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("parse_int");
        }
    }

    public static class ParseDoubleFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            try {
                return Double.parseDouble(arguments.getArgument("value").getStringValue());
            } catch (NumberFormatException ignored) {
                return 0.0d;
            }
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("value", String.class, null)));
        }
    }

    public static class RepeatFunction extends GenericFunction<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            String value = arguments.getArgument("value").getStringValue();
            int times = arguments.getArgument("count").getIntegerValue();

            return String.valueOf(value).repeat(Math.max(0, times));
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("value", String.class, null),
                    new FunctionArguments.Argument<>("count", Integer.class, null)));
        }
    }
}
