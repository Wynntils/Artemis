/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.lootrunpaths;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Service;
import com.wynntils.core.components.Services;
import com.wynntils.features.LootrunFeature;
import com.wynntils.mc.event.PlayerInteractEvent;
import com.wynntils.mc.event.RenderLevelEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.containers.containers.reward.LootChestContainer;
import com.wynntils.services.lootrunpaths.event.LootrunPathCacheRefreshEvent;
import com.wynntils.services.lootrunpaths.type.LootrunNote;
import com.wynntils.services.lootrunpaths.type.LootrunPath;
import com.wynntils.services.lootrunpaths.type.LootrunSaveResult;
import com.wynntils.services.lootrunpaths.type.LootrunState;
import com.wynntils.services.lootrunpaths.type.LootrunUndoResult;
import com.wynntils.utils.FileUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.PosUtils;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;

public final class LootrunPathsService extends Service {
    public static final File LOOTRUNS = WynntilsMod.getModStorageDir("lootruns");

    private List<LootrunPathInstance> lootrunPathInstanceCache = new ArrayList<>();

    private UncompiledLootrunPath uncompiled = null;

    private LootrunState state = LootrunState.DISABLED;

    private LootrunPathInstance lootrun = null;
    private LootrunPathInstance recordingCompiled = null;
    private UncompiledLootrunPath recording = null;

    private RecordingInformation recordingInformation = null;

    public LootrunPathsService() {
        super(List.of());

        FileUtils.mkdir(Services.LootrunPaths.LOOTRUNS);
    }

    public LootrunState getState() {
        return state;
    }

    public int addNote(Component text) {
        UncompiledLootrunPath current = getActiveLootrun();
        if (current == null) return 0;

        Entity root = McUtils.player().getRootVehicle();

        current.notes().add(new LootrunNote(root.position(), text));
        return recompileLootrun(true);
    }

    public LootrunPathInstance getCurrentLootrun() {
        return lootrun;
    }

    public int recompileLootrun(boolean saveToFile) {
        if (recording != null) {
            recordingInformation.setDirty(true);
        } else if (uncompiled != null) {
            lootrun = LootrunCompiler.compile(uncompiled, false);
            if (saveToFile && uncompiled.file() != null) {
                LootrunSaveResult lootrunSaveResult =
                        saveCurrentLootrun(uncompiled.file().getName().replace(".json", ""));

                if (lootrunSaveResult == null) {
                    return 0;
                }

                switch (lootrunSaveResult) {
                    case SAVED -> {
                        return 1;
                    }
                    case ERROR_SAVING, ERROR_ALREADY_EXISTS -> {
                        return 0;
                    }
                }
            }
        }
        return 1;
    }

    public void clearCurrentLootrun() {
        state = LootrunState.DISABLED;
        lootrun = null;
        uncompiled = null;
        recording = null;
        recordingCompiled = null;
        recordingInformation = null;
    }

    public void stopRecording() {
        // At this point, we already have LootrunFeature registered to the event bus
        state = LootrunState.LOADED;
        lootrun = LootrunCompiler.compile(recording, false);
        uncompiled = recording;
        recording = null;
        recordingCompiled = null;
        recordingInformation = null;
    }

    public void startRecording() {
        state = LootrunState.RECORDING;
        recording =
                new UncompiledLootrunPath(new LootrunPath(new ArrayList<>()), new HashSet<>(), new ArrayList<>(), null);
        recordingInformation = new RecordingInformation();
    }

    public List<LootrunPathInstance> getLootruns() {
        return lootrunPathInstanceCache;
    }

    public void refreshLootrunCache() {
        List<LootrunPathInstance> lootruns = new ArrayList<>();

        File[] files = LOOTRUNS.listFiles();
        for (File file : files != null ? files : new File[0]) {
            if (file.getName().endsWith(".json")) {
                try {
                    FileReader reader = new FileReader(file, StandardCharsets.UTF_8);
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    UncompiledLootrunPath uncompiled = LootrunPathFileParser.readJson(file, json);
                    lootruns.add(LootrunCompiler.compile(uncompiled, false));
                } catch (Exception e) {
                    WynntilsMod.warn("Could not parse lootrun file.", e);
                }
            }
        }

        lootrunPathInstanceCache = lootruns;
        WynntilsMod.postEvent(new LootrunPathCacheRefreshEvent());
    }

