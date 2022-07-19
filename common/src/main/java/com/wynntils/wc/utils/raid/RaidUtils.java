/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.utils.raid;

import com.wynntils.core.WynntilsMod;
import com.wynntils.wc.utils.raid.enums.RaidPowerup;
import com.wynntils.wc.utils.raid.enums.RaidRoom;
import com.wynntils.wc.utils.raid.enums.RaidState;
import java.io.File;
import java.util.List;
import java.util.Set;

public class RaidUtils {

    private static final File RAIDS = new File(WynntilsMod.MOD_STORAGE_ROOT, "raids");
    private static long startTime = -1;
    private static long endTime = -1;
    private static RaidState state = RaidState.DISABLED;

    private static RaidInformation raidInformation = null;

    public static class RaidInformation {
        private List<RaidPowerup> powerupsList;
        private Set<RaidRoom> rooms;

        public List<RaidPowerup> getPowerupsList() {
            return powerupsList;
        }

        public Set<RaidRoom> getRooms() {
            return rooms;
        }

        public void addPowerup(RaidPowerup powerup) {
            powerupsList.add(powerup);
        }

        public void addRoom(RaidRoom room) {
            rooms.add(room);
        }
    }
}
