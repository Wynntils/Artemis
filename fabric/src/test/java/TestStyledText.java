/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.type.IterationDecision;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestStyledText {
    @BeforeAll
    public static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void advancedComponentTree_shouldProduceCorrectString() {
        final Component component = Component.empty()
                .append(Component.literal("italicred")
                        .withStyle(ChatFormatting.ITALIC)
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal("blue")
                                .withStyle(ChatFormatting.BLUE)
                                .withStyle(style -> style.withHoverEvent(
                                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover")))))
                        .append(Component.literal("nonitalic").withStyle(Style.EMPTY.withItalic(false)))
                        .append(Component.literal("inherited")
                                .append(Component.literal("bold").withStyle(ChatFormatting.BOLD))))
                .append(Component.literal("after")
                        .withStyle(style ->
                                style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/command"))));

        final String expectedIncludeEvents = "§c§oitalicred§9§o§<1>blue§cnonitalic§oinherited§lbold§r§[1]after";
        final String expectedDefault = "§c§oitalicred§9§oblue§cnonitalic§oinherited§lbold§rafter";
        final String expectedNoFormat = "italicredbluenonitalicinheritedboldafter";

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertEquals(
                expectedIncludeEvents,
                styledText.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");
        Assertions.assertEquals(
                expectedDefault,
                styledText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString(DEFAULT) returned an unexpected value.");
        Assertions.assertEquals(
                expectedNoFormat,
                styledText.getString(PartStyle.StyleType.NONE),
                "StyledText.getString(NONE) returned an unexpected value.");
    }

    @Test
    public void fromStringDoesntParseEvents_worksCorrectly() {
        final String codedString = "§o§<1>inherited hover effect";
        final StyledText styledText = StyledText.fromString(codedString);

        // Make sure there are no events
        for (StyledTextPart styledTextPart : styledText) {
            Style style = styledTextPart.getPartStyle().getStyle();

            if (style.getClickEvent() != null || style.getHoverEvent() != null) {
                Assertions.fail(
                        "StyledText.fromString() parsed events when it should not have (since it does not have any underlying events).");
            }
        }

        // The output should look the same, as the event chars would be normal text
        Assertions.assertEquals(
                codedString,
                styledText.getString(),
                "StyledText.fromString() did not parse event-like text correctly.");
    }

    @Test
    public void simpleEventInheritance_worksCorrectly() {
        final Component component = Component.empty()
                .withStyle(ChatFormatting.ITALIC)
                .withStyle(style ->
                        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover"))))
                .append(Component.literal("inherited hover effect"));
        final String expected = "§o§<1>inherited hover effect";

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertEquals(
                expected,
                styledText.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");

        StyledText fromString = StyledText.fromModifiedString(expected, styledText);

        Assertions.assertEquals(
                expected,
                fromString.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");
    }

    @Test
    public void eventInheritsImplicitly_worksCorrectly() {
        final Component component = Component.empty()
                .withStyle(style ->
                        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover"))))
                .append(Component.literal("inherited hover effect"))
                .append(Component.literal("|inherited hover effect 2 without explicit |"))
                .append(Component.literal("after red effect").withStyle(ChatFormatting.RED));

        final String expected =
                "§<1>inherited hover effect|inherited hover effect 2 without explicit |§c§<1>after red effect";

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertEquals(
                expected,
                styledText.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");

        StyledText fromString = StyledText.fromModifiedString(expected, styledText);

        Assertions.assertEquals(
                expected,
                fromString.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");
    }

    @Test
    public void eventInheritsImplicitlyAndResetWorks_worksCorrectly() {
        final Component component = Component.empty()
                .withStyle(style ->
                        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover"))))
                .append(Component.literal("inherited hover effect"))
                .append(Component.literal("|inherited red hover effect 2 without explicit |")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal("after no effect").withStyle(ChatFormatting.ITALIC));

        final String expected =
                "§<1>inherited hover effect§c§<1>|inherited red hover effect 2 without explicit |§r§o§<1>after no effect";

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertEquals(
                expected,
                styledText.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");

        StyledText fromString = StyledText.fromModifiedString(expected, styledText);

        Assertions.assertEquals(
                expected,
                fromString.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");
    }

    @Test
    public void eventDoesntInheritIfItHasEvent_worksCorrectly() {
        final Component component = Component.empty()
                .withStyle(style ->
                        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover"))))
                .append(Component.literal("inherited hover effect"))
                .append(Component.literal("|explicit hover effect |")
                        .withStyle(style -> style.withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover2")))));

        final String expected = "§<1>inherited hover effect§<2>|explicit hover effect |";

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertEquals(
                expected,
                styledText.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");

        StyledText fromString = StyledText.fromModifiedString(expected, styledText);

        Assertions.assertEquals(
                expected,
                fromString.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");
    }

    @Test
    public void differentEventsStack_worksCorrectly() {
        final Component component = Component.empty()
                .withStyle(style -> style.withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/command")))
                .append(Component.literal("inherited hover effect"))
                .append(Component.literal("|explicit hover effect |")
                        .withStyle(style -> style.withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover2")))));

        final String expected = "§[1]§<1>inherited hover effect§<2>|explicit hover effect |";

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertEquals(
                expected,
                styledText.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");

        StyledText fromString = StyledText.fromModifiedString(expected, styledText);

        Assertions.assertEquals(
                expected,
                fromString.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");
    }

    @Test
    public void fromModifiedStringWithEvents_shouldProduceCorrectString() {
        final String stringWithEvents = "§c§oitalicred§9§o§<1>blue§cnonitalic§oinherited§l§<2>bold§r§[1]after";
        final List<HoverEvent> hoverEvents = List.of(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover")),
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover2")));
        final List<ClickEvent> clickEvents = List.of(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/command"));
        final StyledText originalText = StyledText.fromComponent(Component.empty()
                .append(Component.literal("italicred")
                        .withStyle(ChatFormatting.ITALIC)
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal("blue")
                                .withStyle(ChatFormatting.BLUE)
                                .withStyle(style -> style.withHoverEvent(hoverEvents.getFirst())))
                        .append(Component.literal("nonitalic").withStyle(Style.EMPTY.withItalic(false)))
                        .append(Component.literal("inherited")
                                .append(Component.literal("bold")
                                        .withStyle(ChatFormatting.BOLD)
                                        .withStyle(style -> style.withHoverEvent(hoverEvents.get(1))))))
                .append(Component.literal("after").withStyle(style -> style.withClickEvent(clickEvents.getFirst()))));

        StyledText styledText = StyledText.fromModifiedString(stringWithEvents, originalText);

        int hoverEventCount = 0;
        int clickEventCount = 0;
        for (StyledTextPart part : styledText) {
            if (part.getPartStyle().getStyle().getClickEvent() != null) {
                if (clickEventCount >= clickEvents.size()) {
                    Assertions.fail("StyledText.fromModifiedString() had too many click events.");
                }

                Assertions.assertEquals(
                        clickEvents.get(clickEventCount),
                        part.getPartStyle().getStyle().getClickEvent(),
                        "StyledText.fromModifiedString() did not inherit the correct click event.");

                clickEventCount++;
            }
            if (part.getPartStyle().getStyle().getHoverEvent() != null) {
                if (hoverEventCount >= hoverEvents.size()) {
                    Assertions.fail("StyledText.fromModifiedString() had too many hover events.");
                }

                Assertions.assertEquals(
                        hoverEvents.get(hoverEventCount),
                        part.getPartStyle().getStyle().getHoverEvent(),
                        "StyledText.fromModifiedString() did not inherit the correct hover event.");

                hoverEventCount++;
            }
        }

        Assertions.assertEquals(
                stringWithEvents,
                styledText.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");
    }

    @Test
    public void colorResetsFormatting_shouldProduceCorrectString() {
        // Any color code resets formatting, so the bold should be removed.

        final String testString = "§l§cboldthenred";

        final String expected = "§cboldthenred";

        StyledText styledText = StyledText.fromString(testString);

        Assertions.assertEquals(
                expected,
                styledText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");
    }

    @Test
    public void colorCodedStyledComponent_shouldProduceCorrectString() {
        final Component firstTestComponent = Component.literal("§credstring").withStyle(ChatFormatting.BOLD);
        // Any color code resets formatting, so the bold should be removed.
        final String firstExpected = "§credstring";

        final Component secondTestComponent = Component.literal("§lboldstring").withStyle(ChatFormatting.RED);
        // Style is applied first, so the color does not reset the bold.
        final String secondExpected = "§c§lboldstring";

        final Component thirdTestComponent = Component.literal("boldparent")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("coloredchild").withStyle(ChatFormatting.RED));
        // Style is applied first, so the color does not reset the bold.
        final String thirdExpected = "§lboldparent§c§lcoloredchild";

        final Component fourthTestComponent = Component.literal("bold")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("red").withStyle(ChatFormatting.RED))
                .append(Component.literal("unformatted"));
        final String fourthExpected = "§lbold§c§lred§r§lunformatted";

        final Component fifthTestComponent =
                Component.literal("§c§lredthenbold").append(Component.literal("unformattedchild"));
        final String fifthExpected = "§c§lredthenbold§runformattedchild";

        Assertions.assertEquals(
                firstExpected,
                StyledText.fromComponent(firstTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");

        Assertions.assertEquals(
                secondExpected,
                StyledText.fromComponent(secondTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");

        Assertions.assertEquals(
                thirdExpected,
                StyledText.fromComponent(thirdTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");

        Assertions.assertEquals(
                fourthExpected,
                StyledText.fromComponent(fourthTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");

        Assertions.assertEquals(
                fifthExpected,
                StyledText.fromComponent(fifthTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");
    }

    @Test
    public void normalizedStringCreation_shouldProduceCorrectString() {
        final String badText = "ÀÀHello,ÀÀÀWorld!֎";
        final String expected = "Hello, World!";

        StyledText styledText = StyledText.fromString(badText);

        Assertions.assertEquals(
                expected,
                styledText.getNormalized().getString(PartStyle.StyleType.NONE),
                "StyledText.getNormalized().getString() returned an unexpected value.");
    }

    @Test
    public void alignedStringStripping_shouldProduceCorrectString() {
        final String badText =
                "§c\uDAFF\uDFFC\uDB00\uDC06The war for Detlas Close Suburbs will \uDAFF\uDFFC\uDB00\uDC06start in 30 seconds.";
        final String expected = "§cThe war for Detlas Close Suburbs will start in 30 seconds.";

        StyledText styledText = StyledText.fromString(badText);

        Assertions.assertEquals(
                expected,
                styledText.stripAlignment().getString(PartStyle.StyleType.DEFAULT),
                "StyledText.stripAlignment().getString() returned an unexpected value.");
    }

    @Test
    public void normalStringStripping_shouldHaveNoEffect() {
        final String badText = "Hello, World!";
        final String expected = "Hello, World!";

        StyledText styledText = StyledText.fromString(badText);

        Assertions.assertEquals(
                expected,
                styledText.stripAlignment().getString(PartStyle.StyleType.DEFAULT),
                "StyledText.stripAlignment().getString() returned an unexpected value.");
    }

    @Test
    public void untrimmedText_shouldProduceTrimmedString() {
        final String badText = "   Hello, World!  ";
        final String expected = "Hello, World!";

        StyledText styledText = StyledText.fromString(badText);

        Assertions.assertEquals(
                expected,
                styledText.trim().getString(PartStyle.StyleType.NONE),
                "StyledText.getString() returned an unexpected value.");
    }

    @Test
    public void emptyText_shouldProduceEmptyString() {
        StyledText styledText = StyledText.fromString("");

        Assertions.assertTrue(styledText.isEmpty(), "StyledText.isEmpty() returned an unexpected value.");
    }

    @Test
    public void blankText_shouldProduceBlankString() {
        StyledText styledText = StyledText.fromString("    ");

        Assertions.assertTrue(styledText.isBlank(), "StyledText.isBlank() returned an unexpected value.");
    }

    @Test
    public void contains_shouldProduceCorrectResult() {
        final String testString = "§c§oitalicred§9§oblue§cnonitalic§oinherited§lbold§rafter";
        final String[] expectedParts = {
            "§c§oitalicred",
            "§9§oblue",
            "§cnonitalic",
            "§oinherited",
            "§lbold",
            "§rafter",
            "licred§9§oblue§cno",
            "erited§lbo"
        };

        StyledText styledText = StyledText.fromString(testString);

        for (String expectedPart : expectedParts) {
            Assertions.assertTrue(
                    styledText.contains(expectedPart),
                    "StyledText.contains() could not find a part matching: " + expectedPart);
        }

        for (String expectedPart : expectedParts) {
            Assertions.assertTrue(
                    styledText.contains(StyledText.fromString(expectedPart)),
                    "StyledText.contains() could not find a part matching: " + expectedPart);
            Assertions.assertTrue(
                    styledText.contains(StyledText.fromString(expectedPart), PartStyle.StyleType.NONE),
                    "StyledText.contains(NONE) could not find a part matching: " + expectedPart);
        }
    }

    @Test
    public void startsWith_shouldProduceCorrectResult() {
        final String testString = "§c§oitalicred§9§oblue§cnonitalic§oinherited§lbold§rafter";
        final String expectedStart = "§c§oitalicred";
        final String expectedMultipartStart = "§c§oitalicred§9§oblue§cnonit";

        StyledText styledText = StyledText.fromString(testString);

        Assertions.assertTrue(
                styledText.startsWith(expectedStart),
                "StyledText.startsWith() did not produce a correct result for: " + expectedStart);
        Assertions.assertTrue(
                styledText.startsWith(StyledText.fromString(expectedStart)),
                "StyledText.startsWith() did not produce a correct result for: " + expectedStart);

        Assertions.assertTrue(
                styledText.startsWith(expectedMultipartStart),
                "StyledText.startsWith() did not produce a correct result for: " + expectedMultipartStart);
        Assertions.assertTrue(
                styledText.startsWith(StyledText.fromString(expectedMultipartStart)),
                "StyledText.startsWith() did not produce a correct result for: " + expectedMultipartStart);
    }

    @Test
    public void endsWith_shouldProduceCorrectResult() {
        final String testString = "§c§oitalicred§9§oblue§cnonitalic§oinherited§lbold§rafter";
        final String expectedEnd = "§rafter";
        final String expectedMultipartEnd = "erited§lbold§rafter";

        StyledText styledText = StyledText.fromString(testString);

        Assertions.assertTrue(
                styledText.endsWith(expectedEnd),
                "StyledText.startsWith() did not produce a correct result for: " + expectedEnd);
        Assertions.assertTrue(
                styledText.endsWith(StyledText.fromString(expectedEnd)),
                "StyledText.startsWith() did not produce a correct result for: " + expectedEnd);

        Assertions.assertTrue(
                styledText.endsWith(expectedMultipartEnd),
                "StyledText.startsWith() did not produce a correct result for: " + expectedMultipartEnd);
        Assertions.assertTrue(
                styledText.endsWith(StyledText.fromString(expectedMultipartEnd)),
                "StyledText.startsWith() did not produce a correct result for: " + expectedMultipartEnd);
    }

    @Test
    public void join_shouldProduceCorrectResult() {
        final StyledText firstStyled = StyledText.fromString("Hello");
        final StyledText secondStyled = StyledText.fromString("World");

        final String expected = "Hello, World";

        StyledText joinedStyled = StyledText.join(", ", List.of(firstStyled, secondStyled));

        Assertions.assertEquals(
                expected,
                joinedStyled.getString(PartStyle.StyleType.NONE),
                "StyledText.join() did not produce a correct result.");
    }

    @Test
    public void concat_shouldProduceCorrectResult() {
        final StyledText firstStyled = StyledText.fromString("Hello, ");
        final StyledText secondStyled = StyledText.fromString("World");

        final String expected = "Hello, World";

        StyledText joinedStyled = StyledText.concat(List.of(firstStyled, secondStyled));

        Assertions.assertEquals(
                expected,
                joinedStyled.getString(PartStyle.StyleType.NONE),
                "StyledText.concat() did not produce a correct result.");
    }

    @Test
    public void append_shouldProduceCorrectResult() {
        final StyledText firstStyled = StyledText.fromString("Hello, ");
        final StyledText secondStyled = StyledText.fromString("World");

        final String expected = "Hello, World";

        StyledText joinedStyled = firstStyled.append(secondStyled);

        Assertions.assertEquals(
                expected,
                joinedStyled.getString(PartStyle.StyleType.NONE),
                "StyledText.append() did not produce a correct result.");
    }

    @Test
    public void prepend_shouldProduceCorrectResult() {
        final StyledText firstStyled = StyledText.fromString("Hello, ");
        final StyledText secondStyled = StyledText.fromString("World");

        final String expected = "Hello, World";

        StyledText joinedStyled = secondStyled.prepend(firstStyled);

        Assertions.assertEquals(
                expected,
                joinedStyled.getString(PartStyle.StyleType.NONE),
                "StyledText.prepend() did not produce a correct result.");
    }

    @Test
    public void styledText_shouldProduceCorrectComponent() {
        final Component component = Component.literal("a").append(Component.literal("b"));

        StyledText styledText = StyledText.fromComponent(component);

        // The reconstructed component differs in that StyledText
        // always adds components as a sibling to an empty component
        // Note: The comparison is done on the size of the lists, as Style has an equals method that does not
        //       behave like it's implementation (null != false, when in reality they are equal).
        Assertions.assertEquals(
                component.toFlatList().size(),
                styledText.getComponent().toFlatList().size(),
                "StyledText.getComponent() returned an unexpected value.");
    }

    @Test
    public void styledText_shouldProduceCorrectMatcher() {
        final Component component = Component.literal("This is a test string, where there are ")
                .append(Component.literal("multiple").withStyle(ChatFormatting.BOLD))
                .append(Component.literal(" components."));

        final Pattern unformattedPattern = Pattern.compile("This is a test string, where there are (.+) components\\.");
        final Pattern formattedPattern =
                Pattern.compile("This is a test string, where there are §l(.+)§r components\\.");

        final String expectedMatch = "multiple";

        StyledText styledText = StyledText.fromComponent(component);

        Matcher formattedMatcher = styledText.getMatcher(formattedPattern);
        Matcher unformattedMatcher = styledText.getMatcher(unformattedPattern, PartStyle.StyleType.NONE);

        Assertions.assertTrue(formattedMatcher.matches(), "StyledText.matches(DEFAULT) returned an unexpected value.");
        Assertions.assertEquals(
                expectedMatch,
                formattedMatcher.group(1),
                "StyledText.matches(DEFAULT).group() returned an unexpected value.");
        Assertions.assertTrue(unformattedMatcher.matches(), "StyledText.matches(NONE) returned an unexpected value.");
        Assertions.assertEquals(
                expectedMatch,
                unformattedMatcher.group(1),
                "StyledText.matches(NONE).group() returned an unexpected value.");
    }

    @Test
    public void styledText_shouldMatchCorrectly() {
        final Component component = Component.literal("This is a test string, where there are ")
                .append(Component.literal("multiple").withStyle(ChatFormatting.BOLD))
                .append(Component.literal(" components."));

        final Pattern unformattedPattern = Pattern.compile("This is a test string, where there are (.+) components\\.");
        final Pattern formattedPattern =
                Pattern.compile("This is a test string, where there are §l(.+)§r components\\.");

        final String expectedMatch = "multiple";

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertTrue(
                styledText.matches(formattedPattern), "StyledText.matches(DEFAULT) returned an unexpected value.");
        Assertions.assertTrue(
                styledText.matches(unformattedPattern, PartStyle.StyleType.NONE),
                "StyledText.matches(NONE) returned an unexpected value.");
    }

    @Test
    public void styledText_shouldSplitCorrectly() {
        final Component component = Component.literal("This is a test string, where there are ")
                .append(Component.literal("multiple").withStyle(ChatFormatting.BOLD))
                .append(Component.literal(" components."));

        StyledText styledText = StyledText.fromComponent(component);

        // Split on the word "multiple" to check style inheritance
        StyledText[] splitTexts = styledText.split("\\.|,|(tip)");

        String[] results = {"This is a test string", " where there are §lmul", "§lle§r components"};

        for (int i = 0; i < splitTexts.length; i++) {
            StyledText result = splitTexts[i];

            Assertions.assertEquals(
                    results[i],
                    result.getString(PartStyle.StyleType.DEFAULT),
                    "StyledText.split() returned an unexpected value.");
        }
    }

    @Test
    public void styledText_shouldNotSplitColor() {
        final Component component =
                Component.literal("This is a formatted test string.").withStyle(ChatFormatting.BOLD);

        StyledText styledText = StyledText.fromComponent(component);

        StyledText[] splitTexts = styledText.split("§");

        final String result = "§lThis is a formatted test string.";

        Assertions.assertEquals(1, splitTexts.length, "StyledText.split() returned an unexpected value.");
        Assertions.assertEquals(
                result,
                splitTexts[0].getString(PartStyle.StyleType.DEFAULT),
                "StyledText.split() returned an unexpected value.");
    }

    @Test
    public void styledText_substringShouldSkipBeginningCorrectly() {
        final Component component = Component.literal("This is a test string").withStyle(ChatFormatting.BOLD);

        StyledText styledText = StyledText.fromComponent(component);

        StyledText substringText = styledText.substring(5);

        final String result = "§lis a test string";

        Assertions.assertEquals(
                result,
                substringText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.substring() returned an unexpected value.");
    }

    @Test
    public void styledText_multipartSubstringShouldWork() {
        final Component component = Component.literal("a")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("bb"))
                .append(Component.literal("ccc"))
                .append(Component.literal("dddd"));

        StyledText styledText = StyledText.fromComponent(component);

        StyledText substringText = styledText.substring(1, 8);

        final String result = "§lbbcccdd";

        Assertions.assertEquals(
                result,
                substringText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.substring() returned an unexpected value.");
    }

    @Test
    public void styledText_substringShouldWork() {
        final StyledText text = StyledText.fromString("§1koala§2bear");

        StyledText substringText = text.substring(0, 4);

        String substring = substringText.getString();

        final String result = "§1koal";

        Assertions.assertEquals(result, substring, "StyledText.substring() returned an unexpected value.");
    }

    @Test
    public void styledText_substringWithFormattingShouldWork() {
        final StyledText text = StyledText.fromString("§1koala§2bear");

        StyledText substringText = text.substring(0, 4, PartStyle.StyleType.DEFAULT);

        String substring = substringText.getString();

        final String result = "§1ko";

        Assertions.assertEquals(result, substring, "StyledText.substring() returned an unexpected value.");
    }

    @Test
    public void styledText_substringWithFormattingShouldErrorOnSplitFormattingCode() {
        final StyledText text = StyledText.fromString("§1koala§2bear");

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> text.substring(0, 1, PartStyle.StyleType.DEFAULT),
                "StyledText.substring() did not throw an exception for split formatting code.");
    }

    @Test
    public void styledText_partitionShouldWork() {
        final StyledText text = StyledText.fromString("§1koala§2bear");

        StyledText[] separatedTexts = text.partition(6);

        String first = separatedTexts[0].getString();
        String second = separatedTexts[1].getString();

        final String firstResult = "§1koala§2b";
        final String secondResult = "§2ear";

        Assertions.assertEquals(firstResult, first, "StyledText.separate() returned an unexpected value.");
        Assertions.assertEquals(secondResult, second, "StyledText.separate() returned an unexpected value.");
    }

    @Test
    public void styledText_partitionMultipleIndexesShouldWork() {
        final StyledText text = StyledText.fromString("§1koala§2bear");

        StyledText[] separatedTexts = text.partition(1, 3, 6, 7);

        String first = separatedTexts[0].getString();
        String second = separatedTexts[1].getString();
        String third = separatedTexts[2].getString();
        String fourth = separatedTexts[3].getString();
        String fifth = separatedTexts[4].getString();

        final String firstResult = "§1k";
        final String secondResult = "§1oa";
        final String thirdResult = "§1la§2b";
        final String fourthResult = "§2e";
        final String fifthResult = "§2ar";

        Assertions.assertEquals(firstResult, first, "StyledText.separate() returned an unexpected value.");
        Assertions.assertEquals(secondResult, second, "StyledText.separate() returned an unexpected value.");
        Assertions.assertEquals(thirdResult, third, "StyledText.separate() returned an unexpected value.");
        Assertions.assertEquals(fourthResult, fourth, "StyledText.separate() returned an unexpected value.");
        Assertions.assertEquals(fifthResult, fifth, "StyledText.separate() returned an unexpected value.");
    }

    @Test
    public void styledText_partitionWithUnorderedIndexesShouldError() {
        final StyledText text = StyledText.fromString("§1koala§2bear");

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> text.partition(3, 1),
                "StyledText.separate() did not throw an exception for unordered indexes.");
    }

    @Test
    public void styledText_replaceShouldOnlyReplaceFirstOccurence() {
        final Component component = Component.literal("a")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("bb"))
                .append(Component.literal("ccc"))
                .append(Component.literal("dddd"));

        StyledText styledText = StyledText.fromComponent(component);

        StyledText replacedText = styledText.replaceFirst("b", "x");

        final String result = "§laxbcccdddd";

        Assertions.assertEquals(
                result,
                replacedText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.replace() returned an unexpected value.");
    }

    @Test
    public void styledText_replaceAllShouldReplaceAllOccurences() {
        final Component component = Component.literal("a")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("bb"))
                .append(Component.literal("ccc"))
                .append(Component.literal("dddd"));

        StyledText styledText = StyledText.fromComponent(component);

        StyledText replacedText = styledText.replaceAll("[b|c]", "x");

        final String result = "§laxxxxxdddd";

        Assertions.assertEquals(
                result,
                replacedText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.replaceAll() returned an unexpected value.");
    }

    @Test
    public void styledText_withoutFormattingShouldReplaceStripFormatting() {
        final Component component = Component.literal("a")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("bb"))
                .append(Component.literal("ccc").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.ITALIC))
                .append(Component.literal("dddd").withStyle(ChatFormatting.UNDERLINE));

        StyledText styledText = StyledText.fromComponent(component).withoutFormatting();

        final String result = "abbcccdddd";

        Assertions.assertEquals(
                result,
                styledText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.replaceAll() returned an unexpected value.");

        Assertions.assertEquals(
                result,
                styledText.getString(PartStyle.StyleType.NONE),
                "StyledText.replaceAll() returned an unexpected value.");
    }

    @Test
    public void styledText_getStringWithNonChatFormattingColors() {
        final CustomColor color = new CustomColor(36, 12, 42);
        final Component component = Component.literal("test").withStyle(style -> style.withColor(color.asInt()));

        StyledText styledText = StyledText.fromComponent(component);

        final String result = "§#240c2afftest";
        Assertions.assertEquals(
                result,
                styledText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");
    }

    @Test
    public void styledText_fromStringWithNonChatFormattingColors() {
        final CustomColor color = new CustomColor(36, 12, 42).withAlpha(255);
        final StyledText styledText = StyledText.fromComponent(Component.literal("test"))
                .iterate((part, changes) -> {
                    changes.remove(part);
                    changes.add(part.withStyle(style -> style.withColor(color)));
                    return IterationDecision.CONTINUE;
                });

        StyledTextPart firstPart = styledText.getFirstPart();
        Assertions.assertNotNull(firstPart, "StyledText.fromString() did not produce a part.");

        CustomColor textColor = firstPart.getPartStyle().getColor();

        Assertions.assertEquals(color, textColor, "StyledText.fromString() returned an unexpected value.");
    }

    @Test
    public void styledText_inheritsHoverEvents() {
        final Component component = Component.empty()
                .withStyle(style ->
                        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover"))))
                .append(Component.literal("inherited hover effect"));

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertEquals(
                component.getStyle().getHoverEvent(),
                styledText.getComponent().toFlatList().getFirst().getStyle().getHoverEvent(),
                "StyledText.fromComponent() did not inherit the correct hover event.");
    }
}
