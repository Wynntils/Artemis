/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.features.Configurable;
import com.wynntils.core.features.Feature;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.objects.CustomColor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.reflect.FieldUtils;

public class ConfigManager {
    private static final File CONFIGS = WynntilsMod.getModStorageDir("config");
    private static final String FILE_SUFFIX = ".conf.json";
    private static final List<ConfigHolder> CONFIG_HOLDERS = new ArrayList<>();
    private static File userConfig;
    private static final File defaultConfig = new File(CONFIGS, "default" + FILE_SUFFIX);
    private static JsonObject configObject;
    private static Gson gson;

    public static void registerFeature(Feature feature) {
        List<ConfigHolder> featureConfigOptions = collectConfigOptions(feature);

        registerConfigOptions(feature, featureConfigOptions);
    }

    private static void registerConfigOptions(Configurable configurable, List<ConfigHolder> featureConfigOptions) {
        configurable.addConfigOptions(featureConfigOptions);
        loadConfigOptions(featureConfigOptions, false);
        CONFIG_HOLDERS.addAll(featureConfigOptions);
    }

    public static void init() {
        gson = new GsonBuilder()
                .registerTypeAdapter(CustomColor.class, new CustomColor.CustomColorSerializer())
                .setPrettyPrinting()
                .serializeNulls()
                .create();

        loadConfigFile();
    }

    public static void loadConfigFile() {
        // create config directory if necessary
        CONFIGS.mkdirs();

        // set up config file based on uuid, load it if it exists
        userConfig = new File(CONFIGS, McUtils.mc().getUser().getUuid() + FILE_SUFFIX);
        if (!userConfig.exists()) return;

        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(userConfig), StandardCharsets.UTF_8);
            JsonElement fileElement = JsonParser.parseReader(new JsonReader(reader));
            reader.close();
            if (!fileElement.isJsonObject()) return; // invalid config file

            configObject = fileElement.getAsJsonObject();
        } catch (IOException e) {
            WynntilsMod.error("Failed to load user config file!");
            e.printStackTrace();
        }
    }

    public static void loadConfigOptions(List<ConfigHolder> holders, boolean resetIfNotFound) {
        if (configObject == null) return; // nothing to load from

        for (ConfigHolder holder : holders) {
            // option hasn't been saved to config
            if (!configObject.has(holder.getJsonName())) {
                if (resetIfNotFound) {
                    holder.reset();
                }
                continue;
            }

            // read value and update option
            JsonElement holderJson = configObject.get(holder.getJsonName());
            Object value;

            value = gson.fromJson(holderJson, holder.getType());

            holder.setValue(value);
        }
    }

    public static void saveConfig() {
        try {
            // create file if necessary
            if (!userConfig.exists()) userConfig.createNewFile();

            // create json object, with entry for each option of each container
            JsonObject holderJson = new JsonObject();
            for (ConfigHolder holder : CONFIG_HOLDERS) {
                if (!holder.valueChanged()) continue; // only save options that have been set by the user
                Object value = holder.getValue();

                JsonElement holderElement = gson.toJsonTree(value);
                holderJson.add(holder.getJsonName(), holderElement);
            }

            // write json to file
            OutputStreamWriter fileWriter =
                    new OutputStreamWriter(new FileOutputStream(userConfig), StandardCharsets.UTF_8);
            gson.toJson(holderJson, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            WynntilsMod.error("Failed to save user config file!");
            e.printStackTrace();
        }
    }

    public static void saveDefaultConfig() {
        try {
            // create file if necessary
            if (!defaultConfig.exists()) defaultConfig.createNewFile();

            // create json object, with entry for each option of each container
            JsonObject holderJson = new JsonObject();
            for (ConfigHolder holder : CONFIG_HOLDERS) {
                Object value = holder.getValue();

                JsonElement holderElement = gson.toJsonTree(value);
                holderJson.add(holder.getJsonName(), holderElement);
            }

            // write json to file
            OutputStreamWriter fileWriter =
                    new OutputStreamWriter(new FileOutputStream(defaultConfig), StandardCharsets.UTF_8);
            gson.toJson(holderJson, fileWriter);
            fileWriter.close();
            WynntilsMod.info("Default config file created.");
        } catch (IOException e) {
            WynntilsMod.error("Failed to save user config file!");
            e.printStackTrace();
        }
    }

    private static List<ConfigHolder> collectConfigOptions(Feature feature) {
        FeatureInfo featureInfo = feature.getClass().getAnnotation(FeatureInfo.class);

        String category = "";
        // feature has no category defined, default to empty category
        if (featureInfo != null) {
            category = featureInfo.category();
        }

        loadFeatureOverlayConfigOptions(category, feature);

        return getConfigOptions(category, feature);
    }

    private static void loadFeatureOverlayConfigOptions(String category, Feature feature) {
        // collect feature's overlays' config options
        for (Overlay overlay : feature.getOverlays()) {
            List<ConfigHolder> options = getConfigOptions(category, overlay);

            registerConfigOptions(overlay, options);
        }
    }

    private static List<ConfigHolder> getConfigOptions(String category, Configurable parent) {
        List<ConfigHolder> options = new ArrayList<>();

        for (Field configField : FieldUtils.getFieldsWithAnnotation(parent.getClass(), Config.class)) {
            Config metadata = configField.getAnnotation(Config.class);

            Optional<Field> typeField = Arrays.stream(
                            FieldUtils.getFieldsWithAnnotation(parent.getClass(), TypeOverride.class))
                    .filter(field ->
                            field.getType() == Type.class && field.getName().equals(configField.getName() + "Type"))
                    .findFirst();

            if (typeField.isPresent()) {
                try {
                    Type type = (Type) FieldUtils.readField(typeField.get(), parent, true);
                    options.add(new ConfigHolder(parent, configField, category, metadata, type));
                } catch (IllegalAccessException e) {
                    WynntilsMod.error("Unable to get field " + typeField.get().getName());
                    e.printStackTrace();
                }
            } else {
                options.add(new ConfigHolder(parent, configField, category, metadata, null));
            }
        }
        return options;
    }

    public static Gson getGson() {
        return gson;
    }

    public static List<ConfigHolder> getConfigHolders() {
        return CONFIG_HOLDERS;
    }

    public static List<ConfigHolder> getConfigHolders(Configurable configurable) {
        return CONFIG_HOLDERS.stream()
                .filter(configHolder -> configHolder.getParent() == configurable)
                .toList();
    }
}
