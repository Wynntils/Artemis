/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays.minimap;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.map.MainMapFeature;
import com.wynntils.services.map.MapTexture;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.services.map.pois.WaypointPoi;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.MapRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.PointerType;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.BoundingBox;
import com.wynntils.utils.type.BoundingCircle;
import com.wynntils.utils.type.BoundingShape;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

public class MinimapOverlay extends Overlay {
    private static final int DEFAULT_SIZE = 130;

    @Persisted
    public final Config<Float> zoomLevel = new Config<>(MapRenderer.DEFAULT_ZOOM_LEVEL);

    @Persisted
    public final Config<Float> poiScale = new Config<>(0.6f);

    @Persisted
    public final Config<Float> pointerScale = new Config<>(0.8f);

    @Persisted
    public final Config<Boolean> followPlayerRotation = new Config<>(true);

    @Persisted
    public final Config<UnmappedOption> hideWhenUnmapped = new Config<>(UnmappedOption.MINIMAP_AND_COORDS);

    @Persisted
    public final Config<CustomColor> pointerColor = new Config<>(new CustomColor(1f, 1f, 1f, 1f));

    @Persisted
    public final Config<MapMaskType> maskType = new Config<>(MapMaskType.RECTANGULAR);

    @Persisted
    public final Config<MapBorderType> borderType = new Config<>(MapBorderType.WYNN);

    @Persisted
    public final Config<PointerType> pointerType = new Config<>(PointerType.ARROW);

    @Persisted
    public final Config<CompassRenderType> showCompass = new Config<>(CompassRenderType.ALL);

    @Persisted
    public final Config<Boolean> renderRemoteFriendPlayers = new Config<>(true);

    @Persisted
    public final Config<Boolean> renderRemotePartyPlayers = new Config<>(true);

    @Persisted
    public final Config<Float> remotePlayersHeadScale = new Config<>(0.4f);

    public MinimapOverlay() {
        super(
                new OverlayPosition(
                        20.25f,
                        5,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.LEFT,
                        OverlayPosition.AnchorSection.TOP_LEFT),
                new OverlaySize(DEFAULT_SIZE, DEFAULT_SIZE));
    }

    public void setZoomLevel(float level) {
        // Clamp zoom levels to allowed interval
        float clampedLevel = MathUtils.clamp(level, 1, MapRenderer.ZOOM_LEVELS);

        // If the level is the same, do nothing (avoid recursion loop)
        if (clampedLevel == zoomLevel.get()) return;

        zoomLevel.setValue(clampedLevel);
    }

    public void adjustZoomLevel(int delta) {
        setZoomLevel(zoomLevel.get() + delta);
    }

    // FIXME: This is the only overlay not to use buffer sources for rendering. This is due to `createMask`
    // currently not working with buffer sources.
    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        if (!Models.WorldState.onWorld()) return;

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        float width = getWidth();
        float height = getHeight();
        float renderX = getRenderX();
        float renderY = getRenderY();

        float centerX = renderX + width / 2;
        float centerZ = renderY + height / 2;

        double playerX = McUtils.player().getX();
        double playerZ = McUtils.player().getZ();

        final float zoomRenderScale = MapRenderer.getZoomRenderScaleFromLevel(zoomLevel.get());

        BoundingCircle textureBoundingCircle = BoundingCircle.enclosingCircle(BoundingBox.centered(
                (float) playerX, (float) playerZ, width * zoomRenderScale, height * zoomRenderScale));

        List<MapTexture> maps = Services.Map.getMapsForBoundingCircle(textureBoundingCircle);

        if (hideWhenUnmapped.get() != UnmappedOption.NEITHER && maps.isEmpty()) return;

        // enable mask
        switch (maskType.get()) {
            case RECTANGULAR -> RenderUtils.enableScissor((int) renderX, (int) renderY, (int) width, (int) height);
            case CIRCLE -> RenderUtils.createMask(
                    poseStack, Texture.CIRCLE_MASK, (int) renderX, (int) renderY, (int) (renderX + width), (int)
                            (renderY + height));
        }

        // Always draw a black background to cover transparent map areas
        RenderUtils.drawRect(poseStack, CommonColors.BLACK, renderX, renderY, 0, width, height);

        // enable rotation if necessary
        if (followPlayerRotation.get()) {
            poseStack.pushPose();
            RenderUtils.rotatePose(
                    poseStack,
                    centerX,
                    centerZ,
                    180 - McUtils.mc().gameRenderer.getMainCamera().getYRot());
        }

