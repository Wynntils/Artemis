/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.wynntils.commands.LootrunCommand;
import com.wynntils.commands.ServerCommand;
import com.wynntils.commands.TerritoryCommand;
import com.wynntils.commands.TokenCommand;
import com.wynntils.commands.WynntilsCommand;
import com.wynntils.core.managers.Manager;
import com.wynntils.mc.event.ChatSentEvent;
import com.wynntils.mc.utils.McUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Credits to Earthcomputer and Forge
// Parts of this code originates from https://github.com/Earthcomputer/clientcommands, and other
// parts originate from https://github.com/MinecraftForge/MinecraftForge
// Kudos to both of the above
public final class ClientCommandManager extends Manager {
    private static CommandDispatcher<CommandSourceStack> clientDispatcher;

    public static CommandDispatcher<CommandSourceStack> getClientDispatcher() {
        return clientDispatcher;
    }

    public static void init() {
        clientDispatcher = new CommandDispatcher<>();

        registerCommand(new LootrunCommand());
        registerCommand(new ServerCommand());
        registerCommand(new TerritoryCommand());
        registerCommand(new TokenCommand());
        registerCommand(new WynntilsCommand());
    }

    private static void registerCommand(CommandBase command) {
        command.register(clientDispatcher);
    }

    @SubscribeEvent
    public static void onChatSend(ChatSentEvent e) {
        String message = e.getMessage();

        if (message.startsWith("/")) {
            StringReader reader = new StringReader(message);
            reader.skip();
            if (ClientCommandManager.executeCommand(reader, message)) {
                e.setCanceled(true);
            }
        }
    }

    public static CompletableFuture<Suggestions> getCompletionSuggestions(
            String cmd,
            CommandDispatcher<SharedSuggestionProvider> serverDispatcher,
            ParseResults<CommandSourceStack> clientParse,
            ParseResults<SharedSuggestionProvider> serverParse,
            int cursor) {
        StringReader stringReader = new StringReader(cmd);
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        CommandDispatcher<CommandSourceStack> clientDispatcher = getClientDispatcher();

        CompletableFuture<Suggestions> clientSuggestions =
                clientDispatcher.getCompletionSuggestions(clientParse, cursor);
        CompletableFuture<Suggestions> serverSuggestions =
                serverDispatcher.getCompletionSuggestions(serverParse, cursor);

        CompletableFuture<Suggestions> result = new CompletableFuture<>();

        CompletableFuture.allOf(clientSuggestions, serverSuggestions).thenRun(() -> {
            final List<Suggestions> suggestions = new ArrayList<>();
            suggestions.add(clientSuggestions.join());
            suggestions.add(serverSuggestions.join());
            result.complete(Suggestions.merge(stringReader.getString(), suggestions));
        });

        return result;
    }

    public static ClientCommandSourceStack getSource() {
        LocalPlayer player = McUtils.player();

        if (player == null) return null;

        return new ClientCommandSourceStack(player);
    }

    private static boolean executeCommand(StringReader reader, String command) {
        ClientCommandSourceStack source = getSource();

        if (source == null) return false;

        final ParseResults<CommandSourceStack> parse = clientDispatcher.parse(reader, source);

        if (!parse.getExceptions().isEmpty()
                || (parse.getContext().getCommand() == null
                        && parse.getContext().getChild() == null)) {
            return false; // can't parse - let server handle command
        }

        try {
            clientDispatcher.execute(parse);
        } catch (CommandRuntimeException e) {
            sendError(new TextComponent(e.getMessage()));
        } catch (CommandSyntaxException e) {
            sendError(new TextComponent(e.getRawMessage().getString()));
            if (e.getInput() != null && e.getCursor() >= 0) {
                int cursor = Math.min(e.getCursor(), e.getInput().length());
                MutableComponent text = new TextComponent("")
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.GRAY)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
                if (cursor > 10) text.append("...");

                text.append(e.getInput().substring(Math.max(0, cursor - 10), cursor));
                if (cursor < e.getInput().length()) {
                    text.append(new TextComponent(e.getInput().substring(cursor))
                            .withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
                }

                text.append(new TranslatableComponent("command.context.here")
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                ClientCommandManager.sendError(text);
            }
        } catch (RuntimeException e) {
            TextComponent error =
                    new TextComponent(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            ClientCommandManager.sendError(new TranslatableComponent("command.failed")
                    .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, error))));
            e.printStackTrace();
        }

        return true;
    }

    private static void sendError(MutableComponent error) {
        McUtils.sendMessageToClient(error.withStyle(ChatFormatting.RED));
    }
}
