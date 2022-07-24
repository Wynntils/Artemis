/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.features.overlays;

import com.wynntils.mc.render.HorizontalAlignment;
import com.wynntils.mc.render.VerticalAlignment;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.phys.Vec2;

public class OverlayPosition {

    protected static final Pattern POSITION_PATTERN = Pattern.compile(
            "OverlayPosition\\{verticalOffset=(.+),horizontalOffset=(.+),verticalAlignment=(.+),horizontalAlignment=(.+),anchorSection=(.+)}");

    private float verticalOffset;
    private float horizontalOffset;

    private VerticalAlignment verticalAlignment;
    private HorizontalAlignment horizontalAlignment;

    private AnchorSection anchorSection;

    public OverlayPosition(
            float verticalOffset,
            float horizontalOffset,
            VerticalAlignment verticalAlignment,
            HorizontalAlignment horizontalAlignment,
            AnchorSection anchorSection) {
        this.verticalAlignment = verticalAlignment;
        this.horizontalAlignment = horizontalAlignment;
        this.anchorSection = anchorSection;
        this.verticalOffset = verticalOffset;
        this.horizontalOffset = horizontalOffset;
    }

    public OverlayPosition(String string) {
        Matcher matcher = POSITION_PATTERN.matcher(string.replaceAll(" ", ""));

        if (!matcher.matches()) {
            throw new RuntimeException("Failed to parse OverlayPosition");
        }

        try {
            this.verticalOffset = Float.parseFloat(matcher.group(1));
            this.horizontalOffset = Float.parseFloat(matcher.group(2));
            this.verticalAlignment = VerticalAlignment.valueOf(matcher.group(3));
            this.horizontalAlignment = HorizontalAlignment.valueOf(matcher.group(4));
            this.anchorSection = AnchorSection.valueOf(matcher.group(5));
        } catch (IllegalArgumentException exception) {
            throw new RuntimeException("Failed to parse OverlayPosition");
        }
    }

    public AnchorSection getAnchorSection() {
        return anchorSection;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public float getHorizontalOffset() {
        return horizontalOffset;
    }

    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    public float getVerticalOffset() {
        return verticalOffset;
    }

    @Override
    public String toString() {
        return "OverlayPosition{" + "verticalOffset="
                + verticalOffset + ", horizontalOffset="
                + horizontalOffset + ", verticalAlignment="
                + verticalAlignment + ", horizontalAlignment="
                + horizontalAlignment + ", anchorSection="
                + anchorSection + '}';
    }

    public static OverlayPosition getBestPositionFor(
            Overlay overlay, float oldRenderX, float oldRenderY, float offsetX, float offsetY) {
        Vec2 middleOfOverlay =
                new Vec2(overlay.getRenderX() + overlay.getWidth() / 2, overlay.getRenderY() + overlay.getHeight() / 2);

        // 1. Get the best section (section with the center point of overlay)
        AnchorSection section = Arrays.stream(AnchorSection.values())
                .filter(anchorSection ->
                        OverlayManager.getSection(anchorSection).overlaps(middleOfOverlay.x, middleOfOverlay.y))
                .findAny()
                .orElse(AnchorSection.Middle);
        SectionCoordinates sectionCoordinates = OverlayManager.getSection(section);

        // 2. Calculate the best alignment inside the section
        HorizontalAlignment horizontalAlignment = HorizontalAlignment.Left;

        if ((sectionCoordinates.x1() + sectionCoordinates.x2()) / 2 == (int) middleOfOverlay.x) {
            horizontalAlignment = HorizontalAlignment.Center;
        } else if (Math.abs(sectionCoordinates.x2() - middleOfOverlay.x)
                < Math.abs(sectionCoordinates.x1() - middleOfOverlay.x)) {
            horizontalAlignment = HorizontalAlignment.Right;
        }

        VerticalAlignment verticalAlignment = VerticalAlignment.Top;

        if ((sectionCoordinates.y1() + sectionCoordinates.y2()) / 2 == (int) middleOfOverlay.y) {
            verticalAlignment = VerticalAlignment.Middle;
        } else if (Math.abs(sectionCoordinates.y2() - middleOfOverlay.y)
                < Math.abs(sectionCoordinates.y1() - middleOfOverlay.y)) {
            verticalAlignment = VerticalAlignment.Bottom;
        }

        // 3. Calculate render positions for new alignment
        OverlayPosition newOverlayPositionTemp =
                new OverlayPosition(0, 0, verticalAlignment, horizontalAlignment, section);

        float renderX = Overlay.getRenderX(newOverlayPositionTemp, overlay);
        float renderY = Overlay.getRenderY(newOverlayPositionTemp, overlay);

        // 4. Calculate the alignment offsets to match the current render position, but factor in argument offsets
        return new OverlayPosition(
                oldRenderY - renderY + offsetY,
                oldRenderX - renderX + offsetX,
                verticalAlignment,
                horizontalAlignment,
                section);
    }

    public enum AnchorSection {
        TopLeft(0),
        TopMiddle(1),
        TopRight(2),
        MiddleLeft(3),
        Middle(4),
        MiddleRight(5),
        BottomLeft(6),
        BottomMiddle(7),
        BottomRight(8);

        private final int index;

        AnchorSection(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }
}
