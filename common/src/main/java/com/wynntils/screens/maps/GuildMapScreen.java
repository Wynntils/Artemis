/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.map.GuildMapFeature;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.GuildResourceValues;
import com.wynntils.screens.base.widgets.BasicTexturedButton;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.services.map.pois.WaypointPoi;
import com.wynntils.services.map.type.TerritoryDefenseFilterType;
import com.wynntils.services.mapdata.type.MapFeature;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.CappedValue;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class GuildMapScreen extends AbstractMapScreen {
    private boolean resourceMode = false;
    private boolean territoryDefenseFilterEnabled = false;
    private boolean hybridMode = true;

    private GuildResourceValues territoryDefenseFilterLevel = GuildResourceValues.VERY_HIGH;
    private TerritoryDefenseFilterType territoryDefenseFilterType = TerritoryDefenseFilterType.DEFAULT;

    private BasicTexturedButton territoryDefenseFilterButton;
    private BasicTexturedButton hybridModeButton;

    private GuildMapScreen() {}

    public static Screen create() {
        return new GuildMapScreen();
    }

    @Override
    protected void doInit() {
        super.doInit();

        // Buttons have to be added in reverse order (right to left) so they don't overlap

        this.addRenderableWidget(new BasicTexturedButton(
                width / 2 - Texture.MAP_BUTTONS_BACKGROUND.width() / 2 + 7 + 20 * 6,
                (int) (this.renderHeight
                        - this.renderedBorderYOffset
                        - Texture.MAP_BUTTONS_BACKGROUND.height() / 2f
                        - 8),
                10,
                16,
                Texture.HELP_ICON,
                (b) -> {},
                List.of(
                        Component.literal("[>] ")
                                .withStyle(ChatFormatting.YELLOW)
                                .append(Component.translatable("screens.wynntils.map.help.name")),
                        Component.literal("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable("screens.wynntils.guildMap.help.description1")),
                        Component.literal("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable("screens.wynntils.guildMap.help.description2")),
                        Component.literal("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable("screens.wynntils.guildMap.help.description3")))));

        this.addRenderableWidget(
                hybridModeButton = new BasicTexturedButton(
                        width / 2 - Texture.MAP_BUTTONS_BACKGROUND.width() / 2 + 4 + 20 * 2,
                        (int) (this.renderHeight
                                - this.renderedBorderYOffset
                                - Texture.MAP_BUTTONS_BACKGROUND.height() / 2f
                                - 8),
                        16,
                        16,
                        Texture.OVERLAY_EXTRA_ICON,
                        (b) -> {
                            hybridMode = !hybridMode;
                            hybridModeButton.setTooltip(getHybridModeTooltip());
                        },
                        getHybridModeTooltip()));

        territoryDefenseFilterButton = this.addRenderableWidget(new BasicTexturedButton(
                width / 2 - Texture.MAP_BUTTONS_BACKGROUND.width() / 2 + 4 + 20,
                (int) (this.renderHeight
                        - this.renderedBorderYOffset
                        - Texture.MAP_BUTTONS_BACKGROUND.height() / 2f
                        - 8),
                16,
                16,
                Texture.DEFENSE_FILTER_ICON,
                (b) -> {
                    // Left and right clicks cycle through the defense levels, middle click resets to OFF
                    if (b == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                        territoryDefenseFilterEnabled = false;
                        territoryDefenseFilterType = TerritoryDefenseFilterType.DEFAULT;
                        territoryDefenseFilterButton.setTooltip(getCompleteFilterTooltip());
                        return;
                    }

                    // Holding shift filters higher, ctrl filters lower
                    if (KeyboardUtils.isShiftDown()) {
                        territoryDefenseFilterType = TerritoryDefenseFilterType.HIGHER;
                    } else if (KeyboardUtils.isControlDown()) {
                        territoryDefenseFilterType = TerritoryDefenseFilterType.LOWER;
                    } else {
                        territoryDefenseFilterType = TerritoryDefenseFilterType.DEFAULT;
                    }

                    territoryDefenseFilterEnabled = true;
                    if (b == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        territoryDefenseFilterLevel = territoryDefenseFilterLevel.getFilterNext(
                                territoryDefenseFilterType != TerritoryDefenseFilterType.DEFAULT);
                    } else if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        territoryDefenseFilterLevel = territoryDefenseFilterLevel.getFilterPrevious(
                                territoryDefenseFilterType != TerritoryDefenseFilterType.DEFAULT);
                    }

                    territoryDefenseFilterButton.setTooltip(getCompleteFilterTooltip());
                },
                getCompleteFilterTooltip()));

        this.addRenderableWidget(new BasicTexturedButton(
                width / 2 - Texture.MAP_BUTTONS_BACKGROUND.width() / 2 + 6,
                (int) (this.renderHeight
                        - this.renderedBorderYOffset
                        - Texture.MAP_BUTTONS_BACKGROUND.height() / 2f
                        - 7),
                14,
                14,
                Texture.ADD_ICON,
                (b) -> resourceMode = !resourceMode,
                List.of(
                        Component.literal("[>] ")
                                .withStyle(ChatFormatting.GOLD)
                                .append(Component.translatable("screens.wynntils.guildMap.toggleResourceColor.name")),
                        Component.translatable("screens.wynntils.guildMap.toggleResourceColor.description")
                                .withStyle(ChatFormatting.GRAY))));

        if (firstInit) {
            // When outside of the main map, center to the middle of the map
            if (!isPlayerInsideMainArea()) {
                centerMapOnWorld();
            }

            firstInit = false;
        }
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        if (holdingMapKey
                && !Managers.Feature.getFeatureInstance(GuildMapFeature.class)
                        .openGuildMapKeybind
                        .getKeyMapping()
                        .isDown()) {
            this.onClose();
            return;
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.enableDepthTest();

        renderMap(poseStack);

        RenderUtils.enableScissor(
                (int) (renderX + renderedBorderXOffset), (int) (renderY + renderedBorderYOffset), (int) mapWidth, (int)
                        mapHeight);

        renderMapFeatures(poseStack, mouseX, mouseY);

        renderCursor(
                poseStack,
                1.5f,
                Managers.Feature.getFeatureInstance(GuildMapFeature.class)
                        .pointerColor
                        .get(),
                Managers.Feature.getFeatureInstance(GuildMapFeature.class)
                        .pointerType
                        .get());

        RenderUtils.disableScissor();

        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        renderCoordinates(poseStack, mouseX, mouseY);

        renderMapButtons(guiGraphics, mouseX, mouseY, partialTick);

        renderHoveredTerritoryInfo(poseStack);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected Stream<MapFeature> getRenderedMapFeatures() {
        // FIXME: Add back territory map areas (with hybrid/advancement mode)
        // FIXME: Add territory connection map paths
        // FIXME: Add user markers
        return Stream.empty();
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener child : children()) {
            if (child.isMouseOver(mouseX, mouseY)) {
                child.mouseClicked(mouseX, mouseY, button);
                return true;
            }
        }

        // Manage on shift right click
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && KeyboardUtils.isShiftDown()
                && hovered instanceof TerritoryPoi territoryPoi) {
            Handlers.Command.queueCommand("gu territory " + territoryPoi.getName());
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (hovered instanceof WaypointPoi) {
                Models.Marker.USER_WAYPOINTS_PROVIDER.removeLocation(
                        hovered.getLocation().asLocation());
                return true;
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            setCompassToMouseCoords(mouseX, mouseY, !KeyboardUtils.isShiftDown());
            return true;
        }

        return super.doMouseClicked(mouseX, mouseY, button);
    }

    private void renderHoveredTerritoryInfo(PoseStack poseStack) {
        if (!(hovered instanceof TerritoryPoi territoryPoi)) return;

        poseStack.pushPose();
        poseStack.translate(width - SCREEN_SIDE_OFFSET - 250, SCREEN_SIDE_OFFSET + 40, 101);

        if (territoryPoi.isFakeTerritoryInfo()) {
            renderTerritoryTooltipWithFakeInfo(poseStack, territoryPoi);
        } else {
            renderTerritoryTooltip(poseStack, territoryPoi);
        }

        poseStack.popPose();
    }

    public boolean isResourceMode() {
        return resourceMode;
    }

    private static void renderTerritoryTooltip(PoseStack poseStack, TerritoryPoi territoryPoi) {
        final TerritoryInfo territoryInfo = territoryPoi.getTerritoryInfo();
        final TerritoryProfile territoryProfile = territoryPoi.getTerritoryProfile();

        final int textureWidth = Texture.MAP_INFO_TOOLTIP_CENTER.width();

        final float centerHeight = 75
                + (territoryInfo.getStorage().values().size()
                                + territoryInfo.getGenerators().size())
                        * 10
                + (territoryInfo.isHeadquarters() ? 20 : 0);

        RenderUtils.drawTexturedRect(poseStack, Texture.MAP_INFO_TOOLTIP_TOP, 0, 0);
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.MAP_INFO_TOOLTIP_CENTER.resource(),
                0,
                Texture.MAP_INFO_TOOLTIP_TOP.height(),
                textureWidth,
                centerHeight,
                textureWidth,
                Texture.MAP_INFO_TOOLTIP_CENTER.height());
        RenderUtils.drawTexturedRect(
                poseStack, Texture.MAP_INFO_NAME_BOX, 0, Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight);

        // guild
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(
                                "%s [%s]".formatted(territoryInfo.getGuildName(), territoryInfo.getGuildPrefix())),
                        10,
                        10,
                        CommonColors.MAGENTA,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);

        float renderYOffset = 20;

        for (GuildResource value : GuildResource.values()) {
            int generation = territoryInfo.getGeneration(value);
            CappedValue storage = territoryInfo.getStorage(value);

            if (generation != 0) {
                StyledText formattedGenerated = StyledText.fromString(
                        "%s+%d %s per Hour".formatted(value.getPrettySymbol(), generation, value.getName()));

                FontRenderer.getInstance()
                        .renderText(
                                poseStack,
                                formattedGenerated,
                                10,
                                10 + renderYOffset,
                                CommonColors.WHITE,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.TOP,
                                TextShadow.OUTLINE);
                renderYOffset += 10;
            }

            if (storage != null) {
                StyledText formattedStored = StyledText.fromString("%s%d/%d %s stored"
                        .formatted(value.getPrettySymbol(), storage.current(), storage.max(), value.getName()));

                FontRenderer.getInstance()
                        .renderText(
                                poseStack,
                                formattedStored,
                                10,
                                10 + renderYOffset,
                                CommonColors.WHITE,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.TOP,
                                TextShadow.OUTLINE);
                renderYOffset += 10;
            }
        }

        renderYOffset += 10;

        StyledText treasury = StyledText.fromString(ChatFormatting.GRAY
                + "✦ Treasury: %s"
                        .formatted(territoryInfo.getTreasury().getTreasuryColor()
                                + territoryInfo.getTreasury().getAsString()));
        StyledText defences = StyledText.fromString(ChatFormatting.GRAY
                + "Territory Defences: %s"
                        .formatted(territoryInfo.getDefences().getDefenceColor()
                                + territoryInfo.getDefences().getAsString()));

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        treasury,
                        10,
                        10 + renderYOffset,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);
        renderYOffset += 10;
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        defences,
                        10,
                        10 + renderYOffset,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);

        if (territoryInfo.isHeadquarters()) {
            renderYOffset += 20;
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString("Guild Headquarters"),
                            10,
                            10 + renderYOffset,
                            CommonColors.RED,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP,
                            TextShadow.OUTLINE);
        }

        renderYOffset += 20;

        String timeHeldString = territoryProfile.getGuild().equals(territoryInfo.getGuildName())
                ? territoryProfile.getTimeAcquiredColor() + territoryProfile.getReadableRelativeTimeAcquired()
                : "-";
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(ChatFormatting.GRAY + "Time Held: " + timeHeldString),
                        10,
                        10 + renderYOffset,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);

        // Territory name
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(territoryPoi.getName()),
                        7,
                        textureWidth,
                        Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight,
                        Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight + Texture.MAP_INFO_NAME_BOX.height(),
                        0,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.OUTLINE);
    }

    private static void renderTerritoryTooltipWithFakeInfo(PoseStack poseStack, TerritoryPoi territoryPoi) {
        final TerritoryProfile territoryProfile = territoryPoi.getTerritoryProfile();

        final int textureWidth = Texture.MAP_INFO_TOOLTIP_CENTER.width();

        final float centerHeight = 35;

        RenderUtils.drawTexturedRect(poseStack, Texture.MAP_INFO_TOOLTIP_TOP, 0, 0);
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.MAP_INFO_TOOLTIP_CENTER.resource(),
                0,
                Texture.MAP_INFO_TOOLTIP_TOP.height(),
                textureWidth,
                centerHeight,
                textureWidth,
                Texture.MAP_INFO_TOOLTIP_CENTER.height());
        RenderUtils.drawTexturedRect(
                poseStack, Texture.MAP_INFO_NAME_BOX, 0, Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight);

        // guild
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(
                                "%s [%s]".formatted(territoryProfile.getGuild(), territoryProfile.getGuildPrefix())),
                        10,
                        10,
                        CommonColors.MAGENTA,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromComponent(
                                Component.translatable("screens.wynntils.guildMap.hybridMode.noAdvancementData")),
                        10,
                        30,
                        CommonColors.LIGHT_GRAY,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);

        // Territory name
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(territoryPoi.getName()),
                        7,
                        textureWidth,
                        Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight,
                        Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight + Texture.MAP_INFO_NAME_BOX.height(),
                        0,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.OUTLINE);
    }

    private List<Component> getCompleteFilterTooltip() {
        Component lastLine = territoryDefenseFilterEnabled
                ? Component.translatable("screens.wynntils.guildMap.cycleDefenseFilter.description4")
                        .withStyle(ChatFormatting.GRAY)
                        .append(territoryDefenseFilterLevel.getDefenceColor()
                                + territoryDefenseFilterLevel.getAsString())
                        .append(territoryDefenseFilterType.asComponent())
                : Component.translatable("screens.wynntils.guildMap.cycleDefenseFilter.description4")
                        .withStyle(ChatFormatting.GRAY)
                        .append("Off");
        return List.of(
                Component.literal("[>] ")
                        .withStyle(ChatFormatting.BLUE)
                        .append(Component.translatable("screens.wynntils.guildMap.cycleDefenseFilter.name")),
                Component.translatable("screens.wynntils.guildMap.cycleDefenseFilter.description1")
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("screens.wynntils.guildMap.cycleDefenseFilter.description2")
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("screens.wynntils.guildMap.cycleDefenseFilter.description3")
                        .withStyle(ChatFormatting.GRAY),
                lastLine);
    }

    private List<Component> getHybridModeTooltip() {
        return List.of(
                Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.guildMap.hybridMode.name")),
                Component.translatable("screens.wynntils.guildMap.hybridMode.description1")
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("screens.wynntils.guildMap.hybridMode.description2")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                                (hybridMode
                                        ? Component.translatable("screens.wynntils.guildMap.hybridMode.hybrid")
                                                .withStyle(ChatFormatting.GREEN)
                                        : Component.translatable("screens.wynntils.guildMap.hybridMode.advancement")
                                                .withStyle(ChatFormatting.RED))));
    }
}
