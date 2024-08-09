/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Services;
import com.wynntils.core.persisted.config.HiddenConfig;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.map.MainMapFeature;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.services.map.pois.CustomPoi;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.MapRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class PoiCreationScreen extends AbstractMapScreen {
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("[-+]?\\d{1,8}");

    private static final float GRID_DIVISIONS = 64.0f;

    private float dividedWidth;
    private float dividedHeight;

    private TextInputBoxWidget focusedTextInput;

    private TextInputBoxWidget nameInput;
    private TextInputBoxWidget xInput;
    private TextInputBoxWidget yInput;
    private TextInputBoxWidget zInput;
    private TextInputBoxWidget colorInput;
    private Integer parsedXInput;
    private Integer parsedZInput;

    private Button saveButton;

    private int selectedIconIndex = 0;
    private CustomPoi.Visibility selectedVisiblity = CustomPoi.Visibility.DEFAULT;
    private CustomColor colorCache = CommonColors.WHITE;

    private final Screen returnScreen;
    private CustomPoi oldPoi;
    private PoiLocation setupLocation;
    private boolean firstSetup;

    private PoiCreationScreen(MainMapScreen oldMapScreen) {
        super();
        this.returnScreen = oldMapScreen;

        this.firstSetup = true;
    }

    private PoiCreationScreen(MainMapScreen oldMapScreen, PoiLocation setupLocation) {
        this(oldMapScreen);

        this.setupLocation = setupLocation;
        this.firstSetup = true;
    }

    private PoiCreationScreen(MainMapScreen oldMapScreen, CustomPoi poi) {
        this(oldMapScreen);

        this.oldPoi = poi;
        this.firstSetup = true;
    }

    private PoiCreationScreen(PoiManagementScreen managementScreen, CustomPoi poi) {
        super();
        this.returnScreen = managementScreen;

        this.oldPoi = poi;
        this.firstSetup = true;
    }

    public static Screen create(MainMapScreen oldMapScreen) {
        return new PoiCreationScreen(oldMapScreen);
    }

    public static Screen create(MainMapScreen oldMapScreen, PoiLocation setupLocation) {
        return new PoiCreationScreen(oldMapScreen, setupLocation);
    }

    public static Screen create(MainMapScreen oldMapScreen, CustomPoi poi) {
        return new PoiCreationScreen(oldMapScreen, poi);
    }

    public static Screen create(PoiManagementScreen managementScreen, CustomPoi poi) {
        return new PoiCreationScreen(managementScreen, poi);
    }

    @Override
    protected void doInit() {
        dividedWidth = this.width / GRID_DIVISIONS;
        dividedHeight = this.height / GRID_DIVISIONS;

        renderX = dividedWidth * 32;
        renderWidth = dividedWidth * 29;
        renderY = dividedHeight * 5;
        renderHeight = dividedHeight * 54;

        float borderScaleX = (float) this.width / Texture.FULLSCREEN_MAP_BORDER.width();
        float borderScaleY = (float) this.height / Texture.FULLSCREEN_MAP_BORDER.height();

        renderedBorderXOffset = 3 * borderScaleX;
        renderedBorderYOffset = 3 * borderScaleY;

        mapWidth = renderWidth - renderedBorderXOffset * 2f + 1; // +1 to fix rounding causing black line on the right
        centerX = renderX + renderedBorderXOffset + mapWidth / 2f;
        mapHeight = renderHeight - renderedBorderYOffset * 2f;
        centerZ = renderY + renderedBorderYOffset + mapHeight / 2f;

        // region Name
        this.addRenderableWidget(
                nameInput = new TextInputBoxWidget(
                        (int) (dividedWidth * 10),
                        (int) (dividedHeight * 23),
                        (int) (dividedWidth * 12),
                        20,
                        (s) -> updateSaveStatus(),
                        this,
                        nameInput));
        if (oldPoi != null && firstSetup) {
            nameInput.setTextBoxInput(oldPoi.getName());
        }

        if (firstSetup) {
            setFocusedTextInput(nameInput);
        }
        // endregion

        // region Coordinates
        this.addRenderableWidget(
                xInput = new TextInputBoxWidget(
                        (int) (dividedWidth * 11),
                        (int) (dividedHeight * 28),
                        (int) (dividedWidth * 3),
                        20,
                        s -> {
                            if (COORDINATE_PATTERN.matcher(s).matches()) {
                                parsedXInput = Integer.parseInt(s);
                                xInput.setRenderColor(CommonColors.GREEN);
                                if (parsedZInput != null) {
                                    updateMapCenter(parsedXInput, parsedZInput);
                                }
                            } else {
                                parsedXInput = null;
                                xInput.setRenderColor(CommonColors.RED);
                            }
                            updateSaveStatus();
                        },
                        this,
                        xInput));
        this.addRenderableWidget(
                yInput = new TextInputBoxWidget(
                        (int) (dividedWidth * 15),
                        (int) (dividedHeight * 28),
                        (int) (dividedWidth * 3),
                        20,
                        s -> {
                            yInput.setRenderColor(
                                    COORDINATE_PATTERN.matcher(s).matches() ? CommonColors.GREEN : CommonColors.RED);
                            updateSaveStatus();
                        },
                        this,
                        yInput));
        this.addRenderableWidget(
                zInput = new TextInputBoxWidget(
                        (int) (dividedWidth * 19),
                        (int) (dividedHeight * 28),
                        (int) (dividedWidth * 3),
                        20,
                        s -> {
                            if (COORDINATE_PATTERN.matcher(s).matches()) {
                                parsedZInput = Integer.parseInt(s);
                                zInput.setRenderColor(CommonColors.GREEN);
                                if (parsedXInput != null) {
                                    updateMapCenter(parsedXInput, parsedZInput);
                                }
                            } else {
                                parsedZInput = null;
                                zInput.setRenderColor(CommonColors.RED);
                            }
                            updateSaveStatus();
                        },
                        this,
                        zInput));
        if (firstSetup) {
            if (oldPoi != null) {
                xInput.setTextBoxInput(String.valueOf(oldPoi.getLocation().getX()));
                Optional<Integer> y = oldPoi.getLocation().getY();
                yInput.setTextBoxInput(y.isPresent() ? String.valueOf(y.get()) : "");
                zInput.setTextBoxInput(String.valueOf(oldPoi.getLocation().getZ()));
            } else if (setupLocation != null) {
                xInput.setTextBoxInput(String.valueOf(setupLocation.getX()));
                Optional<Integer> y = setupLocation.getY();
                yInput.setTextBoxInput(y.isPresent() ? String.valueOf(y.get()) : "");
                zInput.setTextBoxInput(String.valueOf(setupLocation.getZ()));
            }
        }

        // endregion

        // region Icon
        this.addRenderableWidget(new Button.Builder(Component.literal("<"), (button) -> {
                    if (selectedIconIndex - 1 < 0) {
                        selectedIconIndex = Services.Poi.POI_ICONS.size() - 1;
                    } else {
                        selectedIconIndex--;
                    }
                })
                .pos((int) (dividedWidth * 10), (int) (dividedHeight * 34))
                .size(20, 20)
                .build());
        this.addRenderableWidget(new Button.Builder(Component.literal(">"), (button) -> {
                    if (selectedIconIndex + 1 >= Services.Poi.POI_ICONS.size()) {
                        selectedIconIndex = 0;
                    } else {
                        selectedIconIndex++;
                    }
                })
                .pos((int) (dividedWidth * 14), (int) (dividedHeight * 34))
                .size(20, 20)
                .build());
        if (oldPoi != null && firstSetup) {
            int index = Services.Poi.POI_ICONS.indexOf(oldPoi.getIcon());
            selectedIconIndex = index == -1 ? 0 : index;
        }
        // endregion

        // region Color
        this.addRenderableWidget(
                colorInput = new TextInputBoxWidget(
                        (int) (dividedWidth * 16.5),
                        (int) (dividedHeight * 34),
                        (int) (dividedWidth * 5.5),
                        20,
                        (s) -> {
                            CustomColor color = CustomColor.fromHexString(s);

                            if (color == CustomColor.NONE) {
                                // Default to white
                                colorCache = CommonColors.WHITE;
                                colorInput.setRenderColor(CommonColors.RED);
                            } else {
                                colorCache = color;
                                colorInput.setRenderColor(CommonColors.GREEN);
                            }

                            updateSaveStatus();
                        },
                        this,
                        colorInput));
        if (oldPoi != null && firstSetup) {
            colorInput.setTextBoxInput(String.valueOf(oldPoi.getColor().toHexString()));
        } else if (colorInput.getTextBoxInput().isEmpty()) {
            colorInput.setTextBoxInput("#FFFFFF");
        }
        // endregion

        // region Visibility
        this.addRenderableWidget(new Button.Builder(
                        Component.literal("<"),
                        (button) -> selectedVisiblity = CustomPoi.Visibility.values()[
                                (selectedVisiblity.ordinal() - 1 + CustomPoi.Visibility.values().length)
                                        % CustomPoi.Visibility.values().length])
                .pos((int) (dividedWidth * 10), (int) (dividedHeight * 40))
                .size(20, 20)
                .build());
        this.addRenderableWidget(new Button.Builder(
                        Component.literal(">"),
                        (button) -> selectedVisiblity = CustomPoi.Visibility.values()[
                                (selectedVisiblity.ordinal() + 1 + CustomPoi.Visibility.values().length)
                                        % CustomPoi.Visibility.values().length])
                .pos((int) (dividedWidth * 22) - 19, (int) (dividedHeight * 40))
                .size(20, 20)
                .build());

        if (oldPoi != null && firstSetup) {
            selectedVisiblity = oldPoi.getVisibility();
        }
        // endregion

        // region Screen Interactions
        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.poiCreation.cancel"), (button) -> this.onClose())
                .pos((int) (dividedWidth * 6), (int) (dividedHeight * 54))
                .size((int) (dividedWidth * 8), 20)
                .build());

        this.addRenderableWidget(
                saveButton = new Button.Builder(
                                Component.translatable("screens.wynntils.poiCreation.save"), (button) -> {
                                    savePoi();
                                    this.onClose();
                                })
                        .pos((int) (dividedWidth * 18), (int) (dividedHeight * 54))
                        .size((int) (dividedWidth * 8), 20)
                        .build());
        // endregion

        updateSaveStatus();
        firstSetup = false;
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        renderGradientBackground(guiGraphics, mouseX, mouseY, partialTick);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.enableDepthTest();
        renderMap(poseStack);
        RenderUtils.enableScissor(
                (int) (renderX + renderedBorderXOffset), (int) (renderY + renderedBorderYOffset), (int) mapWidth, (int)
                        mapHeight);

        if (parsedXInput != null && parsedZInput != null) {
            Poi poi = new CustomPoi(
                    new PoiLocation(parsedXInput, null, parsedZInput),
                    nameInput.getTextBoxInput(),
                    CustomColor.fromHexString(colorInput.getTextBoxInput()) == CustomColor.NONE
                            ? CommonColors.WHITE
                            : CustomColor.fromHexString(colorInput.getTextBoxInput()),
                    Services.Poi.POI_ICONS.get(selectedIconIndex),
                    selectedVisiblity);

            MultiBufferSource.BufferSource bufferSource =
                    McUtils.mc().renderBuffers().bufferSource();

            poi.renderAt(
                    poseStack,
                    bufferSource,
                    MapRenderer.getRenderX(poi, mapCenterX, centerX, zoomRenderScale),
                    MapRenderer.getRenderZ(poi, mapCenterZ, centerZ, zoomRenderScale),
                    hovered == poi,
                    1,
                    zoomRenderScale,
                    zoomLevel,
                    true);

            bufferSource.endBatch();
        }

        renderCursor(
                poseStack,
                1.5f,
                Managers.Feature.getFeatureInstance(MainMapFeature.class)
                        .pointerColor
                        .get(),
                Managers.Feature.getFeatureInstance(MainMapFeature.class)
                        .pointerType
                        .get());

        RenderUtils.disableScissor();

        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.doRender(guiGraphics, mouseX, mouseY, partialTick);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.poiCreation.waypointName") + ":"),
                        (int) (dividedWidth * 10),
                        (int) (dividedHeight * 22),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.poiCreation.coordinates") + ":"),
                        (int) (dividedWidth * 10),
                        (int) (dividedHeight * 27),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString("X"),
                        dividedWidth * 10.5f,
                        (int) (dividedHeight * 28) + 10,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString("Y"),
                        dividedWidth * 14.5f,
                        (int) (dividedHeight * 28) + 10,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString("Z"),
                        dividedWidth * 18.5f,
                        (int) (dividedHeight * 28) + 10,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.poiCreation.icon") + ":"),
                        dividedWidth * 10.0f,
                        dividedHeight * 33.0f,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        renderIcon(poseStack);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.poiCreation.color") + ":"),
                        dividedWidth * 16.5f,
                        dividedHeight * 33.0f,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.poiCreation.visibility") + ":"),
                        dividedWidth * 10.0f,
                        dividedHeight * 39.0f,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(I18n.get(selectedVisiblity.getTranslationKey())),
                        dividedWidth * 16.0f,
                        dividedWidth * 16.0f,
                        dividedHeight * 40.0f,
                        dividedHeight * 40.0f + 20,
                        0,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderIcon(PoseStack poseStack) {
        float[] color = colorCache.asFloatArray();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(color[0], color[1], color[2], 1);

        Texture texture = Services.Poi.POI_ICONS.get(selectedIconIndex);
        // left button x + (center between buttons - half of texture width)
        float x =
                (dividedWidth * 10 + 20) + (((dividedWidth * 14) - (dividedWidth * 10 + 20)) / 2 - texture.width() / 2);
        RenderUtils.drawTexturedRect(poseStack, texture, x, dividedHeight * 34);

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            int gameX = (int) ((mouseX - centerX) / zoomRenderScale + mapCenterX);
            int gameZ = (int) ((mouseY - centerZ) / zoomRenderScale + mapCenterZ);
            xInput.setTextBoxInput(String.valueOf(gameX));
            zInput.setTextBoxInput(String.valueOf(gameZ));
        }
        return super.doMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return (focusedTextInput != null && focusedTextInput.charTyped(codePoint, modifiers))
                || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // When tab is pressed, focus the next text box
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            int index = focusedTextInput == null ? 0 : children().indexOf(focusedTextInput);
            int actualIndex = Math.max(index, 0) + 1;

            // Try to find next text input
            // From index - end
            for (int i = actualIndex; i < children().size(); i++) {
                if (children().get(i) instanceof TextInputBoxWidget textInputBoxWidget) {
                    setFocusedTextInput(textInputBoxWidget);
                    return true;
                }
            }

            // From 0 - index
            for (int i = 0; i < Math.min(actualIndex, children().size()); i++) {
                if (children().get(i) instanceof TextInputBoxWidget textInputBoxWidget) {
                    setFocusedTextInput(textInputBoxWidget);
                    return true;
                }
            }
        }

        return (focusedTextInput != null && focusedTextInput.keyPressed(keyCode, scanCode, modifiers))
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public TextInputBoxWidget getFocusedTextInput() {
        return focusedTextInput;
    }

    @Override
    public void setFocusedTextInput(TextInputBoxWidget focusedTextInput) {
        this.focusedTextInput = focusedTextInput;
    }

    @Override
    public void onClose() {
        McUtils.mc().setScreen(returnScreen);
    }

    private void updateSaveStatus() {
        if (saveButton == null) return;

        saveButton.active = !nameInput.getTextBoxInput().isBlank()
                && CustomColor.fromHexString(colorInput.getTextBoxInput()) != CustomColor.NONE
                && COORDINATE_PATTERN.matcher(xInput.getTextBoxInput()).matches()
                && (COORDINATE_PATTERN.matcher(yInput.getTextBoxInput()).matches()
                        || yInput.getTextBoxInput().isEmpty())
                && COORDINATE_PATTERN.matcher(zInput.getTextBoxInput()).matches();
    }

    private void savePoi() {
        CustomPoi poi = new CustomPoi(
                new PoiLocation(
                        Integer.parseInt(xInput.getTextBoxInput()),
                        yInput.getTextBoxInput().isEmpty() ? null : Integer.parseInt(yInput.getTextBoxInput()),
                        Integer.parseInt(zInput.getTextBoxInput())),
                nameInput.getTextBoxInput(),
                CustomColor.fromHexString(colorInput.getTextBoxInput()),
                Services.Poi.POI_ICONS.get(selectedIconIndex),
                selectedVisiblity);

        HiddenConfig<List<CustomPoi>> customPoiConfig =
                Managers.Feature.getFeatureInstance(MainMapFeature.class).customPois;
        List<CustomPoi> customPois = customPoiConfig.get();
        if (oldPoi != null) {
            customPois.set(customPois.indexOf(oldPoi), poi);
        } else {
            customPois.add(poi);
        }

        customPoiConfig.touched();
        Managers.Feature.getFeatureInstance(MainMapFeature.class).updateWaypoints();
    }
}
