/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.activities.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.caves.CaveInfo;
import com.wynntils.models.activities.type.ActivityType;
import com.wynntils.screens.activities.WynntilsCaveScreen;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.maps.MainMapScreen;
import com.wynntils.utils.EnumUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class CaveButton extends WynntilsButton implements TooltipProvider {
    private static final Pair<CustomColor, CustomColor> BUTTON_COLOR =
            Pair.of(new CustomColor(181, 174, 151), new CustomColor(121, 116, 101));
    private static final Pair<CustomColor, CustomColor> TRACKED_BUTTON_COLOR =
            Pair.of(new CustomColor(176, 197, 148), new CustomColor(126, 211, 106));
    private static final Pair<CustomColor, CustomColor> TRACKING_REQUESTED_BUTTON_COLOR =
            Pair.of(new CustomColor(255, 206, 127), new CustomColor(255, 196, 50));

    private final CaveInfo caveInfo;
    private final WynntilsCaveScreen caveScreen;

    private List<Component> tooltipLines;

    public CaveButton(int x, int y, int width, int height, CaveInfo caveInfo, WynntilsCaveScreen screen) {
        super(x, y, width, height, Component.literal("Cave Button"));
        this.caveInfo = caveInfo;
        this.caveScreen = screen;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        CustomColor backgroundColor = getBackgroundColor();
        RenderUtils.drawRect(poseStack, backgroundColor, this.getX(), this.getY(), 0, this.width, this.height);

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(caveInfo.name()),
                        this.getX() + 14,
                        this.getY() + 1,
                        this.width - 15,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE,
                        1f);

        Texture stateTexture =
                switch (caveInfo.status()) {
                    case STARTED -> Texture.ACTIVITY_STARTED;
                    case COMPLETED -> Texture.ACTIVITY_FINISHED;
                    case AVAILABLE -> Texture.CAVE_AVALIABLE_ICON;
                    case UNAVAILABLE -> Texture.ACTIVITY_CANNOT_START;
                };

        RenderUtils.drawTexturedRect(
                poseStack,
                stateTexture.resource(),
                this.getX() + 1,
                this.getY() + 1,
                stateTexture.width(),
                stateTexture.height(),
                stateTexture.width(),
                stateTexture.height());
    }

    private CustomColor getBackgroundColor() {
        Pair<CustomColor, CustomColor> colors;

        if (this.caveInfo.equals(Models.Activity.getTrackedCaveInfo())) {
            colors = TRACKED_BUTTON_COLOR;
        } else if (this.caveInfo.equals(caveScreen.getTrackingRequested())) {
            colors = TRACKING_REQUESTED_BUTTON_COLOR;
        } else {
            colors = BUTTON_COLOR;
        }

        return (this.isHovered ? colors.b() : colors.a());
    }

    // Not called
    @Override
    public void onPress() {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            trackCave();
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            Optional<Location> nextLocation = this.caveInfo.getNextLocation();

            nextLocation.ifPresent(location -> McUtils.mc().setScreen(MainMapScreen.create(location.x, location.z)));
        }

        return true;
    }

    private void trackCave() {
        if (this.caveInfo.isTrackable()) {
            McUtils.playSoundUI(SoundEvents.ANVIL_LAND);
            if (this.caveInfo.equals(Models.Activity.getTrackedCaveInfo())) {
                Models.Activity.stopTracking();
                caveScreen.setTrackingRequested(null);
            } else {
                Models.Activity.startTracking(this.caveInfo.name(), ActivityType.CAVE);
                caveScreen.setTrackingRequested(this.caveInfo);
            }
        }
    }

    public CaveInfo getCaveInfo() {
        return caveInfo;
    }

    @Override
    public List<Component> getTooltipLines() {
        if (tooltipLines == null) {
            tooltipLines = generateTooltipLines();
        }

        List<Component> lines = new ArrayList<>(tooltipLines);

        lines.add(Component.literal(""));

        if (caveInfo.isTrackable()) {
            if (caveInfo.equals(Models.Activity.getTrackedCaveInfo())) {
                lines.add(Component.literal("Left click to stop tracking it!")
                        .withStyle(ChatFormatting.RED)
                        .withStyle(ChatFormatting.BOLD));
            } else {
                lines.add(Component.literal("Left click to track it!")
                        .withStyle(ChatFormatting.GREEN)
                        .withStyle(ChatFormatting.BOLD));
            }
        }

        lines.add(Component.literal("Middle click to view on map!")
                .withStyle(ChatFormatting.YELLOW)
                .withStyle(ChatFormatting.BOLD));

        return lines;
    }

    private List<Component> generateTooltipLines() {
        List<Component> tooltipLines = new ArrayList<>();

        tooltipLines.add(Component.literal(caveInfo.name())
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.GOLD));
        tooltipLines.add(
                switch (caveInfo.status()) {
                    case AVAILABLE, STARTED -> Component.literal("Can be explored")
                            .withStyle(ChatFormatting.YELLOW);
                    case UNAVAILABLE -> Component.literal("Cannot be explored").withStyle(ChatFormatting.RED);
                    case COMPLETED -> Component.literal("Completed").withStyle(ChatFormatting.GREEN);
                });
        tooltipLines.add(Component.literal(""));

        tooltipLines.add((Models.CombatXp.getCombatLevel().current() >= caveInfo.recommendedLevel()
                        ? Component.literal("✔").withStyle(ChatFormatting.GREEN)
                        : Component.literal("✖").withStyle(ChatFormatting.RED))
                .append(Component.literal(" Recommended Combat Lv. Min: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(caveInfo.recommendedLevel()))
                        .withStyle(ChatFormatting.WHITE)));

        tooltipLines.add(Component.literal("-")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" Length: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(EnumUtils.toNiceString(caveInfo.length()))
                        .withStyle(ChatFormatting.WHITE)));

        tooltipLines.add(Component.literal("-")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" Difficulty: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(EnumUtils.toNiceString(caveInfo.difficulty()))
                        .withStyle(ChatFormatting.WHITE)));

        tooltipLines.add(Component.literal("-")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" Distance: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(EnumUtils.toNiceString(caveInfo.distance()))
                        .withStyle(ChatFormatting.WHITE)));

        tooltipLines.add(Component.literal(""));

        tooltipLines.addAll(ComponentUtils.splitComponent(
                Component.literal(caveInfo.description()).withStyle(ChatFormatting.GRAY), 150));

        tooltipLines.add(Component.literal(""));

        // rewards
        tooltipLines.add(Component.literal("Rewards:").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (String reward : caveInfo.rewards()) {
            tooltipLines.add(Component.literal("- ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(reward).withStyle(ChatFormatting.GRAY)));
        }

        return tooltipLines;
    }
}
