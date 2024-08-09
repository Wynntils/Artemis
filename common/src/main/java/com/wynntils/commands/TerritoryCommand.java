/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wynntils.core.consumers.commands.Command;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class TerritoryCommand extends Command {
    private final CompassCommand delagate = new CompassCommand();

    @Override
    public String getCommandName() {
        return "territory";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return Commands.literal("territory")
                .then(Commands.argument("territory", StringArgumentType.greedyString())
                        .suggests(CompassCommand.TERRITORY_SUGGESTION_PROVIDER)
                        .executes(delagate::territory))
                .executes(delagate::syntaxError);
    }
}