        // avoid rotational overpass - This is a rather loose oversizing, if possible later
        // use trignometry, etc. to find a better one
        float extraFactor = 1f;
        if (followPlayerRotation.get()) {
            // 1.5 > sqrt(2);
            extraFactor = 1.5F;

            if (width > height) {
                extraFactor *= width / height;
            } else {
                extraFactor *= height / width;
            }
        }

        for (MapTexture map : maps) {
            float textureX = map.getTextureXPosition(playerX);
            float textureZ = map.getTextureZPosition(playerZ);
            MapRenderer.renderMapQuad(
                    map,
                    poseStack,
                    centerX,
                    centerZ,
                    textureX,
                    textureZ,
                    width * extraFactor,
                    height * extraFactor,
                    zoomRenderScale);
        }

        // disable rotation if necessary
        if (followPlayerRotation.get()) {
            poseStack.popPose();
        }

        renderPois(
                poseStack,
                centerX,
                centerZ,
                width,
                height,
                playerX,
                playerZ,
                zoomRenderScale,
                zoomLevel.get(),
                textureBoundingCircle);

        // cursor
        MapRenderer.renderCursor(
                poseStack,
                centerX,
                centerZ,
                this.pointerScale.get(),
                this.pointerColor.get(),
                this.pointerType.get(),
                followPlayerRotation.get());

        // disable mask & render border
        switch (maskType.get()) {
            case RECTANGULAR -> RenderUtils.disableScissor();
            case CIRCLE -> RenderUtils.clearMask();
        }

        // render border
        renderMapBorder(poseStack, renderX, renderY, width, height);

