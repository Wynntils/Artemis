/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.upfixers.ConfigUpfixerManager;
import com.wynntils.core.features.Configurable;
import com.wynntils.core.features.Feature;
import com.wynntils.core.features.overlays.DynamicOverlay;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.annotations.OverlayGroup;
import com.wynntils.core.json.JsonManager;
import com.wynntils.utils.JsonUtils;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class ConfigManager extends Manager {
    private static final File CONFIGS = WynntilsMod.getModStorageDir("config");
    private static final String FILE_SUFFIX = ".conf.json";
    private static final File DEFAULT_CONFIG = new File(CONFIGS, "default" + FILE_SUFFIX);
    private static final String OVERLAY_GROUPS_JSON_KEY = "overlayGroups";
    private static final Set<ConfigHolder> CONFIG_HOLDERS = new TreeSet<>();

    private static final List<ConfigHolder> OVERLAY_GROUP_CONFIG_HOLDERS = new ArrayList<>();
    private static final List<OverlayGroupHolder> OVERLAY_GROUP_FIELDS = new ArrayList<>();

    private final File userConfig;
    private JsonObject configObject;

    public ConfigManager(ConfigUpfixerManager configUpfixerManager, JsonManager jsonManager) {
        super(List.of(configUpfixerManager, jsonManager));

        userConfig = new File(CONFIGS, McUtils.mc().getUser().getUuid() + FILE_SUFFIX);

        // First, we load the config file
        configObject = Managers.Json.loadPreciousJson(userConfig);

        // Now, we have to apply upfixers, before any config loading happens
        if (configUpfixerManager.runUpfixers(configObject)) {
            Managers.Json.savePreciousJson(userConfig, configObject);
        }
    }

    public void registerFeature(Feature feature) {
        registerOverlayGroups(feature);

        for (Overlay overlay : feature.getOverlays()) {
            registerConfigOptions(overlay);
        }

        registerConfigOptions(feature);
    }

    private void registerOverlayGroups(Feature feature) {
        List<OverlayGroupHolder> holders = Stream.of(feature.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(OverlayGroup.class))
                .map(field -> new OverlayGroupHolder(
                        field, feature, field.getAnnotation(OverlayGroup.class).instances()))
                .toList();

        if (holders.isEmpty()) return;

        OVERLAY_GROUP_FIELDS.addAll(holders);
        feature.addOverlayGroups(holders);

        for (OverlayGroupHolder holder : holders) {
            holder.initGroup(
                    IntStream.rangeClosed(1, holder.getDefaultCount()).boxed().toList());

            List<ConfigHolder> overlayHolders = holder.getOverlays().stream()
                    .map(this::getConfigOptions)
                    .flatMap(List::stream)
                    .toList();

            holder.getOverlays().forEach(overlay -> overlay.addConfigOptions(this.getConfigOptions(overlay)));

            OVERLAY_GROUP_CONFIG_HOLDERS.addAll(overlayHolders);
        }
    }

    private void registerConfigOptions(Configurable configurable) {
        List<ConfigHolder> configOptions = getConfigOptions(configurable);

        configurable.addConfigOptions(configOptions);
        CONFIG_HOLDERS.addAll(configOptions);
    }

    public void reloadConfiguration() {
        configObject = Managers.Json.loadPreciousJson(userConfig);
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
        List<ConfigHolder> oldOverlayHolders = new ArrayList<>(OVERLAY_GROUP_CONFIG_HOLDERS);
        OVERLAY_GROUP_CONFIG_HOLDERS.clear();

        JsonObject overlayGroups = JsonUtils.getNullableJsonObject(configObject, OVERLAY_GROUPS_JSON_KEY);

        for (OverlayGroupHolder holder : OVERLAY_GROUP_FIELDS) {
            if (initOverlayGroups) {
                JsonArray ids = JsonUtils.getNullableJsonArray(overlayGroups, holder.getConfigKey());

                List<Integer> idList = overlayGroups.has(holder.getConfigKey())
                        ? ids.asList().stream().map(JsonElement::getAsInt).toList()
                        : IntStream.rangeClosed(1, holder.getDefaultCount())
                                .boxed()
                                .toList();

                holder.initGroup(idList);
            }

            List<ConfigHolder> overlayHolders = holder.getOverlays().stream()
                    .map(this::getConfigOptions)
                    .flatMap(List::stream)
                    .toList();

            holder.getOverlays().forEach(overlay -> overlay.removeConfigOptions(oldOverlayHolders));
            holder.getOverlays().forEach(overlay -> overlay.addConfigOptions(this.getConfigOptions(overlay)));

            holder.getParent().initOverlayGroups();

            OVERLAY_GROUP_CONFIG_HOLDERS.addAll(overlayHolders);
        }

        for (ConfigHolder holder : getConfigHolderList()) {
            // option hasn't been saved to config
            if (!configObject.has(holder.getJsonName())) {
                if (resetIfNotFound) {
                    holder.reset();
                }
                continue;
            }

            // read value and update option
            JsonElement holderJson = configObject.get(holder.getJsonName());
            Object value = Managers.Json.GSON.fromJson(holderJson, holder.getType());
            holder.setValue(value);
        }

        // Newly created group overlays need to be enabled
        for (OverlayGroupHolder holder : OVERLAY_GROUP_FIELDS) {
            holder.getParent().enableOverlays();
        }
    }

    private static List<ConfigHolder> getConfigHolderList() {
        return Stream.concat(CONFIG_HOLDERS.stream(), OVERLAY_GROUP_CONFIG_HOLDERS.stream())
                .toList();
    }

    public void saveConfig() {
        // create json object, with entry for each option of each container
        JsonObject holderJson = new JsonObject();
        for (ConfigHolder holder : getConfigHolderList()) {
            if (!holder.valueChanged()) continue; // only save options that have been set by the user
            Object value = holder.getValue();

            JsonElement holderElement = Managers.Json.GSON.toJsonTree(value);
            holderJson.add(holder.getJsonName(), holderElement);
        }

        // Also save upfixer data
        holderJson.add(
                Managers.ConfigUpfixer.UPFIXER_JSON_MEMBER_NAME,
                configObject.get(Managers.ConfigUpfixer.UPFIXER_JSON_MEMBER_NAME));

        // Save overlay groups
        JsonObject overlayGroups = new JsonObject();
        for (OverlayGroupHolder holder : OVERLAY_GROUP_FIELDS) {
            JsonArray ids = new JsonArray();

            holder.getOverlays().stream()
                    .map(overlay -> ((DynamicOverlay) overlay).getId())
                    .forEach(ids::add);

            overlayGroups.add(holder.getConfigKey(), ids);
        }

        holderJson.add(OVERLAY_GROUPS_JSON_KEY, overlayGroups);

        Managers.Json.savePreciousJson(userConfig, holderJson);
    }

    public void saveDefaultConfig() {
        // create json object, with entry for each option of each container
        JsonObject holderJson = new JsonObject();
        for (ConfigHolder holder : getConfigHolderList()) {
            Object value = holder.getDefaultValue();

            JsonElement holderElement = Managers.Json.GSON.toJsonTree(value);
            holderJson.add(holder.getJsonName(), holderElement);
        }

        WynntilsMod.info("Creating default config file with " + holderJson.size() + " config values.");
        Managers.Json.savePreciousJson(DEFAULT_CONFIG, holderJson);
    }

    private List<ConfigHolder> getConfigOptions(Configurable parent) {
        List<ConfigHolder> options = new ArrayList<>();

        Field[] annotatedConfigs = FieldUtils.getFieldsWithAnnotation(parent.getClass(), ConfigInfo.class);

        List<Field> fields = FieldUtils.getAllFieldsList(parent.getClass());
        List<Field> configFields =
                fields.stream().filter(f -> f.getType().equals(Config.class)).toList();

        for (Field configField : configFields) {
            ConfigInfo configInfo = Arrays.stream(annotatedConfigs)
                    .filter(f -> f.equals(configField))
                    .findFirst()
                    .map(f -> f.getAnnotation(ConfigInfo.class))
                    .orElse(null);
            String subcategory = configInfo != null ? configInfo.subcategory() : "";
            String i18nKey = configInfo != null ? configInfo.key() : "";
            boolean visible = configInfo != null ? configInfo.visible() : true;

            Type typeOverride = Managers.Json.findFieldTypeOverride(parent, configField);

            ConfigHolder configHolder =
                    new ConfigHolder(parent, configField, subcategory, i18nKey, visible, typeOverride);
            if (WynntilsMod.isDevelopmentEnvironment()) {
                if (visible) {
                    if (configHolder.getDisplayName().startsWith("feature.wynntils.")) {
                        WynntilsMod.error("Config displayName i18n is missing for " + configHolder.getDisplayName());
                        throw new AssertionError("Missing i18n for " + configHolder.getDisplayName());
                    }
                    if (configHolder.getDescription().startsWith("feature.wynntils.")) {
                        WynntilsMod.error("Config description i18n is missing for " + configHolder.getDescription());
                        throw new AssertionError("Missing i18n for " + configHolder.getDescription());
                    }
                    if (configHolder.getDescription().isEmpty()) {
                        WynntilsMod.error("Config description is empty for " + configHolder.getDisplayName());
                        throw new AssertionError("Missing i18n for " + configHolder.getDisplayName());
                    }
                }
            }
            options.add(configHolder);
        }
        return options;
    }

    public Stream<ConfigHolder> getConfigHolders() {
        return getConfigHolderList().stream();
    }
}
