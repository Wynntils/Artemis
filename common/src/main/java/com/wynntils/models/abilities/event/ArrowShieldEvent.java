/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilities.event;

import net.neoforged.bus.api.Event;

public abstract class ArrowShieldEvent extends Event {
    public static final class Created extends ArrowShieldEvent {
        private final int charges;

        public Created(int charges) {
            this.charges = charges;
        }

        public int getCharges() {
            return charges;
        }
    }

    public static final class Degraded extends ArrowShieldEvent {
        private final int chargesRemaining;

        public Degraded(int chargesRemaining) {
            this.chargesRemaining = chargesRemaining;
        }

        public int getChargesRemaining() {
            return chargesRemaining;
        }
    }

    public static final class Removed extends ArrowShieldEvent {}
}