        // Directional Text
        renderCardinalDirections(poseStack, width, height, centerX, centerZ);
    }

    private void renderPois(
            PoseStack poseStack,
            float centerX,
            float centerZ,
            float width,
            float height,
            double playerX,
            double playerZ,
            float zoomRenderScale,
            float zoomLevel,
            BoundingCircle textureBoundingCircle) {
        float sinRotationRadians;
        float cosRotationRadians;

        if (followPlayerRotation.get()) {
            double rotationRadians =
                    Math.toRadians(McUtils.mc().gameRenderer.getMainCamera().getYRot());
            sinRotationRadians = (float) StrictMath.sin(rotationRadians);
            cosRotationRadians = (float) -StrictMath.cos(rotationRadians);
        } else {
            sinRotationRadians = 0f;
            cosRotationRadians = 0f;
        }

        float currentZoom = 1f / zoomRenderScale;

        Stream<? extends Poi> poisToRender = Services.Poi.getServicePois();
        poisToRender = Stream.concat(poisToRender, Services.Poi.getCombatPois());
        poisToRender = Stream.concat(
                poisToRender, Managers.Feature.getFeatureInstance(MainMapFeature.class).customPois.get().stream());
        poisToRender = Stream.concat(poisToRender, Services.Poi.getProvidedCustomPois().stream());
        poisToRender = Stream.concat(poisToRender, Models.Marker.getAllPois());
        poisToRender = Stream.concat(
                poisToRender,
                Services.Hades.getMiniPlayerPois(renderRemotePartyPlayers.get(), renderRemoteFriendPlayers.get()));

        MultiBufferSource.BufferSource bufferSource =
                McUtils.mc().renderBuffers().bufferSource();

        Poi[] pois = poisToRender.toArray(Poi[]::new);
        for (Poi poi : pois) {
            float dX = (poi.getLocation().getX() - (float) playerX) / zoomRenderScale;
            float dZ = (poi.getLocation().getZ() - (float) playerZ) / zoomRenderScale;

            if (followPlayerRotation.get()) {
                float tempdX = dX * cosRotationRadians - dZ * sinRotationRadians;

                dZ = dX * sinRotationRadians + dZ * cosRotationRadians;
                dX = tempdX;
            }

            float poiRenderX = centerX + dX;
            float poiRenderZ = centerZ + dZ;

            float poiWidth = poi.getWidth(currentZoom, poiScale.get());
            float poiHeight = poi.getHeight(currentZoom, poiScale.get());

            BoundingBox box = BoundingBox.centered(
                    poi.getLocation().getX(), poi.getLocation().getZ(), (int) poiWidth, (int) poiHeight);

            if (BoundingShape.intersects(box, textureBoundingCircle)) {
                poi.renderAt(
                        poseStack,
                        bufferSource,
                        poiRenderX,
                        poiRenderZ,
                        false,
                        poiScale.get(),
                        currentZoom,
                        zoomLevel,
                        false);
            }
        }

        bufferSource.endBatch();

        // Compass icon
        List<WaypointPoi> waypointPois =
                Models.Marker.USER_WAYPOINTS_PROVIDER.getPois().toList();
        for (WaypointPoi waypointPoi : waypointPois) {
            PoiLocation compassLocation = waypointPoi.getLocation();
            if (compassLocation == null) return;

            float compassOffsetX = (compassLocation.getX() - (float) playerX) / zoomRenderScale;
            float compassOffsetZ = (compassLocation.getZ() - (float) playerZ) / zoomRenderScale;

            if (followPlayerRotation.get()) {
                float tempCompassOffsetX = compassOffsetX * cosRotationRadians - compassOffsetZ * sinRotationRadians;

                compassOffsetZ = compassOffsetX * sinRotationRadians + compassOffsetZ * cosRotationRadians;
                compassOffsetX = tempCompassOffsetX;
            }

            final float compassSize = Math.max(
                            waypointPoi.getWidth(currentZoom, poiScale.get()),
                            waypointPoi.getHeight(currentZoom, poiScale.get()))
                    * 0.8f;

            float compassRenderX = compassOffsetX + centerX;
            float compassRenderZ = compassOffsetZ + centerZ;

            // Normalize offset for later
            float distance = MathUtils.magnitude(compassOffsetX, compassOffsetZ);
            compassOffsetX /= distance;
            compassOffsetZ /= distance;

            // Subtract compassSize so scaled remains within boundary
            float scaledWidth = width - 2 * compassSize;
            float scaledHeight = height - 2 * compassSize;

            float toBorderScale = 1f;

            if (maskType.get() == MapMaskType.RECTANGULAR) {
                // Scale as necessary
                toBorderScale =
                        Math.min(scaledWidth / Math.abs(compassOffsetX), scaledHeight / Math.abs(compassOffsetZ)) / 2;
            } else if (maskType.get() == MapMaskType.CIRCLE) {
                toBorderScale = scaledWidth
                        / (MathUtils.magnitude(compassOffsetX, compassOffsetZ * scaledWidth / scaledHeight))
                        / 2;
            }

            if (toBorderScale < distance) {
                // Scale to border
                compassRenderX = centerX + compassOffsetX * toBorderScale;
                compassRenderZ = centerZ + compassOffsetZ * toBorderScale;

                // Replace with pointer
                float angle = (float) Math.toDegrees(StrictMath.atan2(compassOffsetZ, compassOffsetX)) + 90f;

                poseStack.pushPose();
                RenderUtils.rotatePose(poseStack, compassRenderX, compassRenderZ, angle);
                waypointPoi
                        .getPointerPoi()
                        .renderAt(
                                poseStack,
                                bufferSource,
                                compassRenderX,
                                compassRenderZ,
                                false,
                                poiScale.get(),
                                1f / zoomRenderScale,
                                zoomLevel,
                                false);
                poseStack.popPose();
            } else {
                waypointPoi.renderAt(
                        poseStack,
                        bufferSource,
                        compassRenderX,
                        compassRenderZ,
                        false,
                        poiScale.get(),
                        currentZoom,
                        zoomLevel,
                        false);
            }

            bufferSource.endBatch();

            poseStack.pushPose();
            poseStack.translate(centerX, centerZ, 0);
            poseStack.scale(0.8f, 0.8f, 1);
            poseStack.translate(-centerX, -centerZ, 0);

            FontRenderer fontRenderer = FontRenderer.getInstance();
            Font font = fontRenderer.getFont();

            String text = StringUtils.integerToShortString(Math.round(distance * zoomRenderScale)) + "m";
            float w = font.width(text) / 2f, h = font.lineHeight / 2f;

            RenderUtils.drawRect(
                    poseStack,
                    new CustomColor(0f, 0f, 0f, 0.7f),
                    compassRenderX - w - 3f,
                    compassRenderZ - h - 1f,
                    0,
                    2 * w + 6,
                    2 * h + 1);
            fontRenderer.renderText(
                    poseStack,
                    StyledText.fromString(text),
                    compassRenderX,
                    compassRenderZ - 3f,
                    CommonColors.WHITE,
                    HorizontalAlignment.CENTER,
                    VerticalAlignment.TOP,
                    TextShadow.NORMAL);

            poseStack.popPose();
        }
    }

    private void renderCardinalDirections(
            PoseStack poseStack, float width, float height, float centerX, float centerZ) {
        if (showCompass.get() == CompassRenderType.NONE) return;

        float northDX;
        float northDY;

        if (followPlayerRotation.get()) {
            float yawRadians = (float)
                    Math.toRadians(McUtils.mc().gameRenderer.getMainCamera().getYRot());
            northDX = (float) StrictMath.sin(yawRadians);
            northDY = (float) StrictMath.cos(yawRadians);

            double toBorderScaleNorth = 1;

            if (maskType.get() == MapMaskType.RECTANGULAR) {
                toBorderScaleNorth = Math.min(width / Math.abs(northDX), height / Math.abs(northDY)) / 2;
            } else if (maskType.get() == MapMaskType.CIRCLE) {
                toBorderScaleNorth = width / (MathUtils.magnitude(northDX, northDY * width / height)) / 2;
            }

            northDX *= toBorderScaleNorth;
            northDY *= toBorderScaleNorth;

        } else {
            northDX = 0;
            northDY = -height / 2;
        }

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        centerX + northDX,
                        centerZ + northDY,
                        new TextRenderTask("N", TextRenderSetting.CENTERED));

        if (showCompass.get() == CompassRenderType.NORTH) return;

        // we can't do manipulations from north to east as it might not be square
        float eastDX;
        float eastDY;

        if (followPlayerRotation.get()) {
            eastDX = -northDY;
            eastDY = northDX;

            double toBorderScaleEast = 1f;

            if (maskType.get() == MapMaskType.RECTANGULAR) {
                toBorderScaleEast = Math.min(width / Math.abs(northDY), height / Math.abs(northDX)) / 2;
            } else if (maskType.get() == MapMaskType.CIRCLE) {
                toBorderScaleEast = width / (MathUtils.magnitude(eastDX, eastDY * width / height)) / 2;
            }

            eastDX *= toBorderScaleEast;
            eastDY *= toBorderScaleEast;
        } else {
            eastDX = width / 2;
            eastDY = 0;
        }

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        centerX + eastDX,
                        centerZ + eastDY,
                        new TextRenderTask("E", TextRenderSetting.CENTERED));
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        centerX - northDX,
                        centerZ - northDY,
                        new TextRenderTask("S", TextRenderSetting.CENTERED));
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        centerX - eastDX,
                        centerZ - eastDY,
                        new TextRenderTask("W", TextRenderSetting.CENTERED));
    }

    private void renderMapBorder(PoseStack poseStack, float renderX, float renderY, float width, float height) {
        Texture texture = borderType.get().texture();
        int grooves = borderType.get().groovesSize();
        BorderInfo borderInfo = maskType.get() == MapMaskType.CIRCLE
                ? borderType.get().circle()
                : borderType.get().square();
        int tx1 = borderInfo.tx1();
        int ty1 = borderInfo.ty1();
        int tx2 = borderInfo.tx2();
        int ty2 = borderInfo.ty2();

        // Scale to stay the same.
        float groovesWidth = grooves * width / DEFAULT_SIZE;
        float groovesHeight = grooves * height / DEFAULT_SIZE;

        RenderUtils.drawTexturedRect(
                poseStack,
                texture.resource(),
                renderX - groovesWidth,
                renderY - groovesHeight,
                0,
                width + 2 * groovesWidth,
                height + 2 * groovesHeight,
                tx1,
                ty1,
                tx2 - tx1,
                ty2 - ty1,
                texture.width(),
                texture.height());
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        if (config == zoomLevel) {
            // Make sure it is a valid level
            setZoomLevel(zoomLevel.get());
        }
    }

    public enum UnmappedOption {
        MINIMAP,
        MINIMAP_AND_COORDS,
        NEITHER
    }

    private enum CompassRenderType {
        NONE,
        NORTH,
        ALL
    }

    private enum MapMaskType {
        RECTANGULAR,
        CIRCLE
    }

    private enum MapBorderType {
        GILDED(Texture.GILDED_MAP_TEXTURES, new BorderInfo(0, 262, 262, 524), new BorderInfo(0, 0, 262, 262), 1),
        PAPER(Texture.PAPER_MAP_TEXTURES, new BorderInfo(0, 0, 217, 217), new BorderInfo(0, 217, 217, 438), 3),
        WYNN(Texture.WYNN_MAP_TEXTURES, new BorderInfo(0, 0, 112, 112), new BorderInfo(0, 112, 123, 235), 3);

        private final Texture texture;
        private final BorderInfo square;
        private final BorderInfo circle;
        private final int groovesSize;

        MapBorderType(Texture texture, BorderInfo square, BorderInfo circle, int groovesSize) {
            this.texture = texture;
            this.square = square;
            this.circle = circle;
            this.groovesSize = groovesSize;
        }

        private Texture texture() {
            return texture;
        }

        private int groovesSize() {
            return groovesSize;
        }

        private BorderInfo square() {
            return square;
        }

        private BorderInfo circle() {
            return circle;
        }
    }

    private record BorderInfo(int tx1, int ty1, int tx2, int ty2) {}
}
