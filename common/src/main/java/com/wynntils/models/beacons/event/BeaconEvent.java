/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.beacons.event;

import com.wynntils.models.beacons.type.Beacon;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;

public abstract class BeaconEvent extends Event {
    protected final Beacon beacon;

    protected BeaconEvent(Beacon beacon) {
        this.beacon = beacon;
    }

    public Beacon getBeacon() {
        return beacon;
    }

    public static class Added extends BeaconEvent {
        private final Entity entity;

        public Added(Beacon verifiedBeacon, Entity entities) {
            super(verifiedBeacon);
            this.entity = entities;
        }

        public Entity getEntity() {
            return entity;
        }
    }

    public static class Moved extends BeaconEvent {
        private final Beacon newBeacon;

        public Moved(Beacon oldBeacon, Beacon newBeacon) {
            super(oldBeacon);
            this.newBeacon = newBeacon;
        }

        @Override
        public Beacon getBeacon() {
            throw new UnsupportedOperationException("Use getOldBeacon() or getNewBeacon() instead");
        }

        public Beacon getOldBeacon() {
            return beacon;
        }

        public Beacon getNewBeacon() {
            return newBeacon;
        }
    }

    public static class Removed extends BeaconEvent {
        public Removed(Beacon beacon) {
            super(beacon);
        }
    }
}
