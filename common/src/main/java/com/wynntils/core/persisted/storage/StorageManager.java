/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.mod.event.WynncraftConnectionEvent;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.PersistedValue;
import com.wynntils.core.persisted.upfixers.UpfixerType;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class StorageManager extends Manager {
    private static final long SAVE_INTERVAL = 10_000;

    private static final File STORAGE_DIR = WynntilsMod.getModStorageDir("storage");
    private static final String FILE_SUFFIX = ".data.json";
    private final File userStorageFile;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Map<String, Storage<?>> storages = new TreeMap<>();
    private final Map<Storage<?>, Type> storageTypes = new HashMap<>();
    private final Map<Storage<?>, Storageable> storageOwner = new HashMap<>();

    private JsonObject storageObject;

    private long lastPersisted;
    private boolean scheduledPersist;

    private boolean storageInitialized = false;

    public StorageManager() {
        super(List.of());

        userStorageFile = new File(
                STORAGE_DIR, UndashedUuid.toString(McUtils.mc().getUser().getProfileId()) + FILE_SUFFIX);
    }

    public void initComponents() {
        readFromJson();
    }

    public void initFeatures() {
        // Register all storageables
        Managers.Feature.getFeatures().forEach(this::registerStorageable);

        runUpfixers();

        readFromJson();

        storageInitialized = true;

        // We might have missed a persist call in between feature init and storage manager init
        persist();
    }

    public void registerStorageable(Storageable owner) {
        Managers.Persisted.verifyAnnotations(owner);

        Managers.Persisted.getPersisted(owner, Storage.class).forEach(p -> processStorage(owner, p.a(), p.b()));
    }

    public File getUserStorageFile() {
        return userStorageFile;
    }

    private void processStorage(Storageable owner, Field field, Persisted annotation) {
        try {
            String baseName = owner.getStorageJsonName();
            Storage<?> storage = (Storage<?>) FieldUtils.readField(field, owner, true);
            String jsonName = baseName + "." + field.getName();
            storages.put(jsonName, storage);

            Type valueType = Managers.Json.getJsonValueType(field);
            storageTypes.put(storage, valueType);
            storageOwner.put(storage, owner);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public void onWynncraftDisconnect(WynncraftConnectionEvent.Disconnected event) {
        // Always save when disconnecting
        writeToJson();
    }

    void persist() {
        // We cannot persist before the storage is initialized, or we will overwrite our storage
        if (!storageInitialized || scheduledPersist) return;

        long now = System.currentTimeMillis();
        long delay = Math.max((lastPersisted + SAVE_INTERVAL) - now, 0);

        executor.schedule(
                () -> {
                    scheduledPersist = false;
                    lastPersisted = System.currentTimeMillis();
                    writeToJson();
                },
                delay,
                TimeUnit.MILLISECONDS);
        scheduledPersist = true;
    }

    private void runUpfixers() {
        storageObject = Managers.Json.loadPreciousJson(userStorageFile);

        // Now, we have to apply upfixers, before any storage loading happens
        // FIXME: Solve generics type issue
        Set<PersistedValue<?>> workaround = new HashSet<>(storages.values());
        if (Managers.Upfixer.runUpfixers(storageObject, workaround, UpfixerType.STORAGE)) {
            Managers.Json.savePreciousJson(userStorageFile, storageObject);

            // Re-read the storage file after upfixing
            readFromJson();
        }
    }

    private void readFromJson() {
        storageObject = Managers.Json.loadPreciousJson(userStorageFile);
        storages.forEach((jsonName, storage) -> {
            if (!storageObject.has(jsonName)) return;

            // read value and update option
            JsonElement jsonElem = storageObject.get(jsonName);
            Object value = Managers.Json.GSON.fromJson(jsonElem, storageTypes.get(storage));
            Managers.Persisted.setRaw(storage, value);

            Storageable owner = storageOwner.get(storage);
            owner.onStorageLoad();
        });
    }

    private void writeToJson() {
        JsonObject storageJson = new JsonObject();

        // Save upfixers
        String upfixerJsonMemberName = Managers.Upfixer.UPFIXER_JSON_MEMBER_NAME;
        storageJson.add(upfixerJsonMemberName, storageObject.get(upfixerJsonMemberName));

        storages.forEach((jsonName, storage) -> {
            try {
                JsonElement jsonElem = Managers.Json.GSON.toJsonTree(storage.get(), storageTypes.get(storage));
                storageJson.add(jsonName, jsonElem);
            } catch (Throwable t) {
                WynntilsMod.error("Failed to save storage " + jsonName, t);
            }
        });

        Managers.Json.savePreciousJson(userStorageFile, storageJson);
    }
}
