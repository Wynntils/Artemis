/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public abstract class Command {
    public Command() {}

    public abstract void register(CommandDispatcher<CommandSourceStack> dispatcher);

    public abstract void registerName();
}
