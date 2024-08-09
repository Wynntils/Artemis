/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.statistics.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.statistics.WynntilsStatisticsScreen;
import com.wynntils.services.statistics.type.StatisticEntry;
import com.wynntils.services.statistics.type.StatisticKind;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class StatisticButton extends WynntilsButton implements TooltipProvider {
    private static final CustomColor BUTTON_COLOR = new CustomColor(181, 174, 151);
    private static final CustomColor BUTTON_COLOR_HOVERED = new CustomColor(121, 116, 101);
    private static final CustomColor TRACKED_BUTTON_COLOR = new CustomColor(176, 197, 148);
    private static final CustomColor TRACKED_BUTTON_COLOR_HOVERED = new CustomColor(126, 211, 106);

    private final StatisticKind statistic;
    private final WynntilsStatisticsScreen screen;

    public StatisticButton(
            int x, int y, int width, int height, StatisticKind statistic, WynntilsStatisticsScreen screen) {
        super(x, y, width, height, Component.literal("Statistics Button"));
        this.statistic = statistic;
        this.screen = screen;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        CustomColor backgroundColor = getButtonBackgroundColor();
        RenderUtils.drawRect(poseStack, backgroundColor, this.getX(), this.getY(), 0, this.width, this.height);

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(statistic.getName()),
                        this.getX() + 2,
                        this.getY() + 1,
                        this.width - 3,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE,
                        1f);
    }

    private CustomColor getButtonBackgroundColor() {
        if (isSelected()) {
            return isHovered ? TRACKED_BUTTON_COLOR_HOVERED : TRACKED_BUTTON_COLOR;
        } else {
            return isHovered ? BUTTON_COLOR_HOVERED : BUTTON_COLOR;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isSelected()) {
                screen.setHighlightedStatisticKind(null);
            } else {
                screen.setHighlightedStatisticKind(this.statistic);
            }
            return true;
        }

        if (KeyboardUtils.isShiftDown() && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (Services.Statistics.screenOverallMode.get()) {
                Services.Statistics.resetStatisticOverall(statistic);
            } else {
                Services.Statistics.resetStatisticForCharacter(statistic);
            }
            screen.reloadElements();
            return true;
        }

        return true;
    }

    @Override
    public void onPress() {}

    @Override
    public List<Component> getTooltipLines() {
        List<Component> lines = new ArrayList<>();

        StatisticEntry entry = Services.Statistics.screenOverallMode.get()
                ? Services.Statistics.getOverallStatistic(statistic)
                : Services.Statistics.getStatistic(statistic);

        lines.add(Component.literal(statistic.getName()).withStyle(ChatFormatting.BOLD));
        lines.add(Component.translatable(
                "screens.wynntils.statistics.total", statistic.getFormattedValue(entry.total())));
        lines.add(Component.translatable(
                "screens.wynntils.statistics.lastModified",
                entry.lastModified() == 0 ? "-" : StringUtils.formatDateTime(entry.lastModified())));
        lines.add(Component.empty());

        if (isSelected()) {
            lines.add(Component.translatable("screens.wynntils.statistics.leftClickToUnselect")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.translatable("screens.wynntils.statistics.leftClickToView")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.GREEN));
        }

        lines.add((Services.Statistics.screenOverallMode.get()
                        ? Component.translatable("screens.wynntils.statistics.shiftMiddleClickToResetOverall")
                        : Component.translatable("screens.wynntils.statistics.shiftMiddleClickToResetCurrent"))
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.RED));

        return ComponentUtils.wrapTooltips(lines, 250);
    }

    private boolean isSelected() {
        return screen.getHighlightedStatisticKind() == this.statistic;
    }
}
