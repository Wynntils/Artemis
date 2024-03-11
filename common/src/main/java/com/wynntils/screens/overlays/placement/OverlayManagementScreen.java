/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.overlays.placement;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.overlays.Corner;
import com.wynntils.core.consumers.overlays.CustomNameProperty;
import com.wynntils.core.consumers.overlays.Edge;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.consumers.overlays.SectionCoordinates;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.widgets.WynntilsCheckbox;
import com.wynntils.screens.overlays.selection.OverlaySelectionScreen;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;

public final class OverlayManagementScreen extends WynntilsScreen {
    // This is used to calculate alignment lines
    // If the value is set to 4, alignment lines will render at 1/2, 1/3, 2/3, 1/4, 3/4
    // of the screen both vertically and horizontally.
    private static final int ALIGNMENT_LINES_MAX_SECTIONS_PER_AXIS = 4;
    private static final int ALIGNMENT_SNAP_DISTANCE = 1;
    // Bigger this value is, the harder it is to not align overlay to alignment line
    private static final double ALIGNMENT_SNAP_HARDNESS = 6;

    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MAX_CLICK_DISTANCE = 5;
    private static final int ANIMATION_LENGTH = 30;

    private static final List<Component> HELP_TOOLTIP_LINES = ComponentUtils.wrapTooltips(
            List.of(
                    Component.translatable("screens.wynntils.overlayManagement.helpTooltip1"),
                    Component.translatable("screens.wynntils.overlayManagement.helpTooltip2"),
                    Component.translatable("screens.wynntils.overlayManagement.helpTooltip3"),
                    Component.translatable("screens.wynntils.overlayManagement.helpTooltip4"),
                    Component.translatable("screens.wynntils.overlayManagement.helpTooltip5"),
                    Component.translatable("screens.wynntils.overlayManagement.helpTooltip6")
                            .withStyle(ChatFormatting.RED)),
            200);

    private final Set<Float> verticalAlignmentLinePositions = new HashSet<>();
    private final Set<Float> horizontalAlignmentLinePositions = new HashSet<>();
    private final Map<Edge, Double> edgeAlignmentSnapMap = new EnumMap<>(Edge.class);
    private final Map<Edge, Float> alignmentLinesToRender = new EnumMap<>(Edge.class);

    private SelectionMode selectionMode = SelectionMode.NONE;
    private Overlay selectedOverlay;
    private final boolean fixedSelection;
    private Corner selectedCorner = null;
    private Edge selectedEdge = null;

    private boolean buttonsAtBottom = true;
    private boolean renderAllOverlays = true;
    private boolean showPreview = true;

    private boolean snappingEnabled = true;

    private boolean userInteracted = false;
    private int animationLengthRemaining;
    private double snapOffsetX;
    private double snapOffsetY;

    private final OverlaySelectionScreen previousScreen;

    private OverlayManagementScreen(OverlaySelectionScreen previousScreen, Overlay overlay) {
        super(Component.translatable("screens.wynntils.overlayManagement.name"));
        this.previousScreen = previousScreen;
        selectedOverlay = overlay;
        fixedSelection = true;
        animationLengthRemaining = ANIMATION_LENGTH;
    }

    private OverlayManagementScreen(OverlaySelectionScreen previousScreen) {
        super(Component.translatable("screens.wynntils.overlayManagement.name"));
        this.previousScreen = previousScreen;
        selectedOverlay = null;
        fixedSelection = false;
        animationLengthRemaining = 0;
    }

    public static Screen create(OverlaySelectionScreen previousScreen) {
        return new OverlayManagementScreen(previousScreen);
    }

    public static Screen create(OverlaySelectionScreen previousScreen, Overlay overlay) {
        return new OverlayManagementScreen(previousScreen, overlay);
    }

