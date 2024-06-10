/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.map;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.event.RenderTileLevelLastEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.services.map.pois.WaypointPoi;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapAttributes;
import com.wynntils.services.mapdata.attributes.type.ResolvedMarkerOptions;
import com.wynntils.services.mapdata.type.MapLocation;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.CustomBeaconRenderer;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ConfigCategory(Category.MAP)
public class WorldMarkersFeature extends Feature {
    private static final float MINIMUM_RENDER_VISIBILITY = 0.1f;
    private static final int RAINBOW_CHANGE_RATE = 10;

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
    public final Config<Integer> maxMarkerDistance = new Config<>(5000);

    @Persisted
    public final Config<CustomColor> waypointBeamColor = new Config<>(CommonColors.RED);

    private final List<RenderedMapLocation> renderedMapLocations = new ArrayList<>();
    private CustomColor currentRainbowColor = CommonColors.RED;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        int r = currentRainbowColor.r;
        int g = currentRainbowColor.g;
        int b = currentRainbowColor.b;

        if (r > 0 && b == 0) {
            r -= RAINBOW_CHANGE_RATE;
            g += RAINBOW_CHANGE_RATE;
        }
        if (g > 0 && r == 0) {
            g -= RAINBOW_CHANGE_RATE;
            b += RAINBOW_CHANGE_RATE;
        }
        if (b > 0 && g == 0) {
            r += RAINBOW_CHANGE_RATE;
            b -= RAINBOW_CHANGE_RATE;
        }

