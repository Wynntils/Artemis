/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.map.poi;

import java.util.function.Supplier;

public abstract class DynamicIconPoi extends IconPoi {
    Supplier<MapLocation> locationSupplier;

    protected DynamicIconPoi(Supplier<MapLocation> locationSupplier) {
        this.locationSupplier = locationSupplier;
    }

    @Override
    public boolean hasStaticLocation() {
        return false;
    }

    @Override
    public MapLocation getLocation() {
        return locationSupplier.get();
    }
}
