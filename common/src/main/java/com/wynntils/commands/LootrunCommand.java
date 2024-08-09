/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.screens.base.WynntilsMenuScreenBase;
import com.wynntils.screens.lootrunpaths.WynntilsLootrunPathsScreen;
import com.wynntils.services.lootrunpaths.type.LootrunNote;
import com.wynntils.services.lootrunpaths.type.LootrunSaveResult;
import com.wynntils.services.lootrunpaths.type.LootrunState;
import com.wynntils.services.lootrunpaths.type.LootrunUndoResult;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.PosUtils;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
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

public class LootrunCommand extends Command {
    private static final SuggestionProvider<CommandSourceStack> LOOTRUN_SUGGESTION_PROVIDER =
            (context, suggestions) -> SharedSuggestionProvider.suggest(
                    Stream.of(Services.LootrunPaths.LOOTRUNS.list())
                            .map((name) -> name.replaceAll("\\.json$", ""))
                            .map(StringArgumentType::escapeIfRequired),
                    suggestions);

    @Override
    public String getCommandName() {
        return "lootrun";
    }

    @Override
    public List<String> getAliases() {
        return List.of("lr");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return base.then(Commands.literal("load")
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
                                        .then(Commands.argument("text", ComponentArgument.textComponent(context))
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
                .then(Commands.literal("screen").executes(this::screenLootrun))
                .executes(this::syntaxError);
    }

    private int loadLootrun(CommandContext<CommandSourceStack> context) {
        String fileName = StringArgumentType.getString(context, "lootrun");

        Services.LootrunPaths.tryLoadLootrun(fileName);

        return 1;
    }

    private int recordLootrun(CommandContext<CommandSourceStack> context) {
        if (Services.LootrunPaths.getState() != LootrunState.RECORDING) {
            Services.LootrunPaths.startRecording();
            context.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                    "command.wynntils.lootrun.recordStart",
                                    Component.literal("/lootrun record")
                                            .withStyle(ChatFormatting.UNDERLINE)
                                            .withStyle((style) -> style.withClickEvent(
                                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lootrun record")))),
                            false);
        } else {
            Services.LootrunPaths.stopRecording();
            context.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                            "command.wynntils.lootrun.recordStop1",
                                            Component.literal("/lootrun clear")
                                                    .withStyle(ChatFormatting.UNDERLINE)
                                                    .withStyle((style) -> style.withClickEvent(new ClickEvent(
                                                            ClickEvent.Action.RUN_COMMAND, "/lootrun clear"))))
                                    .withStyle(ChatFormatting.RED)
                                    .append("\n")
                                    .append(Component.translatable(
                                                    "command.wynntils.lootrun.recordStop2",
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
        LootrunSaveResult lootrunSaveResult = Services.LootrunPaths.saveCurrentLootrun(name);

        if (lootrunSaveResult == null) {
            return 0;
        }

        switch (lootrunSaveResult) {
            case SAVED -> {
                context.getSource()
                        .sendSuccess(
                                () -> Component.translatable("command.wynntils.lootrun.savedLootrun")
                                        .withStyle(ChatFormatting.GREEN),
                                false);
                return 1;
            }
            case ERROR_SAVING -> {
                context.getSource()
                        .sendFailure(Component.translatable("command.wynntils.lootrun.errorSavingLootrun")
                                .withStyle(ChatFormatting.RED));
                return 0;
            }
            case ERROR_ALREADY_EXISTS -> {
                context.getSource()
                        .sendFailure(Component.translatable("command.wynntils.lootrun.errorSavingLootrunAlreadyExists")
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
                        () -> Component.translatable("command.wynntils.lootrun.addedNote", pos.toShortString())
                                .append("\n" + text),
                        false);
        return Services.LootrunPaths.addNote(text);
    }

    private int addTextLootrunNote(CommandContext<CommandSourceStack> context) {
        Component text = Component.literal(StringArgumentType.getString(context, "text"));
        Entity root = McUtils.player().getRootVehicle();
        BlockPos pos = root.blockPosition();
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.lootrun.addedNote", pos.toShortString())
                                .append("\n" + text),
                        false);
        return Services.LootrunPaths.addNote(text);
    }

    private int listLootrunNote(CommandContext<CommandSourceStack> context) {
        List<LootrunNote> notes = Services.LootrunPaths.getCurrentNotes();
        if (notes.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.wynntils.lootrun.listNoteNoNote"));
        } else {
            MutableComponent component = Component.translatable("command.wynntils.lootrun.listNoteHeader");
            for (LootrunNote note : notes) {
                BlockPos pos = PosUtils.newBlockPos(note.position());
                String posString = pos.toShortString();

                component
                        .append("\n")
                        .append(Component.literal("[X]").withStyle((style) -> style.withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("command.wynntils.lootrun.listClickToDelete")))
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/lootrun note delete " + posString.replace(",", "")))
                                .withColor(ChatFormatting.RED)))
                        .append(" " + posString + ": ")
                        .append(note.component());
            }
            context.getSource().sendSuccess(() -> component, false);
        }
        return 1;
    }

    private int deleteLootrunNote(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getSpawnablePos(context, "pos");
        LootrunNote removedNote = Services.LootrunPaths.deleteNoteAt(pos);

        if (removedNote != null) {
            context.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                            "command.wynntils.lootrun.noteRemovedSuccessfully", removedNote.component())
                                    .withStyle(ChatFormatting.GREEN),
                            false);
        } else {
            String posString = pos.toShortString();
            context.getSource()
                    .sendFailure(Component.translatable("command.wynntils.lootrun.noteUnableToFind", posString));
        }
        return Services.LootrunPaths.recompileLootrun(true);
    }

    private int clearLootrun(CommandContext<CommandSourceStack> context) {
        if (Services.LootrunPaths.getState() == LootrunState.DISABLED) {
            context.getSource().sendFailure(Component.translatable("command.wynntils.lootrun.noActiveLootrun"));
            return 0;
        }

        Services.LootrunPaths.clearCurrentLootrun();

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.lootrun.clearSuccessful")
                                .withStyle(ChatFormatting.GREEN),
                        false);
        return 1;
    }

    private int deleteLootrun(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        File file = new File(Services.LootrunPaths.LOOTRUNS, name + ".json");
        if (!file.exists()) {
            context.getSource()
                    .sendFailure(Component.translatable("command.wynntils.lootrun.lootrunDoesntExist", name));
        } else if (file.delete()) {
            context.getSource()
                    .sendSuccess(
                            () -> Component.translatable("command.wynntils.lootrun.lootrunDeleted", name)
                                    .withStyle(ChatFormatting.GREEN),
                            false);
            return 1;
        } else {
            context.getSource()
                    .sendFailure(Component.translatable("command.wynntils.lootrun.lootrunCouldNotBeDeleted", name));
        }
        return 0;
    }

    private int renameLootrun(CommandContext<CommandSourceStack> context) {
        String oldName = StringArgumentType.getString(context, "old");
        String newName = StringArgumentType.getString(context, "new");
        File oldFile = new File(Services.LootrunPaths.LOOTRUNS, oldName + ".json");
        File newFile = new File(Services.LootrunPaths.LOOTRUNS, newName + ".json");
        if (!oldFile.exists()) {
            context.getSource()
                    .sendFailure(Component.translatable("command.wynntils.lootrun.lootrunDoesntExist", oldName));
        } else if (oldFile.renameTo(newFile)) {
            context.getSource()
                    .sendSuccess(
                            () -> Component.translatable("command.wynntils.lootrun.lootrunRenamed", oldName, newName)
                                    .withStyle(ChatFormatting.GREEN),
                            false);
            return 1;
        } else {
            context.getSource()
                    .sendFailure(Component.translatable(
                            "command.wynntils.lootrun.lootrunCouldNotBeRenamed", oldName, newName));
        }
        return 0;
    }

    private int addChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getSpawnablePos(context, "pos");

        boolean successful = Services.LootrunPaths.addChest(pos);

        if (successful) {
            context.getSource()
                    .sendSuccess(
                            () -> Component.translatable("command.wynntils.lootrun.chestAdded", pos.toShortString())
                                    .withStyle(ChatFormatting.GREEN),
                            false);
        } else {
            context.getSource()
                    .sendFailure(
                            Component.translatable("command.wynntils.lootrun.chestAlreadyAdded", pos.toShortString()));
        }

        return Services.LootrunPaths.recompileLootrun(true);
    }

