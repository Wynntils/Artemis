/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.bus.api.Event;

public class CommandsAddedEvent extends Event {
    private final CommandBuildContext context;

    private RootCommandNode<SharedSuggestionProvider> root;

    public CommandsAddedEvent(RootCommandNode<SharedSuggestionProvider> root, CommandBuildContext context) {
        this.root = root;
        this.context = context;
    }

    public RootCommandNode<SharedSuggestionProvider> getRoot() {
        return root;
    }

    public CommandBuildContext getContext() {
        return context;
    }

    public void setRoot(RootCommandNode<SharedSuggestionProvider> root) {
        this.root = root;
    }
}
