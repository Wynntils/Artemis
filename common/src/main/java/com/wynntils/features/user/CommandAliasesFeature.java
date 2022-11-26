/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.TypeOverride;
import com.wynntils.core.features.UserFeature;
import com.wynntils.mc.event.ChatSentEvent;
import com.wynntils.mc.event.CommandsPacketEvent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@SuppressWarnings({"unchecked", "rawtypes"})
public class CommandAliasesFeature extends UserFeature {
    @Config(visible = false)
    public List<CommandAlias> aliases = new ArrayList<>(List.of(
            new CommandAlias("guild attack", List.of("gu a", "guild a")),
            new CommandAlias("guild manage", List.of("gu m", "gu man", "guild m", "guild man")),
            new CommandAlias("guild territory", List.of("gu t", "gu terr", "guild t", "guild terr"))));

    @TypeOverride
    private final Type aliasesType = new TypeToken<List<CommandAlias>>() {}.getType();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChatSend(ChatSentEvent e) {
        String message = e.getMessage();

        if (message.startsWith("/")) {
            final String command = message.substring(1);
            for (CommandAlias commandAlias : aliases) {
                if (commandAlias.getAliases().stream().anyMatch(alias -> Objects.equals(alias, command))) {
                    e.setMessage("/" + commandAlias.getOriginalCommand());
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onCommandPacket(CommandsPacketEvent event) {
        RootCommandNode root = event.getRoot();

        for (CommandAlias commandAlias : aliases) {
            for (String alias : commandAlias.getAliases()) {
                String[] parts = alias.split(" ");
                LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(parts[0]);

                for (int i = 1; i < parts.length; i++) {
                    builder.then(Commands.literal(parts[i]));
                }

                root.addChild(builder.build());
            }
        }
    }

    public static class CommandAlias {
        private final String originalCommand;
        private final List<String> aliases;

        public CommandAlias(String originalCommand, List<String> aliases) {
            this.originalCommand = originalCommand;
            this.aliases = aliases;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public String getOriginalCommand() {
            return originalCommand;
        }
    }
}
