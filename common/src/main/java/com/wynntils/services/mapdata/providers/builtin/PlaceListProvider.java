/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.builtin;

import com.google.gson.reflect.TypeToken;
import com.wynntils.core.components.Managers;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.UrlId;
import com.wynntils.services.mapdata.features.PlaceLocation;
import com.wynntils.services.mapdata.type.MapFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PlaceListProvider extends BuiltInProvider {
    private static final List<MapFeature> PROVIDED_FEATURES = new ArrayList<>();

    public PlaceListProvider() {
        reloadData();
    }

    @Override
    public void reloadData() {
        loadPlaces();
    }

    @Override
    public String getProviderId() {
        return "place-list";
    }

    @Override
    public Stream<MapFeature> getFeatures() {
        return PROVIDED_FEATURES.stream();
    }

    private void loadPlaces() {
        Download dl = Managers.Net.download(UrlId.DATA_STATIC_PLACE_MAPFEATURES);
        dl.handleReader(reader -> {
            TypeToken<List<PlaceLocation>> type = new TypeToken<>() {};
            List<PlaceLocation> places = Managers.Json.GSON.fromJson(reader, type.getType());
            PROVIDED_FEATURES.forEach(this::notifyCallbacks);
            PROVIDED_FEATURES.clear();
            PROVIDED_FEATURES.addAll(places);
        });
    }
}
