/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.raid.event;

import com.wynntils.models.raid.type.RaidKind;
import com.wynntils.models.raid.type.RaidRoomType;
import net.neoforged.bus.api.Event;

public abstract class RaidChallengeEvent extends Event {
    private final RaidKind raidKind;
    private final RaidRoomType challengeRoom;

    protected RaidChallengeEvent(RaidKind raidKind, RaidRoomType challengeRoom) {
        this.raidKind = raidKind;
        this.challengeRoom = challengeRoom;
    }

    public RaidKind getRaid() {
        return raidKind;
    }

    public RaidRoomType getChallengeRoom() {
        return challengeRoom;
    }

    public static class Started extends RaidChallengeEvent {
        public Started(RaidKind raidKind, RaidRoomType challengeRoom) {
            super(raidKind, challengeRoom);
        }
    }

    public static class Completed extends RaidChallengeEvent {
        public Completed(RaidKind raidKind, RaidRoomType challengeRoom) {
            super(raidKind, challengeRoom);
        }
    }
}
