/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.map;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.event.RenderLevelEvent;
import com.wynntils.models.marker.type.MarkerInfo;
import com.wynntils.services.map.pois.WaypointPoi;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ConfigCategory(Category.MAP)
public class WorldWaypointDistanceFeature extends Feature {
    private static final MultiBufferSource.BufferSource BUFFER_SOURCE =
            MultiBufferSource.immediate(new ByteBufferBuilder(256));
    private static final WaypointPoi DUMMY_WAYPOINT = new WaypointPoi(() -> null, "");

    @Persisted
    public final Config<Float> backgroundOpacity = new Config<>(0.2f);

    @Persisted
    public final Config<Float> scale = new Config<>(1.0f);

    @Persisted
    public final Config<TextShadow> textShadow = new Config<>(TextShadow.NONE);

    @Persisted
    public final Config<Float> bottomBoundingDistance = new Config<>(100f);

    @Persisted
    public final Config<Float> topBoundingDistance = new Config<>(40f);

    @Persisted
    public final Config<Float> horizontalBoundingDistance = new Config<>(30f);

    @Persisted
    public final Config<Integer> maxWaypointTextDistance = new Config<>(5000);

    private final List<RenderedMarkerInfo> renderedMarkers = new ArrayList<>();

    @SubscribeEvent
    public void onRenderLevelPost(RenderLevelEvent.Post event) {
        this.renderedMarkers.clear();

        List<MarkerInfo> markers = Models.Marker.getAllMarkers().toList();
        if (markers.isEmpty()) return;

        for (MarkerInfo marker : markers) {
            Location location = marker.location();
            Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
            Camera camera = event.getCamera();
            Position cameraPos = camera.getPosition();

            // apply camera rotation
            Vector3f xp = new Vector3f(1, 0, 0);
            Vector3f yp = new Vector3f(0, 1, 0);
            Quaternionf xRotation = new Quaternionf().rotationAxis((float) Math.toRadians(camera.getXRot()), xp);
            Quaternionf yRotation = new Quaternionf().rotationAxis((float) Math.toRadians(camera.getYRot() + 180f), yp);
            projection.mul(new Matrix4f().rotation(xRotation));
            projection.mul(new Matrix4f().rotation(yRotation));

            // offset to put text to the center of the block
            float dx = (float) (location.x + 0.5 - cameraPos.x());
            float dy = (float) (location.y + 0.5 - cameraPos.y());
            float dz = (float) (location.z + 0.5 - cameraPos.z());

            if (location.y <= 0 || location.y > 255) {
                dy = 0;
            }

            double squaredDistance = dx * dx + dy * dy + dz * dz;

            double distance = Math.sqrt(squaredDistance);
            int maxDistance = McUtils.options().renderDistance().get() * 16;

            String distanceText = Math.round((float) distance) + "m";

            // move the position to avoid ndc z leak past 1
            if (distance > maxDistance) {
                double posScale = maxDistance / distance;
                dx *= posScale;
                dz *= posScale;
            }

            this.renderedMarkers.add(new RenderedMarkerInfo(
                    distance, distanceText, marker, worldToScreen(new Vector3f(dx, dy, dz), projection)));
        }
    }

