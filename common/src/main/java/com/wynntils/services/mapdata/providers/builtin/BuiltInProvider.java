/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.builtin;

import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.providers.MapDataProvider;
import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.services.mapdata.type.MapDataProvidedType;
import com.wynntils.services.mapdata.type.MapFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class BuiltInProvider implements MapDataProvider {
    protected final List<Consumer<MapDataProvidedType>> callbacks = new ArrayList<>();

    public abstract String getProviderId();

    @Override
    public Stream<MapFeature> getFeatures() {
        return Stream.empty();
    }

    @Override
    public Stream<MapCategory> getCategories() {
        return Stream.empty();
    }

    @Override
    public Stream<MapIcon> getIcons() {
        return Stream.empty();
    }

    @Override
    public void onChange(Consumer<MapDataProvidedType> callback) {
        callbacks.add(callback);
    }

    protected void notifyCallbacks(MapDataProvidedType type) {
        callbacks.forEach(c -> c.accept(type));
    }
}
