/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model;

import com.wynntils.core.managers.Model;
import com.wynntils.mc.event.ClientTickEvent;
import com.wynntils.mc.event.SetSpawnEvent;
import com.wynntils.mc.objects.Location;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wynn.model.map.poi.MapLocation;
import com.wynntils.wynn.model.map.poi.WaypointPoi;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CompassModel extends Model {
    private static Supplier<Location> locationSupplier = null;
    private static Location compassLocation = null; // this field acts as a cache for the supplier

    public static void init() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Start e) {
        if (locationSupplier == null) return;

        Location newLocation = locationSupplier.get();

        if (newLocation == null) { // drop location
            reset();
        } else if (compassLocation != newLocation) { // update location
            compassLocation = newLocation;

            if (McUtils.mc().level != null) {
                McUtils.mc().level.setDefaultSpawnPos(compassLocation.toBlockPos(), 0);
            }
        }
    }

    public static Optional<Location> getCompassLocation() {
        return Optional.ofNullable(compassLocation);
    }

    public static Optional<WaypointPoi> getCompassWaypoint() {
        if (locationSupplier != null && locationSupplier.get() != null) {
            WaypointPoi waypointPoi = new WaypointPoi(() -> {
                Location location = locationSupplier.get();

                // Make sure to always render on top
                location.set(location.x, Double.MAX_VALUE, location.z);

                return MapLocation.fromLocation(location);
            });

            return Optional.of(waypointPoi);
        }

        return Optional.empty();
    }

    public static void setDynamicCompassLocation(Supplier<Location> compassSupplier) {
        if (compassSupplier == null) {
            return;
        }

        locationSupplier = compassSupplier;
        compassLocation = compassSupplier.get();
    }

    public static void setCompassLocation(Location location) {
        locationSupplier = () -> location;
        compassLocation = location;
    }

    public static void reset() {
        compassLocation = null;
        locationSupplier = null;

        if (McUtils.mc().level != null) {
            // We can't remove the compass behavior, so arbitrarily set it to our
            // current position
            McUtils.mc().level.setDefaultSpawnPos(McUtils.player().blockPosition(), 0);
        }
    }

    @SubscribeEvent
    public static void onSetSpawn(SetSpawnEvent e) {
        BlockPos spawnPos = e.getSpawnPos();

        if (McUtils.player() == null) {
            // Reset compass
            compassLocation = null;
            locationSupplier = null;

            if (McUtils.mc().level != null) {
                McUtils.mc().level.setDefaultSpawnPos(spawnPos, 0);
            }
        } else if (locationSupplier != null) {
            // If we have a set location, do not update our spawn point
            e.setCanceled(true);
        }
    }
}
