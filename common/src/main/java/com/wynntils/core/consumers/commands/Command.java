/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.consumers.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wynntils.core.persisted.Translatable;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public abstract class Command implements Translatable {
    public abstract String getCommandName();

    protected List<String> getAliases() {
        return List.of();
    }

    public String getDescription() {
        return getTranslation("description");
    }

    @Override
    public String getTypeName() {
        return "Command";
    }

    protected abstract LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context);

    public final List<LiteralArgumentBuilder<CommandSourceStack>> getCommandBuilders(CommandBuildContext context) {
        return Stream.concat(
                        Stream.of(Commands.literal(getCommandName())),
                        getAliases().stream().map(Commands::literal))
                .map(base -> getCommandBuilder(base, context))
                .toList();
    }
}