    @SubscribeEvent
    public void onRenderGuiPost(RenderEvent.Post event) {
        for (RenderedMarkerInfo renderedMarker : renderedMarkers) {
            if (maxWaypointTextDistance.get() != 0 && maxWaypointTextDistance.get() < renderedMarker.distance) continue;

            float backgroundWidth = FontRenderer.getInstance().getFont().width(renderedMarker.distanceText);
            float backgroundHeight = FontRenderer.getInstance().getFont().lineHeight;

            float displayPositionX;
            float displayPositionY;

            Vec2 intersectPoint = getBoundingIntersectPoint(renderedMarker.screenCoordinates, event.getWindow());
            Texture icon = renderedMarker.markerInfo.texture();
            float[] color = renderedMarker.markerInfo.textureColor().asFloatArray();
            RenderSystem.setShaderColor(color[0], color[1], color[2], 1f);

            // The set waypoint is visible on the screen, so we render the icon + distance
            if (intersectPoint == null) {
                displayPositionX = (float) renderedMarker.screenCoordinates.x;
                displayPositionY = (float) renderedMarker.screenCoordinates.y;

                RenderUtils.drawScalingTexturedRect(
                        event.getPoseStack(),
                        icon.resource(),
                        displayPositionX - scale.get() * icon.width() / 2,
                        displayPositionY - scale.get() * (icon.height() + backgroundHeight / 2 + 3f),
                        0,
                        scale.get() * icon.width(),
                        scale.get() * icon.height(),
                        icon.width(),
                        icon.height());
                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderUtils.drawRect(
                        event.getPoseStack(),
                        CommonColors.BLACK.withAlpha(backgroundOpacity.get()),
                        displayPositionX - scale.get() * (backgroundWidth / 2 + 2),
                        displayPositionY - scale.get() * (backgroundHeight / 2 + 2),
                        0,
                        scale.get() * (backgroundWidth + 3),
                        scale.get() * (backgroundHeight + 2));
                FontRenderer.getInstance()
                        .renderAlignedTextInBox(
                                event.getPoseStack(),
                                StyledText.fromString(renderedMarker.distanceText),
                                displayPositionX - scale.get() * backgroundWidth,
                                displayPositionX + scale.get() * backgroundWidth,
                                displayPositionY - scale.get() * backgroundHeight,
                                displayPositionY + scale.get() * backgroundHeight,
                                0,
                                renderedMarker.markerInfo.textColor(),
                                HorizontalAlignment.CENTER,
                                VerticalAlignment.MIDDLE,
                                textShadow.get(),
                                scale.get());
            } else {
                displayPositionX = intersectPoint.x;
                displayPositionY = intersectPoint.y;

                // pointer position is determined by finding the point on circle centered around displayPosition
                double angle = Math.toDegrees(StrictMath.atan2(
                                displayPositionY - event.getWindow().getGuiScaledHeight() / 2,
                                displayPositionX - event.getWindow().getGuiScaledWidth() / 2))
                        + 90f;
                float radius = icon.width() / 2 + 8f;

                float pointerOffsetX = radius * (float) StrictMath.cos((angle - 90) * 3 / 180);
                float pointerOffsetY = radius * (float) StrictMath.sin((angle - 90) * 3 / 180);

                float pointerDisplayPositionX = displayPositionX + pointerOffsetX;
                float pointerDisplayPositionY = displayPositionY + pointerOffsetY;

                RenderUtils.drawScalingTexturedRect(
                        event.getPoseStack(),
                        icon.resource(),
                        displayPositionX - scale.get() * icon.width() / 2 + pointerOffsetX * (1 - scale.get()),
                        displayPositionY - scale.get() * icon.height() / 2 + pointerOffsetY * (1 - scale.get()),
                        0,
                        scale.get() * icon.width(),
                        scale.get() * icon.height(),
                        icon.width(),
                        icon.height());
                RenderSystem.setShaderColor(1, 1, 1, 1);

                // apply rotation
                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                poseStack.translate(pointerDisplayPositionX, pointerDisplayPositionY, 0);
                poseStack.mulPose(new Quaternionf().rotationXYZ(0, 0, (float) Math.toRadians(angle)));
                poseStack.translate(-pointerDisplayPositionX, -pointerDisplayPositionY, 0);

                DUMMY_WAYPOINT
                        .getPointerPoi()
                        .renderAt(
                                poseStack,
                                BUFFER_SOURCE,
                                pointerDisplayPositionX,
                                pointerDisplayPositionY,
                                false,
                                scale.get(),
                                1,
                                50,
                                true);
                BUFFER_SOURCE.endBatch();
                poseStack.popPose();
            }
        }
    }

    private Vec3 worldToScreen(Vector3f delta, Matrix4f projection) {
        Vector4f clipCoords = new Vector4f(delta.x(), delta.y(), delta.z(), 1.0f);
        projection.transform(clipCoords);

        // stands for Normalized Device Coordinates
        Vector3d ndc = new Vector3d(
                clipCoords.x() / clipCoords.w(), clipCoords.y() / clipCoords.w(), clipCoords.z() / clipCoords.w());

        Window window = McUtils.window();

        return new Vec3(
                (float) ((ndc.x + 1.0f) / 2.0f) * window.getGuiScaledWidth(),
                (float) ((1.0f - ndc.y) / 2.0f) * window.getGuiScaledHeight(),
                (float) ndc.z);
    }

