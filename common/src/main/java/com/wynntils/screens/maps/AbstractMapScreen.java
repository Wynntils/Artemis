/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.services.map.MapTexture;
import com.wynntils.services.map.pois.IconPoi;
import com.wynntils.services.map.pois.LabelPoi;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.MapRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.PointerType;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.BoundingBox;
import com.wynntils.utils.type.BoundingShape;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractMapScreen extends WynntilsScreen {
    protected static final float SCREEN_SIDE_OFFSET = 10;
    private static final float BORDER_OFFSET = 6;
    private static final int MAP_CENTER_X = -150;
    private static final int MAP_CENTER_Z = -3000;
    private static final int MAX_X = 1650;
    private static final int MAX_Z = -150;
    private static final int MIN_X = -2400;
    private static final int MIN_Z = -6600;

    protected boolean holdingMapKey = false;
    protected boolean firstInit = true;

    protected float renderWidth;
    protected float renderHeight;
    protected float renderX;
    protected float renderY;

    protected float renderedBorderXOffset;
    protected float renderedBorderYOffset;

    protected float mapWidth;
    protected float mapHeight;
    protected float centerX;
    protected float centerZ;

    protected float mapCenterX;
    protected float mapCenterZ;

    // Zooming updates currentZoomStep, but we also cache the currentZoom for rendering
    protected float currentZoomStep = MapRenderer.DEFAULT_ZOOM_STEP;
    protected float currentZoom = MapRenderer.getZoomFromSteps(currentZoomStep);

    protected Poi hovered = null;

    protected AbstractMapScreen() {
        super(Component.literal("Map"));
        centerMapAroundPlayer();
    }

    protected AbstractMapScreen(float mapCenterX, float mapCenterZ) {
        super(Component.literal("Map"));
        updateMapCenter(mapCenterX, mapCenterZ);
    }

    @Override
    protected void doInit() {
        // FIXME: Figure out a way to not need this.
        //        At the moment, this is needed for Minecraft not to forget we hold keys when we open the GUI...
        Options options = McUtils.options();
        KeyMapping.set(options.keyUp.key, KeyboardUtils.isKeyDown(options.keyUp.key.getValue()));
        KeyMapping.set(options.keyDown.key, KeyboardUtils.isKeyDown(options.keyDown.key.getValue()));
        KeyMapping.set(options.keyLeft.key, KeyboardUtils.isKeyDown(options.keyLeft.key.getValue()));
        KeyMapping.set(options.keyRight.key, KeyboardUtils.isKeyDown(options.keyRight.key.getValue()));
        KeyMapping.set(options.keyJump.key, KeyboardUtils.isKeyDown(options.keyJump.key.getValue()));
        KeyMapping.set(options.keyShift.key, KeyboardUtils.isKeyDown(options.keyShift.key.getValue()));

        renderWidth = this.width - SCREEN_SIDE_OFFSET * 2f;
        renderHeight = this.height - SCREEN_SIDE_OFFSET * 2f;
        renderX = SCREEN_SIDE_OFFSET;
        renderY = SCREEN_SIDE_OFFSET;

        float borderScaleX = (float) this.width / Texture.FULLSCREEN_MAP_BORDER.width();
        float borderScaleY = (float) this.height / Texture.FULLSCREEN_MAP_BORDER.height();

        renderedBorderXOffset = BORDER_OFFSET * borderScaleX;
        renderedBorderYOffset = BORDER_OFFSET * borderScaleY;

        mapWidth = renderWidth - renderedBorderXOffset * 2f;
        mapHeight = renderHeight - renderedBorderYOffset * 2f;
        centerX = renderX + renderedBorderXOffset + mapWidth / 2f;
        centerZ = renderY + renderedBorderYOffset + mapHeight / 2f;
    }

    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (GuiEventListener child : children) {
            if (child instanceof TooltipProvider tooltipProvider && child.isMouseOver(mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(
                        FontRenderer.getInstance().getFont(), tooltipProvider.getTooltipLines(), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.FULLSCREEN_MAP_BORDER.resource(),
                renderX,
                renderY,
                100,
                renderWidth,
                renderHeight,
                Texture.FULLSCREEN_MAP_BORDER.width(),
                Texture.FULLSCREEN_MAP_BORDER.height());
    }

    protected void renderGradientBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected void renderPois(
            List<Poi> pois,
            PoseStack poseStack,
            BoundingBox textureBoundingBox,
            float poiScale,
            int mouseX,
            int mouseY) {
        hovered = null;

        List<Poi> filteredPois = getRenderedPois(pois, textureBoundingBox, poiScale, mouseX, mouseY);

        MultiBufferSource.BufferSource bufferSource =
                McUtils.mc().renderBuffers().bufferSource();

        // Reverse and Render
        for (int i = filteredPois.size() - 1; i >= 0; i--) {
            Poi poi = filteredPois.get(i);

            float poiRenderX = MapRenderer.getRenderX(poi, mapCenterX, centerX, currentZoom);
            float poiRenderZ = MapRenderer.getRenderZ(poi, mapCenterZ, centerZ, currentZoom);

            poi.renderAt(poseStack, bufferSource, poiRenderX, poiRenderZ, hovered == poi, poiScale, currentZoom);
        }

        bufferSource.endBatch();
    }

    protected List<Poi> getRenderedPois(
            List<Poi> pois, BoundingBox textureBoundingBox, float poiScale, int mouseX, int mouseY) {
        List<Poi> filteredPois = new ArrayList<>();

        // Filter and find hovered
        for (int i = pois.size() - 1; i >= 0; i--) {
            Poi poi = pois.get(i);
            PoiLocation location = poi.getLocation();
            // This is due to bad design of the compass dynamic waypoint provider,
            // once that is fixed this can be removed
            if (location == null) continue;

            if (poi instanceof IconPoi iconPoi) {
                // Check if the poi is visible
                if (iconPoi.getIconAlpha(currentZoom) < 0.1f) {
                    continue;
                }
            } else if (poi instanceof LabelPoi labelPoi) {
                // Check if label is visible
                if (labelPoi.getAlphaFromScale(currentZoom) < 0.1f) {
                    continue;
                }
            }

            float poiRenderX = MapRenderer.getRenderX(poi, mapCenterX, centerX, currentZoom);
            float poiRenderZ = MapRenderer.getRenderZ(poi, mapCenterZ, centerZ, currentZoom);

            float poiWidth = poi.getWidth(currentZoom, poiScale);
            float poiHeight = poi.getHeight(currentZoom, poiScale);

            BoundingBox filterBox = BoundingBox.centered(location.getX(), location.getZ(), poiWidth, poiHeight);
            BoundingBox mouseBox = BoundingBox.centered(poiRenderX, poiRenderZ, poiWidth, poiHeight);

            if (BoundingShape.intersects(filterBox, textureBoundingBox)) {
                filteredPois.add(poi);
                if (hovered == null && mouseBox.contains(mouseX, mouseY)) {
                    hovered = poi;
                }
            }
        }

        // Add hovered poi as first
        if (hovered != null) {
            filteredPois.remove(hovered);
            filteredPois.add(0, hovered);
        }

        return filteredPois;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        adjustZoomStep((float) (2f * deltaY));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
            // Take steps of 2 to make it easier to zoom in and out
            adjustZoomStep(2);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            // Take steps of 2 to make it easier to zoom in and out
            adjustZoomStep(-2);
            return true;
        }

        // Pass along key press to move
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        KeyMapping.set(key, true);

        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Pass along key press to move
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        KeyMapping.set(key, false);

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0
                && mouseX >= renderX
                && mouseX <= renderX + renderWidth
                && mouseY >= renderY
                && mouseY <= renderY + renderHeight) {
            updateMapCenter((float) (mapCenterX - dragX / currentZoom), (float) (mapCenterZ - dragY / currentZoom));
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected void renderCoordinates(PoseStack poseStack, int mouseX, int mouseY) {
        int gameX = (int) ((mouseX - centerX) / currentZoom + mapCenterX);
        int gameZ = (int) ((mouseY - centerZ) / currentZoom + mapCenterZ);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(gameX + ", " + gameZ),
                        this.centerX,
                        this.renderHeight - this.renderedBorderYOffset - 40,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);
    }

    protected void renderMapButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.MAP_BUTTONS_BACKGROUND,
                this.centerX - Texture.MAP_BUTTONS_BACKGROUND.width() / 2f,
                this.renderHeight - this.renderedBorderYOffset - Texture.MAP_BUTTONS_BACKGROUND.height());

        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }

    protected void renderCursor(
            PoseStack poseStack, float pointerScale, CustomColor pointerColor, PointerType pointerType) {
        double pX = McUtils.player().getX();
        double pZ = McUtils.player().getZ();

        double distanceX = pX - mapCenterX;
        double distanceZ = pZ - mapCenterZ;

        float cursorX = (float) (centerX + distanceX * currentZoom);
        float cursorZ = (float) (centerZ + distanceZ * currentZoom);

        MapRenderer.renderCursor(poseStack, cursorX, cursorZ, pointerScale, pointerColor, pointerType, false);
    }

    protected void renderMap(PoseStack poseStack) {
        RenderUtils.enableScissor(
                (int) (renderX + renderedBorderXOffset), (int) (renderY + renderedBorderYOffset), (int) mapWidth, (int)
                        mapHeight);

        // Background black void color
        RenderUtils.drawRect(
                poseStack,
                CommonColors.BLACK,
                renderX + renderedBorderXOffset,
                renderY + renderedBorderYOffset,
                0,
                mapWidth,
                mapHeight);

        BoundingBox textureBoundingBox =
                BoundingBox.centered(mapCenterX, mapCenterZ, width / currentZoom, height / currentZoom);

        List<MapTexture> maps = Services.Map.getMapsForBoundingBox(textureBoundingBox);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(new BufferBuilder(256));

        for (MapTexture map : maps) {
            float textureX = map.getTextureXPosition(mapCenterX);
            float textureZ = map.getTextureZPosition(mapCenterZ);

            MapRenderer.renderMapQuad(
                    map,
                    poseStack,
                    bufferSource,
                    centerX,
                    centerZ,
                    textureX,
                    textureZ,
                    mapWidth,
                    mapHeight,
                    1f / currentZoom);
        }

        bufferSource.endBatch();

        RenderUtils.disableScissor();
    }

    protected void centerMapAroundPlayer() {
        updateMapCenter(
                (float) McUtils.player().getX(), (float) McUtils.player().getZ());
    }

    protected void centerMap() {
        updateMapCenter(MAP_CENTER_X, MAP_CENTER_Z);
        setZoomStep(0);
    }

    protected boolean isPlayerInsideMainArea() {
        return MathUtils.isInside(
                (int) McUtils.player().getX(), (int) McUtils.player().getZ(), MIN_X, MAX_X, MIN_Z, MAX_Z);
    }

    protected void adjustZoomStep(float delta) {
        currentZoomStep = MathUtils.clamp(currentZoomStep + delta, 1, MapRenderer.ZOOM_STEPS);
        recalculateZoom();
    }

    protected void setZoomStep(float zoomStep) {
        currentZoomStep = MathUtils.clamp(zoomStep, 1, MapRenderer.ZOOM_STEPS);
        recalculateZoom();
    }

    protected void recalculateZoom() {
        this.currentZoom = MapRenderer.getZoomFromSteps(currentZoomStep);
    }

    protected void updateMapCenter(float newX, float newZ) {
        this.mapCenterX = newX;
        this.mapCenterZ = newZ;
    }

    public void setHoldingMapKey(boolean holdingMapKey) {
        this.holdingMapKey = holdingMapKey;
    }
}
