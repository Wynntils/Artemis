/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.leaderboard.type;

public record LeaderboardBadge(int uOffset, int vOffset) {
    public static final int WIDTH = 19;
    public static final int HEIGHT = 17;

    public static LeaderboardBadge from(LeaderboardType leaderboardType, int standing) {
        int uOffset = leaderboardType.ordinal() * WIDTH;

        int color = 2; // just in case Athena gives a number not between 1 and 9

        if (standing >= 1 && standing <= 3) {
            color = 0;
        }
        if (standing >= 4 && standing <= 6) {
            color = 1;
        }
        if (standing >= 7 && standing <= 9) {
            color = 2;
        }

        int vOffset = color * HEIGHT;

        return new LeaderboardBadge(uOffset, vOffset);
    }
}
