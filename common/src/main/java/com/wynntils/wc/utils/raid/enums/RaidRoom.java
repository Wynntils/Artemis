/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.utils.raid.enums;

public enum RaidRoom {
    // Grootslang
    GROOTSLANG_THREE_PLATFORMS(0, 0, 0, 0),
    GROOTSLANG_SLIMEY_PLATFORM(0, 0, 0, 0),
    GROOTSLANG_PARKOUR_ONE(0, 0, 0, 0),
    GROOTSLANG_PARKOUR_TWO(0, 0, 0, 0),
    GROOTSLANG_BATTLE_ONE(0, 0, 0, 0),
    GROOTSLANG_BATTLE_TWO(0, 0, 0, 0),
    GROOTSLANG_PUZZLE_ONE(0, 0, 0, 0),
    GROOTSLANG_PUZZLE_TWO(0, 0, 0, 0),
    GROOTSLANG_SLIMEY_GOO(0, 0, 0, 0),
    GROOTSLANG_MINIBOSSES(0, 0, 0, 0),
    GROOTSLANG_BOSSFIGHT(0, 0, 0, 0),

    // The Canyon Colossus
    TCC_MINIGAME(0, 0, 0, 0),
    TCC_LAVA_LAKE(0, 0, 0, 0),
    TCC_THREE_PLATFORMS(0, 0, 0, 0),
    TCC_MAZE(0, 0, 0, 0),
    TCC_TWO_PLATFORMS(0, 0, 0, 0),
    TCC_THREE_GOLEMS(0, 0, 0, 0),
    TCC_BOSSFIGHT(0, 0, 0, 0),

    // Nexus of Light
    NOL_HUB(0, 0, 0, 0),
    NOL_DECAYING_CLOUDS(0, 0, 0, 0),
    NOL_PLATFORMS(0, 0, 0, 0),
    NOL_CLOUDS(0, 0, 0, 0),
    NOL_RESOURCE(0, 0, 0, 0),
    NOL_TOWER(0, 0, 0, 0),
    NOL_MAZE(0, 0, 0, 0),
    NOL_BOSSFIGHT(0, 0, 0, 0);
    private int x1, y1, x2, y2;

    RaidRoom(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
}
