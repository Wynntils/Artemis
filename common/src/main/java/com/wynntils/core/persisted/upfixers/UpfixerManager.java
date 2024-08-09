/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.upfixers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.persisted.PersistedValue;
import com.wynntils.core.persisted.upfixers.config.CombatXpGainToXpGainUpfixer;
import com.wynntils.core.persisted.upfixers.config.CustomBankQuickJumpsBankNameUpfixer;
import com.wynntils.core.persisted.upfixers.config.CustomBankQuickJumpsUpfixer;
import com.wynntils.core.persisted.upfixers.config.CustomCommandKeybindSlashStartUpfixer;
import com.wynntils.core.persisted.upfixers.config.CustomPoiIconEnumBugUpfixer;
import com.wynntils.core.persisted.upfixers.config.CustomPoiVisbilityUpfixer;
import com.wynntils.core.persisted.upfixers.config.EnumNamingUpfixer;
import com.wynntils.core.persisted.upfixers.config.GameBarOverlayMoveUpfixer;
import com.wynntils.core.persisted.upfixers.config.MapToMainMapRenamedConfigsUpfixer;
import com.wynntils.core.persisted.upfixers.config.NpcDialoguesOverlayConfigsMovedUpfixer;
import com.wynntils.core.persisted.upfixers.config.NpcDialoguesRenamedUpfixer;
import com.wynntils.core.persisted.upfixers.config.OverlayConfigsIntegrationUpfixer;
import com.wynntils.core.persisted.upfixers.config.OverlayRestructuringUpfixer;
import com.wynntils.core.persisted.upfixers.config.QuestBookToContentRenamedConfigsUpfixer;
import com.wynntils.core.persisted.upfixers.config.TowerAuraVignetteAndOverlayMovedToCommonFeature;
import com.wynntils.core.persisted.upfixers.config.TowerAuraVignetteNameUpfixer;
import com.wynntils.core.persisted.upfixers.storage.BankToAccountBankUpfixer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class UpfixerManager extends Manager {
    public static final String UPFIXER_JSON_MEMBER_NAME = "wynntils.upfixers";

    private final List<Upfixer> configUpfixers = new ArrayList<>();
    private final List<Upfixer> storageUpfixers = new ArrayList<>();

    public UpfixerManager() {
        super(List.of());

        // Register config upfixers here, in order of run priority
        registerConfigUpfixer(new CustomPoiVisbilityUpfixer());
        registerConfigUpfixer(new CustomCommandKeybindSlashStartUpfixer());
        registerConfigUpfixer(new GameBarOverlayMoveUpfixer());
        registerConfigUpfixer(new EnumNamingUpfixer());
        registerConfigUpfixer(new CustomPoiIconEnumBugUpfixer());
        registerConfigUpfixer(new QuestBookToContentRenamedConfigsUpfixer());
        registerConfigUpfixer(new MapToMainMapRenamedConfigsUpfixer());
        registerConfigUpfixer(new OverlayRestructuringUpfixer());
        registerConfigUpfixer(new OverlayConfigsIntegrationUpfixer());
        registerConfigUpfixer(new CustomBankQuickJumpsUpfixer());
        registerConfigUpfixer(new CustomBankQuickJumpsBankNameUpfixer());
        registerConfigUpfixer(new NpcDialoguesRenamedUpfixer());
        registerConfigUpfixer(new NpcDialoguesOverlayConfigsMovedUpfixer());
        registerConfigUpfixer(new TowerAuraVignetteNameUpfixer());
        registerConfigUpfixer(new TowerAuraVignetteAndOverlayMovedToCommonFeature());
        registerConfigUpfixer(new CombatXpGainToXpGainUpfixer());

        // Register storage upfixers here, in order of run priority
        registerStorageUpfixer(new BankToAccountBankUpfixer());
    }

    private void registerConfigUpfixer(Upfixer upfixer) {
        configUpfixers.add(upfixer);
    }

    private void registerStorageUpfixer(Upfixer upfixer) {
        storageUpfixers.add(upfixer);
    }

    /**
     * Runs all registered upfixers on the given persisted object.
     *
     * @param persistedObject  The persisted object to run upfixers on.
     * @param persistedValues All registered persisted values.
     */
    public boolean runUpfixers(
            JsonObject persistedObject, Set<PersistedValue<?>> persistedValues, UpfixerType upfixerType) {
        List<Upfixer> missingUpfixers = getMissingUpfixers(persistedObject, upfixerType);

        boolean anyChange = false;

        for (Upfixer upfixer : missingUpfixers) {
            try {
                if (upfixer.apply(persistedObject, persistedValues)) {
                    anyChange = true;
                    addUpfixerToPersistedFile(persistedObject, upfixer);
                    WynntilsMod.info("Applied upfixer \"" + upfixer.getUpfixerName() + "\" to "
                            + upfixerType.name().toLowerCase(Locale.ROOT) + " file.");
                }
            } catch (Throwable t) {
                WynntilsMod.warn(
                        "Failed to apply upfixer \"" + upfixer.getUpfixerName() + "\" to "
                                + upfixerType.name().toLowerCase(Locale.ROOT) + " file!",
                        t);
            }
        }

        return anyChange;
    }

    private void addUpfixerToPersistedFile(JsonObject configObject, Upfixer upfixer) {
        JsonElement upfixers = configObject.get(UPFIXER_JSON_MEMBER_NAME);

        if (upfixers == null || upfixers.isJsonNull()) {
            upfixers = new JsonArray();
            configObject.add(UPFIXER_JSON_MEMBER_NAME, upfixers);
        } else if (!upfixers.isJsonArray()) {
            WynntilsMod.warn("Invalid upfixer JSON member in config file! Expected array, got "
                    + upfixers.getClass().getSimpleName());

            // Try to proceed with a new array
            upfixers = new JsonArray();
            configObject.add(UPFIXER_JSON_MEMBER_NAME, upfixers);
        }

        upfixers.getAsJsonArray().add(upfixer.getUpfixerName());
    }

    private List<Upfixer> getMissingUpfixers(JsonObject persistedObject, UpfixerType type) {
        final List<Upfixer> typeUpfixers = type == UpfixerType.CONFIG ? configUpfixers : storageUpfixers;

        if (!persistedObject.has(UPFIXER_JSON_MEMBER_NAME)) return typeUpfixers;

        JsonElement upfixersJson = persistedObject.get(UPFIXER_JSON_MEMBER_NAME);
        if (upfixersJson == null || upfixersJson.isJsonNull()) return typeUpfixers;

        if (!upfixersJson.isJsonArray()) {
            WynntilsMod.warn("Invalid upfixer JSON member in " + type.name().toLowerCase(Locale.ROOT)
                    + " file! Expected array, got " + upfixersJson.getClass().getSimpleName());
            return typeUpfixers;
        }

        List<String> appliedUpfixers = new ArrayList<>();
        for (JsonElement upfixer : upfixersJson.getAsJsonArray()) {
            appliedUpfixers.add(upfixer.getAsString());
        }

        return typeUpfixers.stream()
                .filter(upfixer -> !appliedUpfixers.contains(upfixer.getUpfixerName()))
                .toList();
    }
}
