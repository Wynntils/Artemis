/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
import com.wynntils.utils.colors.CustomColor;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestCustomColor {
    @BeforeAll
    public static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void customColor_toHexStringWorks() {
        final CustomColor color = CustomColor.fromInt(11141290).withAlpha(255);

        final String expected = "#aa00aaff";

        String result = color.toHexString();

        Assertions.assertEquals(expected, result, "CustomColor#toHexString() did not return the expected value");
    }

    @Test
    public void customColor_toHexStringWithAlphaWorks() {
        final CustomColor color = CustomColor.fromInt(11141290).withAlpha(170);

        final String expected = "#aa00aaaa";

        String result = color.toHexString();

        Assertions.assertEquals(expected, result, "CustomColor#toHexString() did not return the expected value");
    }

    @Test
    public void customColor_toHexStringWithLeadingZeroWorks() {
        final CustomColor color = CustomColor.fromInt(0x00aabb).withAlpha(255);

        final String expected = "#00aabbff";

        String result = color.toHexString();

        Assertions.assertEquals(expected, result, "CustomColor#toHexString() did not return the expected value");
    }

    @Test
    public void customColor_toHexStringWithSingleDigitAlphaWorks() {
        final CustomColor color = CustomColor.fromInt(11141290).withAlpha(1);

        final String expected = "#aa00aa01";

        String result = color.toHexString();

        Assertions.assertEquals(expected, result, "CustomColor#toHexString() did not return the expected value");
    }

    @Test
    public void customColor_fromHexStringWorks() {
        final String hex = "#bbcdaa";

        final CustomColor expected = CustomColor.fromInt(12307882).withAlpha(255);

        CustomColor result = CustomColor.fromHexString(hex);

        Assertions.assertEquals(expected, result, "CustomColor#fromHexString() did not return the expected value");
    }

    @Test
    public void customColor_fromHexStringWithAlphaWorks() {
        final String hex = "#bbcdaaaa";

        final CustomColor expected = CustomColor.fromInt(12307882).withAlpha(170);

        CustomColor result = CustomColor.fromHexString(hex);

        Assertions.assertEquals(expected, result, "CustomColor#fromHexString() did not return the expected value");
    }

    @Test
    public void customColor_fromRGBWorks() {
        final CustomColor color = new CustomColor(0, 255, 255);

        final int expected = 0xff00ffff;

        Assertions.assertEquals(expected, color.asInt(), "CustomColor#init() did not return the expected value");
    }

    @Test
    public void customColor_fromIntWithoutAlphaWorks() {
        final int color = 0x00aabb;

        final CustomColor expected = new CustomColor(0, 170, 187, 255);

        CustomColor result = CustomColor.fromInt(color);

        Assertions.assertEquals(expected, result, "CustomColor#fromInt() did not return the expected value");
    }

    @Test
    public void customColor_fromIntWithAlphaWorks() {
        final int color = 0xcc00aabb;

        final CustomColor expected = new CustomColor(0, 170, 187, 204);

        CustomColor result = CustomColor.fromInt(color);

        Assertions.assertEquals(expected, result, "CustomColor#fromInt() did not return the expected value");
    }
}
