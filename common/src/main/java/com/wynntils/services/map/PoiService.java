/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.map;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Service;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.services.map.pois.CustomPoi;
import com.wynntils.services.map.type.CustomPoiProvider;
import com.wynntils.utils.render.Texture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PoiService extends Service {
    public static final List<Texture> POI_ICONS = List.of(
            Texture.FLAG,
            Texture.DIAMOND,
            Texture.FIREBALL,
            Texture.SIGN,
            Texture.STAR,
            Texture.WALL,
            Texture.CHEST_T1,
            Texture.CHEST_T2,
            Texture.CHEST_T3,
            Texture.CHEST_T4,
            Texture.FARMING,
            Texture.FISHING,
            Texture.MINING,
            Texture.WOODCUTTING);

    private final Map<CustomPoiProvider, List<CustomPoi>> providedCustomPois = new ConcurrentHashMap<>();

    @Persisted
    private final Storage<List<CustomPoiProvider>> customPoiProviders = new Storage<>(new ArrayList<>());

    public PoiService() {
        super(List.of());

        loadData();
    }

    @Override
    public void reloadData() {
        loadData();
    }

    @Override
    public void onStorageLoad(Storage<?> storage) {
        loadCustomPoiProviders();
    }

    private void loadData() {
        loadCustomPoiProviders();
    }

    public void loadCustomPoiProviders() {
        for (CustomPoiProvider poiProvider : customPoiProviders.get()) {
            Managers.Net.download(poiProvider.getUrl(), poiProvider.getName()).handleJsonArray(elements -> {
                List<CustomPoi> pois = new ArrayList<>();

                for (JsonElement jsonElement : elements) {
                    CustomPoi poi = Managers.Json.GSON.fromJson(jsonElement, CustomPoi.class);
                    pois.add(poi);
                }

                providedCustomPois.put(poiProvider, ImmutableList.copyOf(pois));
            });
        }
    }

    public List<CustomPoi> getProvidedCustomPois() {
        return customPoiProviders.get().stream()
                .filter(CustomPoiProvider::isEnabled)
                .map(provider -> providedCustomPois.getOrDefault(provider, List.of()))
                .flatMap(List::stream)
                .toList();
    }

    public List<CustomPoiProvider> getCustomPoiProviders() {
        return customPoiProviders.get();
    }

    public void addCustomPoiProvider(CustomPoiProvider poiProvider) {
        customPoiProviders.get().add(poiProvider);
        customPoiProviders.touched();
        loadCustomPoiProviders();
    }

    public boolean isPoiProvided(CustomPoi customPoi) {
        return getProvidedCustomPois().contains(customPoi);
    }

    public boolean removeCustomPoiProvider(String name) {
        Optional<CustomPoiProvider> provider = customPoiProviders.get().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();

        if (provider.isEmpty()) return false;

        customPoiProviders.get().remove(provider.get());
        providedCustomPois.remove(provider.get());

        return true;
    }
}
