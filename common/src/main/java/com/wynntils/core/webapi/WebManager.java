/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.webapi;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.webapi.profiles.ItemGuessProfile;
import com.wynntils.core.webapi.request.RequestBuilder;
import com.wynntils.core.webapi.request.RequestHandler;
import com.wynntils.wc.objects.items.IdentificationContainer;
import com.wynntils.wc.objects.items.ItemProfile;
import com.wynntils.wc.objects.items.ItemType;
import com.wynntils.wc.objects.items.MajorIdentification;
import com.wynntils.wc.utils.IdentificationOrderer;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.jetbrains.annotations.Nullable;

/** Provides and loads web content on demand */
public class WebManager {
    public static final File API_CACHE_ROOT = new File(WynntilsMod.MOD_STORAGE_ROOT, "apicache");

    private static boolean setup = false;
    private static final RequestHandler handler = new RequestHandler();

    public static @Nullable WebReader apiUrls = null;

    private static final Gson gson = new Gson();

    private static HashMap<String, ItemProfile> items = new HashMap<>();
    private static Collection<ItemProfile> directItems = new ArrayList<>();
    private static final HashMap<String, ItemGuessProfile> itemGuesses = new HashMap<>();
    private static HashMap<String, String> translatedReferences = new HashMap<>();
    private static HashMap<String, String> internalIdentifications = new HashMap<>();
    private static HashMap<String, MajorIdentification> majorIds = new HashMap<>();
    private static HashMap<ItemType, String[]> materialTypes = new HashMap<>();

    public static void init() {
        tryReloadApiUrls(false);
    }

    public static boolean tryLoadItemGuesses() {
        if (!isItemGuessesLoaded()) {
            handler.addRequest(
                    new RequestBuilder(apiUrls.get("ItemGuesses"), "item_guesses")
                            .cacheTo(new File(API_CACHE_ROOT, "item_guesses.json"))
                            .handleJsonObject(
                                    json -> {
                                        Type type =
                                                new TypeToken<
                                                        HashMap<
                                                                String,
                                                                ItemGuessProfile>>() {}.getType();

                                        GsonBuilder gsonBuilder = new GsonBuilder();
                                        gsonBuilder.registerTypeHierarchyAdapter(
                                                HashMap.class,
                                                new ItemGuessProfile.ItemGuessDeserializer());
                                        Gson gson = gsonBuilder.create();

                                        itemGuesses.putAll(gson.fromJson(json, type));

                                        markFlag(0);
                                        return true;
                                    })
                            .useCacheAsBackup()
                            .build());

            handler.dispatch(false);

            // Check for success
            return isItemGuessesLoaded();
        }

        return true;
    }

    public static boolean tryLoadItemList() {
        if (!isItemListLoaded()) {
            handler.addRequest(
                    new RequestBuilder(apiUrls.get("Athena") + "/cache/get/itemList", "item_list")
                            .cacheTo(new File(API_CACHE_ROOT, "item_list.json"))
                            .handleJsonObject(
                                    json -> {
                                        System.out.println("Got json");
                                        translatedReferences =
                                                gson.fromJson(
                                                        json.getAsJsonObject(
                                                                "translatedReferences"),
                                                        HashMap.class);
                                        internalIdentifications =
                                                gson.fromJson(
                                                        json.getAsJsonObject(
                                                                "internalIdentifications"),
                                                        HashMap.class);

                                        Type majorIdsType =
                                                new TypeToken<
                                                        HashMap<
                                                                String,
                                                                MajorIdentification>>() {}.getType();
                                        majorIds =
                                                gson.fromJson(
                                                        json.getAsJsonObject(
                                                                "majorIdentifications"),
                                                        majorIdsType);
                                        Type materialTypesType =
                                                new TypeToken<
                                                        HashMap<ItemType, String[]>>() {}.getType();
                                        materialTypes =
                                                gson.fromJson(
                                                        json.getAsJsonObject("materialTypes"),
                                                        materialTypesType);

                                        IdentificationOrderer.INSTANCE =
                                                gson.fromJson(
                                                        json.getAsJsonObject("identificationOrder"),
                                                        IdentificationOrderer.class);

                                        ItemProfile[] gItems =
                                                gson.fromJson(
                                                        json.getAsJsonArray("items"),
                                                        ItemProfile[].class);

                                        HashMap<String, ItemProfile> citems = new HashMap<>();
                                        for (ItemProfile prof : gItems) {
                                            prof.getStatuses()
                                                    .values()
                                                    .forEach(
                                                            IdentificationContainer
                                                                    ::calculateMinMax);
                                            prof.addMajorIds(majorIds);
                                            citems.put(prof.getDisplayName(), prof);
                                        }

                                        citems.values().forEach(ItemProfile::registerIdTypes);

                                        directItems = citems.values();
                                        items = citems;

                                        markFlag(1);
                                        return true;
                                    })
                            .useCacheAsBackup()
                            .build());

            handler.dispatch(false);

            // Check for success
            return isItemListLoaded();
        }

        return true;
    }

    private static boolean tryReloadApiUrls(boolean async) {
        if (apiUrls == null) {
            handler.addRequest(
                    new RequestBuilder("https://api.wynntils.com/webapi", "webapi")
                            .cacheTo(new File(API_CACHE_ROOT, "webapi.txt"))
                            .handleWebReader(
                                    reader -> {
                                        apiUrls = reader;
                                        if (!setup) {
                                            setup = true;
                                        }
                                        return true;
                                    })
                            .build());

            handler.dispatch(async);
            return setup;
        }

        return true;
    }

    public static boolean isItemGuessesLoaded() {
        return hasFlag(0);
    }

    public static boolean isItemListLoaded() {
        return hasFlag(1);
    }

    public static HashMap<String, ItemGuessProfile> getItemGuesses() {
        return itemGuesses;
    }

    public static Collection<ItemProfile> getItemsCollection() {
        return directItems;
    }

    public static HashMap<String, ItemProfile> getItemsMap() {
        return items;
    }

    public static HashMap<ItemType, String[]> getMaterialTypes() {
        return materialTypes;
    }

    public static HashMap<String, MajorIdentification> getMajorIds() {
        return majorIds;
    }

    public static HashMap<String, String> getInternalIdentifications() {
        return internalIdentifications;
    }

    public static HashMap<String, String> getTranslatedReferences() {
        return translatedReferences;
    }

    public static boolean isSetup() {
        return setup;
    }

    private static long flag = 0L;

    private static boolean hasFlag(int i) {
        return (flag & (1L << i)) == 1;
    }

    private static void markFlag(int i) {
        flag |= (1L << i);
    }
}