        r = MathUtils.clamp(r, 0, 255);
        g = MathUtils.clamp(g, 0, 255);
        b = MathUtils.clamp(b, 0, 255);
        currentRainbowColor = new CustomColor(r, g, b, 255);
    }

    // Rendered map location calculation happens here
    // Beacon beam rendering happens here
    @SubscribeEvent
    public void onRenderLevelLast(RenderTileLevelLastEvent event) {
        // We update the rendered map locations here so that we can render the beacon beams in the same event
        // and also use it below to render the icons and labels
        updateRenderedMapLocations(event.getProjectionMatrix(), event.getCamera());
        if (renderedMapLocations.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource =
                MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        // render beacon beams
        for (RenderedMapLocation mapLocationPair : renderedMapLocations) {
            double alpha = mapLocationPair.visibility();
            MapLocation mapLocation = mapLocationPair.mapLocation();

            ResolvedMapAttributes resolvedMapAttributes = Services.MapData.resolveMapAttributes(mapLocation);
            ResolvedMarkerOptions resolvedMarkerOptions = resolvedMapAttributes.markerOptions();

            Position camera = event.getCamera().getPosition();
            Location location = mapLocation.getLocation();

            double dx = location.x - camera.x();
            double dy = location.y - camera.y();
            double dz = location.z - camera.z();

            double distance = MathUtils.magnitude(dx, dz);
            int maxDistance = McUtils.options().renderDistance().get() * 16;

            if (distance > maxDistance) {
                double scale = maxDistance / distance;

                dx *= scale;
                dz *= scale;
            }

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);

            CustomColor color = resolvedMarkerOptions.beaconColor() == CustomColor.NONE
                    ? waypointBeamColor.get()
                    : resolvedMarkerOptions.beaconColor();

            float[] colorArray;
            if (color == CommonColors.RAINBOW) {
                colorArray = currentRainbowColor.asFloatArray();
            } else {
                colorArray = color.asFloatArray();
            }

            CustomBeaconRenderer.renderBeaconBeam(
                    poseStack,
                    bufferSource,
                    BeaconRenderer.BEAM_LOCATION,
                    event.getPartialTick(),
                    1f,
                    McUtils.player().level().getGameTime(),
                    0,
                    1024,
                    colorArray,
                    (float) alpha,
                    0.166f,
                    0.33f);

            poseStack.popPose();
        }

        bufferSource.endLastBatch();
    }

    private void updateRenderedMapLocations(Matrix4f projectionMatrix, Camera camera) {
        this.renderedMapLocations.clear();

        List<Pair<Double, MapLocation>> mapLocations = Services.MapData.getFeatures()
                .filter(mapFeature -> mapFeature instanceof MapLocation)
                .map(mapFeature -> (MapLocation) mapFeature)
                .map(mapLocation -> {
                    ResolvedMapAttributes resolvedMapAttributes = Services.MapData.resolveMapAttributes(mapLocation);
                    return Pair.of(
                            getMarkerVisibility(mapLocation.getLocation(), resolvedMapAttributes.markerOptions()),
                            mapLocation);
                })
                .filter(pair -> pair.a() >= MINIMUM_RENDER_VISIBILITY)
                .toList();

        if (mapLocations.isEmpty()) return;

        for (Pair<Double, MapLocation> mapLocationPair : mapLocations) {
            double visibility = mapLocationPair.a();
            MapLocation mapLocation = mapLocationPair.b();

            Location location = mapLocation.getLocation();

            Matrix4f projection = new Matrix4f(projectionMatrix);
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

            // move the position to avoid ndc z leak past 1
            if (distance > maxDistance) {
                double posScale = maxDistance / distance;
                dx *= (float) posScale;
                dz *= (float) posScale;
            }

            this.renderedMapLocations.add(new RenderedMapLocation(
                    mapLocation, distance, visibility, worldToScreen(new Vector3f(dx, dy, dz), projection)));
        }
    }

    // Rendering the icons and labels/distance happens here
    @SubscribeEvent
    public void onRenderGuiPost(RenderEvent.Post event) {
        for (RenderedMapLocation renderedMapLocation : renderedMapLocations) {
            if (maxMarkerDistance.get() != 0 && maxMarkerDistance.get() < renderedMapLocation.distance) {
                continue;
            }

            ResolvedMapAttributes resolvedMapAttributes =
                    Services.MapData.resolveMapAttributes(renderedMapLocation.mapLocation);
            ResolvedMarkerOptions resolvedMarkerOptions = resolvedMapAttributes.markerOptions();

            String renderedDistanceText = Math.round((float) renderedMapLocation.distance) + "m";
            float backgroundWidth = Math.max(
                    resolvedMarkerOptions.renderDistance()
                            ? FontRenderer.getInstance().getFont().width(renderedDistanceText)
                            : 0,
                    resolvedMarkerOptions.renderLabel()
                            ? FontRenderer.getInstance().getFont().width(resolvedMapAttributes.label())
                            : 0);
            float backgroundHeight =
                    ((resolvedMarkerOptions.renderDistance() ? 1 : 0) + (resolvedMarkerOptions.renderLabel() ? 1 : 0))
                            * FontRenderer.getInstance().getFont().lineHeight;

            float displayPositionX;
            float displayPositionY;

            Vec2 intersectPoint = getBoundingIntersectPoint(renderedMapLocation.screenCoordinates, event.getWindow());

            MapIcon icon = Services.MapData.getIconOrFallback(resolvedMapAttributes.iconId());
            float[] iconColor = resolvedMapAttributes.iconColor().asFloatArray();

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(
                    iconColor[0], iconColor[1], iconColor[2], (float) renderedMapLocation.visibility);

            // The set waypoint is visible on the screen, so we render the icon + distance
            if (intersectPoint == null) {
                displayPositionX = (float) renderedMapLocation.screenCoordinates.x;
                displayPositionY = (float) renderedMapLocation.screenCoordinates.y;

                if (resolvedMarkerOptions.renderIcon()) {
                    RenderUtils.drawScalingTexturedRect(
                            event.getPoseStack(),
                            icon.getResourceLocation(),
                            displayPositionX - scale.get() * icon.getWidth() / 2,
                            displayPositionY - scale.get() * (icon.getHeight() + backgroundHeight / 2 + 3f),
                            0,
                            scale.get() * icon.getWidth(),
                            scale.get() * icon.getHeight(),
                            icon.getWidth(),
                            icon.getHeight());
                }

                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderSystem.disableBlend();

                if (resolvedMarkerOptions.renderDistance() || resolvedMarkerOptions.renderLabel()) {
                    RenderUtils.drawRect(
                            event.getPoseStack(),
                            CommonColors.BLACK.withAlpha(backgroundOpacity.get()),
                            displayPositionX - scale.get() * (backgroundWidth / 2 + 2),
                            displayPositionY - scale.get() * (backgroundHeight / 2 + 2),
                            0,
                            scale.get() * (backgroundWidth + 3),
                            scale.get() * (backgroundHeight + 2));
                }

                int textYOffset = 0;
                if (resolvedMarkerOptions.renderDistance()) {
                    FontRenderer.getInstance()
                            .renderAlignedTextInBox(
                                    event.getPoseStack(),
                                    StyledText.fromString(renderedDistanceText),
                                    displayPositionX - scale.get() * backgroundWidth,
                                    displayPositionX + scale.get() * backgroundWidth,
                                    displayPositionY - scale.get() * backgroundHeight,
                                    displayPositionY + scale.get() * backgroundHeight,
                                    0,
                                    resolvedMapAttributes.labelColor().withAlpha((float)
                                            renderedMapLocation.visibility),
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.MIDDLE,
                                    textShadow.get(),
                                    scale.get());

                    textYOffset += FontRenderer.getInstance().getFont().lineHeight;
                }

                if (resolvedMarkerOptions.renderLabel()) {
                    FontRenderer.getInstance()
                            .renderAlignedTextInBox(
                                    event.getPoseStack(),
                                    StyledText.fromString(resolvedMapAttributes.label()),
                                    displayPositionX - scale.get() * backgroundWidth,
                                    displayPositionX + scale.get() * backgroundWidth,
                                    displayPositionY - scale.get() * backgroundHeight + textYOffset,
                                    displayPositionY + scale.get() * backgroundHeight + textYOffset,
                                    0,
                                    resolvedMapAttributes.labelColor().withAlpha((float)
                                            renderedMapLocation.visibility),
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.MIDDLE,
                                    textShadow.get(),
                                    scale.get());
                }
            } else if (resolvedMarkerOptions.renderIcon()) {
                displayPositionX = intersectPoint.x;
                displayPositionY = intersectPoint.y;

                // pointer position is determined by finding the point on circle centered around displayPosition
                double angle = Math.toDegrees(StrictMath.atan2(
                                displayPositionY - event.getWindow().getGuiScaledHeight() / 2f,
                                displayPositionX - event.getWindow().getGuiScaledWidth() / 2f))
                        + 90f;
                float radius = icon.getWidth() / 2f + 8f;

                float pointerOffsetX = radius * (float) StrictMath.cos((angle - 90) * 3 / 180);
                float pointerOffsetY = radius * (float) StrictMath.sin((angle - 90) * 3 / 180);

                float pointerDisplayPositionX = displayPositionX + pointerOffsetX;
                float pointerDisplayPositionY = displayPositionY + pointerOffsetY;

                RenderUtils.drawScalingTexturedRect(
                        event.getPoseStack(),
                        icon.getResourceLocation(),
                        displayPositionX - scale.get() * icon.getWidth() / 2 + pointerOffsetX * (1 - scale.get()),
                        displayPositionY - scale.get() * icon.getHeight() / 2 + pointerOffsetY * (1 - scale.get()),
                        0,
                        scale.get() * icon.getWidth(),
                        scale.get() * icon.getHeight(),
                        icon.getWidth(),
                        icon.getHeight());
                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderSystem.disableBlend();

                // apply rotation
                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                poseStack.translate(pointerDisplayPositionX, pointerDisplayPositionY, 0);
                poseStack.mulPose(new Quaternionf().rotationXYZ(0, 0, (float) Math.toRadians(angle)));
                poseStack.translate(-pointerDisplayPositionX, -pointerDisplayPositionY, 0);

                MultiBufferSource.BufferSource bufferSource =
                        MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
                DUMMY_WAYPOINT
                        .getPointerPoi()
                        .renderAt(
                                poseStack,
                                bufferSource,
                                pointerDisplayPositionX,
                                pointerDisplayPositionY,
                                false,
                                scale.get(),
                                1,
                                50,
                                true);
                bufferSource.endBatch();
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
        Vec3 centerPoint = new Vec3(window.getGuiScaledWidth() / 2f, window.getGuiScaledHeight() / 2f, 0);

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

    private double getMarkerVisibility(Location location, ResolvedMarkerOptions resolvedMarkerOptions) {
        double distanceToPlayer =
                Math.sqrt(location.distanceToSqr(McUtils.player().position()));

        double startFadeInDistance = resolvedMarkerOptions.outerRadius() + resolvedMarkerOptions.fade();
        double stopFadeInDistance = resolvedMarkerOptions.outerRadius() - resolvedMarkerOptions.fade();
        float startFadeOutDistance = resolvedMarkerOptions.innerRadius() + resolvedMarkerOptions.fade();
        float stopFadeOutDistance = resolvedMarkerOptions.innerRadius() - resolvedMarkerOptions.fade();

        if (distanceToPlayer > startFadeInDistance) {
            return 0;
        }

        if (distanceToPlayer > stopFadeInDistance) {
            return 1 - (distanceToPlayer - stopFadeInDistance) / (resolvedMarkerOptions.fade() * 2);
        }

        if (distanceToPlayer > startFadeOutDistance) {
            return 1;
        }

        if (distanceToPlayer > stopFadeOutDistance) {
            return (distanceToPlayer - stopFadeOutDistance) / (resolvedMarkerOptions.fade() * 2);
        }

        return 0;
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

    private record RenderedMapLocation(
            MapLocation mapLocation, double distance, double visibility, Vec3 screenCoordinates) {}
}
