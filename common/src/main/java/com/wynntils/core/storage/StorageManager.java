/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.json.JsonManager;
import com.wynntils.core.mod.event.WynncraftConnectionEvent;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class StorageManager extends Manager {
    private static final long SAVE_INTERVAL = 10_000;

    private static final File STORAGE_DIR = WynntilsMod.getModStorageDir("storage");
    private static final String FILE_SUFFIX = ".data.json";
    private final File userStorageFile;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Map<String, Storage<?>> storages = new TreeMap<>();
    private final Map<Storage<?>, Type> storageTypes = new HashMap<>();

    private long lastPersisted;
    private boolean scheduledPersist;

    public StorageManager(JsonManager jsonManager) {
        super(List.of(jsonManager));
        userStorageFile = new File(STORAGE_DIR, McUtils.mc().getUser().getUuid() + FILE_SUFFIX);
    }

    public void registerStorageable(Storageable storageable) {
        String baseName = storageable.getStorageJsonName();

        List<Field> fields = FieldUtils.getAllFieldsList(storageable.getClass());
        List<Field> storageFields =
                fields.stream().filter(f -> f.getType().equals(Storage.class)).toList();

        for (Field storageField : storageFields) {
            try {
                Storage<?> storage = (Storage<?>) FieldUtils.readField(storageField, storageable, true);
                String jsonName = baseName + "." + storageField.getName();
                storages.put(jsonName, storage);

                Type typeOverride = Managers.Json.findFieldTypeOverride(storageable, storageField);
                storageTypes.put(
                        storage,
                        typeOverride != null ? typeOverride : storage.get().getClass());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void restorePersisted() {
        readFromJson();
    }

    @SubscribeEvent
    public void onWynncraftDisconnect(WynncraftConnectionEvent.Disconnected event) {
        // Always save when disconnecting
        writeToJson();
    }

    void persist() {
        if (scheduledPersist) return;

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

    private void readFromJson() {
        JsonObject storageJson = Managers.Json.loadPreciousJson(userStorageFile);
        storages.forEach((jsonName, storage) -> {
            if (!storageJson.has(jsonName)) return;

            // read value and update option
            JsonElement jsonElem = storageJson.get(jsonName);
            Object value = Managers.Json.GSON.fromJson(jsonElem, storageTypes.get(storage));
            storage.set(value);
        });
    }

    private void writeToJson() {
        JsonObject storageJson = new JsonObject();

        storages.forEach((jsonName, storage) -> {
            JsonElement jsonElem = Managers.Json.GSON.toJsonTree(storage.get(), storageTypes.get(storage));
            storageJson.add(jsonName, jsonElem);
        });

        Managers.Json.savePreciousJson(userStorageFile, storageJson);
    }
}
