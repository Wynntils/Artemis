/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class MathUtils {
    private static final Map<Character, Integer> ROMAN_NUMERALS_MAP =
            Map.of('I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000);

    private static final TreeMap<Integer, String> INT_TO_ROMAN_MAP = new TreeMap<>(Map.ofEntries(
            Map.entry(1000, "M"),
            Map.entry(900, "CM"),
            Map.entry(500, "D"),
            Map.entry(400, "CD"),
            Map.entry(100, "C"),
            Map.entry(90, "XC"),
            Map.entry(50, "L"),
            Map.entry(40, "XL"),
            Map.entry(10, "X"),
            Map.entry(9, "IX"),
            Map.entry(5, "V"),
            Map.entry(4, "IV"),
            Map.entry(1, "I")));

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static float inverseLerp(float a, float b, float value) {
        return (value - a) / (b - a);
    }

    public static double inverseLerp(double a, double b, double value) {
        return (value - a) / (b - a);
    }

    public static int clamp(int num, int min, int max) {
        if (num < min) {
            return min;
        } else {
            return Math.min(num, max);
        }
    }

    public static float clamp(float num, float min, float max) {
        if (num < min) {
            return min;
        } else {
            return Math.min(num, max);
        }
    }

    public static int integerFromRoman(String numeral) {
        String normalized = numeral.trim()
                .toUpperCase(Locale.ROOT)
                .replace("IV", "IIII")
                .replace("IX", "VIIII")
                .replace("XL", "XXXX")
                .replace("XC", "LXXXX")
                .replace("CD", "CCCC")
                .replace("CM", "DCCCC");

        return normalized
                .chars()
                .map(c -> ROMAN_NUMERALS_MAP.getOrDefault(c, 0))
                .sum();
    }

    public static String toRoman(int number) {
        int l = INT_TO_ROMAN_MAP.floorKey(number);
        if (number == l) {
            return INT_TO_ROMAN_MAP.get(number);
        }
        return INT_TO_ROMAN_MAP.get(l) + toRoman(number - l);
    }

    public static float map(float sourceNumber, float fromA, float fromB, float toA, float toB) {
        return MathUtils.lerp(toA, toB, MathUtils.inverseLerp(fromA, fromB, sourceNumber));
    }

    public static float magnitude(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    public static double magnitude(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public static boolean isInside(int testX, int testZ, int x1, int x2, int z1, int z2) {
        return x1 <= testX && testX <= x2 && z1 <= testZ && testZ <= z2;
    }

    public static boolean boundingBoxIntersects(
            int aX1, int aX2, int aZ1, int aZ2, int bX1, int bX2, int bZ1, int bZ2) {
        boolean xIntersects = aX1 < bX2 && bX1 < aX2;
        boolean zIntersects = aZ1 < bZ2 && bZ1 < aZ2;
        return xIntersects && zIntersects;
    }
}
