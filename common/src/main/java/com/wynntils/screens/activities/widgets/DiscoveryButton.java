/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.activities.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.discoveries.DiscoveryInfo;
import com.wynntils.models.activities.type.ActivitySortOrder;
import com.wynntils.models.activities.type.DiscoveryType;
import com.wynntils.screens.activities.WynntilsDiscoveriesScreen;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class DiscoveryButton extends WynntilsButton implements TooltipProvider {
    private static final CustomColor BUTTON_COLOR = new CustomColor(181, 174, 151);
    private static final CustomColor BUTTON_COLOR_HOVERED = new CustomColor(121, 116, 101);

    private final DiscoveryInfo discoveryInfo;
    private final WynntilsDiscoveriesScreen discoveriesScreen;

    public DiscoveryButton(
            int x, int y, int width, int height, DiscoveryInfo discoveryInfo, WynntilsDiscoveriesScreen screen) {
        super(x, y, width, height, Component.literal("Discovery Button"));
        this.discoveryInfo = discoveryInfo;
        this.discoveriesScreen = screen;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        CustomColor backgroundColor = this.isHovered ? BUTTON_COLOR_HOVERED : BUTTON_COLOR;

        RenderUtils.drawRect(poseStack, backgroundColor, this.getX(), this.getY(), 0, this.width, this.height);

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(discoveryInfo.name()),
                        this.getX() + 14,
                        this.getY() + 1,
                        this.width - 15,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE,
                        1f);

        Texture stateTexture = discoveryInfo.discovered()
                ? switch (discoveryInfo.type()) {
                    case TERRITORY -> Texture.DISCOVERED_TERRITORY_ICON;
                    case WORLD -> Texture.DISCOVERED_WORLD_ICON;
                    case SECRET -> Texture.DISCOVERED_SECRET_ICON;
                }
                : switch (discoveryInfo.type()) {
                    case TERRITORY -> Texture.UNDISCOVERED_TERRITORY_ICON;
                    case WORLD -> Texture.UNDISCOVERED_WORLD_ICON;
                    case SECRET -> Texture.UNDISCOVERED_SECRET_ICON;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Models.Discovery.setDiscoveryCompass(discoveryInfo);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            Models.Discovery.openDiscoveryOnMap(discoveryInfo);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && discoveryInfo.type() == DiscoveryType.SECRET) {
            Models.Discovery.openSecretDiscoveryWiki(discoveryInfo);
        }

        return true;
    }

    // not called
    @Override
    public void onPress() {}

    @Override
    public List<Component> getTooltipLines() {
        List<Component> lines = new ArrayList<>(discoveryInfo.displayLore());

        // We need to inject requirements into lore here, as we only have updated discovery info here.
        if (!discoveryInfo.requirements().isEmpty()) {
            List<String> unmet = discoveryInfo.requirements().stream()
                    .filter(requirement -> Models.Discovery.getAllCompletedDiscoveries(ActivitySortOrder.ALPHABETIC)
                            .noneMatch(discovery -> discovery.name().equals(requirement)))
                    .toList();

            if (!unmet.isEmpty()) {
                lines.add(Component.empty());
                lines.add(Component.translatable("screens.wynntils.wynntilsDiscoveries.requirements")
                        .withStyle(ChatFormatting.DARK_AQUA));
                unmet.forEach(requirement -> lines.add(Component.literal(" - ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(requirement).withStyle(ChatFormatting.GRAY))));
            }
        }

        if (discoveryInfo.type() == DiscoveryType.SECRET
                || Models.Territory.getTerritoryProfile(discoveryInfo.name()) != null) {
            lines.add(Component.empty());
            lines.add(Component.translatable("screens.wynntils.wynntilsDiscoveries.leftClickToSetCompass")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.GREEN));
            lines.add(Component.translatable("screens.wynntils.wynntilsDiscoveries.middleClickToOpenOnMap")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.YELLOW));
        }

        if (discoveryInfo.type() == DiscoveryType.SECRET) {
            lines.add(Component.translatable("screens.wynntils.wynntilsDiscoveries.rightClickToOpenWiki")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.GOLD));
        }

        return lines;
    }
}
