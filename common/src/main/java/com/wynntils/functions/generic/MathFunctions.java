/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions.generic;

import com.wynntils.core.functions.GenericFunction;
import com.wynntils.core.functions.arguments.FunctionArguments;
import java.util.List;

public final class MathFunctions {
    public static class AddFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            List<Number> values =
                    arguments.<Number>getArgument("values").asList().getValues();

            return values.stream().mapToDouble(Number::doubleValue).sum();
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.ListArgument<>("values", Number.class)));
        }
    }

    public static class SubtractFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return arguments.getArgument("first").getDoubleValue()
                    - arguments.getArgument("second").getDoubleValue();
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("first", Number.class, null),
                    new FunctionArguments.Argument<>("second", Number.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("sub");
        }
    }

    public static class MultiplyFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            List<Number> values =
                    arguments.<Number>getArgument("values").asList().getValues();

            return values.stream().mapToDouble(Number::doubleValue).reduce(1, (a, b) -> a * b);
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.ListArgument<>("values", Number.class)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("mul");
        }
    }

    public static class DivideFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return arguments.getArgument("divident").getDoubleValue()
                    / arguments.getArgument("divisor").getDoubleValue();
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("divident", Number.class, null),
                    new FunctionArguments.Argument<>("divisor", Number.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("div");
        }
    }

    public static class ModuloFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return arguments.getArgument("divident").getDoubleValue()
                    % arguments.getArgument("divisor").getDoubleValue();
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("divident", Number.class, null),
                    new FunctionArguments.Argument<>("divisor", Number.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("mod");
        }
    }

    public static class PowerFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Math.pow(
                    arguments.getArgument("base").getDoubleValue(),
                    arguments.getArgument("exponent").getDoubleValue());
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("base", Number.class, null),
                    new FunctionArguments.Argument<>("exponent", Number.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("pow");
        }
    }

    public static class SquareRootFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Math.sqrt(arguments.getArgument("value").getDoubleValue());
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("value", Number.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("sqrt");
        }
    }

    public static class MaxFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Math.max(
                    arguments.getArgument("first").getDoubleValue(),
                    arguments.getArgument("second").getDoubleValue());
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("first", Number.class, null),
                    new FunctionArguments.Argument<>("second", Number.class, null)));
        }
    }

    public static class MinFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Math.min(
                    arguments.getArgument("first").getDoubleValue(),
                    arguments.getArgument("second").getDoubleValue());
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("first", Number.class, null),
                    new FunctionArguments.Argument<>("second", Number.class, null)));
        }
    }

    public static class RoundFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            double roundingValue =
                    Math.pow(10, arguments.getArgument("decimals").getIntegerValue());
            return Math.round(arguments.getArgument("value").getDoubleValue() * roundingValue) / roundingValue;
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("value", Number.class, null),
                    new FunctionArguments.Argument<>("decimals", Integer.class, null)));
        }
    }

    public static class IntegerFunction extends GenericFunction<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return arguments.getArgument("value").getIntegerValue();
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("value", Number.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("int");
        }
    }

    public static class RandomFunction extends GenericFunction<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            double min = arguments.getArgument("min").getIntegerValue();
            double max = arguments.getArgument("max").getIntegerValue();
            return (Math.random() * (max - min)) + min;
        }

        @Override
        public FunctionArguments.RequiredArgumentBuilder getRequiredArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("min", Number.class, null),
                    new FunctionArguments.Argument<>("max", Number.class, null)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("rand");
        }
    }
}
