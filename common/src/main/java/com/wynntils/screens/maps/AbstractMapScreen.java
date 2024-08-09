/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.debug.MappingProgressFeature;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.services.map.MapTexture;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
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
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractMapScreen extends WynntilsScreen {
    protected static final MultiBufferSource.BufferSource BUFFER_SOURCE =
            MultiBufferSource.immediate(new ByteBufferBuilder(256));

    protected static final float SCREEN_SIDE_OFFSET = 10;
    private static final float BORDER_OFFSET = 6;
    private static final int MAP_CENTER_X = -400;
    private static final int MAP_CENTER_Z = -1700;
    private static final int MAX_X = 1650;
    private static final int MAX_Z = -150;
    private static final int MIN_X = -2400;
    private static final int MIN_Z = -6600;
    private static final int CENTER_ZOOM_LEVEL = 20;

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

    // Zooming updates zoomLevel, but we also cache zoomRenderScale for rendering
    protected float zoomLevel = MapRenderer.DEFAULT_ZOOM_LEVEL;
    protected float zoomRenderScale = MapRenderer.getZoomRenderScaleFromLevel(zoomLevel);

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

            float poiRenderX = MapRenderer.getRenderX(poi, mapCenterX, centerX, zoomRenderScale);
            float poiRenderZ = MapRenderer.getRenderZ(poi, mapCenterZ, centerZ, zoomRenderScale);

            poi.renderAt(
                    poseStack,
                    bufferSource,
                    poiRenderX,
                    poiRenderZ,
                    hovered == poi,
                    poiScale,
                    zoomRenderScale,
                    zoomLevel,
                    true);
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

            if (!poi.isVisible(zoomRenderScale, zoomLevel)) {
                continue;
            }

            float poiRenderX = MapRenderer.getRenderX(poi, mapCenterX, centerX, zoomRenderScale);
            float poiRenderZ = MapRenderer.getRenderZ(poi, mapCenterZ, centerZ, zoomRenderScale);

            float poiWidth = poi.getWidth(zoomRenderScale, poiScale);
            float poiHeight = poi.getHeight(zoomRenderScale, poiScale);

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
            filteredPois.addFirst(hovered);
        }

        return filteredPois;
    }

    protected void setCompassToMouseCoords(double mouseX, double mouseY, boolean removeAll) {
        if (removeAll) {
            Models.Marker.USER_WAYPOINTS_PROVIDER.removeAllLocations();
        }

        double gameX = (mouseX - centerX) / zoomRenderScale + mapCenterX;
        double gameZ = (mouseY - centerZ) / zoomRenderScale + mapCenterZ;
        Location compassLocation = Location.containing(gameX, 0, gameZ);
        Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(compassLocation);

        McUtils.playSoundUI(SoundEvents.EXPERIENCE_ORB_PICKUP);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        adjustZoomLevel((float) (2f * deltaY));
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
            adjustZoomLevel(2);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            // Take steps of 2 to make it easier to zoom in and out
            adjustZoomLevel(-2);
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
            updateMapCenter(
                    (float) (mapCenterX - dragX / zoomRenderScale), (float) (mapCenterZ - dragY / zoomRenderScale));
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected void renderCoordinates(PoseStack poseStack, int mouseX, int mouseY) {
        int gameX = (int) ((mouseX - centerX) / zoomRenderScale + mapCenterX);
        int gameZ = (int) ((mouseY - centerZ) / zoomRenderScale + mapCenterZ);

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

    protected void renderZoomWidget(PoseStack poseStack, int mouseX, int mouseY) {
        if (!KeyboardUtils.isShiftDown()) return;

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString("Zoom " + Math.round(zoomLevel)),
                        renderX + renderedBorderXOffset + mapWidth - 40,
                        this.renderHeight - this.renderedBorderYOffset - 10,
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

        float cursorX = (float) (centerX + distanceX * zoomRenderScale);
        float cursorZ = (float) (centerZ + distanceZ * zoomRenderScale);

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
                BoundingBox.centered(mapCenterX, mapCenterZ, width / zoomRenderScale, height / zoomRenderScale);

        List<MapTexture> maps = Services.Map.getMapsForBoundingBox(textureBoundingBox);

        for (MapTexture map : maps) {
            float textureX = map.getTextureXPosition(mapCenterX);
            float textureZ = map.getTextureZPosition(mapCenterZ);

            MapRenderer.renderMapQuad(
                    map,
                    poseStack,
                    BUFFER_SOURCE,
                    centerX,
                    centerZ,
                    textureX,
                    textureZ,
                    mapWidth,
                    mapHeight,
                    1f / zoomRenderScale);
        }

        BUFFER_SOURCE.endBatch();

        RenderUtils.disableScissor();
    }

    protected void renderChunkBorders(PoseStack poseStack) {
        BoundingBox textureBoundingBox =
                BoundingBox.centered(mapCenterX, mapCenterZ, width / zoomRenderScale, height / zoomRenderScale);

        // If the user is holding shift, only render close-by pois
        float pX = (float) McUtils.player().getX();
        float pZ = (float) McUtils.player().getZ();

        BoundingBox chunkBoundingBox = KeyboardUtils.isShiftDown()
                ? textureBoundingBox
                : BoundingBox.centered(
                        pX,
                        pZ,
                        McUtils.options().renderDistance().get() * 16,
                        McUtils.options().renderDistance().get() * 16);

        Set<Long> mappedChunks = Managers.Feature.getFeatureInstance(MappingProgressFeature.class)
                .getMappedChunks();

        MapRenderer.renderChunks(
                poseStack,
                BUFFER_SOURCE,
                chunkBoundingBox,
                mappedChunks,
                mapCenterX,
                centerX,
                mapCenterZ,
                centerZ,
                zoomRenderScale);
    }

    protected void centerMapAroundPlayer() {
        updateMapCenter(
                (float) McUtils.player().getX(), (float) McUtils.player().getZ());
    }

    protected void centerMapOnWorld() {
        updateMapCenter(MAP_CENTER_X, MAP_CENTER_Z);
        setZoomLevel(CENTER_ZOOM_LEVEL);
    }

    protected boolean isPlayerInsideMainArea() {
        return MathUtils.isInside(
                (int) McUtils.player().getX(), (int) McUtils.player().getZ(), MIN_X, MAX_X, MIN_Z, MAX_Z);
    }

    protected void setZoomLevel(float zoomLevel) {
        this.zoomLevel = MathUtils.clamp(zoomLevel, 1, MapRenderer.ZOOM_LEVELS);
        // Recalculate the cached zoom render scale
        this.zoomRenderScale = MapRenderer.getZoomRenderScaleFromLevel(this.zoomLevel);
    }

    protected void adjustZoomLevel(float delta) {
        setZoomLevel(zoomLevel + delta);
    }

    protected void updateMapCenter(float newX, float newZ) {
        this.mapCenterX = newX;
        this.mapCenterZ = newZ;
    }

    public void setHoldingMapKey(boolean holdingMapKey) {
        this.holdingMapKey = holdingMapKey;
    }
}
