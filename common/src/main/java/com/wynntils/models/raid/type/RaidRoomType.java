/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.raid.type;

public enum RaidRoomType {
    INTRO,
    INSTRUCTIONS_1,
    CHALLENGE_1,
    BUFF_1,
    INSTRUCTIONS_2,
    CHALLENGE_2,
    BUFF_2,
    INSTRUCTIONS_3,
    CHALLENGE_3,
    BUFF_3,
    BOSS_INTERMISSION,
    BOSS_FIGHT;

    public static RaidRoomType fromName(String name) {
        for (RaidRoomType roomType : RaidRoomType.values()) {
            if (roomType.name().equalsIgnoreCase(name)) {
                return roomType;
            }
        }

        return null;
    }
}
