/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.config.HiddenConfig;
import com.wynntils.mc.event.CommandSentEvent;
import com.wynntils.mc.event.CommandsAddedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.COMMANDS)
public class CommandAliasesFeature extends Feature {
    @Persisted
    public final HiddenConfig<List<CommandAlias>> aliases = new HiddenConfig<>(new ArrayList<>(List.of(
            new CommandAlias("guild attack", List.of("gu a", "guild a")),
            new CommandAlias("guild manage", List.of("gu m", "gu man", "guild m", "guild man")),
            new CommandAlias("guild territory", List.of("gu t", "gu terr", "guild t", "guild terr")),
            new CommandAlias("partyfinder", List.of("pf")))));

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCommandSent(CommandSentEvent e) {
        String message = e.getCommand();

        for (CommandAlias commandAlias : aliases.get()) {
            if (commandAlias.getAliases().stream().anyMatch(alias -> Objects.equals(alias, message))) {
                e.setCanceled(true);
                Handlers.Command.sendCommandImmediately(commandAlias.getOriginalCommand());
                break;
            }
        }
    }

    @SubscribeEvent
    public void onCommandsAdded(CommandsAddedEvent event) {
        RootCommandNode<SharedSuggestionProvider> root = event.getRoot();

        for (CommandAlias commandAlias : aliases.get()) {
            for (String alias : commandAlias.getAliases()) {
                String[] parts = alias.split(" ");
                LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(parts[0]);

                for (int i = 1; i < parts.length; i++) {
                    builder.then(Commands.literal(parts[i]));
                }

                Managers.Command.addNode(root, builder.build());
            }
        }
    }

    private static final class CommandAlias {
        private final String originalCommand;
        private final List<String> aliases;

        private CommandAlias(String originalCommand, List<String> aliases) {
            this.originalCommand = originalCommand;
            this.aliases = aliases;
        }

        private List<String> getAliases() {
            return aliases;
        }

        private String getOriginalCommand() {
            return originalCommand;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;

            CommandAlias that = (CommandAlias) other;
            return originalCommand.equals(that.originalCommand) && aliases.equals(that.aliases);
        }

        @Override
        public int hashCode() {
            return Objects.hash(originalCommand, aliases);
        }
    }
}
