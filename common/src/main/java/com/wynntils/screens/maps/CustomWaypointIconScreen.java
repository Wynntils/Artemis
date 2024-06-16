/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.WynntilsGridLayoutScreen;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.maps.widgets.CustomIconWidget;
import com.wynntils.services.mapdata.providers.json.JsonIcon;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class CustomWaypointIconScreen extends WynntilsGridLayoutScreen {
    private static final int ICONS_PER_PAGE = 16;
    private static final int SCROLLBAR_HEIGHT = 40;
    private static final float SCROLL_FACTOR = 10f;
    // Accepted resource location name pattern `^[a-z0-9/._-]+`
    // This is made stricter to avoid weird names
    private static final Pattern ICON_NAME_PATTERN = Pattern.compile("^[a-z0-9-]+$");

    private final WaypointCreationScreen creationScreen;
    private List<CustomIconWidget> iconWidgets = new ArrayList<>();

    private boolean draggingScroll;
    private Button saveIconButton;
    private float scrollRenderY;
    private int scrollOffset = 0;
    private JsonIcon newIcon;
    private TextInputBoxWidget iconNameInput;
    private TextInputBoxWidget iconBase64Input;

    private CustomWaypointIconScreen(WaypointCreationScreen creationScreen) {
        super(Component.literal("Custom Waypoint Icon Screen"));

        this.creationScreen = creationScreen;
    }

    public static Screen create(WaypointCreationScreen creationScreen) {
        return new CustomWaypointIconScreen(creationScreen);
    }

    @Override
    protected void doInit() {
        super.doInit();

        iconNameInput = new TextInputBoxWidget(
                (int) (dividedWidth * 49),
                (int) (dividedHeight * 24),
                (int) (dividedWidth * 14),
                BUTTON_HEIGHT,
                (s) -> tryParseIcon(),
                this,
                iconNameInput);
        iconNameInput.setTooltip(
                Tooltip.create(Component.translatable("screens.wynntils.customWaypointIcon.nameTooltip")));
        this.addRenderableWidget(iconNameInput);

        iconBase64Input = new TextInputBoxWidget(
                (int) (dividedWidth * 49),
                (int) (dividedHeight * 32),
                (int) (dividedWidth * 14),
                BUTTON_HEIGHT,
                (s) -> tryParseIcon(),
                this,
                iconBase64Input);
        iconBase64Input.setTooltip(
                Tooltip.create(Component.translatable("screens.wynntils.customWaypointIcon.base64Tooltip")));
        this.addRenderableWidget(iconBase64Input);

        saveIconButton = new Button.Builder(
                        Component.translatable("screens.wynntils.customWaypointIcon.saveIcon"), (button) -> saveIcon())
                .pos((int) (dividedWidth * 42), (int) (dividedHeight * 37))
                .size((int) (dividedWidth * 8), BUTTON_HEIGHT)
                .build();
        saveIconButton.active = newIcon != null;
        this.addRenderableWidget(saveIconButton);

        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.customWaypointIcon.back"), (button) -> onClose())
                .pos((int) (dividedWidth * 55), (int) (dividedHeight * 37))
                .size((int) (dividedWidth * 8), BUTTON_HEIGHT)
                .build());

        populateIcons();
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawRect(
                poseStack,
                CommonColors.BLACK.withAlpha(100),
                dividedWidth * 36,
                dividedHeight * 22,
                0,
                dividedWidth * 28,
                dividedHeight * 20);

        super.doRender(guiGraphics, mouseX, mouseY, partialTick);

        for (CustomIconWidget iconWidget : iconWidgets) {
            iconWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (iconWidgets.size() > ICONS_PER_PAGE) {
            renderScrollBar(poseStack);
        } else if (iconWidgets.isEmpty()) {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromComponent(
                                    Component.translatable("screens.wynntils.customWaypointIcon.noIcons")),
                            dividedWidth * 2,
                            dividedHeight * 32,
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NORMAL,
                            2);
        }

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.customWaypointIcon.iconName") + ":"),
                        dividedWidth * 42,
                        dividedHeight * 26,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL,
                        1);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString("base64:"),
                        dividedWidth * 42,
                        dividedHeight * 34,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL,
                        1);

        renderPreview(poseStack);
    }

    @Override
    public void onClose() {
        McUtils.mc().setScreen(creationScreen);
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        for (CustomIconWidget widget : iconWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                return widget.mouseClicked(mouseX, mouseY, button);
            }
        }

        if (!draggingScroll
                && (iconWidgets.size() > ICONS_PER_PAGE)
                && MathUtils.isInside(
                        (int) mouseX,
                        (int) mouseY,
                        (int) (dividedWidth * 32),
                        (int) (dividedWidth * 32) + (int) (dividedWidth / 2),
                        (int) scrollRenderY,
                        (int) (scrollRenderY + SCROLLBAR_HEIGHT))) {
            draggingScroll = true;
            return true;
        }

        return super.doMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroll) {
            int newOffset = Math.round(
                    MathUtils.map((float) mouseY, 20, 20 + this.height - SCROLLBAR_HEIGHT, 0, getMaxScrollOffset()));

            newOffset = Math.max(0, Math.min(newOffset, getMaxScrollOffset()));

            scroll(newOffset);

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int scrollAmount = (int) (-deltaY * SCROLL_FACTOR);

        if (iconWidgets.size() > ICONS_PER_PAGE) {
            int newOffset = Math.max(0, Math.min(scrollOffset + scrollAmount, getMaxScrollOffset()));
            scroll(newOffset);
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    public void selectIcon(JsonIcon icon) {
        creationScreen.setSelectedIcon(icon);
        McUtils.mc().setScreen(creationScreen);
    }

    public void removeIcon(JsonIcon iconToRemove) {
        Services.Waypoints.removeCustomIcon(iconToRemove);

        scrollOffset = 0;
        populateIcons();
    }

    private void tryParseIcon() {
        if (iconNameInput.getTextBoxInput().isEmpty()
                || iconBase64Input.getTextBoxInput().isEmpty()) {
            newIcon = null;
            saveIconButton.active = false;
            return;
        } else if (!ICON_NAME_PATTERN.matcher(iconNameInput.getTextBoxInput()).matches()) {
            newIcon = null;
            saveIconButton.active = false;
            return;
        }

        try {
            byte[] texture = Base64.getDecoder().decode(iconBase64Input.getTextBoxInput());
            newIcon = new JsonIcon("wynntils:icon:personal:" + iconNameInput.getTextBoxInput(), texture);
            saveIconButton.active = true;
        } catch (IOException | IllegalArgumentException e) {
            WynntilsMod.warn("Bad icon texture for " + iconNameInput.getTextBoxInput(), e);
            newIcon = null;
            saveIconButton.active = false;
        }
    }

    private void saveIcon() {
        Services.Waypoints.addCustomIcon(newIcon);
        newIcon = null;
        iconNameInput.resetTextBoxInput();
        iconBase64Input.resetTextBoxInput();

        populateIcons();
    }

    private void renderPreview(PoseStack poseStack) {
        if (newIcon == null) return;

        RenderUtils.drawScalingTexturedRect(
                poseStack,
                newIcon.getResourceLocation(),
                dividedWidth * 37,
                dividedHeight * 31,
                1,
                newIcon.getWidth(),
                newIcon.getHeight(),
                newIcon.getWidth(),
                newIcon.getHeight());
    }

    private void populateIcons() {
        iconWidgets = new ArrayList<>();

        int renderY = 0;

        for (JsonIcon icon : Services.Waypoints.getCustomIcons()) {
            CustomIconWidget iconWidget =
                    new CustomIconWidget(renderY, (int) (dividedWidth * 32), (int) (dividedHeight * 4), icon, this);
            iconWidgets.add(iconWidget);

            renderY += (int) (dividedHeight * 4);
        }

        scroll(scrollOffset);
    }

    private void renderScrollBar(PoseStack poseStack) {
        RenderUtils.drawRect(
                poseStack, CommonColors.LIGHT_GRAY, (dividedWidth * 32), 0, 0, (dividedWidth / 2), this.height);

        scrollRenderY = (int) (MathUtils.map(scrollOffset, 0, getMaxScrollOffset(), 0, this.height - SCROLLBAR_HEIGHT));

        RenderUtils.drawRect(
                poseStack,
                draggingScroll ? CommonColors.BLACK : CommonColors.GRAY,
                (dividedWidth * 32),
                scrollRenderY,
                0,
                (dividedWidth / 2),
                SCROLLBAR_HEIGHT);
    }

    private void scroll(int newOffset) {
        scrollOffset = newOffset;

        for (CustomIconWidget iconWidget : iconWidgets) {
            int newY = (iconWidgets.indexOf(iconWidget) * (int) (dividedHeight * 4)) - scrollOffset;

            iconWidget.setY(newY);
        }
    }

    private int getMaxScrollOffset() {
        return (Services.Waypoints.getCustomIcons().size() - ICONS_PER_PAGE) * (int) (dividedHeight * 4);
    }
}
