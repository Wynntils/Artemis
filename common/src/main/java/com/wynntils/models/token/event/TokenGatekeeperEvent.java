/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.token.event;

import com.wynntils.models.token.type.TokenGatekeeper;
import net.neoforged.bus.api.Event;

public abstract class TokenGatekeeperEvent extends Event {
    private final TokenGatekeeper gatekeeper;

    protected TokenGatekeeperEvent(TokenGatekeeper gatekeeper) {
        this.gatekeeper = gatekeeper;
    }

    public TokenGatekeeper getGatekeeper() {
        return gatekeeper;
    }

    public static final class Added extends TokenGatekeeperEvent {
        public Added(TokenGatekeeper gatekeeper) {
            super(gatekeeper);
        }
    }

    public static final class Deposited extends TokenGatekeeperEvent {
        public Deposited(TokenGatekeeper gatekeeper) {
            super(gatekeeper);
        }
    }

    public static final class InventoryUpdated extends TokenGatekeeperEvent {
        private final int count;
        private final int oldCount;

        public InventoryUpdated(TokenGatekeeper gatekeeper, int count, int oldCount) {
            super(gatekeeper);
            this.count = count;
            this.oldCount = oldCount;
        }

        public int getCount() {
            return count;
        }

        public int getOldCount() {
            return oldCount;
        }
    }

    public static final class Removed extends TokenGatekeeperEvent {
        public Removed(TokenGatekeeper gatekeeper) {
            super(gatekeeper);
        }
    }
}
