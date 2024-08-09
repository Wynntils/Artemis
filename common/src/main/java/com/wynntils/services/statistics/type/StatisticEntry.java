/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.statistics.type;

public record StatisticEntry(long total, long count, long min, long max, long firstModified, long lastModified) {
    public static final StatisticEntry EMPTY = new StatisticEntry(0, 0, 0, 0, 0, 0);

    public StatisticEntry getUpdatedEntry(long amount) {
        return new StatisticEntry(
                total + amount,
                count + 1,
                Math.min(min, amount),
                Math.max(max, amount),
                firstModified,
                System.currentTimeMillis());
    }

    public long average() {
        if (count == 0) return 0;

        return total / count;
    }
}
