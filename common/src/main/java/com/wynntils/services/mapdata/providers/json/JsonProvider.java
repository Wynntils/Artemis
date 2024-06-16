/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.MalformedJsonException;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.net.Download;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.providers.MapDataProvider;
import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.services.mapdata.type.MapDataProvidedType;
import com.wynntils.services.mapdata.type.MapFeature;
import com.wynntils.utils.EnumUtils;
import com.wynntils.utils.JsonUtils;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class JsonProvider implements MapDataProvider {
    // FIXME: Add JsonFeatureSerializer once it's fixed
    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(JsonCategory.class, new JsonCategorySerializer())
            .registerTypeHierarchyAdapter(JsonIcon.class, new JsonIconSerializer())
            .registerTypeHierarchyAdapter(CustomColor.class, new CustomColor.CustomColorSerializer())
            .registerTypeAdapterFactory(new EnumUtils.EnumTypeAdapterFactory<>())
            .enableComplexMapKeySerialization()
            .create();

    // Note: The version field is not used, but is kept for future compatibility
    //       If the need arises, it can be used to handle different versions of the json format
    //       This is easily achieved by GSON switching to different deserializers based on the version
    private final int version;
    private final List<MapFeature> features;
    private final List<MapCategory> categories;
    private final List<MapIcon> icons;

    private JsonProvider(int version, List<MapFeature> features, List<MapCategory> categories, List<MapIcon> icons) {
        this.version = version;
        this.features = features;
        this.categories = categories;
        this.icons = icons;
    }

    /**
     * Load a bundled resource from the mod jar
     *
     * @param id       The id of the resource
     * @param filename The filename of the resource
     * @return The loaded json provider
     */
    public static JsonProvider loadBundledResource(String id, String filename) {
        try (InputStream inputStream = WynntilsMod.getModResourceAsStream(filename);
                Reader targetReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(targetReader, JsonProvider.class);
        } catch (MalformedJsonException e) {
            McUtils.sendErrorToClient("Error parsing map data for '" + id + "'");
            WynntilsMod.warn("Error parsing map data for '" + id + "'", e);
        } catch (IOException e) {
            McUtils.sendErrorToClient("Error reading map data for '" + id + "'");
            WynntilsMod.warn("Error reading map data for '" + id + "'", e);
        } catch (Throwable e) {
            // This is typically a NPE in GSON parsing
            McUtils.sendErrorToClient("Error parsing map data for '" + id + "'");
            WynntilsMod.warn("Error parsing map data for '" + id + "'", e);
        }
        return null;
    }

    /**
     * Load a local resource from a file
     *
     * @param id   The id of the resource
     * @param file The file to load
     * @return The loaded json provider
     */
    public static JsonProvider loadLocalFile(String id, File file) {
        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonProvider.class);
        } catch (MalformedJsonException e) {
            McUtils.sendErrorToClient("Error parsing map data for '" + id + "'");
            WynntilsMod.warn("Error parsing map data for '" + id + "'", e);
        } catch (IOException e) {
            McUtils.sendErrorToClient("Error reading map data for '" + id + "'");
            WynntilsMod.warn("Error reading map data for '" + id + "'", e);
        } catch (Throwable e) {
            // This is either a json parse error or a NPE in GSON parsing
            McUtils.sendErrorToClient("Error parsing map data for '" + id + "'");
            WynntilsMod.warn("Error parsing map data for '" + id + "'", e);
        }
        return null;
    }

    /**
     * Load an online resource from a url
     *
     * @param id               The id of the resource
     * @param url              The url to load
     * @param registerCallback The callback to call with the loaded provider
     */
    public static void loadOnlineResource(String id, String url, BiConsumer<String, MapDataProvider> registerCallback) {
        Download dl = Managers.Net.download(URI.create(url), id);
        dl.handleReader(
                reader -> {
                    try {
                        registerCallback.accept(id, GSON.fromJson(reader, JsonProvider.class));
                    } catch (Throwable e) {
                        // This is either a json parse error or a NPE in GSON parsing
                        McUtils.sendErrorToClient("Error parsing map data for '" + id + "'");
                        WynntilsMod.warn("Error parsing map data for '" + id + "'", e);
                    }
                },
                onError -> {
                    McUtils.sendErrorToClient("Error downloading map data for '" + id + "'");
                    WynntilsMod.warn("Error occurred while downloading map data for '" + id + "'", onError);
                });
    }

    public int getVersion() {
        return version;
    }

    @Override
    public Stream<MapFeature> getFeatures() {
        return features.stream();
    }

    @Override
    public Stream<MapCategory> getCategories() {
        return categories.stream();
    }

    @Override
    public Stream<MapIcon> getIcons() {
        return icons.stream();
    }

    @Override
    public void onChange(Consumer<MapDataProvidedType> callback) {
        // The json does not change, as long as we do not implement a
        // reload of the file, so we do not need to register callbacks.
    }

    @Override
    public void reloadData() {
        // FIXME: To be implemented if needed (when the first json provider is added)
    }

    public static final class JsonCategorySerializer
            implements JsonDeserializer<JsonCategory>, JsonSerializer<JsonCategory> {
        @Override
        public JsonCategory deserialize(JsonElement jsonElement, Type jsonType, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject json = jsonElement.getAsJsonObject();

            String id = json.get("id").getAsString();
            String name = JsonUtils.getNullableJsonString(json, "name");
            JsonElement attributesJson = json.get("attributes");
            JsonMapAttributes attributes =
                    attributesJson == null ? null : GSON.fromJson(attributesJson, JsonMapAttributes.class);

            return new JsonCategory(id, name, attributes);
        }

        @Override
        public JsonElement serialize(JsonCategory mapCategory, Type type, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("id", mapCategory.getCategoryId());
            json.addProperty("name", mapCategory.getName().orElse(""));
            json.add("attributes", GSON.toJsonTree(mapCategory.getAttributes()));
            return json;
        }
    }

    // FIXME: The serializer should be able to handle the different types of map features, not just locations
    //        (Also needs a serializer implementation)
    public static final class JsonFeatureDeserializer implements JsonDeserializer<MapFeature> {
        @Override
        public MapFeature deserialize(JsonElement jsonElement, Type jsonType, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject json = jsonElement.getAsJsonObject();

            String id = JsonUtils.getNullableJsonString(json, "id");
            String category = JsonUtils.getNullableJsonString(json, "category");
            JsonElement locationJson = json.get("location");
            Location location = GSON.fromJson(locationJson, Location.class);
            JsonElement attributesJson = json.get("attributes");
            JsonMapAttributes attributes =
                    attributesJson == null ? null : GSON.fromJson(attributesJson, JsonMapAttributes.class);

            return new JsonMapLocation(id, category, attributes, location);
        }
    }

    public static final class JsonIconSerializer implements JsonDeserializer<JsonIcon>, JsonSerializer<JsonIcon> {
        @Override
        public JsonIcon deserialize(JsonElement jsonElement, Type jsonType, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject json = jsonElement.getAsJsonObject();

            String id = json.get("id").getAsString();
            String base64Texture = json.get("texture").getAsString();
            byte[] texture = Base64.getDecoder().decode(base64Texture);

            try {
                return new JsonIcon(id, texture);
            } catch (IOException e) {
                WynntilsMod.warn("Bad icon texture for " + id, e);
                return null;
            }
        }

        @Override
        public JsonElement serialize(JsonIcon mapIcon, Type type, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("id", mapIcon.getIconId());
            json.addProperty("texture", Base64.getEncoder().encodeToString(mapIcon.getTextureBytes()));
            return json;
        }
    }
}
