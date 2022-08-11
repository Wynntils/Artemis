/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.features.overlays;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ComparisonChain;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.features.Configurable;
import com.wynntils.core.features.Translatable;
import com.wynntils.core.features.overlays.sizes.GuiScaledOverlaySize;
import com.wynntils.core.features.overlays.sizes.OverlaySize;
import com.wynntils.mc.render.HorizontalAlignment;
import com.wynntils.mc.render.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.phys.Vec2;

public abstract class Overlay implements Translatable, Configurable {
    private final List<ConfigHolder> configOptions = new ArrayList<>();

    @Config(key = "overlay.wynntils.overlay.position")
    protected OverlayPosition position;

    @Config(key = "overlay.wynntils.overlay.size")
    protected OverlaySize size;

    @Config(key = "overlay.wynntils.overlay.userEnabled")
    protected Boolean userEnabled = null;

    // This is used in rendering.
    // Initially we use the overlay position horizontal alignment
    // but the user can modify this config field to use an override.
    // Example use case: Overlay is aligned to the left in the TopRight section,
    //                   but the user wants to use right text alignment
    @Config(key = "overlay.wynntils.overlay.horizontalAlignmentOverride")
    protected HorizontalAlignment horizontalAlignmentOverride = null;

    @Config(key = "overlay.wynntils.overlay.verticalAlignmentOverride")
    protected VerticalAlignment verticalAlignmentOverride = null;

    protected Overlay(OverlayPosition position, float width, float height) {
        this.position = position;
        this.size = new GuiScaledOverlaySize(width, height);
    }

    protected Overlay(OverlayPosition position, OverlaySize size) {
        this.position = position;
        this.size = size;
    }

    protected Overlay(
            OverlayPosition position,
            OverlaySize size,
            HorizontalAlignment horizontalAlignmentOverride,
            VerticalAlignment verticalAlignmentOverride) {
        this.position = position;
        this.size = size;
        this.horizontalAlignmentOverride = horizontalAlignmentOverride;
        this.verticalAlignmentOverride = verticalAlignmentOverride;
    }

    public abstract void render(PoseStack poseStack, float partialTicks, Window window);

    public void renderPreview(PoseStack poseStack, float partialTicks, Window window) {
        this.render(poseStack, partialTicks, window);
    }

    @Override
    public final void updateConfigOption(ConfigHolder configHolder) {
        // if user toggle was changed, enable/disable feature accordingly
        if (configHolder.getFieldName().equals("userEnabled")) {
            // This is done so all state checks run in order
            OverlayManager.disableOverlays(List.of(this));
            OverlayManager.enableOverlays(List.of(this), false);
        }

        onConfigUpdate(configHolder);
    }

    protected abstract void onConfigUpdate(ConfigHolder configHolder);

    /** Registers the overlay's config options. Called by ConfigManager when overlay is loaded */
    @Override
    public final void addConfigOptions(List<ConfigHolder> options) {
        configOptions.addAll(options);
    }

    /** Returns all config options registered in this overlay that should be visible to the user */
    public final List<ConfigHolder> getVisibleConfigOptions() {
        return configOptions.stream().filter(c -> c.getMetadata().visible()).collect(Collectors.toList());
    }

    /** Returns the config option matching the given name, if it exists */
    public final Optional<ConfigHolder> getConfigOptionFromString(String name) {
        return getVisibleConfigOptions().stream()
                .filter(c -> c.getFieldName().equals(name))
                .findFirst();
    }

    /** Gets the name of a feature */
    @Override
    public String getTranslatedName() {
        return getTranslation("name");
    }

    @Override
    public String getTranslation(String keySuffix) {
        return I18n.get("feature.wynntils." + getDeclaringFeatureNameCamelCase() + ".overlay." + getNameCamelCase()
                + "." + keySuffix);
    }

    public String getShortName() {
        return this.getClass().getSimpleName();
    }

    public Class<?> getDeclaringClass() {
        return this.getClass().getDeclaringClass();
    }

    protected String getNameCamelCase() {
        return CaseFormat.UPPER_CAMEL.to(
                CaseFormat.LOWER_CAMEL, this.getClass().getSimpleName().replace("Overlay", ""));
    }

    protected String getDeclaringFeatureNameCamelCase() {
        String name = this.getClass().getDeclaringClass().getSimpleName().replace("Feature", "");
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
    }

    public Boolean isUserEnabled() {
        return userEnabled;
    }

    public boolean isEnabled() {
        if (this.isUserEnabled() != null) {
            return this.isUserEnabled();
        }

        return OverlayManager.getOverlayInfo(this).enabled();
    }

    public final boolean isParentEnabled() {
        return OverlayManager.getOverlayParent(this).isEnabled();
    }

    public float getWidth() {
        return this.size.getWidth();
    }

    public float getHeight() {
        return this.size.getHeight();
    }

    public float getRenderedWidth() {
        return this.size.getRenderedWidth();
    }

    public float getRenderedHeight() {
        return this.size.getRenderedHeight();
    }

    public OverlaySize getSize() {
        return size;
    }

    public OverlayPosition getPosition() {
        return position;
    }

    public void setPosition(OverlayPosition position) {
        this.position = position;
    }

    // Return the X where the overlay should be rendered
    public float getRenderX() {
        return getRenderX(this.position);
    }

    // Return the Y where the overlay should be rendered
    public float getRenderY() {
        return getRenderY(this.position);
    }

    public float getRenderX(OverlayPosition position) {
        final SectionCoordinates section = OverlayManager.getSection(position.getAnchorSection());
        return switch (position.getHorizontalAlignment()) {
            case Left -> section.x1() + position.getHorizontalOffset();
            case Center -> (section.x1() + section.x2() - this.getWidth()) / 2 + position.getHorizontalOffset();
            case Right -> (section.x2() + position.getHorizontalOffset() - this.getWidth());
        };
    }

    public float getRenderY(OverlayPosition position) {
        final SectionCoordinates section = OverlayManager.getSection(position.getAnchorSection());
        return switch (position.getVerticalAlignment()) {
            case Top -> section.y1() + position.getVerticalOffset();
            case Middle -> (section.y1() + section.y2() - this.getHeight()) / 2 + position.getVerticalOffset();
            case Bottom -> section.y2() + position.getVerticalOffset() - this.getHeight();
        };
    }

    public HorizontalAlignment getRenderHorizontalAlignment() {
        return horizontalAlignmentOverride == null ? position.getHorizontalAlignment() : horizontalAlignmentOverride;
    }

    public VerticalAlignment getRenderVerticalAlignment() {
        return verticalAlignmentOverride == null ? position.getVerticalAlignment() : verticalAlignmentOverride;
    }

    public Vec2 getCornerPoints(Corner corner) {
        return switch (corner) {
            case TopLeft -> new Vec2(getRenderX(), getRenderY());
            case TopRight -> new Vec2(getRenderX() + getWidth(), getRenderY());
            case BottomLeft -> new Vec2(getRenderX(), getRenderY() + getHeight());
            case BottomRight -> new Vec2(getRenderX() + getWidth(), getRenderY() + getHeight());
        };
    }

    public int compareTo(Overlay other) {
        return ComparisonChain.start()
                .compareTrueFirst(this.isParentEnabled(), other.isParentEnabled())
                .compare(
                        this.getDeclaringClass().getSimpleName(),
                        other.getDeclaringClass().getSimpleName())
                .compare(this.getTranslatedName(), other.getTranslatedName())
                .result();
    }
}
