/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.wynntils.core.features.UserFeature;
import com.wynntils.mc.event.CommandsPacketEvent;
import java.util.Arrays;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AddCommandExpansionFeature extends UserFeature {
    // These commands are extracted from https://wynncraft.fandom.com/wiki/Commands

    private static final List<String> WYNN_COMMANDS = Arrays.asList(
            "buy",
            "claimingredientbomb",
            "claimitembomb",
            "class",
            "crates",
            "daily",
            "duel",
            "find",
            "fixquests",
            "fixstart",
            "forum",
            "g",
            "help",
            "hub",
            "itemlock",
            "kill",
            "msg",
            "p",
            "pet",
            "r",
            "relore",
            "renameitem",
            "renamepet",
            "report",
            "rules",
            "skiptutorial",
            "stream",
            "switch",
            "totems",
            "trade",
            "use");

    private static final List<String> WYNN_ALIASES = Arrays.asList(
            "cash",
            "change",
            "classes",
            "die",
            "f",
            "gc",
            "gold",
            "goldcoins",
            "lobby",
            "pets",
            "share",
            "shop",
            "store",
            "suicide",
            "tell",
            "trade");

    @SubscribeEvent
    public void onCommandPacket(CommandsPacketEvent event) {
        RootCommandNode root = event.getRoot();
        // Add commands with no structured arguments
        // FIXME: Some of these can be provided with structure
        for (String command : WYNN_COMMANDS) {
            root.addChild(literal(command).build());
        }
        // Add aliases with no structured arguments
        for (String command : WYNN_ALIASES) {
            root.addChild(literal(command).build());
        }
        // Add commands with structured arguments
        addChangetagCommandNode(root);
        addFriendCommandNode(root);
        addGuildCommandNode(root);
        addIgnoreCommandNode(root);
        addHousingCommandNode(root);
        addParticlesCommandNode(root);
        addPartyCommandNode(root);
        getToggleCommandNode(root);
    }

    private void addChangetagCommandNode(RootCommandNode root) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("changetag")
                .then(literal("VIP"))
                .then(literal("VIP+"))
                .then(literal("HERO"))
                .then(literal("CHAMPION"))
                .then(literal("RESET"));

        root.addChild(builder.build());
    }

    private void addFriendCommandNode(RootCommandNode root) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("friend")
                .then(literal("list"))
                .then(literal("online"))
                .then(literal("add").then(argument("name", StringArgumentType.string())))
                .then(literal("remove").then(argument("name", StringArgumentType.string())));

        root.addChild(builder.build());
    }

    private void addGuildCommandNode(RootCommandNode root) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("guild")
                .then(literal("join").then(argument("tag", StringArgumentType.greedyString())))
                .then(literal("leaderboard"))
                .then(literal("leave"))
                .then(literal("list"))
                .then(literal("log"))
                .then(literal("manage"))
                .then(literal("rewards"))
                .then(literal("stats"))
                .then(literal("territory"))
                .then(literal("xp").then(argument("amount", IntegerArgumentType.integer())));
        ;

        CommandNode<CommandSourceStack> node = builder.build();
        root.addChild(node);

        root.addChild(literal("gu").redirect(node).build());
    }

    private void addIgnoreCommandNode(RootCommandNode root) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("ignore")
                .then(literal("add").then(argument("name", StringArgumentType.string())))
                .then(literal("remove").then(argument("name", StringArgumentType.string())));

        root.addChild(builder.build());
    }

    private void addHousingCommandNode(RootCommandNode root) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("housing")
                .then(literal("allowedit").then(argument("name", StringArgumentType.string())))
                .then(literal("ban").then(argument("name", StringArgumentType.string())))
                .then(literal("disallowedit").then(argument("name", StringArgumentType.string())))
                .then(literal("edit"))
                .then(literal("invite").then(argument("name", StringArgumentType.string())))
                .then(literal("kick").then(argument("name", StringArgumentType.string())))
                .then(literal("kickall"))
                .then(literal("leave"))
                .then(literal("public"))
                .then(literal("unban").then(argument("name", StringArgumentType.string())))
                .then(literal("visit"));

        CommandNode<CommandSourceStack> node = builder.build();
        root.addChild(node);

        root.addChild(literal("is").redirect(node).build());
    }

    private void addParticlesCommandNode(RootCommandNode root) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("particles")
                .then(literal("off"))
                .then(literal("low"))
                .then(literal("medium"))
                .then(literal("high"))
                .then(literal("veryhigh"))
                .then(literal("highest"))
                .then(argument("particles_per_tick", IntegerArgumentType.integer()));

        CommandNode<CommandSourceStack> node = builder.build();
        root.addChild(node);

        root.addChild(literal("pq").redirect(node).build());
    }

    private void addPartyCommandNode(RootCommandNode root) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("party")
                .then(literal("ban").then(argument("name", StringArgumentType.string())))
                .then(literal("create"))
                .then(literal("disband"))
                .then(literal("finder"))
                .then(literal("invite").then(argument("name", StringArgumentType.string())))
                .then(literal("join").then(argument("name", StringArgumentType.string())))
                .then(literal("kick").then(argument("name", StringArgumentType.string())))
                .then(literal("leave"))
                .then(literal("list"))
                .then(literal("promote").then(argument("name", StringArgumentType.string())))
                .then(literal("unban").then(argument("name", StringArgumentType.string())));

        root.addChild(builder.build());
    }

    private void getToggleCommandNode(RootCommandNode root) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("toggle")
                .then(literal("100"))
                .then(literal("attacksound"))
                .then(literal("autojoin"))
                .then(literal("autotracking"))
                .then(literal("beacon"))
                .then(literal("blood"))
                .then(literal("bombbell"))
                .then(literal("combatbar"))
                .then(literal("friendpopups"))
                .then(literal("ghosts")
                        .then(literal("none"))
                        .then(literal("low"))
                        .then(literal("medium"))
                        .then(literal("high")))
                .then(literal("guildjoin"))
                .then(literal("guildpopups"))
                .then(literal("insults"))
                .then(literal("music"))
                .then(literal("outlines"))
                .then(literal("popups"))
                .then(literal("pouchmsg"))
                .then(literal("pouchpickup"))
                .then(literal("queststartbeacon"))
                .then(literal("rpwarning"))
                .then(literal("sb"))
                .then(literal("swears"))
                .then(literal("vet"))
                .then(literal("war"));

        root.addChild(builder.build());
    }
}
