/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.config;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.util.UndashedUuid;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Configurable;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.DynamicOverlay;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.PersistedOwner;
import com.wynntils.core.persisted.PersistedValue;
import com.wynntils.core.persisted.upfixers.UpfixerType;
import com.wynntils.utils.JsonUtils;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class ConfigManager extends Manager {
    private static final File CONFIG_DIR = WynntilsMod.getModStorageDir("config");
    private static final String FILE_SUFFIX = ".conf.json";
    private static final File DEFAULT_CONFIG = new File(CONFIG_DIR, "default" + FILE_SUFFIX);
    private static final String OVERLAY_GROUPS_JSON_KEY = "overlayGroups";
    private static final Set<Config<?>> CONFIGS = new TreeSet<>();

    private File userConfigFile;
    private JsonObject configObject;

    public ConfigManager() {
        super(List.of());

        userConfigFile = new File(
                CONFIG_DIR, UndashedUuid.toString(McUtils.mc().getUser().getProfileId()) + FILE_SUFFIX);
    }

    public void init() {
        // First, we load the config file
        configObject = Managers.Json.loadPreciousJson(userConfigFile);

        // Register all features and overlays
        Managers.Feature.getFeatures().forEach(this::registerFeature);

        // Now, we have to apply upfixers, before any config loading happens
        // FIXME: Solve generics type issue
        Set<PersistedValue<?>> workaround = new HashSet<>(CONFIGS);
        if (Managers.Upfixer.runUpfixers(configObject, workaround, UpfixerType.CONFIG)) {
            Managers.Json.savePreciousJson(userConfigFile, configObject);
        }

        // Finish off the config init process

        // Load configs for all features
        Managers.Config.reloadConfiguration();

        // Save config file after loading all configurables' options
        Managers.Config.saveConfig();

        // Create default config file containing all configurables' options
        Managers.Config.saveDefaultConfig();
    }

    private void registerFeature(Feature feature) {
        registerConfigOptions(feature);

        for (Overlay overlay : Managers.Overlay.getFeatureOverlays(feature).stream()
                .filter(overlay -> Managers.Overlay.getFeatureOverlayGroups(feature).stream()
                        .noneMatch(overlayGroupHolder ->
                                overlayGroupHolder.getOverlays().contains(overlay)))
                .toList()) {
            registerConfigOptions(overlay);
        }
    }

    private void registerConfigOptions(Configurable configurable) {
        // Hook this in here for the time being
        Managers.Persisted.registerOwner(configurable);

        List<Config<?>> configs = getConfigOptions(configurable);
        configurable.addConfigOptions(configs);
        CONFIGS.addAll(configs);
    }

    public void reloadConfiguration() {
        configObject = Managers.Json.loadPreciousJson(userConfigFile);
        loadConfigOptions(true, true);
    }

    // Info: The purpose of initOverlayGroups is to use the config system in a way that is really "hacky".
    //       Overlay group initialization needs:
    //          1, Overlay instances to be loaded (at init, the default number of instances, then the number defined in
    //             configObject)
    //          2, We need to handle dynamic overlays' configs as regular configs, so that they can be loaded from the
    //             config file
    //       The problem is that the config system "save" is used to remove unused configs, and that "load" is used to
    //       init dynamic overlay instances.
    //
    //       This really becomes a problem when modifying overlay group sizes at runtime.
    //       We want to do 4 things: Save new overlay group size, init overlay instances, load configs, and remove
    //       unused configs.
    //       This means we need to save - load - save, which we should not do. initOverlayGroups is the solution to
    //       this, for now.
    public void loadConfigOptions(boolean resetIfNotFound, boolean initOverlayGroups) {
        // We have to set up the overlay groups first, so that the overlays' configs can be loaded
        JsonObject overlayGroups = JsonUtils.getNullableJsonObject(configObject, OVERLAY_GROUPS_JSON_KEY);

        for (OverlayGroupHolder holder : Managers.Overlay.getOverlayGroups()) {
            if (initOverlayGroups) {
                if (overlayGroups.has(holder.getConfigKey())) {
                    JsonArray ids = JsonUtils.getNullableJsonArray(overlayGroups, holder.getConfigKey());

                    List<Integer> idList =
                            ids.asList().stream().map(JsonElement::getAsInt).toList();

                    Managers.Overlay.createOverlayGroupWithIds(holder, idList);
                } else {
                    Managers.Overlay.createOverlayGroupWithDefaults(holder);
                }
            }

            // Hook this in here for the time being
            holder.getOverlays().forEach(Managers.Persisted::registerOwner);

            holder.getOverlays().forEach(overlay -> overlay.addConfigOptions(this.getConfigOptions(overlay)));
        }

        for (Config<?> config : getConfigList()) {
            // option hasn't been saved to config
            if (!configObject.has(config.getJsonName())) {
                if (resetIfNotFound) {
                    config.reset();
                }
                continue;
            }

            // read value and update option
            JsonElement configJson = configObject.get(config.getJsonName());
            Object value = Managers.Json.GSON.fromJson(configJson, config.getType());
            config.restoreValue(value);
        }

        // Newly created group overlays need to be enabled
        for (OverlayGroupHolder holder : Managers.Overlay.getOverlayGroups()) {
            Managers.Overlay.enableOverlays(holder.getParent());
        }
    }

    private static List<Config<?>> getConfigList() {
        // This breaks the concept of "manager holds all config holders at all times". Instead we get the group
        // overlays' configs from the overlay instance itself, to save us some trouble.

        return Stream.concat(
                        CONFIGS.stream(),
                        Managers.Overlay.getOverlayGroups().stream()
                                .map(OverlayGroupHolder::getOverlays)
                                .flatMap(List::stream)
                                .map(Overlay::getConfigOptions)
                                .flatMap(List::stream))
                .toList();
    }

    public void saveConfig() {
        // Requesting to save before we have read the old config? Just skip it
        if (configObject == null) return;

        // create json object, with entry for each option of each container
        JsonObject configJson = new JsonObject();
        for (Config<?> config : getConfigList()) {
            if (!config.valueChanged()) continue; // only save options that have been set by the user
            Object value = config.get();

            JsonElement configElement = Managers.Json.GSON.toJsonTree(value);
            configJson.add(config.getJsonName(), configElement);
        }

        // Also save upfixer data
        String upfixerJsonMemberName = Managers.Upfixer.UPFIXER_JSON_MEMBER_NAME;
        configJson.add(upfixerJsonMemberName, configObject.get(upfixerJsonMemberName));

        // Save overlay groups
        JsonObject overlayGroups = new JsonObject();
        for (OverlayGroupHolder holder : Managers.Overlay.getOverlayGroups()) {
            JsonArray ids = new JsonArray();

            holder.getOverlays().stream()
                    .map(overlay -> ((DynamicOverlay) overlay).getId())
                    .forEach(ids::add);

            overlayGroups.add(holder.getConfigKey(), ids);
        }

        configJson.add(OVERLAY_GROUPS_JSON_KEY, overlayGroups);

        Managers.Json.savePreciousJson(userConfigFile, configJson);
    }

    public File getUserConfigFile() {
        return userConfigFile;
    }

    private void saveDefaultConfig() {
        // create json object, with entry for each option of each container
        JsonObject configJson = new JsonObject();
        for (Config<?> config : getConfigList()) {
            Object value = config.getDefaultValue();

            JsonElement configElement = Managers.Json.GSON.toJsonTree(value);
            configJson.add(config.getJsonName(), configElement);
        }

        WynntilsMod.info("Creating default config file with " + configJson.size() + " config values.");
        Managers.Json.savePreciousJson(DEFAULT_CONFIG, configJson);
    }

    private List<Config<?>> getConfigOptions(PersistedOwner owner) {
        return new ArrayList<>(Managers.Persisted.getPersisted(owner, Config.class).stream()
                .map(p -> processConfig(owner, p.a(), p.b()))
                .toList());
    }

    private static Config<?> processConfig(PersistedOwner owner, Field configField, Persisted configInfo) {
        Config<?> configObj;
        try {
            configObj = (Config<?>) FieldUtils.readField(configField, owner, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot read Config field: " + configField, e);
        }

        if (WynntilsMod.isDevelopmentEnvironment()) {
            if (configObj.isVisible()) {
                if (configObj.getDisplayName().startsWith("feature.wynntils.")) {
                    WynntilsMod.error("Config displayName i18n is missing for " + configObj.getDisplayName());
                    throw new AssertionError("Missing i18n for " + configObj.getDisplayName());
                }
                if (configObj.getDescription().startsWith("feature.wynntils.")) {
                    WynntilsMod.error("Config description i18n is missing for " + configObj.getDescription());
                    throw new AssertionError("Missing i18n for " + configObj.getDescription());
                }
                if (configObj.getDescription().isEmpty()) {
                    WynntilsMod.error("Config description is empty for " + configObj.getDisplayName());
                    throw new AssertionError("Missing i18n for " + configObj.getDisplayName());
                }
            }
        }
        return configObj;
    }

    public Stream<Config<?>> getConfigs() {
        return getConfigList().stream();
    }

    public Stream<Config<?>> getConfigsForOwner(PersistedOwner owner) {
        return getConfigs()
                .filter(config -> Managers.Persisted.getMetadata(config).owner() == owner);
    }

    public boolean importConfig(String jsonInput, List<Configurable> configsToImport) {
        try {
            Map<String, Object> configData =
                    Managers.Json.GSON.fromJson(jsonInput, new TypeToken<HashMap<String, Object>>() {}.getType());

            if (configData == null) {
                WynntilsMod.warn("Unable to import config due to invalid input");
                return false;
            }

            // Loop through all features chosen to import to
            for (Configurable feature : configsToImport) {
                // Loop through the visible configs only as they are the only configs to be imported
                for (Config<?> configOption : feature.getVisibleConfigOptions()) {
                    String configOptionName = configOption.getJsonName();

                    // If the config data contains this config option, then it can be imported
                    if (configData.containsKey(configOptionName)) {
                        Object configOptionValue = configData.get(configOptionName);
                        setConfigValue(configOption, configOptionValue);
                    }
                }
            }

            return true;
        } catch (JsonSyntaxException ex) {
            WynntilsMod.warn("Failed to import config ", ex);
            return false;
        }
    }

    public String exportConfig(List<Configurable> featuresToExport) {
        Map<String, Object> configData = new HashMap<>();

        // Loop through all features to be exported
        for (Configurable feature : featuresToExport) {
            List<Config<?>> visibleConfigOptions = feature.getVisibleConfigOptions();

            // Loop through visible config options, as we don't want this to export
            // hidden configs since they should be exportable in their
            // own features, like favorites and waypoints
            for (Config<?> configOption : visibleConfigOptions) {
                String configOptionName = configOption.getJsonName();
                Object configOptionValue = configOption.get();

                // Save the config option to the map
                configData.put(configOptionName, configOptionValue);
            }
        }

        // Return the json string of the exported settings
        return Managers.Json.GSON.toJson(configData);
    }

    private <T> void setConfigValue(Config<T> config, Object value) {
        T typedValue = config.tryParseStringValue(value.toString());

        if (typedValue != null) {
            config.setValue(typedValue);
        }
    }

    public void reloadAll() {
        userConfigFile = new File(
                CONFIG_DIR, UndashedUuid.toString(McUtils.mc().getUser().getProfileId()) + FILE_SUFFIX);
        init();
    }
}
