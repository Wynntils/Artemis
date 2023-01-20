/*
 * Copyright © Wynntils 2018-2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net.translation;

import com.google.gson.reflect.TypeToken;
import com.wynntils.core.WynntilsMod;
import com.wynntils.utils.TaskUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;

public abstract class CachingTranslationService implements TranslationService {
    private static final File TRANSLATION_CACHE_ROOT = WynntilsMod.getModStorageDir("translationcache");

    // Map language code (String) to a translation map (String -> String)
    private static Map<String, ConcurrentHashMap<String, List<String>>> translationCaches = new HashMap<>();
    private static int counter;

    protected abstract void translateNew(
            List<String> message, String toLanguage, Consumer<List<String>> handleTranslation);

    protected void saveTranslation(String toLanguage, List<String> message, List<String> translatedMessage) {
        Map<String, List<String>> translationCache = translationCaches.get(toLanguage);
        translationCache.put(createKey(message), translatedMessage);
        if (++counter % 16 == 0) {
            // Persist translation cache in background
            TaskUtils.runAsync(CachingTranslationService::saveTranslationCache);
        }
    }

    @Override
    public void translate(List<String> message, String toLanguage, Consumer<List<String>> handleTranslation) {
        if (message.isEmpty()) {
            TaskUtils.runAsync(() -> handleTranslation.accept(List.of()));
            return;
        }

        Map<String, List<String>> translationCache =
                translationCaches.computeIfAbsent(toLanguage, k -> new ConcurrentHashMap<>());
        List<String> cachedTranslation = translationCache.get(createKey(message));
        if (cachedTranslation != null) {
            TaskUtils.runAsync(() -> handleTranslation.accept(cachedTranslation));
            return;
        }

        translateNew(message, toLanguage, handleTranslation);
    }

    public static synchronized void saveTranslationCache() {
        try {
            if (translationCaches == null) return;

            File f = new File(TRANSLATION_CACHE_ROOT, "translations.json");
            String json = WynntilsMod.GSON.toJson(translationCaches);
            FileUtils.writeStringToFile(f, json, "UTF-8");
        } catch (IOException e) {
            WynntilsMod.error("Error when trying to save translation cache.", e);
        }
    }

    public static synchronized void loadTranslationCache() {
        File f = new File(TRANSLATION_CACHE_ROOT, "translations.json");

        if (!f.exists()) {
            translationCaches = new HashMap<>();
            return;
        }

        try {
            String json = FileUtils.readFileToString(f, "UTF-8");

            Type type = new TypeToken<HashMap<String, ConcurrentHashMap<String, String>>>() {}.getType();
            translationCaches = WynntilsMod.GSON.fromJson(json, type);
        } catch (IOException e) {
            WynntilsMod.error("Error when trying to load translation cache.", e);
        } finally {
            if (translationCaches == null) {
                translationCaches = new HashMap<>();
            }
        }
    }

    private String createKey(List<String> message) {
        return String.join("", message);
    }
}
