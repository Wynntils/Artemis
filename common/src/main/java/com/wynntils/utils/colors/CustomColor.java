/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.colors;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.wynntils.utils.MathUtils;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import net.minecraft.ChatFormatting;

public class CustomColor {
    public static final CustomColor NONE = new CustomColor(-1, -1, -1, -1);

    private static final Pattern HEX_PATTERN = Pattern.compile("#?([0-9a-fA-F]{6})([0-9a-fA-F]{2})?");
    private static final Pattern STRING_PATTERN = Pattern.compile("rgba\\((\\d+),(\\d+),(\\d+),(\\d+)\\)");
    private static final Map<String, CustomColor> REGISTERED_HASHED_COLORS = new HashMap<>();

    public final int r;
    public final int g;
    public final int b;
    public final int a;

    public CustomColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public CustomColor(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public CustomColor(float r, float g, float b) {
        this(r, g, b, 1f);
    }

    public CustomColor(float r, float g, float b, float a) {
        this.r = (int) (r * 255);
        this.g = (int) (g * 255);
        this.b = (int) (b * 255);
        this.a = (int) (a * 255);
    }

    public CustomColor(CustomColor color) {
        this(color.r, color.g, color.b, color.a);
    }

    public CustomColor(CustomColor color, int alpha) {
        this(color.r, color.g, color.b, alpha);
    }

    public CustomColor(String toParse) {
        String noSpace = toParse.replace(" ", "");

        CustomColor parseTry = CustomColor.fromString(noSpace);

        if (parseTry == CustomColor.NONE) {
            parseTry = CustomColor.fromHexString(noSpace);

            if (parseTry == CustomColor.NONE) {
                throw new RuntimeException("Failed to parse CustomColor");
            }
        }

        this.r = parseTry.r;
        this.g = parseTry.g;
        this.b = parseTry.b;
        this.a = parseTry.a;
    }

    public static CustomColor fromChatFormatting(ChatFormatting cf) {
        return fromInt(cf.getColor() | 0xFF000000);
    }

    /**
     * This method takes a color in the format 0x(AA)RRGGBB.
     * If the alpha is not set, it will be set to 255.
     * If the alpha is set to 0, it will be set to 255. If 0 is desired, use {@link CustomColor#withAlpha(int)}
     * @param num the color
     * @return the color
     */
    public static CustomColor fromInt(int num) {
        // if alpha is not set, set it to 255
        if ((num & 0xFF000000) == 0) num |= 0xFF000000;
        return new CustomColor(num >> 16 & 255, num >> 8 & 255, num & 255, num >> 24 & 255);
    }

    public static CustomColor fromHSV(float h, float s, float v, float a) {
        a = MathUtils.clamp(a, 0, 1);
        if (v <= 0) return new CustomColor(0, 0, 0, a);
        if (v > 1) v = 1;
        if (s <= 0) return new CustomColor(v, v, v, a);
        if (s > 1) s = 1;

        float vh = ((h % 1 + 1) * 6) % 6;

        int vi = MathUtils.floor(vh);
        float v1 = v * (1 - s);
        float v2 = v * (1 - s * (vh - vi));
        float v3 = v * (1 - s * (1 - (vh - vi)));

        return switch (vi) {
            case 0 -> new CustomColor(v, v3, v1, a);
            case 1 -> new CustomColor(v2, v, v1, a);
            case 2 -> new CustomColor(v1, v, v3, a);
            case 3 -> new CustomColor(v1, v2, v, a);
            case 4 -> new CustomColor(v3, v1, v, a);
            default -> new CustomColor(v, v1, v2, a);
        };
    }

    /** "#rrggbb(aa)" or "rrggbb(aa)" */
    public static CustomColor fromHexString(String hex) {
        Matcher hexMatcher = HEX_PATTERN.matcher(hex.trim());

        // invalid format
        if (!hexMatcher.matches()) return CustomColor.NONE;

        // parse hex
        if (hexMatcher.group(2) == null) {
            return fromInt(Integer.parseInt(hexMatcher.group(1), 16)).withAlpha(255);
        } else {
            return fromInt(Integer.parseInt(hexMatcher.group(1), 16))
                    .withAlpha(Integer.parseInt(hexMatcher.group(2), 16));
        }
    }

    /** "rgba(r,g,b,a)" format as defined in toString() */
    public static CustomColor fromString(String string) {
        Matcher stringMatcher = STRING_PATTERN.matcher(string.trim());

        // invalid format
        if (!stringMatcher.matches()) return CustomColor.NONE;

        return new CustomColor(
                Integer.parseInt(stringMatcher.group(1)),
                Integer.parseInt(stringMatcher.group(2)),
                Integer.parseInt(stringMatcher.group(3)),
                Integer.parseInt(stringMatcher.group(4)));
    }

    /**
     * Generates a Color based in the input string
     * The color will be always the same if the string is the same
     *
     * @param input the input stream
     * @return the color
     */
    public static CustomColor colorForStringHash(String input) {
        if (REGISTERED_HASHED_COLORS.containsKey(input)) return REGISTERED_HASHED_COLORS.get(input);

        CRC32 crc32 = new CRC32();
        crc32.update(input.getBytes(StandardCharsets.UTF_8));

        RandomGenerator random = new Random(crc32.getValue());
        final float minS = .5f;
        final float minV = .75f;
        CustomColor color = fromHSV(
                random.nextFloat(),
                minS + (1f - minS) * random.nextFloat(),
                minV + (1f - minV) * random.nextFloat(),
                1f);
        REGISTERED_HASHED_COLORS.put(input, color);

        return color;
    }

    public CustomColor withAlpha(int a) {
        return new CustomColor(this, a);
    }

    public CustomColor withAlpha(float a) {
        return new CustomColor(this, (int) (a * 255));
    }

    /** 0xAARRGGBB format */
    public int asInt() {
        int a = Math.min(this.a, 255);
        int r = Math.min(this.r, 255);
        int g = Math.min(this.g, 255);
        int b = Math.min(this.b, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public float[] asFloatArray() {
        return new float[] {r / 255f, g / 255f, b / 255f};
    }

    /**
     * #rrggbbaa format
     * The alpha is always included, so it can be parsed more easily
     */
    public String toHexString() {
        return "#" + String.format("%08x", ((r << 24) | (g << 16) | (b << 8) | a));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CustomColor color)) return false;

        // colors are equal as long as rgba values match
        return (this.r == color.r && this.g == color.g && this.b == color.b && this.a == color.a);
    }

    @Override
    public String toString() {
        return toHexString();
    }

    public static class CustomColorSerializer implements JsonSerializer<CustomColor>, JsonDeserializer<CustomColor> {
        @Override
        public CustomColor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            CustomColor customColor = CustomColor.fromHexString(json.getAsString());
            return customColor == NONE ? CustomColor.fromString(json.getAsString()) : customColor;
        }

        @Override
        public JsonElement serialize(CustomColor src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.toString());
        }
    }
}
