/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;

public final class StringUtils {
    private static final String[] SUFFIXES = {"", "k", "m", "b", "t"}; // kilo, million, billion, trillion (short scale)
    private static final DecimalFormat FRACTIONAL_FORMAT = new DecimalFormat("#.#");
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * Converts a delimited list into a {@link java.util.List} of strings
     *
     * <p>e.g. "1, 2, 3, 4," for delimiter "," -> returns a list of "1", "2", "3", "4"
     */
    public static List<String> parseStringToList(String input, String delimiter) {
        List<String> result = new ArrayList<>();

        for (String element : input.split(Pattern.quote(delimiter))) {
            result.add(element.strip());
        }

        return result;
    }

    public static List<String> parseStringToList(String input) {
        return parseStringToList(input, ",");
    }

    public static String capitalizeFirst(String input) {
        if (input.isEmpty()) return "";
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    public static String uncapitalizeFirst(String input) {
        if (input.isEmpty()) return "";
        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }

    public static String capitalized(String input) {
        if (input.isEmpty()) return "";
        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Format an integer to have SI suffixes, if it is sufficiently large
     */
    public static String integerToShortString(int count) {
        if (count < 1000) return Integer.toString(count);
        int exp = (int) (Math.log(count) / Math.log(1000));
        DecimalFormat format = new DecimalFormat("0.#");
        String value = format.format(count / Math.pow(1000, exp));
        return String.format("%s%c", value, "kMBTPE".charAt(exp - 1));
    }

    public static String encodeUrl(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    public static String createSlug(String input) {
        // based on https://stackoverflow.com/a/1657250
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    public static String formatAmount(double value) {
        if (value < 0.75) return "0";

        int suffix = 0;
        while (suffix < SUFFIXES.length && value >= 750) {
            value /= 1000;
            ++suffix;
        }

        return FRACTIONAL_FORMAT.format(value) + SUFFIXES[suffix];
    }

    /**
     * Matches a string to a specific search term
     */
    public static boolean partialMatch(String toMatchStr, String searchTermStr) {
        String searchTerm = searchTermStr.toLowerCase(Locale.ROOT);
        String toMatch = toMatchStr.toLowerCase(Locale.ROOT);

        int firstIndexToMatch = 0;

        for (int i = 0; i < searchTerm.length(); i++) {
            char currentChar = searchTerm.charAt(i);

            int indexOfFirstMatch = toMatch.indexOf(currentChar, firstIndexToMatch);

            if (indexOfFirstMatch == -1) {
                return false;
            }

            firstIndexToMatch = indexOfFirstMatch + 1;
        }

        return true;
    }

    /** This is slightly less generous in allowing a match than partialMatch,
     * but not as strict as a contains(). It will require all sequences of characters
     * in the searchTerm to be consecutive in the toMatch string as well, until
     * a whitespace is encountered. E.g. "t vo" would match "The void", but "tvo" would not.
     */
    public static boolean initialMatch(String toMatch, String searchTerm) {
        String lookAt = toMatch.toLowerCase(Locale.ROOT);
        String searchFor = searchTerm.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        // Turn all spaces into .*, including start and end
        String regex = (" " + searchFor + " ").replace(" ", ".*");
        return lookAt.matches(regex);
    }

    // Convenience wrappers due to class name collision
    public static boolean containsIgnoreCase(String str, String searchStr) {
        return org.apache.commons.lang3.StringUtils.containsIgnoreCase(str, searchStr);
    }

    public static String substringBeforeLast(String str, String separator) {
        return org.apache.commons.lang3.StringUtils.substringBeforeLast(str, separator);
    }

    public static String toSignedString(int value) {
        if (value >= 0) {
            return "+" + value;
        } else {
            return Integer.toString(value);
        }
    }

    public static String convertMarkdownToColorCode(String input) {
        return ChatFormatting.RESET + input.replaceFirst("#+\\s+", String.valueOf(ChatFormatting.BOLD));
    }

    public static ByteBuffer decodeBase64(String base64) {
        if (base64 == null) return null;

        return Base64.getDecoder()
                .decode(ByteBuffer.wrap(base64.replaceAll("\n", "").getBytes(StandardCharsets.UTF_8)));
    }

    public static int tryParseInt(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
