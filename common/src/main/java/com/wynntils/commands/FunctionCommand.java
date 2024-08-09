/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.GenericFunction;
import com.wynntils.core.consumers.functions.arguments.FunctionArguments;
import com.wynntils.core.text.StyledText;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class FunctionCommand extends Command {
    private static final SuggestionProvider<CommandSourceStack> FUNCTION_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Stream.concat(
                            Managers.Function.getFunctions().stream().map(Function::getName),
                            Managers.Function.getFunctions().stream()
                                    .map(Function::getAliasList)
                                    .flatMap(Collection::stream)),
                    builder);

    private static final SuggestionProvider<CommandSourceStack> CRASHED_FUNCTION_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Managers.Function.getFunctions().stream()
                            .filter(Managers.Function::isCrashed)
                            .map(Function::getName),
                    builder);

    private static final SuggestionProvider<CommandSourceStack> FUNCTION_LIST_TYPES_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(ListType.values())
                            .map(Enum::name)
                            .map(s -> s.toLowerCase(Locale.ROOT))
                            .toList(),
                    builder);

    @Override
    public String getCommandName() {
        return "function";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return base.then(Commands.literal("list")
                        .executes(this::listFunctions)
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(FUNCTION_LIST_TYPES_SUGGESTION_PROVIDER)
                                .executes(this::listFunctions)))
                .then(Commands.literal("enable")
                        .then(Commands.argument("function", StringArgumentType.word())
                                .suggests(CRASHED_FUNCTION_SUGGESTION_PROVIDER)
                                .executes(this::enableFunction)))
                .then(Commands.literal("get")
                        .then(Commands.argument("function", StringArgumentType.word())
                                .suggests(FUNCTION_SUGGESTION_PROVIDER)
                                .executes(this::getValue)
                                .then(Commands.argument("argument", StringArgumentType.greedyString())
                                        .executes(this::getValue))))
                .then(Commands.literal("help")
                        .then(Commands.argument("function", StringArgumentType.word())
                                .suggests(FUNCTION_SUGGESTION_PROVIDER)
                                .executes(this::helpForFunction)))
                .then(Commands.literal("test")
                        .then(Commands.argument("template", StringArgumentType.greedyString())
                                .executes(this::testExpression))
                        .build())
                .executes(this::syntaxError);
    }

    private int testExpression(CommandContext<CommandSourceStack> context) {
        String template = context.getArgument("template", String.class);

        StyledText[] result = Managers.Function.doFormatLines(template);

        StyledText resultString = StyledText.join(", ", result);

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(
                                "Template calculated: \"%s\" -> [%s]".formatted(template, resultString.getString())),
                        false);

        return 1;
    }

    private int listFunctions(CommandContext<CommandSourceStack> context) {
        String type;

        try {
            type = context.getArgument("type", String.class);
        } catch (Exception e) {
            type = "all";
        }

        boolean all = type.equalsIgnoreCase("all");
        boolean onlyGeneric = type.equalsIgnoreCase("generic");
        boolean onlyNormal = type.equalsIgnoreCase("normal");

        List<Function<?>> functions = Managers.Function.getFunctions().stream()
                .filter(function -> all
                        || (onlyNormal && !(function instanceof GenericFunction<?>))
                        || (onlyGeneric && function instanceof GenericFunction<?>))
                .sorted(Comparator.comparing(function -> function instanceof GenericFunction<?>)
                        .thenComparing(o -> ((Function<?>) o).getName()))
                .toList();

        MutableComponent response = Component.literal(
                        onlyGeneric ? "Available generic functions: " : "Available functions:")
                .withStyle(ChatFormatting.AQUA);

        for (Function<?> function : functions) {
            MutableComponent functionComponent = Component.literal("\n - ").withStyle(ChatFormatting.GRAY);

            functionComponent
                    .append(Component.literal(function.getName())
                            .withStyle(
                                    function instanceof GenericFunction<?>
                                            ? ChatFormatting.GOLD
                                            : ChatFormatting.YELLOW))
                    .withStyle(style -> style.withClickEvent(
                            new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/function help " + function.getName())));
            if (!function.getAliasList().isEmpty()) {
                String aliasList = String.join(", ", function.getAliasList());

                functionComponent
                        .append(Component.literal(" [alias: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(aliasList).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("]").withStyle(ChatFormatting.GRAY));
            }

            functionComponent.withStyle(style -> style.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(function.getDescription()))));

            response.append(functionComponent);
        }

        context.getSource().sendSuccess(() -> response, false);

        return 1;
    }

    private int enableFunction(CommandContext<CommandSourceStack> context) {
        String functionName = context.getArgument("function", String.class);

        Optional<Function<?>> functionOptional = Managers.Function.forName(functionName);

        if (functionOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Function not found!").withStyle(ChatFormatting.RED));
            return 0;
        }

        Function<?> function = functionOptional.get();
        if (!Managers.Function.isCrashed(function)) {
            context.getSource()
                    .sendFailure(Component.literal("Function does not need to be enabled")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        Managers.Function.enableFunction(function);

        Component response = Component.literal(function.getName())
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" is now enabled").withStyle(ChatFormatting.WHITE));
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int getValue(CommandContext<CommandSourceStack> context) {
        Component argument;
        try {
            argument = Component.literal(StringArgumentType.getString(context, "argument"));
        } catch (IllegalArgumentException e) {
            argument = Component.literal("");
        }

        String functionName = context.getArgument("function", String.class);
        Optional<Function<?>> functionOptional = Managers.Function.forName(functionName);

        if (functionOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Function not found").withStyle(ChatFormatting.RED));
            return 0;
        }
        Function<?> function = functionOptional.get();

        MutableComponent result = Component.literal("");
        result.append(
                Managers.Function.getSimpleValueString(function, argument.getString(), ChatFormatting.YELLOW, true));
        context.getSource().sendSuccess(() -> result, false);
        return 1;
    }

    private int helpForFunction(CommandContext<CommandSourceStack> context) {
        String functionName = context.getArgument("function", String.class);

        Optional<Function<?>> functionOptional = Managers.Function.forName(functionName);

        if (functionOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Function not found").withStyle(ChatFormatting.RED));
            return 0;
        }

        Function<?> function = functionOptional.get();

        MutableComponent helpComponent = Component.empty();

        boolean isArgumentOptional =
                function.getArgumentsBuilder() instanceof FunctionArguments.OptionalArgumentBuilder;

        helpComponent.append(ChatFormatting.GRAY + "Type: " + ChatFormatting.WHITE
                + (function instanceof GenericFunction<?> ? "Generic" : "Normal") + "\n");
        helpComponent.append(
                ChatFormatting.GRAY + "Description: " + ChatFormatting.WHITE + function.getDescription() + "\n");
        helpComponent.append(ChatFormatting.GRAY + "Aliases:" + ChatFormatting.WHITE + " ["
                + String.join(", ", function.getAliasList()) + "]\n");
        helpComponent.append(ChatFormatting.GRAY + "Returns: " + ChatFormatting.WHITE
                + function.getFunctionType().getSimpleName() + "\n");
        helpComponent.append(ChatFormatting.GRAY + "Arguments:" + ChatFormatting.WHITE + " ("
                + (isArgumentOptional ? "Optional" : "Required")
                + ")");

        for (FunctionArguments.Argument<?> argument :
                function.getArgumentsBuilder().getArguments()) {
            String type;

            if (isArgumentOptional) {
                type = "(%s, default: %s)"
                        .formatted(
                                argument.getType().getSimpleName(),
                                argument.getDefaultValue().toString());
            } else if (argument instanceof FunctionArguments.ListArgument<?>) {
                type = "(List<" + argument.getType().getSimpleName() + ">)";
            } else {
                type = ("(" + argument.getType().getSimpleName() + ")");
            }

            String argumentDescription = "\n - " + ChatFormatting.YELLOW + argument.getName() + " " + type
                    + ChatFormatting.WHITE + ": " + function.getArgumentDescription(argument.getName());

            helpComponent.append(argumentDescription);
        }

        Component response = Component.literal("Function Manual: ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(function.getName() + "\n").withStyle(ChatFormatting.BOLD))
                .append(helpComponent.withStyle(ChatFormatting.WHITE));

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int syntaxError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Missing argument").withStyle(ChatFormatting.RED));
        return 0;
    }

    private enum ListType {
        ALL,
        GENERIC,
        NORMAL
    }
}
