/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.type;

public record RangedValue(int low, int high) {
    public static final RangedValue NONE = new RangedValue(0, 0);

    public static RangedValue of(int low, int high) {
        return new RangedValue(low, high);
    }

    public static RangedValue fromString(String range) {
        String[] pair = range.split("-");
        return new RangedValue(Integer.parseInt(pair[0]), Integer.parseInt(pair[1]));
    }

    public static RangedValue fromStringSafe(String range) {
        // Try parse the range as an int, it might be a fixed value
        try {
            int value = Integer.parseInt(range);
            return new RangedValue(value, value);
        } catch (NumberFormatException e) {
            // Not a fixed value, try parse as a range
        }

        // Try parse the range as a range
        String[] pair = range.split("-");
        if (pair.length != 2) {
            // Not a valid range
            return null;
        }

        try {
            int low = Integer.parseInt(pair[0]);
            int high = Integer.parseInt(pair[1]);
            return new RangedValue(low, high);
        } catch (NumberFormatException e) {
            // Not a valid range
            return null;
        }
    }

    public boolean isFixed() {
        return low == high;
    }

    public boolean inRange(int value) {
        return value >= low && value <= high;
    }

    public String asString() {
        return low + "-" + high;
    }

    @Override
    public String toString() {
        return "<" + low + "-" + high + '>';
    }
}
