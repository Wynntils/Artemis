/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.overlays.placement;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.features.overlays.Corner;
import com.wynntils.core.features.overlays.Edge;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.OverlaySize;
import com.wynntils.core.features.overlays.SectionCoordinates;
import com.wynntils.core.text.StyledText2;
import com.wynntils.screens.base.WynntilsScreen;
import com.wynntils.screens.overlays.selection.OverlaySelectionScreen;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.TooltipUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.language.I18n;
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

    private static final List<Component> HELP_TOOLTIP_LINES = List.of(
            Component.literal("Resize the overlay by dragging the edges or corners."),
            Component.literal("Move it by dragging the center of the overlay."),
            Component.literal("By holding shift, you can disable alignment lines."),
            Component.literal("Use your arrows to change vertical"),
            Component.literal("and horizontal alignment."),
            Component.literal("The overlay name will render respecting"),
            Component.literal("the current overlay alignments."),
            Component.literal("Shift-Middle click on an overlay to reset it to it's original state.")
                    .withStyle(ChatFormatting.RED));

    private static final Component CLOSE_TOOLTIP = Component.literal("Click here to stop editing and reset changes.");

    private static final Component TEST_TOOLTIP = Component.literal(
            "Click here to toggle test mode. In test mode, you can see how your overlay setup would look in-game, using preview render mode.");

    private static final Component APPLY_TOOLTIP = Component.literal("Click here to apply changes to current overlay.");

    private final Set<Float> verticalAlignmentLinePositions = new HashSet<>();
    private final Set<Float> horizontalAlignmentLinePositions = new HashSet<>();
    private final Map<Edge, Double> edgeAlignmentSnapMap = new EnumMap<>(Edge.class);
    private final Map<Edge, Float> alignmentLinesToRender = new EnumMap<>(Edge.class);

    private SelectionMode selectionMode = SelectionMode.NONE;
    private Overlay selectedOverlay;
    private final boolean fixedSelection;
    private Corner selectedCorner = null;
    private Edge selectedEdge = null;

    private boolean testMode = false;

    private boolean snappingEnabled = true;

    private boolean userInteracted = false;
    private int animationLengthRemaining;

    private OverlayManagementScreen(Overlay overlay) {
        super(Component.translatable("screens.wynntils.overlayManagement.name"));
        selectedOverlay = overlay;
        fixedSelection = true;
        animationLengthRemaining = ANIMATION_LENGTH;
    }

    private OverlayManagementScreen() {
        super(Component.translatable("screens.wynntils.overlayManagement.name"));
        selectedOverlay = null;
        fixedSelection = false;
        animationLengthRemaining = 0;
    }

    public static Screen create() {
        return new OverlayManagementScreen();
    }

    public static Screen create(Overlay overlay) {
        return new OverlayManagementScreen(overlay);
    }

    @Override
    protected void doInit() {
        setupButtons();
        calculateAlignmentLinePositions();
    }

    @Override
    public void onClose() {
        reloadConfigForOverlay();
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
    public void doRender(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (testMode) {
            TextRenderTask renderTask = new TextRenderTask(
                    I18n.get("screens.wynntils.overlayManagement.testModeOn"),
                    new TextRenderSetting(
                            0,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.TOP,
                            TextShadow.NORMAL));
            FontRenderer.getInstance().renderText(poseStack, this.width / 2f, this.height - 160, renderTask);
        } else {
            if (selectionMode != SelectionMode.NONE) {
                renderAlignmentLines(poseStack);
            } else {
                renderSections(poseStack);
            }

            Set<Overlay> overlays = Managers.Overlay.getOverlays().stream()
                    .filter(Managers.Overlay::isEnabled)
                    .collect(Collectors.toSet());

            for (Overlay overlay : overlays) {
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
                                StyledText2.of(overlay.getTranslatedName()),
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

            if (isMouseHoveringOverlay(selectedOverlay, mouseX, mouseY) && selectionMode == SelectionMode.NONE) {
                List<ClientTooltipComponent> clientTooltipComponents =
                        TooltipUtils.componentToClientTooltipComponent(HELP_TOOLTIP_LINES);
                int tooltipWidth = TooltipUtils.getToolTipWidth(
                        clientTooltipComponents, FontRenderer.getInstance().getFont());
                int tooltipHeight = TooltipUtils.getToolTipHeight(clientTooltipComponents);

                float renderX = mouseX > McUtils.window().getGuiScaledWidth() / 2f ? mouseX - tooltipWidth : mouseX;
                float renderY = mouseY > McUtils.window().getGuiScaledHeight() / 2f ? mouseY - tooltipHeight : mouseY;

                RenderUtils.drawTooltipAt(
                        poseStack,
                        renderX,
                        renderY,
                        100,
                        HELP_TOOLTIP_LINES,
                        FontRenderer.getInstance().getFont(),
                        false);
            }
        }

        super.doRender(poseStack, mouseX, mouseY, partialTick); // This renders widgets
        // This renders button tooltips
        if (this.deferredTooltipRendering != null) {
            this.renderTooltip(poseStack, this.deferredTooltipRendering, mouseX, mouseY);
            this.deferredTooltipRendering = null;
        }
    }

    private CustomColor getOverlayColor(Overlay overlay) {
        if (overlay == selectedOverlay) return CommonColors.GREEN;

        return fixedSelection ? new CustomColor(200, 200, 200, 255) : CommonColors.LIGHT_BLUE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Let the buttons of the Screen have priority
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (testMode) return false;

        userInteracted = true;
        animationLengthRemaining = 0;

        // Order:
        //  - Corners
        //  - Edges
        //  - OverlayArea

        // reset
        resetSelection();

        if (!fixedSelection) {
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

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && KeyboardUtils.isShiftDown()) {
            selectedOverlay.getConfigOptionFromString("position").ifPresent(ConfigHolder::reset);
            selectedOverlay.getConfigOptionFromString("size").ifPresent(ConfigHolder::reset);
            selectedOverlay
                    .getConfigOptionFromString("horizontalAlignmentOverride")
                    .ifPresent(ConfigHolder::reset);
            selectedOverlay
                    .getConfigOptionFromString("verticalAlignmentOverride")
                    .ifPresent(ConfigHolder::reset);

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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Let the buttons of the Screen have priority
        if (super.mouseReleased(mouseX, mouseY, button)) return true;
        if (testMode) return false;

        resetSelection();
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (testMode) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                testMode = false;
            }

            return false;
        }

        userInteracted = true;
        animationLengthRemaining = 0;

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            Managers.Config.saveConfig();
            onClose();
            McUtils.mc().setScreen(OverlaySelectionScreen.create());
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            McUtils.mc().setScreen(OverlaySelectionScreen.create());
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
                        .ifPresent(configHolder -> configHolder.setValue(values[finalIndex]));
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
                        .ifPresent(configHolder -> configHolder.setValue(values[finalIndex]));
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

            selectedOverlay
                    .getConfigOptionFromString("position")
                    .ifPresent(configHolder -> configHolder.setValue(OverlayPosition.getBestPositionFor(
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Let the buttons of the Screen have priority
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (testMode) return false;

        if (selectedOverlay == null) return false;

        switch (selectionMode) {
            case CORNER -> handleOverlayCornerDrag(dragX, dragY);
            case EDGE -> handleOverlayEdgeDrag(dragX, dragY);
            case AREA -> handleOverlayBodyDrag(dragX, dragY);
            default -> {}
        }

        return false;
    }

    private boolean isMouseHoveringOverlay(Overlay overlay, double mouseX, double mouseY) {
        if (overlay == null) {
            return false;
        }

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
        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.overlayManagement.closeSettingsScreen"), button -> {
                            onClose();
                            McUtils.mc().setScreen(OverlaySelectionScreen.create());
                        })
                .pos(this.width / 2 - BUTTON_WIDTH * 2, this.height - 150)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(CLOSE_TOOLTIP))
                .build());

        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.overlayManagement.testSettings"),
                        button -> testMode = !testMode)
                .pos(this.width / 2 - BUTTON_WIDTH / 2, this.height - 150)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(TEST_TOOLTIP))
                .build());

        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.overlayManagement.applySettings"), button -> {
                            Managers.Config.saveConfig();
                            onClose();
                            McUtils.mc().setScreen(OverlaySelectionScreen.create());
                        })
                .pos(this.width / 2 + BUTTON_WIDTH, this.height - 150)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(APPLY_TOOLTIP))
                .build());
    }

    private void resetSelection() {
        selectionMode = SelectionMode.NONE;
        selectedCorner = null;
        selectedEdge = null;
    }

    public boolean isTestMode() {
        return testMode;
    }

    private enum SelectionMode {
        NONE,
        CORNER,
        EDGE,
        AREA
    }
}
