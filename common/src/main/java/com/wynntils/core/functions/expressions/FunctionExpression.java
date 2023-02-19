/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.functions.expressions;

import com.wynntils.core.components.Managers;
import com.wynntils.core.functions.Function;
import com.wynntils.core.functions.arguments.FunctionArguments;
import com.wynntils.core.functions.arguments.parser.ArgumentParser;
import com.wynntils.utils.type.ErrorOr;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionExpression extends Expression {
    // Function format:
    //   function_name(argument1; argument2; ...)
    //   function_name()
    //   function_name
    // Format appendix: F - formatted, 2 - decimal precision
    //   :F2
    //   :2
    //   :F
    private static final Pattern FUNCTION_EXPRESSION_PATTERN =
            Pattern.compile("(?<function>.+?)(\\((?<argument>.*)\\))?(\\:(?<formatted>F)?(?<decimals>[0-9]+)?)?");

    private final Function function;
    private final FunctionArguments arguments;
    private final boolean formatted;
    private final int decimals;

    protected FunctionExpression(
            String rawExpression, Function function, FunctionArguments arguments, boolean formatted, int decimals) {
        super(rawExpression);
        this.function = function;
        this.arguments = arguments;
        this.formatted = formatted;
        this.decimals = decimals;
    }

    @Override
    public ErrorOr<String> calculate() {
        return ErrorOr.of(Managers.Function.getRawValueString(function, arguments, formatted, decimals));
    }

    // This method attempts to parse a function expression in the following ways:
    //   1. The expression is not a function expression, in which case it returns an empty optional.
    //   2. The expression could be a function expression, but the function name is not a valid function, in which case
    // it returns an empty optional.
    //   3. The expression is a function expression, and the function name is a valid function, but the arguments are
    // invalid, in which case it returns an error.
    //   4. The expression is a function expression, and the function name is a valid function, and the arguments are
    // valid, in which case it returns the parsed expression.
    //
    //   Arguments are valid if:
    //     1. The number of arguments is equal to the number of arguments the function expects.
    //     2. There is no argument part in the expression, in which case the function is called with the default
    // arguments.
    //     3. The argument part is empty, in which case the function is called with the default arguments.
    public static ErrorOr<Optional<Expression>> tryParse(String rawExpression) {
        Matcher matcher = FUNCTION_EXPRESSION_PATTERN.matcher(rawExpression);

        if (!matcher.matches()) return ErrorOr.of(Optional.empty());

        // Handle function parsing

        Optional<Function<?>> functionOptional = Managers.Function.forName(matcher.group("function"));

        if (functionOptional.isEmpty()) {
            return ErrorOr.of(Optional.empty());
        }

        Function<?> function = functionOptional.get();

        // Handle formatting parsing

        boolean isFormatted = matcher.group("formatted") != null;
        String decimalMatch = matcher.group("decimals");
        int decimals = decimalMatch != null ? Integer.parseInt(decimalMatch) : 2;

        // Handle argument parsing

        FunctionArguments.Builder argumentsBuilder = function.getArguments();

        if (matcher.groupCount() < 3) {
            return ErrorOr.of(Optional.of(new FunctionExpression(
                    rawExpression, function, argumentsBuilder.buildWithDefaults(), isFormatted, decimals)));
        }

        String rawArguments = matcher.group("argument");

        if (rawArguments == null || rawArguments.isEmpty()) {
            return ErrorOr.of(Optional.of(new FunctionExpression(
                    rawExpression, function, argumentsBuilder.buildWithDefaults(), isFormatted, decimals)));
        }

        ErrorOr<FunctionArguments> value = ArgumentParser.parseArguments(argumentsBuilder, rawArguments);
        return value.hasError()
                ? ErrorOr.error(value.getError())
                : ErrorOr.of(Optional.of(
                        new FunctionExpression(rawExpression, function, value.getValue(), isFormatted, decimals)));
    }
}