    @Override
    protected void doInit() {
        setupButtons();
        calculateAlignmentLinePositions();
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        if (selectionMode != SelectionMode.NONE) {
            renderAlignmentLines(poseStack);
        } else {
            renderSections(poseStack);
        }

        Set<Overlay> overlays = Managers.Overlay.getOverlays().stream()
                .filter(Managers.Overlay::isEnabled)
                .collect(Collectors.toSet());

        // We want to render the tooltip for what will actually be interacted with
        boolean renderedTooltip = fixedSelection;

        // Buttons have the highest priority so check those first
        for (GuiEventListener listener : this.children) {
            if (listener.isMouseOver(mouseX, mouseY)) {
                renderedTooltip = true;
                break;
            }
        }

        for (Overlay overlay : overlays) {
            if (!renderAllOverlays && overlay != selectedOverlay) continue;

            CustomColor color = getOverlayColor(overlay);
            RenderUtils.drawRectBorders(
                    poseStack,
                    color,
                    overlay.getRenderX(),
                    overlay.getRenderY(),
                    overlay.getRenderX() + overlay.getWidth(),
                    overlay.getRenderY() + overlay.getHeight(),
                    1,
                    1.8f);
            int colorAlphaRect = fixedSelection && overlay == selectedOverlay
                    ? (int) Math.max(MathUtils.map(animationLengthRemaining, 0, ANIMATION_LENGTH, 30, 255), 30)
                    : 30;
            RenderUtils.drawRect(
                    poseStack,
                    color.withAlpha(colorAlphaRect),
                    overlay.getRenderX(),
                    overlay.getRenderY(),
                    0,
                    overlay.getWidth(),
                    overlay.getHeight());

            String overlayName = overlay.getTranslatedName();

            // Show the custom name for info boxes/custom bars if given
            if (overlay instanceof CustomNameProperty customNameProperty) {
                if (!customNameProperty.getCustomName().get().isEmpty()) {
                    overlayName = customNameProperty.getCustomName().get();
                }
            }

            // Only display overlay name when not rendering preview of the overlay
            if (!showPreview) {
                float yOffset =
                        switch (overlay.getRenderVerticalAlignment()) {
                            case TOP -> 1.8f;
                            case MIDDLE -> 0f;
                            case BOTTOM -> -1.8f;
                        };

                float xOffset =
                        switch (overlay.getRenderHorizontalAlignment()) {
                            case LEFT -> 1.8f;
                            case CENTER -> 0f;
                            case RIGHT -> -1.8f;
                        };

                float renderX = overlay.getRenderX() + xOffset;
                float renderY = overlay.getRenderY() + yOffset;

                FontRenderer.getInstance()
                        .renderAlignedTextInBox(
                                poseStack,
                                StyledText.fromString(overlayName),
                                renderX,
                                renderX + overlay.getWidth(),
                                renderY,
                                renderY + overlay.getHeight(),
                                overlay.getWidth(),
                                color,
                                overlay.getRenderHorizontalAlignment(),
                                overlay.getRenderVerticalAlignment(),
                                TextShadow.OUTLINE);
            }

            // If tooltip has yet been rendered then we need to check
            // if an overlay is hovered and display the tooltip for that.
            if (!renderedTooltip
                    && showPreview
                    && overlay != selectedOverlay
                    && isMouseHoveringOverlay(overlay, mouseX, mouseY)
                    && selectionMode == SelectionMode.NONE) {
                McUtils.mc()
                        .screen
                        .setTooltipForNextRenderPass(Lists.transform(
                                List.of(Component.literal(overlayName)), Component::getVisualOrderText));

                renderedTooltip = true;
            } else if (!renderedTooltip
                    && overlay == selectedOverlay
                    && isMouseHoveringOverlay(overlay, mouseX, mouseY)
                    && selectionMode == SelectionMode.NONE) {
                McUtils.mc()
                        .screen
                        .setTooltipForNextRenderPass(
                                Lists.transform(HELP_TOOLTIP_LINES, Component::getVisualOrderText));
                renderedTooltip = true;
            }
        }

        // Render widgets
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void tick() {
        if (userInteracted) return;

        if (animationLengthRemaining <= 0) {
            animationLengthRemaining = ANIMATION_LENGTH;
        }

        animationLengthRemaining--;
    }

    @Override
    public void onClose() {
        reloadConfigForOverlay();
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        // Let the buttons of the Screen have priority
        if (super.doMouseClicked(mouseX, mouseY, button)) return true;

        userInteracted = true;
        animationLengthRemaining = 0;

        // Order:
        //  - Corners
        //  - Edges
        //  - OverlayArea

        // reset
        resetSelection();

        // Ignore clicking on other overlays if editing a specific overlay
        // or other overlays aren't currently rendered
        if (!fixedSelection && renderAllOverlays) {
            Set<Overlay> overlays = Managers.Overlay.getOverlays().stream()
                    .filter(Managers.Overlay::isEnabled)
                    .collect(Collectors.toSet());

            for (Overlay overlay : overlays) {
                if (isMouseHoveringOverlay(overlay, mouseX, mouseY)) {
                    selectedOverlay = overlay;
                    break;
                }
            }
        }

        if (selectedOverlay == null) return false;

        Overlay selected = selectedOverlay;

        setupButtons();

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && KeyboardUtils.isShiftDown()) {
            selectedOverlay.getConfigOptionFromString("position").ifPresent(Config::reset);
            selectedOverlay.getConfigOptionFromString("size").ifPresent(Config::reset);
            selectedOverlay
                    .getConfigOptionFromString("horizontalAlignmentOverride")
                    .ifPresent(Config::reset);
            selectedOverlay
                    .getConfigOptionFromString("verticalAlignmentOverride")
                    .ifPresent(Config::reset);

            return true;
        }

        Vec2 mousePos = new Vec2((float) mouseX, (float) mouseY);

        for (Corner corner : Corner.values()) {
            float distance = selected.getCornerPoints(corner).distanceToSqr(mousePos);
            if (distance < MAX_CLICK_DISTANCE) {
                selectedCorner = corner;
                selectionMode = SelectionMode.CORNER;

                return false;
            }
        }

        for (Edge value : Edge.values()) {
            float minX, maxX, minY, maxY;

            switch (value) {
                case TOP -> {
                    minX = selected.getRenderX();
                    maxX = selected.getRenderX() + selected.getWidth();
                    minY = selected.getRenderY() - MAX_CLICK_DISTANCE / 2f;
                    maxY = selected.getRenderY() + MAX_CLICK_DISTANCE / 2f;
                }
                case LEFT -> {
                    minX = selected.getRenderX() - MAX_CLICK_DISTANCE / 2f;
                    maxX = selected.getRenderX() + MAX_CLICK_DISTANCE / 2f;
                    minY = selected.getRenderY();
                    maxY = selected.getRenderY() + selected.getHeight();
                }
                case RIGHT -> {
                    minX = selected.getRenderX() + selected.getWidth() - MAX_CLICK_DISTANCE / 2f;
                    maxX = selected.getRenderX() + selected.getWidth() + MAX_CLICK_DISTANCE / 2f;
                    minY = selected.getRenderY();
                    maxY = selected.getRenderY() + selected.getHeight();
                }
                case BOTTOM -> {
                    minX = selected.getRenderX();
                    maxX = selected.getRenderX() + selected.getWidth();
                    minY = selected.getRenderY() + selected.getHeight() - MAX_CLICK_DISTANCE / 2f;
                    maxY = selected.getRenderY() + selected.getHeight() + MAX_CLICK_DISTANCE / 2f;
                }
                default -> {
                    // should not happen
                    continue;
                }
            }

            if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
                selectedEdge = value;
                selectionMode = SelectionMode.EDGE;

                return false;
            }
        }

