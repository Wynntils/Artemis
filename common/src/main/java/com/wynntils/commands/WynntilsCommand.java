/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import com.wynntils.screens.base.WynntilsMenuScreenBase;
import com.wynntils.screens.wynntilsmenu.WynntilsMenuScreen;
import com.wynntils.services.athena.UpdateService;
import com.wynntils.utils.FileUtils;
import com.wynntils.utils.mc.McUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class WynntilsCommand extends Command {
    private static final Pattern STATUS_HEADING = Pattern.compile("<h1 class='status-page__title'>(.*)</h1>");

    public void registerWithCommands(
            Consumer<LiteralArgumentBuilder<CommandSourceStack>> consumer,
            CommandBuildContext context,
            List<Command> commands) {
        List<LiteralArgumentBuilder<CommandSourceStack>> commandBuilders = getCommandBuilders(context);

        // Also register all our commands as subcommands under the wynntils command and it's aliases
        for (LiteralArgumentBuilder<CommandSourceStack> builder : commandBuilders) {
            for (Command commandInstance : commands) {
                if (commandInstance == this) continue;

                commandInstance.getCommandBuilders(context).forEach(builder::then);
            }

            consumer.accept(builder);
        }
    }

    @Override
    public String getCommandName() {
        return "wynntils";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return base.then(Commands.literal("clearcaches")
                        .then(Commands.literal("run").executes(this::doClearCaches))
                        .executes(this::clearCaches))
                .then(Commands.literal("debug")
                        .then(Commands.literal("profile")
                                .then(Commands.literal("reset").executes(this::profileReset))
                                .then(Commands.literal("showAnnotations").executes(this::profileShowAnnotations))
                                .then(Commands.literal("showOverlays").executes(this::profileShowOverlays))))
                .then(Commands.literal("discord").executes(this::discordLink))
                .then(Commands.literal("donate").executes(this::donateLink))
                .then(Commands.literal("help").executes(this::help))
                .then(Commands.literal("menu").executes(this::menu))
                .then(Commands.literal("reauth").executes(this::reauth))
                .then(Commands.literal("reloadcaches").executes(this::reloadCaches))
                .then(Commands.literal("rescan").executes(this::rescan))
                .then(Commands.literal("status").executes(this::status))
                .then(Commands.literal("token").executes(this::token))
                .then(Commands.literal("update").executes(this::update))
                .then(Commands.literal("version").executes(this::version))
                .executes(this::help);
    }

    private int profileReset(CommandContext<CommandSourceStack> context) {
        Handlers.Item.resetProfiling();
        Managers.Overlay.resetProfiling();
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.debug.profile.cleared")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int profileShowAnnotations(CommandContext<CommandSourceStack> context) {
        Map<Class<?>, Integer> profilingTimes = Handlers.Item.getProfilingTimes();
        Map<Class<?>, Integer> profilingCounts = Handlers.Item.getProfilingCounts();

        showProfilingData(context, profilingTimes, profilingCounts);

        return 1;
    }

    private int profileShowOverlays(CommandContext<CommandSourceStack> context) {
        Map<Class<?>, Integer> profilingTimes = Managers.Overlay.getProfilingTimes();
        Map<Class<?>, Integer> profilingCounts = Managers.Overlay.getProfilingCounts();

        showProfilingData(context, profilingTimes, profilingCounts);

        return 1;
    }

    private void showProfilingData(
            CommandContext<CommandSourceStack> context,
            Map<Class<?>, Integer> profilingTimes,
            Map<Class<?>, Integer> profilingCounts) {
        StringBuilder resList = new StringBuilder();
        profilingTimes.entrySet().stream()
                .sorted(Map.Entry.<Class<?>, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    int time = entry.getValue();
                    int count = profilingCounts.get(entry.getKey());
                    double average = (double) time / count;
                    resList.append("%7d ms, %7d c, avg: %7.2f ms/c  %s\n"
                            .formatted(time, count, average, entry.getKey().getSimpleName()));
                });

        context.getSource()
                .sendSuccess(() -> Component.literal(resList.toString()).withStyle(ChatFormatting.AQUA), false);

        int totalCount = profilingCounts.values().stream().reduce(0, Integer::sum);
        int totalTime = profilingTimes.values().stream().reduce(0, Integer::sum);
        double average = (double) totalTime / totalCount;

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.debug.profile.total", totalTime, totalCount)
                                .withStyle(ChatFormatting.AQUA),
                        false);
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.debug.profile.avg", average)
                                .withStyle(ChatFormatting.AQUA),
                        false);
    }

    private int reauth(CommandContext<CommandSourceStack> context) {
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.reauth.tryReauth")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        Services.Hades.tryDisconnect();
        Services.WynntilsAccount.reauth();
        Models.Player.reset();
        // No need to try to re-connect to Hades, we will do that automatically when we get the new token

        return 1;
    }

    private int clearCaches(CommandContext<CommandSourceStack> context) {
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.clearCaches.warn")
                                .withStyle(ChatFormatting.DARK_RED),
                        false);
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.clearCaches.clickHere")
                                .withStyle(ChatFormatting.BLUE)
                                .withStyle(ChatFormatting.UNDERLINE)
                                .withStyle(style -> style.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wynntils clearcaches run"))),
                        false);

        return 1;
    }

    private int doClearCaches(CommandContext<CommandSourceStack> context) {
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.clearCaches.deleting")
                                .withStyle(ChatFormatting.YELLOW),
                        false);

        Managers.TickScheduler.scheduleLater(
                () -> {
                    FileUtils.deleteFolder(Managers.Net.getCacheDir());
                    FileUtils.deleteFolder(Services.Update.getUpdatesFolder());

                    System.exit(0);
                },
                100);

        return 1;
    }

    private int reloadCaches(CommandContext<CommandSourceStack> context) {
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.reloadCaches.reloading")
                                .withStyle(ChatFormatting.YELLOW),
                        false);

        // Reload all downloaded data
        WynntilsMod.reloadAllComponentData();

        return 1;
    }

    private int version(CommandContext<CommandSourceStack> context) {
        MutableComponent buildText;

        if (WynntilsMod.getVersion().isEmpty()) {
            buildText = Component.literal("Unknown Version");
        } else if (WynntilsMod.isDevelopmentBuild()) {
            buildText = Component.literal("Development Build");
        } else {
            buildText = Component.literal("Version " + WynntilsMod.getVersion());
        }

        buildText.setStyle(buildText.getStyle().withColor(ChatFormatting.YELLOW));

        context.getSource().sendSuccess(() -> buildText, false);
        return 1;
    }

    private int status(CommandContext<CommandSourceStack> context) {
        MutableComponent component =
                Component.literal("Reading status of Wynntils services from ").withStyle(ChatFormatting.WHITE);
        MutableComponent url = Component.literal(Managers.Url.getUrl(UrlId.LINK_WYNNTILS_STATUS))
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.LIGHT_PURPLE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.OPEN_URL, Managers.Url.getUrl(UrlId.LINK_WYNNTILS_STATUS)))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click here to open in your browser."))));

        context.getSource().sendSuccess(() -> component.append(url), false);

        ApiResponse result = Managers.Net.callApi(UrlId.LINK_WYNNTILS_STATUS);
        result.handleInputStream(
                is -> {
                    try (InputStreamReader isReader = new InputStreamReader(is, StandardCharsets.UTF_8);
                            BufferedReader reader = new BufferedReader(isReader)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Matcher m = STATUS_HEADING.matcher(line);
                            if (m.matches()) {
                                String status = m.group(1);
                                McUtils.sendMessageToClient(Component.literal("Wynntils status: ")
                                        .withStyle(ChatFormatting.WHITE)
                                        .append(Component.literal(status).withStyle(ChatFormatting.AQUA)));
                                return;
                            }
                        }
                    } catch (IOException e) {
                        WynntilsMod.warn("Failed to read status page", e);
                    }
                    McUtils.sendErrorToClient("Failed to read status page");
                },
                onError -> {
                    WynntilsMod.warn("Failed to read status page", onError);
                    McUtils.sendErrorToClient("Failed to read status page");
                });
        return 1;
    }

    private int donateLink(CommandContext<CommandSourceStack> context) {
        MutableComponent c =
                Component.literal("You can donate to Wynntils at: ").withStyle(ChatFormatting.AQUA);
        MutableComponent url = Component.literal(Managers.Url.getUrl(UrlId.LINK_WYNNTILS_PATREON))
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.LIGHT_PURPLE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.OPEN_URL, Managers.Url.getUrl(UrlId.LINK_WYNNTILS_PATREON)))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click here to open in your browser."))));

        context.getSource().sendSuccess(() -> c.append(url), false);
        return 1;
    }

    private int discordLink(CommandContext<CommandSourceStack> context) {
        MutableComponent msg = Component.literal("You're welcome to join our Discord server at:\n")
                .withStyle(ChatFormatting.GOLD);
        String discordInvite = Managers.Url.getUrl(UrlId.LINK_WYNNTILS_DISCORD_INVITE);
        MutableComponent link =
                Component.literal(discordInvite).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA));
        link.setStyle(link.getStyle()
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, discordInvite))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click here to join our Discord" + " server."))));
        context.getSource().sendSuccess(() -> msg.append(link), false);
        return 1;
    }

    private int token(CommandContext<CommandSourceStack> context) {
        if (!Services.WynntilsAccount.isLoggedIn()) {
            MutableComponent failed = Component.literal(
                            "Either setting up your Wynntils account or accessing the token failed. To try to set up the Wynntils account again, run ")
                    .withStyle(ChatFormatting.GREEN);
            failed.append(Component.literal("/wynntils reauth")
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wynntils reauth"))));
            context.getSource().sendFailure(failed);
            return 1;
        }

        String token = Services.WynntilsAccount.getToken();

        MutableComponent text = Component.literal("Wynntils Token ").withStyle(ChatFormatting.AQUA);
        MutableComponent response = Component.literal(token)
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT, Component.literal("Click me to register an account.")))
                        .withClickEvent((new ClickEvent(
                                ClickEvent.Action.OPEN_URL,
                                Managers.Url.buildUrl(UrlId.LINK_WYNNTILS_REGISTER_ACCOUNT, Map.of("token", token)))))
                        .withColor(ChatFormatting.DARK_AQUA)
                        .withUnderlined(true));
        text.append(response);

        context.getSource().sendSuccess(() -> text, false);

        return 1;
    }

    private int update(CommandContext<CommandSourceStack> context) {
        if (WynntilsMod.isDevelopmentEnvironment()) {
            context.getSource()
                    .sendFailure(Component.translatable("feature.wynntils.updates.error.development")
                            .withStyle(ChatFormatting.DARK_RED));
            WynntilsMod.error("Development environment detected, cannot update!");
            return 0;
        }

        CompletableFuture.runAsync(() -> {
            WynntilsMod.info("Attempting to fetch Wynntils update.");
            CompletableFuture<UpdateService.UpdateResult> completableFuture = Services.Update.tryUpdate();

            completableFuture.whenComplete((result, throwable) -> McUtils.sendMessageToClient(result.getMessage()));
        });

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("feature.wynntils.updates.checking")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int menu(CommandContext<CommandSourceStack> context) {
        // Delay is needed to prevent chat screen overwriting the menu screen
        Managers.TickScheduler.scheduleLater(() -> WynntilsMenuScreenBase.openBook(WynntilsMenuScreen.create()), 2);
        return 1;
    }

    private int rescan(CommandContext<CommandSourceStack> context) {
        Models.Character.scanCharacterInfo(true);
        return 1;
    }

    private int help(CommandContext<CommandSourceStack> context) {
        MutableComponent text = Component.literal("Available Wynntils commands: \n")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));

        //        describeWynntilsSubcommand(text, "changelog", "Show the changelog of your installed version");
        describeWynntilsSubcommand(text, "clearcaches", "Clears all Wynntils caches and closes the game");
        describeWynntilsSubcommand(text, "debug", "Debug command for developers.");
        describeWynntilsSubcommand(text, "discord", "Provide an invite link to our Discord server");
        describeWynntilsSubcommand(text, "donate", "Provides a link to our Patreon");
        describeWynntilsSubcommand(text, "help", "List of all available commands for Wynntils");
        describeWynntilsSubcommand(text, "menu", "Opens Wynntils Menu");
        describeWynntilsSubcommand(text, "reauth", "Re-authorize Wynntils online services (Athena and Hades)");
        describeWynntilsSubcommand(text, "reloadcaches", "Clear and re-download caches of online data");
        describeWynntilsSubcommand(text, "status", "Show Wynntils server status");
        describeWynntilsSubcommand(text, "token", "Provide a link for creating a Wynntils account");
        describeWynntilsSubcommand(text, "update", "Update Wynntils to the latest version");
        describeWynntilsSubcommand(text, "version", "Shows the version of Wynntils currently installed");

        List<Command> otherCommands = Managers.Command.getCommandInstanceSet().stream()
                .filter(c -> !(c instanceof WynntilsCommand))
                .toList();
        for (Command command : otherCommands) {
            describeCommand(text, command.getCommandName(), command.getDescription());
        }
        context.getSource().sendSuccess(() -> text, false);
        return 1;
    }

    private static void describeWynntilsSubcommand(MutableComponent text, String subcommand, String description) {
        describeCommand(text, "wynntils " + subcommand, description);
    }

    private static void describeCommand(MutableComponent text, String command, String description) {
        MutableComponent clickComponent = Component.empty();
        clickComponent.setStyle(clickComponent
                .getStyle()
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + command))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.literal("Click here to run this command"))));

        clickComponent.append(Component.literal("/" + command).withStyle(ChatFormatting.GREEN));
        clickComponent.append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY));
        clickComponent.append(Component.literal(description).withStyle(ChatFormatting.GRAY));

        text.append("\n");
        text.append(clickComponent);
    }
}
