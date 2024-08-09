/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.models.worlds.type.BombInfo;
import com.wynntils.models.worlds.type.BombType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class BombCommand extends Command {
    private static final SuggestionProvider<CommandSourceStack> BOMB_TYPE_SUGGESTION_PROVIDER = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(BombType.values()).map(Enum::name), builder);

    @Override
    public String getCommandName() {
        return "bomb";
    }

    @Override
    public List<String> getAliases() {
        return List.of("bombbell", "bombs");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return base.then(Commands.literal("list").executes(this::listBombs))
                .then(Commands.literal("get")
                        .then(Commands.argument("bombType", StringArgumentType.word())
                                .suggests(BOMB_TYPE_SUGGESTION_PROVIDER)
                                .executes(this::getBombTypeList)))
                .executes(this::listBombs);
    }

    private int getBombTypeList(CommandContext<CommandSourceStack> context) {
        BombType bombType;

        try {
            bombType = BombType.valueOf(context.getArgument("bombType", String.class));
        } catch (IllegalArgumentException e) {
            context.getSource()
                    .sendFailure(Component.literal("Invalid bomb type").withStyle(ChatFormatting.RED));
            return 0;
        }

        Set<BombInfo> bombBells = Models.Bomb.getBombBells().stream()
                .filter(bombInfo -> bombInfo.bomb() == bombType)
                .collect(Collectors.toSet());

        MutableComponent component = getBombListComponent(bombBells);

        context.getSource().sendSuccess(() -> component, false);

        return 1;
    }

    private int listBombs(CommandContext<CommandSourceStack> context) {
        Set<BombInfo> bombBells = Models.Bomb.getBombBells();

        MutableComponent component = getBombListComponent(bombBells);

        context.getSource().sendSuccess(() -> component, false);

        return 1;
    }

    private static MutableComponent getBombListComponent(Set<BombInfo> bombBells) {
        MutableComponent response = Component.literal("Bombs: ").withStyle(ChatFormatting.GOLD);

        if (bombBells.isEmpty()) {
            response.append(Component.literal(
                                    "There are no active bombs at the moment! This might be because there are no bombs currently, or you do not have the ")
                            .withStyle(ChatFormatting.RED))
                    .append(Component.literal("CHAMPION").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(
                                    " rank on Wynncraft, which is necessary to receive bomb alerts from other servers.")
                            .withStyle(ChatFormatting.RED));
            return response;
        }

        for (BombInfo bomb : bombBells.stream()
                .sorted(Comparator.comparing(BombInfo::bomb)
                        .reversed()
                        .thenComparing(BombInfo::startTime)
                        .reversed())
                .toList()) {
            response.append(Component.literal("\n" + bomb.bomb().getName())
                            .withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(" on ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(bomb.server()).withStyle(ChatFormatting.WHITE)))
                    .append(Component.literal(" for: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(bomb.getRemainingString()).withStyle(ChatFormatting.WHITE)));
        }

        return response;
    }
}