    private int removeChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getSpawnablePos(context, "pos");

        boolean successful = Services.LootrunPaths.removeChest(pos);

        if (successful) {
            context.getSource()
                    .sendSuccess(
                            () -> Component.translatable("command.wynntils.lootrun.chestRemoved", pos.toShortString())
                                    .withStyle(ChatFormatting.GREEN),
                            false);
        } else {
            context.getSource()
                    .sendFailure(
                            Component.translatable("command.wynntils.lootrun.chestDoesNotExist", pos.toShortString()));
        }

        return Services.LootrunPaths.recompileLootrun(true);
    }

    private int undoLootrun(CommandContext<CommandSourceStack> context) {
        if (Services.LootrunPaths.getState() != LootrunState.RECORDING) {
            context.getSource().sendFailure(Component.translatable("command.wynntils.lootrun.notRecording"));
        } else {
            LootrunUndoResult lootrunUndoResult = Services.LootrunPaths.tryUndo();
            switch (lootrunUndoResult) {
                case SUCCESSFUL -> {
                    context.getSource()
                            .sendSuccess(
                                    () -> Component.translatable("command.wynntils.lootrun.undoSuccessful"), false);
                    return 1;
                }
                case ERROR_STAND_NEAR_POINT -> {
                    context.getSource().sendFailure(Component.translatable("command.wynntils.lootrun.undoStandNear"));
                    return 0;
                }
                case ERROR_NOT_FAR_ENOUGH -> {
                    context.getSource()
                            .sendFailure(Component.translatable("command.wynntils.lootrun.undoNotFarEnough"));
                    return 0;
                }
            }
        }
        return 0;
    }

    private int folderLootrun(CommandContext<CommandSourceStack> context) {
        Util.getPlatform().openFile(Services.LootrunPaths.LOOTRUNS);
        return 1;
    }

    private int syntaxError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Missing argument").withStyle(ChatFormatting.RED));
        return 0;
    }

    private int screenLootrun(CommandContext<CommandSourceStack> context) {
        // Delay is needed to prevent chat screen overwriting the lootrun screen
        Managers.TickScheduler.scheduleLater(
                () -> WynntilsMenuScreenBase.openBook(WynntilsLootrunPathsScreen.create()), 2);
        return 1;
    }
}
