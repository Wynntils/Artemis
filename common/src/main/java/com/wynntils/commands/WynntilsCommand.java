/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.wynntils.commands.wynntils.WynntilsConfigCommand;
import com.wynntils.commands.wynntils.WynntilsFeatureCommand;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.commands.CommandBase;
import com.wynntils.core.features.Feature;
import com.wynntils.core.features.FeatureRegistry;
import com.wynntils.core.webapi.WebManager;
import com.wynntils.mc.utils.McUtils;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;

public class WynntilsCommand extends CommandBase {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wynntils")
                .then(Commands.literal("help").executes(this::help))
                .then(Commands.literal("discord").executes(this::discordLink))
                .then(Commands.literal("donate").executes(this::donateLink))
                .then(Commands.literal("reload").executes(this::reload))
                .then(Commands.literal("version").executes(this::version))
                .then(Commands.literal("config")
                        .then(WynntilsConfigCommand.buildGetConfigNode())
                        .then(WynntilsConfigCommand.buildSetConfigNode())
                        .then(WynntilsConfigCommand.buildResetConfigNode())
                        .then(WynntilsConfigCommand.buildReloadConfigNode()))
                .then(Commands.literal("feature")
                        .then(WynntilsFeatureCommand.buildListNode())
                        .then(WynntilsFeatureCommand.enableFeatureNode())
                        .then(WynntilsFeatureCommand.disableFeatureNode())
                        .executes(this::help)));
    }

    private int version(CommandContext<CommandSourceStack> context) {
        // TODO: Handle if dev env

        MutableComponent buildText;

        if (WynntilsMod.getVersion().isEmpty()) {
            buildText = new TextComponent("Unknown Version");
        } else {
            buildText = new TextComponent("Version " + WynntilsMod.getVersion());
        }

        buildText.append("\n");

        if (WynntilsMod.getBuildNumber() == -1) {
            buildText.append(new TextComponent("Unknown Build"));
        } else {
            buildText.append(new TextComponent("Build " + WynntilsMod.getBuildNumber()));
        }

        buildText.setStyle(buildText.getStyle().withColor(ChatFormatting.YELLOW));

        context.getSource().sendSuccess(buildText, false);
        return 1;
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        for (Feature feature : FeatureRegistry.getFeatures()) { // disable all active features before resetting web
            if (feature.isEnabled()) {
                feature.disable();
            }
        }

        WebManager.reset();

        WebManager.init(); // reloads api urls as well as web manager

        for (Feature feature : FeatureRegistry.getFeatures()) { // re-enable all features which should be
            if (feature.canEnable()) {
                feature.enable();

                if (!feature.isEnabled()) {
                    McUtils.sendMessageToClient(new TextComponent("Failed to reload ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(new TextComponent(feature.getTranslatedName()).withStyle(ChatFormatting.AQUA)));
                } else {
                    McUtils.sendMessageToClient(new TextComponent("Reloaded ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(new TextComponent(feature.getTranslatedName()).withStyle(ChatFormatting.AQUA)));
                }
            }
        }

        context.getSource()
                .sendSuccess(new TextComponent("Finished reloading everything").withStyle(ChatFormatting.GREEN), false);

        return 1;
    }

    private int donateLink(CommandContext<CommandSourceStack> context) {
        MutableComponent c = new TextComponent("You can donate to Wynntils at: ").withStyle(ChatFormatting.AQUA);
        MutableComponent url = new TextComponent("https://www.patreon.com/Wynntils")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.LIGHT_PURPLE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.patreon.com/Wynntils"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                new TextComponent("Click here to open in your" + " browser."))));

        context.getSource().sendSuccess(c.append(url), false);
        return 1;
    }

    private int help(CommandContext<CommandSourceStack> context) {
        MutableComponent text =
                new TextComponent("Wynntils' command list: ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));
        addCommandDescription(
                text, "wynntils", List.of("help"), "This shows a list of all available commands for Wynntils.");
        addCommandDescription(
                text, "wynntils", List.of("discord"), "This provides you with an invite to our Discord server.");
        addCommandDescription(text, "wynntils", List.of("version"), "This shows the installed Wynntils version.");
        //            addCommandDescription(text, "-wynntils", " changelog [major/latest]",
        // "This shows the changelog of your installed version.");
        //            text.append("\n");
        addCommandDescription(text, "wynntils", List.of("reload"), "This reloads all API data.");
        addCommandDescription(text, "wynntils", List.of("donate"), "This provides our Patreon link.");
        addCommandDescription(
                text,
                "token",
                List.of(),
                "This provides a clickable token for you to create a Wynntils account to manage" + " your cosmetics.");
        addCommandDescription(
                text, "territory", List.of(), "This makes your compass point towards a specified territory.");
        context.getSource().sendSuccess(text, false);
        return 1;
    }

    private int discordLink(CommandContext<CommandSourceStack> context) {
        MutableComponent msg =
                new TextComponent("You're welcome to join our Discord server at:\n").withStyle(ChatFormatting.GOLD);
        String discordInvite =
                WebManager.getApiUrls() == null ? null : WebManager.getApiUrls().get("DiscordInvite");
        MutableComponent link = new TextComponent(discordInvite == null ? "<Wynntils servers are down>" : discordInvite)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA));
        if (discordInvite != null) {
            link.setStyle(link.getStyle()
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, discordInvite))
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Click here to join our Discord" + " server."))));
        }
        context.getSource().sendSuccess(msg.append(link), false);
        return 1;
    }

    private static void addCommandDescription(
            MutableComponent text, String prefix, List<String> suffix, String description) {
        text.append("\n");

        StringBuilder suffixString = new StringBuilder("");

        for (String argument : suffix) {
            suffixString.append(" ").append(argument);
        }

        MutableComponent clickComponent = new TextComponent("");
        {
            clickComponent.setStyle(clickComponent
                    .getStyle()
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + prefix + suffixString))
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT, new TextComponent("Click here to run this command."))));

            MutableComponent prefixText = new TextComponent("-" + prefix).withStyle(ChatFormatting.DARK_GRAY);
            clickComponent.append(prefixText);

            if (!suffix.isEmpty()) {
                MutableComponent nameText = new TextComponent(suffixString.toString()).withStyle(ChatFormatting.GREEN);
                clickComponent.append(nameText);
            }

            clickComponent.append(" ");

            MutableComponent descriptionText =
                    new TextComponent(description).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            clickComponent.append(descriptionText);
        }

        text.append(clickComponent);
    }
}