    // draw a line from screen center to the target's screenspace coordinate
    // and find the intersect point on one of the screen's bounding
    private Vec2 getBoundingIntersectPoint(Vec3 position, Window window) {
        if (isInBound(position, window)) return null;
        Vec3 centerPoint = new Vec3(window.getGuiScaledWidth() / 2, window.getGuiScaledHeight() / 2, 0);

        float pointerScaleCorrection = (float) Texture.POINTER.height() / 2 * (1 - scale.get());

        // minecraft's origin point is top left corner
        // so positive Y is at the screen bottom
        float minX = (float) -(centerPoint.x - horizontalBoundingDistance.get() + pointerScaleCorrection);
        float maxX = (float) centerPoint.x - horizontalBoundingDistance.get() + pointerScaleCorrection;
        float minY = (float) -(centerPoint.y - topBoundingDistance.get() + pointerScaleCorrection);
        float maxY = (float) centerPoint.y - bottomBoundingDistance.get() + pointerScaleCorrection;

        // drag the origin point to center since indicator's screenspace position / rotation is in relation to it
        Vec3 centerRelativePosition = position.subtract(centerPoint);

        // invert xy axis if target is behind camera
        if (centerRelativePosition.z > 1) {
            centerRelativePosition = centerRelativePosition.multiply(-1, -1, 1);
        }

        // since center point is now the origin point, atan2 is used to get the angle, and angle is used to get the
        // line's slope
        double angle = StrictMath.atan2(centerRelativePosition.y, centerRelativePosition.x);
        double m = StrictMath.tan(angle);

        // trying to solve (y2 - y1) = m (x2 - x1) + c here
        // starting from origin point/screen center (x1, y1), end at one of the screen bounding
        // (y2 - y1) is the equivalent of the y position
        // (x2 - x1) is the equivalent of the x position
        // c is the line's y-intercept, but line pass through origin, so this will be 0
        // finalize to y = mx, or x = y/m

        if (centerRelativePosition.x > 0) {
            centerRelativePosition = new Vec3(maxX, maxX * m, 0);
        } else {
            centerRelativePosition = new Vec3(minX, minX * m, 0);
        }

        if (centerRelativePosition.y > maxY) {
            centerRelativePosition = new Vec3(maxY / m, maxY, 0);
        } else if (centerRelativePosition.y < minY) {
            centerRelativePosition = new Vec3(minY / m, minY, 0);
        }

        // bring the position back to normal screen space (top left origin point)
        return new Vec2(
                (float) (centerRelativePosition.x + centerPoint.x), (float) (centerRelativePosition.y + centerPoint.y));
    }

    private boolean isInBound(Position position, Window window) {
        return position.x() > 0
                && position.x() < window.getGuiScaledWidth()
                && position.y() > 0
                && position.y() < window.getGuiScaledHeight()
                && position.z() < 1;
    }

    // limit the bounding distance to prevent divided by zero in getBoundingIntersectPoint
    @Override
    protected void onConfigUpdate(Config<?> unknownConfig) {
        Window window = McUtils.window();

        switch (unknownConfig.getFieldName()) {
            case "topBoundingDistance", "bottomBoundingDistance" -> {
                Config<Float> config = (Config<Float>) unknownConfig;
                if (config.get() > window.getGuiScaledHeight() * 0.4f) {
                    config.setValue(window.getGuiScaledHeight() * 0.4f);
                }
            }
            case "horizontalBoundingDistance" -> {
                Config<Float> config = (Config<Float>) unknownConfig;
                if (config.get() > window.getGuiScaledWidth() * 0.4f) {
                    config.setValue(window.getGuiScaledWidth() * 0.4f);
                }
            }
        }
    }

    private record RenderedMarkerInfo(
            double distance, String distanceText, MarkerInfo markerInfo, Vec3 screenCoordinates) {}
}
