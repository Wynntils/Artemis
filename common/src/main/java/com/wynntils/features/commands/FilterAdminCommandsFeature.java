/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.commands;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.CommandsAddedEvent;
import java.util.Set;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.COMMANDS)
public class FilterAdminCommandsFeature extends Feature {
    private static final Set<String> FILTERED_COMMANDS = Set.of(
            "bungee",
            "change",
            "connect",
            "galert",
            "gcountdown",
            "glist",
            "gsend",
            "lobby",
            "perms",
            "pfind",
            "plist",
            "pwlist",
            "sendtoall",
            "servers",
            "sparkb",
            "sparkbungee",
            "wcl",
            "wynnproxy");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCommandPacket(CommandsAddedEvent event) {
        RootCommandNode<SharedSuggestionProvider> root = event.getRoot();

        RootCommandNode<SharedSuggestionProvider> newRoot = new RootCommandNode<>();
        for (CommandNode<SharedSuggestionProvider> child : root.getChildren()) {
            // Only add literal nodes, not argument nodes
            if (child instanceof LiteralCommandNode<SharedSuggestionProvider> literalChild) {
                if (!FILTERED_COMMANDS.contains(literalChild.getName())) {
                    newRoot.addChild(literalChild);
                }
            }
        }

        event.setRoot(newRoot);
    }
}