        if (isMouseHoveringOverlay(selectedOverlay, mouseX, mouseY)) {
            selectionMode = SelectionMode.AREA;

            return false;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Let the buttons of the Screen have priority
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;

        if (selectedOverlay == null) return false;

        switch (selectionMode) {
            case CORNER -> handleOverlayCornerDrag(dragX, dragY);
            case EDGE -> handleOverlayEdgeDrag(dragX, dragY);
            case AREA -> handleOverlayBodyDrag(dragX, dragY);
            default -> {}
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Let the buttons of the Screen have priority
        if (super.mouseReleased(mouseX, mouseY, button)) return true;

        resetSelection();
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        userInteracted = true;
        animationLengthRemaining = 0;

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            Managers.Config.saveConfig();
            onClose();
            McUtils.mc().setScreen(previousScreen);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            McUtils.mc().setScreen(previousScreen);
            return true;
        }

        if (selectedOverlay == null) return false;

        // Shirt + Arrow keys change overlay alignment
        if (KeyboardUtils.isShiftDown()) {
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
                int index = selectedOverlay.getRenderVerticalAlignment().ordinal();

                if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    index += 1;
                } else {
                    index -= 1;
                }

                VerticalAlignment[] values = VerticalAlignment.values();
                index = (values.length + index) % values.length;

                int finalIndex = index;
                selectedOverlay
                        .getConfigOptionFromString("verticalAlignmentOverride")
                        .ifPresent(config -> ((Config<VerticalAlignment>) config).setValue(values[finalIndex]));

            } else if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_LEFT) {
                int index = selectedOverlay.getRenderHorizontalAlignment().ordinal();

                if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                    index += 1;
                } else {
                    index -= 1;
                }

                HorizontalAlignment[] values = HorizontalAlignment.values();
                index = (values.length + index) % values.length;

                int finalIndex = index;
                selectedOverlay
                        .getConfigOptionFromString("horizontalAlignmentOverride")
                        .ifPresent(config -> ((Config<HorizontalAlignment>) config).setValue(values[finalIndex]));
            }
        } else {
            // Arrow keys change overlay position
            int offsetX = 0;
            int offsetY = 0;

            if (keyCode == GLFW.GLFW_KEY_UP) offsetY = -1;
            else if (keyCode == GLFW.GLFW_KEY_DOWN) offsetY = 1;
            else if (keyCode == GLFW.GLFW_KEY_RIGHT) offsetX = 1;
            else if (keyCode == GLFW.GLFW_KEY_LEFT) offsetX = -1;

            final int finalOffsetX = offsetX;
            final int finalOffsetY = offsetY;

            selectedOverlay.getConfigOptionFromString("position").ifPresent(config -> ((Config<OverlayPosition>) config)
                    .setValue(OverlayPosition.getBestPositionFor(
                            selectedOverlay,
                            selectedOverlay.getRenderX(),
                            selectedOverlay.getRenderY(),
                            finalOffsetX,
                            finalOffsetY)));
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            snappingEnabled = false;
            edgeAlignmentSnapMap.clear();
            alignmentLinesToRender.clear();
        }

        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            snappingEnabled = true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    public Overlay getSelectedOverlay() {
        return selectedOverlay;
    }

    public boolean shouldRenderAllOverlays() {
        return renderAllOverlays;
    }

    public boolean showPreview() {
        return showPreview;
    }

    private CustomColor getOverlayColor(Overlay overlay) {
        if (overlay == selectedOverlay) return CommonColors.GREEN;

        return fixedSelection ? new CustomColor(200, 200, 200, 255) : CommonColors.LIGHT_BLUE;
    }

    private boolean isMouseHoveringOverlay(Overlay overlay, double mouseX, double mouseY) {
        if (overlay == null) return false;

        return (overlay.getRenderX() <= mouseX && overlay.getRenderX() + overlay.getWidth() >= mouseX)
                && (overlay.getRenderY() <= mouseY && overlay.getRenderY() + overlay.getHeight() >= mouseY);
    }

    private void reloadConfigForOverlay() {
        Managers.Config.reloadConfiguration();
    }

    private void handleOverlayEdgeDrag(double dragX, double dragY) {
        if (selectedEdge == null || selectedOverlay == null) {
            return;
        }

        Pair<Double, Double> newDrag = calculateDragAfterSnapping(dragX, dragY);
        dragX = newDrag.a();
        dragY = newDrag.b();

        Overlay overlay = selectedOverlay;
        Edge edge = selectedEdge;

        OverlaySize overlaySize = overlay.getSize();

        final float renderX = overlay.getRenderX();
        final float renderY = overlay.getRenderY();

        switch (edge) {
            case TOP -> {
                overlay.setHeight((float) (overlaySize.getHeight() - dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) 0, (float) dragY));
            }
            case LEFT -> {
                overlay.setWidth((float) (overlaySize.getWidth() - dragX));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) dragX, (float) 0));
            }
            case RIGHT -> {
                overlay.setWidth((float) (overlaySize.getWidth() + dragX));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) 0, (float) 0));
            }
            case BOTTOM -> {
                overlay.setHeight((float) (overlaySize.getHeight() + dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) 0, (float) 0));
            }
        }
    }

    private void handleOverlayBodyDrag(double dragX, double dragY) {
        if (selectedOverlay == null) {
            return;
        }

        Pair<Double, Double> newDrag = calculateDragAfterSnapping(dragX, dragY);
        dragX = newDrag.a();
        dragY = newDrag.b();

        Overlay overlay = selectedOverlay;

        overlay.setPosition(OverlayPosition.getBestPositionFor(
                overlay, overlay.getRenderX(), overlay.getRenderY(), (float) dragX, (float) dragY));
    }

    private void handleOverlayCornerDrag(double dragX, double dragY) {
        if (selectedCorner == null || selectedOverlay == null) {
            return;
        }

        Pair<Double, Double> newDrag = calculateDragAfterSnapping(dragX, dragY);
        dragX = newDrag.a();
        dragY = newDrag.b();

        Overlay overlay = selectedOverlay;
        Corner corner = selectedCorner;

        OverlaySize overlaySize = overlay.getSize();

        final float renderX = overlay.getRenderX();
        final float renderY = overlay.getRenderY();

        switch (corner) {
            case TOP_LEFT -> {
                overlay.setWidth((float) (overlaySize.getWidth() - dragX));
                overlay.setHeight((float) (overlaySize.getHeight() - dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) dragX, (float) dragY));
            }
            case TOP_RIGHT -> {
                overlay.setWidth((float) (overlaySize.getWidth() + dragX));
                overlay.setHeight((float) (overlaySize.getHeight() - dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) 0, (float) dragY));
            }
            case BOTTOM_LEFT -> {
                overlay.setWidth((float) (overlaySize.getWidth() - dragX));
                overlay.setHeight((float) (overlaySize.getHeight() + dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) dragX, (float) 0));
            }
            case BOTTOM_RIGHT -> {
                overlay.setWidth((float) (overlaySize.getWidth() + dragX));
                overlay.setHeight((float) (overlaySize.getHeight() + dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) 0, (float) 0));
            }
        }
    }

    // Pair<dragX, dragY>
    private Pair<Double, Double> calculateDragAfterSnapping(double dragX, double dragY) {
        if (!snappingEnabled) {
            return new Pair<>(dragX, dragY);
        }

        dragX += snapOffsetX;
        dragY += snapOffsetY;
        double originalDragX = dragX;
        double originalDragY = dragY;

        List<Edge> edgesToSnapTo =
                switch (this.selectionMode) {
                    case NONE -> List.of();
                    case CORNER -> this.selectedCorner.getEdges();
                    case EDGE -> List.of(this.selectedEdge);
                    case AREA -> Arrays.stream(Edge.values()).toList();
                };

        double originalX = dragX;
        double originalY = dragY;

        for (Edge edge : edgesToSnapTo.stream()
                .filter(edge -> !edgeAlignmentSnapMap.containsKey(edge))
                .toList()) {
            Pair<Vec2, Vec2> edgePos = edge.getEdgePos(selectedOverlay);

            if (edge.isVerticalLine()) {
                for (Float alignmentLinePosition : verticalAlignmentLinePositions) {
                    if (Math.abs(this.width * alignmentLinePosition - edgePos.a().x) < ALIGNMENT_SNAP_DISTANCE) {
                        edgeAlignmentSnapMap.put(edge, ALIGNMENT_SNAP_HARDNESS);
                        alignmentLinesToRender.put(edge, alignmentLinePosition);
                        break;
                    }
                }
            } else {
                for (Float alignmentLinePosition : horizontalAlignmentLinePositions) {
                    if (Math.abs(this.height * alignmentLinePosition - edgePos.a().y) < ALIGNMENT_SNAP_DISTANCE) {
                        edgeAlignmentSnapMap.put(edge, ALIGNMENT_SNAP_HARDNESS);
                        alignmentLinesToRender.put(edge, alignmentLinePosition);
                        break;
                    }
                }
            }
        }

        Set<Edge> toBeRemoved = new HashSet<>();

        for (Map.Entry<Edge, Double> entry : edgeAlignmentSnapMap.entrySet()) {
            double newSnapValue;
            if (entry.getKey().isVerticalLine()) {
                newSnapValue = entry.getValue() - Math.abs(dragX);
                dragX = 0;
            } else {
                newSnapValue = entry.getValue() - Math.abs(dragY);
                dragY = 0;
            }

            if (newSnapValue <= 0) {
                toBeRemoved.add(entry.getKey());
                if (entry.getKey().isVerticalLine()) {
                    dragX = originalX;
                } else {
                    dragY = originalY;
                }
            } else {
                edgeAlignmentSnapMap.put(entry.getKey(), newSnapValue);
            }
        }

        for (Edge edge : toBeRemoved) {
            edgeAlignmentSnapMap.remove(edge);
            alignmentLinesToRender.remove(edge);
        }

        snapOffsetX = originalDragX - dragX;
        snapOffsetY = originalDragY - dragY;
        return new Pair<>(dragX, dragY);
    }

    private void renderSections(PoseStack poseStack) {
        for (SectionCoordinates section : Managers.Overlay.getSections()) {
            RenderUtils.drawRectBorders(
                    poseStack, CommonColors.WHITE, section.x1(), section.y1(), section.x2(), section.y2(), 0, 1);
        }
    }

    private void renderAlignmentLines(PoseStack poseStack) {
        for (Map.Entry<Edge, Float> entry : alignmentLinesToRender.entrySet()) {
            if (entry.getKey().isVerticalLine()) {
                RenderUtils.drawLine(
                        poseStack,
                        CommonColors.ORANGE,
                        this.width * entry.getValue(),
                        0,
                        this.width * entry.getValue(),
                        this.height,
                        1,
                        1);
            } else {
                RenderUtils.drawLine(
                        poseStack,
                        CommonColors.ORANGE,
                        0,
                        this.height * entry.getValue(),
                        this.width,
                        this.height * entry.getValue(),
                        1,
                        1);
            }
        }
    }

    private void calculateAlignmentLinePositions() {
        verticalAlignmentLinePositions.clear();
        horizontalAlignmentLinePositions.clear();

        verticalAlignmentLinePositions.add(0f);
        horizontalAlignmentLinePositions.add(0f);
        verticalAlignmentLinePositions.add(1f);
        horizontalAlignmentLinePositions.add(1f);

        for (int i = 2; i <= ALIGNMENT_LINES_MAX_SECTIONS_PER_AXIS; i++) {
            for (int j = 1; j < i; j++) {
                verticalAlignmentLinePositions.add((float) j / i);
                horizontalAlignmentLinePositions.add((float) j / i);
            }
        }

        for (Overlay overlay : Managers.Overlay.getOverlays().stream()
                .filter(Managers.Overlay::isEnabled)
                .toList()) {
            if (overlay == selectedOverlay) continue;

            for (Edge edge : Edge.values()) {
                Pair<Vec2, Vec2> edgePos = edge.getEdgePos(overlay);

                if (edge.isVerticalLine()) {
                    verticalAlignmentLinePositions.add(edgePos.a().x / this.width);
                } else {
                    horizontalAlignmentLinePositions.add(edgePos.a().y / this.height);
                }
            }
        }
    }

    private void setupButtons() {
        // Remove previous buttons
        this.children.stream().toList().forEach(this::removeWidget);

        // Determine if buttons should be at the top or bottom of the screen
        int yPos = buttonsAtBottom ? this.height - 25 : 5;

        this.addRenderableWidget(new WynntilsCheckbox(
                this.width / 2 - BUTTON_WIDTH - 12 - 100,
                yPos,
                BUTTON_HEIGHT,
                BUTTON_HEIGHT,
                Component.translatable("screens.wynntils.overlayManagement.showPreview"),
                showPreview,
                80,
                true,
                (b) -> showPreview = !showPreview,
                ComponentUtils.wrapTooltips(
                        List.of(Component.translatable("screens.wynntils.overlayManagement.showPreviewTooltip")),
                        150)));

        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.overlayManagement.close"), button -> {
                            onClose();
                            McUtils.mc().setScreen(previousScreen);
                        })
                .pos(this.width / 2 - BUTTON_WIDTH - 12, yPos)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screens.wynntils.overlayManagement.closeTooltip")))
                .build());

        this.addRenderableWidget(new Button.Builder(
                        buttonsAtBottom ? Component.literal("🠝") : Component.literal("🠟"), button -> {
                            buttonsAtBottom = !buttonsAtBottom;
                            setupButtons();
                        })
                .pos(this.width / 2 - 10, yPos)
                .size(BUTTON_HEIGHT, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(
                        buttonsAtBottom
                                ? Component.translatable("screens.wynntils.overlayManagement.moveButtonsUpTooltip")
                                : Component.translatable("screens.wynntils.overlayManagement.moveButtonsDownTooltip")))
                .build());

        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.overlayManagement.apply"), button -> {
                            Managers.Config.saveConfig();
                            onClose();
                            McUtils.mc().setScreen(previousScreen);
                        })
                .pos(this.width / 2 + 12, yPos)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screens.wynntils.overlayManagement.applyTooltip")))
                .build());

        if (selectedOverlay != null) {
            this.addRenderableWidget(new WynntilsCheckbox(
                    this.width / 2 + 12 + BUTTON_WIDTH + 10,
                    yPos,
                    BUTTON_HEIGHT,
                    BUTTON_HEIGHT,
                    Component.translatable("screens.wynntils.overlayManagement.showOthers"),
                    renderAllOverlays,
                    120,
                    true,
                    (b) -> renderAllOverlays = !renderAllOverlays,
                    ComponentUtils.wrapTooltips(
                            List.of(Component.translatable("screens.wynntils.overlayManagement.showOthersTooltip")),
                            150)));
        }
    }

    private void resetSelection() {
        selectionMode = SelectionMode.NONE;
        selectedCorner = null;
        selectedEdge = null;
    }

    private enum SelectionMode {
        NONE,
        CORNER,
        EDGE,
        AREA
    }
}
