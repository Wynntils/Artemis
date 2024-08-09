/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.consumers.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.wynntils.commands.BombCommand;
import com.wynntils.commands.CompassCommand;
import com.wynntils.commands.ConfigCommand;
import com.wynntils.commands.FeatureCommand;
import com.wynntils.commands.FunctionCommand;
import com.wynntils.commands.LocateCommand;
import com.wynntils.commands.LootrunCommand;
import com.wynntils.commands.MapCommand;
import com.wynntils.commands.OnlineMembersCommand;
import com.wynntils.commands.PlayerCommand;
import com.wynntils.commands.QuestCommand;
import com.wynntils.commands.ServersCommand;
import com.wynntils.commands.StatisticsCommand;
import com.wynntils.commands.TerritoryCommand;
import com.wynntils.commands.WynntilsCommand;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.mc.event.CommandsAddedEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.bus.api.SubscribeEvent;

// Credits to Earthcomputer and Forge
// Parts of this code originates from https://github.com/Earthcomputer/clientcommands, and other
// parts originate from https://github.com/MinecraftForge/MinecraftForge
// Kudos to both of the above

/**
 * We register our commands in two significant ways:
 * <ol>
 *     <li> Registering them to our custom client dispatcher,
 *     which is used to parse and execute commands.
 *     <li> Registering them to the server dispatcher, which is used to suggest commands.
 *     This is done after the server initializes the dispatcher.
 * </ol>
 */
public final class ClientCommandManager extends Manager {
    private final CommandDispatcher<CommandSourceStack> clientDispatcher = new CommandDispatcher<>();
    private final List<Command> commandInstanceSet = new ArrayList<>();

    private WynntilsCommand wynntilsCommand;

    public ClientCommandManager() {
        super(List.of());

        registerAllCommands();
    }

    @SubscribeEvent
    public void onCommandsAdded(CommandsAddedEvent event) {
        CommandBuildContext context = event.getContext();

        for (Command command : commandInstanceSet) {
            // Register the command to the client dispatcher
            command.getCommandBuilders(context).forEach(clientDispatcher::register);

            // Register the command to the server dispatcher
            command.getCommandBuilders(context).stream()
                    .map(LiteralArgumentBuilder::build)
                    .forEach(node -> addNode(event.getRoot(), node));
        }

        // Wynntils command is special,
        // it registers every other command as a subcommand

        // Register the command to the client dispatcher
        wynntilsCommand.registerWithCommands(clientDispatcher::register, context, commandInstanceSet);

        // Register the command to the server dispatcher
        wynntilsCommand.registerWithCommands(
                builder -> {
                    addNode(event.getRoot(), builder.build());
                },
                context,
                commandInstanceSet);
    }

    @SuppressWarnings("unchecked")
    public void addNode(
            RootCommandNode<SharedSuggestionProvider> root, CommandNode<? extends SharedSuggestionProvider> node) {
        root.addChild((LiteralCommandNode<SharedSuggestionProvider>) node);
    }

    public void addNodeToClientDispatcher(LiteralArgumentBuilder<CommandSourceStack> nodeBuilder) {
        clientDispatcher.register(nodeBuilder);
    }

    public boolean handleCommand(String message) {
        StringReader reader = new StringReader(message);
        return executeCommand(reader, message);
    }

    private boolean executeCommand(StringReader reader, String command) {
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
        } catch (CommandSyntaxException e) {
            McUtils.sendErrorToClient(e.getRawMessage().getString());
            if (e.getInput() != null && e.getCursor() >= 0) {
                int cursor = Math.min(e.getCursor(), e.getInput().length());
                MutableComponent text = Component.literal("")
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.GRAY)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
                if (cursor > 10) text.append("...");

                text.append(e.getInput().substring(Math.max(0, cursor - 10), cursor));
                if (cursor < e.getInput().length()) {
                    text.append(Component.literal(e.getInput().substring(cursor))
                            .withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
                }

                text.append(Component.translatable("command.context.here")
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                sendError(text);
            }
        } catch (RuntimeException e) {
            MutableComponent error =
                    Component.literal(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            sendError(Component.translatable("command.failed")
                    .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, error))));
            WynntilsMod.error("Failed to execute command.", e);
        }

        return true;
    }

    private ClientCommandSourceStack getSource() {
        LocalPlayer player = McUtils.player();

        if (player == null) return null;

        return new ClientCommandSourceStack(player);
    }

    private void sendError(MutableComponent error) {
        McUtils.sendMessageToClient(error.withStyle(ChatFormatting.RED));
    }

    public List<Command> getCommandInstanceSet() {
        return commandInstanceSet;
    }

    private void registerCommand(Command command) {
        commandInstanceSet.add(command);
    }

    private void registerCommandWithCommandSet(WynntilsCommand command) {
        wynntilsCommand = command;
    }

    private void registerAllCommands() {
        registerCommand(new BombCommand());
        registerCommand(new CompassCommand());
        registerCommand(new ConfigCommand());
        registerCommand(new FeatureCommand());
        registerCommand(new FunctionCommand());
        registerCommand(new LocateCommand());
        registerCommand(new LootrunCommand());
        registerCommand(new MapCommand());
        registerCommand(new OnlineMembersCommand());
        registerCommand(new PlayerCommand());
        registerCommand(new QuestCommand());
        registerCommand(new ServersCommand());
        registerCommand(new StatisticsCommand());
        registerCommand(new TerritoryCommand());

        // The WynntilsCommand must be registered last, since it
        // need the above commands as aliases
        registerCommandWithCommandSet(new WynntilsCommand());
    }
}