    private boolean loadFile(String fileName) {
        String lootrunFileName = fileName + ".json";
        File lootrunFile = new File(LOOTRUNS, lootrunFileName);
        if (lootrunFile.exists()) {
            try {
                FileReader file = new FileReader(lootrunFile, StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseReader(file).getAsJsonObject();
                uncompiled = LootrunPathFileParser.readJson(lootrunFile, json);
                lootrun = LootrunCompiler.compile(uncompiled, false);
                state = LootrunState.LOADED;
                file.close();
                return true;
            } catch (Exception e) {
                WynntilsMod.error("Error when trying to load lootrun file.", e);
                return false;
            }
        }
        return false;
    }

    public void tryLoadLootrun(String fileName) {
        if (loadFile(fileName)) {
            Position startingPoint = Services.LootrunPaths.getStartingPoint();

            BlockPos start = PosUtils.newBlockPos(startingPoint);
            McUtils.sendMessageToClient(Component.translatable(
                            "service.wynntils.lootrunPaths.lootrunStart", start.getX(), start.getY(), start.getZ())
                    .withStyle(ChatFormatting.GREEN));
        } else {
            McUtils.sendErrorToClient(I18n.get("service.wynntils.lootrunPaths.lootrunCouldNotBeLoaded", fileName));
        }
    }

    public LootrunUndoResult tryUndo() {
        Vec3 position = McUtils.player().position();
        LootrunPath points = recording.path();
        LootrunPath removed = new LootrunPath(new ArrayList<>());
        boolean left = false;
        for (int i = points.points().size() - 1; i >= 0; i--) {
            if (i == 0) {
                if (left) {
                    return LootrunUndoResult.ERROR_STAND_NEAR_POINT;
                } else {
                    return LootrunUndoResult.ERROR_NOT_FAR_ENOUGH;
                }
            }

            if (points.points().get(i).distanceToSqr(position) < 4) {
                if (left) {
                    break;
                }
            } else {
                left = true;
            }

            removed.points().add(points.points().get(i));
        }

        points.points().removeAll(removed.points());
        recordingInformation.setDirty(true);
        return LootrunUndoResult.SUCCESSFUL;
    }

    public boolean addChest(BlockPos pos) {
        UncompiledLootrunPath current = getActiveLootrun();
        if (current == null) return false;

        return current.chests().add(pos);
    }

    public boolean removeChest(BlockPos pos) {
        UncompiledLootrunPath current = getActiveLootrun();
        if (current == null) return false;

        return current.chests().remove(pos);
    }

    public LootrunNote deleteNoteAt(BlockPos pos) {
        UncompiledLootrunPath current = getActiveLootrun();
        if (current == null) return null;

        List<LootrunNote> notes = current.notes();
        for (int i = 0; i < notes.size(); i++) {
            LootrunNote note = notes.get(i);
            if (pos.equals(PosUtils.newBlockPos(note.position()))) {
                return notes.remove(i);
            }
        }
        return null;
    }

    public List<LootrunNote> getCurrentNotes() {
        UncompiledLootrunPath activeLootrun = getActiveLootrun();
        if (activeLootrun == null) return List.of();

        return activeLootrun.notes();
    }

    public Position getStartingPoint() {
        UncompiledLootrunPath activeLootrun = getActiveLootrun();
        if (activeLootrun == null) return null;

        if (activeLootrun.path() == null || activeLootrun.path().points().isEmpty()) return null;

        return activeLootrun.path().points().getFirst();
    }

    public LootrunSaveResult saveCurrentLootrun(String name) {
        UncompiledLootrunPath activeLootrun = getActiveLootrun();
        if (activeLootrun == null) return null;

        File file = new File(LOOTRUNS, name + ".json");
        uncompiled =
                new UncompiledLootrunPath(activeLootrun.path(), activeLootrun.chests(), activeLootrun.notes(), file);

        return LootrunPathFileParser.writeJson(activeLootrun, file);
    }

    @SubscribeEvent
    public void onRenderLastLevel(RenderLevelEvent.Post event) {
        PoseStack poseStack = new PoseStack();

        LootrunRenderer.renderLootrun(
                poseStack,
                lootrun,
                Managers.Feature.getFeatureInstance(LootrunFeature.class)
                        .activePathColor
                        .get()
                        .asInt());
        LootrunRenderer.renderLootrun(
                poseStack,
                recordingCompiled,
                Managers.Feature.getFeatureInstance(LootrunFeature.class)
                        .recordingPathColor
                        .get()
                        .asInt());
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (state != LootrunState.RECORDING) return;

        BlockState block = event.getWorld().getBlockState(event.getPos());
        if (!block.is(Blocks.CHEST)) return;

        BlockPos pos = event.getPos();
        recordingInformation.setLastChest(pos);
    }

    @SubscribeEvent
    public void onOpen(ScreenOpenedEvent.Post event) {
        if (state != LootrunState.RECORDING) return;
        if (recordingInformation.getLastChest() == null) return;
        if (!(Models.Container.getCurrentContainer() instanceof LootChestContainer)) return;

        recording.chests().add(recordingInformation.getLastChest());
        recordingInformation.setDirty(true);
        recordingInformation.setLastChest(null);
    }

    @SubscribeEvent
    public void recordMovement(TickEvent event) {
        if (state != LootrunState.RECORDING) return;

        LocalPlayer player = McUtils.player();
        if (player == null) return;

        Entity root = player.getRootVehicle();
        Vec3 pos = root.position();
        if (recordingInformation.getLastLocation() == null
                || pos.distanceToSqr(recordingInformation.getLastLocation()) >= 4d) {
            recording.path().points().add(pos);
            recordingInformation.setLastLocation(pos);
            recordingInformation.setDirty(true);
        }

        if (recordingInformation.isDirty()) {
            recordingCompiled = LootrunCompiler.compile(recording, true);
            recordingInformation.setDirty(false);
        }
    }

    private UncompiledLootrunPath getActiveLootrun() {
        UncompiledLootrunPath instance = null;
        if (recording != null) {
            instance = recording;
        } else if (uncompiled != null) {
            instance = uncompiled;
        }

        return instance;
    }
}
