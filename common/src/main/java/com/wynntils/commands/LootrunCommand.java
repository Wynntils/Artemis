/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.wynntils.core.commands.CommandBase;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wynn.model.LootrunModel;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class LootrunCommand extends CommandBase {
    private static final SuggestionProvider<CommandSourceStack> LOOTRUN_SUGGESTION_PROVIDER =
            (context, suggestions) -> SharedSuggestionProvider.suggest(
                    Stream.of(LootrunModel.LOOTRUNS.list())
                            .map((name) -> name.replaceAll("\\.json$", ""))
                            .map(StringArgumentType::escapeIfRequired),
                    suggestions);

    private int loadLootrun(CommandContext<CommandSourceStack> context) {
        String fileName = StringArgumentType.getString(context, "lootrun");

        boolean successful = LootrunModel.tryLoadFile(fileName);
        Vec3 startingPoint = LootrunModel.getStartingPoint();

        if (!successful || startingPoint == null) {
            context.getSource()
                    .sendFailure(Component.translatable("feature.wynntils.lootrunUtils.lootrunCouldNotBeLoaded")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        BlockPos start = new BlockPos(startingPoint);
        context.getSource()
                .sendSuccess(
                        Component.translatable(
                                        "feature.wynntils.lootrunUtils.lootrunStart",
                                        start.getX(),
                                        start.getY(),
                                        start.getZ())
                                .withStyle(ChatFormatting.GREEN),
                        false);
        return 1;
    }

    private int recordLootrun(CommandContext<CommandSourceStack> context) {
        if (LootrunModel.getState() != LootrunModel.LootrunState.RECORDING) {
            LootrunModel.startRecording();
            context.getSource()
                    .sendSuccess(
                            Component.translatable(
                                    "feature.wynntils.lootrunUtils.recordStart",
                                    Component.literal("/lootrun record")
                                            .withStyle(ChatFormatting.UNDERLINE)
                                            .withStyle((style) -> style.withClickEvent(
                                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lootrun record")))),
                            false);
        } else {
            LootrunModel.stopRecording();
            context.getSource()
                    .sendSuccess(
                            Component.translatable(
                                            "feature.wynntils.lootrunUtils.recordStop1",
                                            Component.literal("/lootrun clear")
                                                    .withStyle(ChatFormatting.UNDERLINE)
                                                    .withStyle((style) -> style.withClickEvent(new ClickEvent(
                                                            ClickEvent.Action.RUN_COMMAND, "/lootrun clear"))))
                                    .withStyle(ChatFormatting.RED)
                                    .append("\n")
                                    .append(Component.translatable(
                                                    "feature.wynntils.lootrunUtils.recordStop2",
                                                    Component.literal("/lootrun save <name>")
                                                            .withStyle(ChatFormatting.UNDERLINE)
                                                            .withStyle((style) -> style.withClickEvent(new ClickEvent(
                                                                    ClickEvent.Action.SUGGEST_COMMAND,
                                                                    "/lootrun save "))))
                                            .withStyle(ChatFormatting.GREEN)),
                            false);
        }
        return 1;
    }

    private int saveLootrun(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        LootrunModel.LootrunSaveResult lootrunSaveResult = LootrunModel.trySaveCurrentLootrun(name);

        if (lootrunSaveResult == null) {
            return 0;
        }

        switch (lootrunSaveResult) {
            case SAVED -> {
                context.getSource()
                        .sendSuccess(
                                Component.translatable("feature.wynntils.lootrunUtils.savedLootrun")
                                        .withStyle(ChatFormatting.GREEN),
                                false);
                return 1;
            }
            case ERROR_SAVING -> {
                context.getSource()
                        .sendFailure(Component.translatable("feature.wynntils.lootrunUtils.errorSavingLootrun")
                                .withStyle(ChatFormatting.RED));
                return 0;
            }
            case ERROR_ALREADY_EXISTS -> {
                context.getSource()
                        .sendFailure(
                                Component.translatable("feature.wynntils.lootrunUtils.errorSavingLootrunAlreadyExists")
                                        .withStyle(ChatFormatting.RED));
                return 0;
            }
        }
        return 0;
    }

    private int addJsonLootrunNote(CommandContext<CommandSourceStack> context) {
        Component text = ComponentArgument.getComponent(context, "text");
        Entity root = McUtils.player().getRootVehicle();
        BlockPos pos = root.blockPosition();
        context.getSource()
                .sendSuccess(
                        Component.translatable("feature.wynntils.lootrunUtils.addedNote", pos.toShortString())
                                .append("\n" + text),
                        false);
        return LootrunModel.addNote(text);
    }

    private int addTextLootrunNote(CommandContext<CommandSourceStack> context) {
        Component text = Component.literal(StringArgumentType.getString(context, "text"));
        Entity root = McUtils.player().getRootVehicle();
        BlockPos pos = root.blockPosition();
        context.getSource()
                .sendSuccess(
                        Component.translatable("feature.wynntils.lootrunUtils.addedNote", pos.toShortString())
                                .append("\n" + text),
                        false);
        return LootrunModel.addNote(text);
    }

    private int listLootrunNote(CommandContext<CommandSourceStack> context) {
        List<LootrunModel.Note> notes = LootrunModel.getCurrentNotes();
        if (notes.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("feature.wynntils.lootrunUtils.listNoteNoNote"));
        } else {
            MutableComponent component = Component.translatable("feature.wynntils.lootrunUtils.listNoteHeader");
            for (LootrunModel.Note note : notes) {
                BlockPos pos = new BlockPos(note.position());
                String posString = pos.toShortString();

                component
                        .append("\n")
                        .append(Component.literal("[X]").withStyle((style) -> style.withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("feature.wynntils.lootrunUtils.listClickToDelete")))
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/lootrun note delete " + posString.replace(",", "")))
                                .withColor(ChatFormatting.RED)))
                        .append(" " + posString + ": ")
                        .append(note.component());
            }
            context.getSource().sendSuccess(component, false);
        }
        return 1;
    }

    private int deleteLootrunNote(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getSpawnablePos(context, "pos");
        var removedNote = LootrunModel.deleteNoteAt(pos);

        if (removedNote != null) {
            context.getSource()
                    .sendSuccess(
                            Component.translatable(
                                            "feature.wynntils.lootrunUtils.noteRemovedSuccessfully",
                                            removedNote.component())
                                    .withStyle(ChatFormatting.GREEN),
                            false);
        } else {
            String posString = pos.toShortString();
            context.getSource()
                    .sendFailure(Component.translatable("feature.wynntils.lootrunUtils.noteUnableToFind", posString));
        }
        return LootrunModel.recompileLootrun(true);
    }

    private int clearLootrun(CommandContext<CommandSourceStack> context) {
        if (LootrunModel.getState() == LootrunModel.LootrunState.DISABLED) {
            context.getSource().sendFailure(Component.translatable("feature.wynntils.lootrunUtils.noActiveLootrun"));
            return 0;
        }

        LootrunModel.clearCurrentLootrun();

        context.getSource()
                .sendSuccess(
                        Component.translatable("feature.wynntils.lootrunUtils.clearSuccessful")
                                .withStyle(ChatFormatting.GREEN),
                        false);
        return 1;
    }

    private int deleteLootrun(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        File file = new File(LootrunModel.LOOTRUNS, name + ".json");
        if (!file.exists()) {
            context.getSource()
                    .sendFailure(Component.translatable("feature.wynntils.lootrunUtils.lootrunDoesntExist", name));
        } else if (file.delete()) {
            context.getSource()
                    .sendSuccess(
                            Component.translatable("feature.wynntils.lootrunUtils.lootrunDeleted", name)
                                    .withStyle(ChatFormatting.GREEN),
                            false);
            return 1;
        } else {
            context.getSource()
                    .sendFailure(
                            Component.translatable("feature.wynntils.lootrunUtils.lootrunCouldNotBeDeleted", name));
        }
        return 0;
    }

    private int renameLootrun(CommandContext<CommandSourceStack> context) {
        String oldName = StringArgumentType.getString(context, "old");
        String newName = StringArgumentType.getString(context, "new");
        File oldFile = new File(LootrunModel.LOOTRUNS, oldName + ".json");
        File newFile = new File(LootrunModel.LOOTRUNS, newName + ".json");
        if (!oldFile.exists()) {
            context.getSource()
                    .sendFailure(Component.translatable("feature.wynntils.lootrunUtils.lootrunDoesntExist", oldName));
        } else if (oldFile.renameTo(newFile)) {
            context.getSource()
                    .sendSuccess(
                            Component.translatable("feature.wynntils.lootrunUtils.lootrunRenamed", oldName, newName)
                                    .withStyle(ChatFormatting.GREEN),
                            false);
            return 1;
        } else {
            context.getSource()
                    .sendFailure(Component.translatable(
                            "feature.wynntils.lootrunUtils.lootrunCouldNotBeRenamed", oldName, newName));
        }
        return 0;
    }

    private int addChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getSpawnablePos(context, "pos");

        boolean successful = LootrunModel.addChest(pos);

        if (successful) {
            context.getSource()
                    .sendSuccess(
                            Component.translatable("feature.wynntils.lootrunUtils.chestAdded", pos.toShortString())
                                    .withStyle(ChatFormatting.GREEN),
                            false);
        } else {
            context.getSource()
                    .sendFailure(Component.translatable(
                            "feature.wynntils.lootrunUtils.chestAlreadyAdded", pos.toShortString()));
        }

        return LootrunModel.recompileLootrun(true);
    }

    private int removeChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getSpawnablePos(context, "pos");

        boolean successful = LootrunModel.removeChest(pos);

        if (successful) {
            context.getSource()
                    .sendSuccess(
                            Component.translatable("feature.wynntils.lootrunUtils.chestRemoved", pos.toShortString())
                                    .withStyle(ChatFormatting.GREEN),
                            false);
        } else {
            context.getSource()
                    .sendFailure(Component.translatable(
                            "feature.wynntils.lootrunUtils.chestDoesNotExist", pos.toShortString()));
        }

        return LootrunModel.recompileLootrun(true);
    }

    private int undoLootrun(CommandContext<CommandSourceStack> context) {
        if (LootrunModel.getState() != LootrunModel.LootrunState.RECORDING) {
            context.getSource().sendFailure(Component.translatable("feature.wynntils.lootrunUtils.notRecording"));
        } else {
            LootrunModel.LootrunUndoResult lootrunUndoResult = LootrunModel.tryUndo();
            switch (lootrunUndoResult) {
                case SUCCESSFUL -> {
                    context.getSource()
                            .sendSuccess(Component.translatable("feature.wynntils.lootrunUtils.undoSuccessful"), false);
                    return 1;
                }
                case ERROR_STAND_NEAR_POINT -> {
                    context.getSource()
                            .sendFailure(Component.translatable("feature.wynntils.lootrunUtils.undoStandNear"));
                    return 0;
                }
                case ERROR_NOT_FAR_ENOUGH -> {
                    context.getSource()
                            .sendFailure(Component.translatable("feature.wynntils.lootrunUtils.undoNotFarEnough"));
                    return 0;
                }
            }
        }
        return 0;
    }

    private int folderLootrun(CommandContext<CommandSourceStack> context) {
        Util.getPlatform().openFile(LootrunModel.LOOTRUNS);
        return 1;
    }

    private int syntaxError(CommandContext<CommandSourceStack> context) {
        context.getSource()
                .sendFailure(Component.literal("Missing Commands.argument").withStyle(ChatFormatting.RED));
        return 0;
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(getBaseCommandBuilder());

        dispatcher.register(Commands.literal("lr").redirect(node));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getBaseCommandBuilder() {
        return Commands.literal("lootrun")
                .then(Commands.literal("load")
                        .then(Commands.argument("lootrun", StringArgumentType.string())
                                .suggests(LOOTRUN_SUGGESTION_PROVIDER)
                                .executes(this::loadLootrun)))
                .then(Commands.literal("record").executes(this::recordLootrun))
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(this::saveLootrun)))
                .then(Commands.literal("note")
                        .then(Commands.literal("add")
                                .then(Commands.literal("json")
                                        .then(Commands.argument("text", ComponentArgument.textComponent())
                                                .executes(this::addJsonLootrunNote)))
                                .then(Commands.literal("text")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(this::addTextLootrunNote))))
                        .then(Commands.literal("list").executes(this::listLootrunNote))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(this::deleteLootrunNote))))
                .then(Commands.literal("clear").executes(this::clearLootrun))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(LOOTRUN_SUGGESTION_PROVIDER)
                                .executes(this::deleteLootrun)))
                .then(Commands.literal("rename")
                        .then(Commands.argument("old", StringArgumentType.string())
                                .suggests(LOOTRUN_SUGGESTION_PROVIDER)
                                .then(Commands.argument("new", StringArgumentType.string())
                                        .executes(this::renameLootrun))))
                .then(Commands.literal("chest")
                        .then(Commands.literal("add")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(this::addChest)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(this::removeChest))))
                .then(Commands.literal("undo").executes(this::undoLootrun))
                .then(Commands.literal("folder").executes(this::folderLootrun))
                .executes(this::syntaxError);
    }
}
