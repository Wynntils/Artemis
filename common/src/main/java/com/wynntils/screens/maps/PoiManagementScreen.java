/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.config.HiddenConfig;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.map.MainMapFeature;
import com.wynntils.screens.base.WynntilsGridLayoutScreen;
import com.wynntils.screens.base.widgets.InfoButton;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.maps.widgets.PoiManagerWidget;
import com.wynntils.screens.maps.widgets.PoiSortButton;
import com.wynntils.services.map.pois.CustomPoi;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public final class PoiManagementScreen extends WynntilsGridLayoutScreen {
    // Constants
    private static final int GRID_ROWS_PER_PAGE = 43;
    private static final int HEADER_HEIGHT = 12;

    // Collections
    private final List<AbstractWidget> poiManagerWidgets = new ArrayList<>();
    private final List<CustomPoi> deletedPois = new ArrayList<>();
    private final List<Integer> deletedIndexes = new ArrayList<>();

    // Previous screen
    private final MainMapScreen oldMapScreen;

    // Widgets
    private Button deleteSelectedButton;
    private Button deselectAllButton;
    private Button downButton;
    private Button exportButton;
    private Button filterButton;
    private Button removeMarkersButton;
    private Button selectAllButton;
    private Button setMarkersButton;
    private Button undoDeleteButton;
    private Button upButton;
    private PoiSortButton activeSortButton;
    private PoiSortButton iconSortButton;
    private PoiSortButton nameSortButton;
    private PoiSortButton xSortButton;
    private PoiSortButton ySortButton;
    private PoiSortButton zSortButton;
    private TextInputBoxWidget focusedTextInput;
    private TextInputBoxWidget searchInput;

    // UI size, position etc
    private boolean draggingScroll = false;
    private float backgroundHeight;
    private float backgroundWidth;
    private float backgroundX;
    private float backgroundY;
    private float scrollButtonHeight;
    private float scrollButtonRenderX;
    private float scrollButtonRenderY;
    private int maxPoisToDisplay;
    private int scrollAreaHeight;
    private int scrollOffset = 0;

    // Poi display
    private boolean selectionMode = false;
    private List<CustomPoi> selectedPois = new ArrayList<>();
    private List<CustomPoi> pois;
    private Map<Texture, Boolean> filteredIcons = new EnumMap<>(Texture.class);
    private PoiSortOrder sortOrder;

    private PoiManagementScreen(MainMapScreen oldMapScreen) {
        super(Component.literal("Poi Management Screen"));

        this.oldMapScreen = oldMapScreen;
    }

    public static Screen create() {
        return new PoiManagementScreen(null);
    }

    public static Screen create(MainMapScreen oldMapScreen) {
        return new PoiManagementScreen(oldMapScreen);
    }

    @Override
    public void onClose() {
        McUtils.mc().setScreen(oldMapScreen);
    }

    @Override
    protected void doInit() {
        super.doInit();
        // How many pois can fit on the background
        maxPoisToDisplay = (int) (dividedHeight * GRID_ROWS_PER_PAGE) / 20;
        backgroundX = dividedWidth * 10;
        backgroundWidth = dividedWidth * 44;
        backgroundY = dividedHeight * 7;
        backgroundHeight = dividedHeight * 50;

        // Height of the scroll button relative to the scaled width
        scrollButtonHeight = ((dividedWidth / 2) / Texture.SCROLL_BUTTON.width()) * Texture.SCROLL_BUTTON.height();

        // How far the scrollbar should be able to go
        scrollAreaHeight = (int) (backgroundHeight - scrollButtonHeight) - (int) (dividedHeight * 4);

        // X position of the scroll button
        scrollButtonRenderX = (int) (dividedWidth * 52) + (dividedWidth / 4);

        int importExportButtonWidth = (int) (dividedWidth * 6);

        // region exit button
        this.addRenderableWidget(
                new Button.Builder(Component.literal("X").withStyle(ChatFormatting.RED), (button) -> this.onClose())
                        .pos((int) (dividedWidth * 60), (int) (dividedHeight * 4))
                        .size(20, 20)
                        .build());
        // endregion

        // region info button
        this.addRenderableWidget(new InfoButton(
                (int) (dividedWidth * 3),
                (int) (dividedHeight * 4),
                Component.literal("")
                        .append(Component.translatable("screens.wynntils.poiManagementGui.help")
                                .withStyle(ChatFormatting.UNDERLINE))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("screens.wynntils.poiManagementGui.help1")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("screens.wynntils.poiManagementGui.help2")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("screens.wynntils.poiManagementGui.help3")
                                .withStyle(ChatFormatting.GRAY))));
        // endregion

        // region import/export
        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.import"),
                        (button) -> importFromClipboard())
                .pos((int) (dividedWidth * 22), (int) (dividedHeight * 58))
                .size(importExportButtonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("screens.wynntils.poiManagementGui.import.tooltip")))
                .build());

        exportButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.export"),
                        (button) -> exportToClipboard())
                .pos((int) (dividedWidth * 36), (int) (dividedHeight * 58))
                .size(importExportButtonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("screens.wynntils.poiManagementGui.exportAll.tooltip")))
                .build();

        this.addRenderableWidget(exportButton);
        // endregion

        // region delete buttons
        undoDeleteButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.undo"), (button) -> undoDelete())
                .pos((int) (dividedWidth * 55), (int) (dividedHeight * 58))
                .size((int) (dividedWidth * 8), 20)
                .build();

        this.addRenderableWidget(undoDeleteButton);

        deleteSelectedButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.deleteSelected"),
                        (button) -> deleteSelectedPois())
                .pos((int) (dividedWidth * 55), (int) (dividedHeight * 58) - 25)
                .size((int) (dividedWidth * 8), 20)
                .build();

        deleteSelectedButton.active = false;

        this.addRenderableWidget(deleteSelectedButton);
        // endregion

        // region marker buttons
        setMarkersButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.setMarkers"),
                        (button) -> toggleMarkers(true))
                .pos((int) dividedWidth, (int) (dividedHeight * 58) - 75)
                .size((int) (dividedWidth * 8), 20)
                .tooltip(Tooltip.create(Component.translatable("screens.wynntils.poiManagementGui.setMarkers.tooltip")))
                .build();

        setMarkersButton.active = false;

        this.addRenderableWidget(setMarkersButton);

        removeMarkersButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.removeMarkers"),
                        (button) -> toggleMarkers(false))
                .pos((int) dividedWidth, (int) (dividedHeight * 58) - 50)
                .size((int) (dividedWidth * 8), 20)
                .tooltip(Tooltip.create(
                        Component.translatable("screens.wynntils.poiManagementGui.removeMarkers.tooltip")))
                .build();

        removeMarkersButton.active = false;

        this.addRenderableWidget(removeMarkersButton);
        // endregion

        // region select buttons
        deselectAllButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.deselectAll"),
                        (button) -> toggleSelectAll(false))
                .pos((int) dividedWidth, (int) (dividedHeight * 58))
                .size((int) (dividedWidth * 8), 20)
                .build();

        deselectAllButton.active = false;

        this.addRenderableWidget(deselectAllButton);

        selectAllButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.selectAll"),
                        (button) -> toggleSelectAll(true))
                .pos((int) dividedWidth, (int) (dividedHeight * 58) - 25)
                .size((int) (dividedWidth * 8), 20)
                .build();

        this.addRenderableWidget(selectAllButton);
        // endregion

        // region up/down buttons
        upButton = new Button.Builder(Component.literal("🠝"), (button) -> updateSelectedPoiPositions(-1))
                .pos((width / 2) - 22, (int) (dividedHeight * 58))
                .size(20, 20)
                .build();

        upButton.visible = false;

        this.addRenderableWidget(upButton);

        downButton = new Button.Builder(Component.literal("🠟"), (button) -> updateSelectedPoiPositions(1))
                .pos((width / 2) + 2, (int) (dividedHeight * 58))
                .size(20, 20)
                .build();

        downButton.visible = false;

        this.addRenderableWidget(downButton);
        // endregion

        // region icon filter button
        int filterButtonWidth = (int) (dividedWidth * 10);

        filterButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.filter"), (button) -> {
                            scrollOffset = 0;
                            McUtils.mc().setScreen(IconFilterScreen.create(this, filteredIcons));
                        })
                .pos((int) (dividedWidth * 44), (int) (dividedHeight * 3))
                .size(filterButtonWidth, 20)
                .build();

        this.addRenderableWidget(filterButton);
        // endregion

        // region search bar
        searchInput = new TextInputBoxWidget(
                (int) (dividedWidth * 10) + 5,
                (int) (dividedHeight * 3),
                (int) (backgroundWidth - filterButtonWidth - 10),
                20,
                (s) -> {
                    scrollOffset = 0;
                    populatePois();
                },
                this,
                searchInput);

        this.addRenderableWidget(searchInput);

        setFocusedTextInput(searchInput);
        // endregion

        // region sort buttons
        int iconTitleWidth = (McUtils.mc().font.width(I18n.get("screens.wynntils.poiManagementGui.icon"))) + 1;

        int nameTitleWidth = (McUtils.mc().font.width(I18n.get("screens.wynntils.poiManagementGui.name"))) + 1;

        int coordinateTitleWidth = (McUtils.mc().font.width("X")) + 1;

        iconSortButton = this.addRenderableWidget(new PoiSortButton(
                (int) (dividedWidth * 13) - (iconTitleWidth / 2),
                (int) (dividedHeight * HEADER_HEIGHT) - 10,
                iconTitleWidth,
                10,
                Component.translatable("screens.wynntils.poiManagementGui.icon"),
                this,
                PoiSortType.ICON));

        nameSortButton = this.addRenderableWidget(new PoiSortButton(
                (int) (dividedWidth * 22) - (nameTitleWidth / 2),
                (int) (dividedHeight * HEADER_HEIGHT) - 10,
                nameTitleWidth,
                10,
                Component.translatable("screens.wynntils.poiManagementGui.name"),
                this,
                PoiSortType.NAME));

        xSortButton = this.addRenderableWidget(new PoiSortButton(
                (int) (dividedWidth * 32) - (coordinateTitleWidth / 2),
                (int) (dividedHeight * HEADER_HEIGHT) - 10,
                coordinateTitleWidth,
                10,
                Component.literal("X"),
                this,
                PoiSortType.X));

        ySortButton = this.addRenderableWidget(new PoiSortButton(
                (int) (dividedWidth * 35) - (coordinateTitleWidth / 2),
                (int) (dividedHeight * HEADER_HEIGHT) - 10,
                coordinateTitleWidth,
                10,
                Component.literal("Y"),
                this,
                PoiSortType.Y));

        zSortButton = this.addRenderableWidget(new PoiSortButton(
                (int) (dividedWidth * 38) - (coordinateTitleWidth / 2),
                (int) (dividedHeight * HEADER_HEIGHT) - 10,
                coordinateTitleWidth,
                10,
                Component.literal("Z"),
                this,
                PoiSortType.Z));
        // endregion

        if (deletedIndexes.isEmpty()) {
            undoDeleteButton.active = false;
        }

        pois = Managers.Feature.getFeatureInstance(MainMapFeature.class)
                .customPois
                .get();

        if (pois.isEmpty()) {
            searchInput.visible = false;
            filterButton.visible = false;
            iconSortButton.visible = false;
            nameSortButton.visible = false;
            xSortButton.visible = false;
            ySortButton.visible = false;
            zSortButton.visible = false;
            exportButton.active = false;
        }

        updateAllUsedIcons();
        populatePois();
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.doRender(guiGraphics, mouseX, mouseY, partialTick);
        PoseStack poseStack = guiGraphics.pose();
        renderScrollButton(poseStack);

        if (Managers.Feature.getFeatureInstance(MainMapFeature.class)
                .customPois
                .get()
                .isEmpty()) {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromComponent(
                                    Component.translatable("screens.wynntils.poiManagementGui.noPois")),
                            (int) (dividedWidth * 32),
                            (int) (dividedHeight * 32),
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NORMAL);

            return;
        }

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromComponent(Component.translatable("screens.wynntils.poiManagementGui.search")),
                        (int) (dividedWidth * 10) + 5,
                        (int) dividedHeight,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NORMAL);

        if (pois.isEmpty()) {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromComponent(
                                    Component.translatable("screens.wynntils.poiManagementGui.noFilteredPois")),
                            (int) (dividedWidth * 32),
                            (int) (dividedHeight * 32),
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NORMAL);
        } else {
            RenderUtils.drawRect(
                    poseStack,
                    CommonColors.WHITE,
                    (int) (dividedWidth * 12),
                    (int) (dividedHeight * HEADER_HEIGHT),
                    0,
                    (int) (dividedWidth * 38),
                    1);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        RenderUtils.drawScalingTexturedRect(
                guiGraphics.pose(),
                Texture.WAYPOINT_MANAGER_BACKGROUND.resource(),
                backgroundX,
                backgroundY,
                0,
                backgroundWidth,
                backgroundHeight,
                Texture.WAYPOINT_MANAGER_BACKGROUND.width(),
                Texture.WAYPOINT_MANAGER_BACKGROUND.height());
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        if (!draggingScroll && (pois.size() > maxPoisToDisplay)) {
            if (MathUtils.isInside(
                    (int) mouseX,
                    (int) mouseY,
                    (int) scrollButtonRenderX,
                    (int) (scrollButtonRenderX + (dividedWidth / 2)),
                    (int) scrollButtonRenderY,
                    (int) (scrollButtonRenderY + scrollButtonHeight))) {
                draggingScroll = true;

                return true;
            }
        }

        return super.doMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingScroll) return false;

        int renderY = (int) ((this.height - backgroundHeight) / 2 + (int) (dividedHeight * 3));
        int scrollAreaStartY = renderY + 7;

        int newValue = Math.round(MathUtils.map(
                (float) mouseY,
                scrollAreaStartY,
                scrollAreaStartY + scrollAreaHeight,
                0,
                Math.max(0, pois.size() - maxPoisToDisplay)));

        scroll(newValue - scrollOffset);

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double scrollValue = -Math.signum(deltaY);
        scroll((int) scrollValue);

        return true;
    }

    public void selectPoi(CustomPoi selectedPoi) {
        boolean addedPoi = true;

        // Deselect a poi
        if (selectedPois.contains(selectedPoi)) {
            selectedPois.remove(selectedPoi);

            addedPoi = false;
        } else {
            selectedPois.add(selectedPoi);
        }

        selectAllButton.active = selectedPois.size() < pois.size();

        if (addedPoi || selectedPois.isEmpty()) {
            selectionMode = addedPoi;

            deselectAllButton.active = addedPoi;
            deleteSelectedButton.active = addedPoi;
            setMarkersButton.active = addedPoi;
            removeMarkersButton.active = addedPoi;

            upButton.visible = addedPoi;
            downButton.visible = addedPoi;

            // Export tooltip should display how many of the pois will be exported
            Component tooltip = addedPoi
                    ? Component.translatable(
                            "screens.wynntils.poiManagementGui.exportSelected.tooltip", selectedPois.size())
                    : Component.translatable("screens.wynntils.poiManagementGui.exportAll.tooltip");

            exportButton.setTooltip(Tooltip.create(tooltip));
        }

        populatePois();
    }

    public void toggleSortType(PoiSortType sortType, PoiSortButton selectedButton) {
        if (activeSortButton != null && activeSortButton != selectedButton) {
            activeSortButton.setSelected(false);
        }

        activeSortButton = selectedButton;

        boolean selected = toggleSortOrder(sortType);

        selectedButton.setSelected(selected);

        populatePois();
    }

    public void deletePoi(CustomPoi poiToDelete) {
        HiddenConfig<List<CustomPoi>> customPois = Managers.Feature.getFeatureInstance(MainMapFeature.class).customPois;
        int deletedPoiIndex = customPois.get().indexOf(poiToDelete);

        customPois.get().remove(poiToDelete);
        customPois.touched();
        Managers.Feature.getFeatureInstance(MainMapFeature.class).updateWaypoints();

        deletedPois.add(poiToDelete);
        deletedIndexes.add(deletedPoiIndex);

        undoDeleteButton.active = true;

        if (customPois.get().isEmpty()) {
            searchInput.visible = false;
            iconSortButton.visible = false;
            nameSortButton.visible = false;
            xSortButton.visible = false;
            ySortButton.visible = false;
            zSortButton.visible = false;
            selectAllButton.active = false;
            exportButton.active = false;
        }

        // Handles scrolling when deleting the last poi
        if (scrollOffset == Math.max(0, pois.size() - maxPoisToDisplay)) {
            scroll(-1);
        }

        updateAllUsedIcons();

        populatePois();
    }

    public void updatePoiPosition(CustomPoi poiToMove, int direction) {
        int poiToMoveIndex = pois.indexOf(poiToMove);

        // If poi is at the top/bottom of list or if it was selected but then filtered out, don't move it
        if (poiToMoveIndex == -1 || poiToMoveIndex + direction < 0 || poiToMoveIndex + direction > pois.size() - 1) {
            return;
        }

        CustomPoi poiToSwap = pois.get(pois.indexOf(poiToMove) + direction);

        HiddenConfig<List<CustomPoi>> customPois = Managers.Feature.getFeatureInstance(MainMapFeature.class).customPois;

        Collections.swap(
                customPois.get(),
                customPois.get().indexOf(poiToMove),
                customPois.get().indexOf(poiToSwap));

        customPois.touched();
        Managers.Feature.getFeatureInstance(MainMapFeature.class).updateWaypoints();

        populatePois();
    }

    public List<CustomPoi> getPois() {
        return Collections.unmodifiableList(pois);
    }

    public void setFilteredIcons(Map<Texture, Boolean> filteredIcons) {
        this.filteredIcons = filteredIcons;
    }

    private void renderScrollButton(PoseStack poseStack) {
        // Don't render the scroll button if it will not be useable
        if (pois.size() <= maxPoisToDisplay) return;

        // Calculate where the scroll button should be on the Y axis
        scrollButtonRenderY = (this.height - backgroundHeight) / 2
                + (int) (dividedHeight * 3)
                + MathUtils.map(scrollOffset, 0, pois.size() - maxPoisToDisplay, 0, scrollAreaHeight);

        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.SCROLL_BUTTON.resource(),
                scrollButtonRenderX,
                scrollButtonRenderY,
                1,
                (dividedWidth / 2),
                scrollButtonHeight,
                Texture.SCROLL_BUTTON.width(),
                Texture.SCROLL_BUTTON.height());
    }

    private void scroll(int delta) {
        // Calculate how many pois should be scrolled past
        scrollOffset = MathUtils.clamp(scrollOffset + delta, 0, Math.max(0, pois.size() - maxPoisToDisplay));

        populatePois();
    }

    private void populatePois() {
        // Remove old widgets
        for (AbstractWidget widget : poiManagerWidgets) {
            this.removeWidget(widget);
        }

        this.poiManagerWidgets.clear();

        // Get full list of pois
        pois = Managers.Feature.getFeatureInstance(MainMapFeature.class).customPois.get().stream()
                .filter(poi -> searchMatches(poi.getName()))
                .collect(Collectors.toList());

        // No pois
        if (pois.isEmpty()) {
            return;
        }

        // Filter pois based on filtered icons
        pois = pois.stream()
                .filter(poi -> filteredIcons.getOrDefault(poi.getIcon(), true))
                .collect(Collectors.toList());

        // Hide buttons if no filtered pois
        iconSortButton.visible = !pois.isEmpty();
        nameSortButton.visible = !pois.isEmpty();
        xSortButton.visible = !pois.isEmpty();
        ySortButton.visible = !pois.isEmpty();
        zSortButton.visible = !pois.isEmpty();
        exportButton.active = !pois.isEmpty();
        selectAllButton.active = !pois.isEmpty();
        deselectAllButton.active = !selectedPois.isEmpty();
        setMarkersButton.active = !selectedPois.isEmpty();
        removeMarkersButton.active = !selectedPois.isEmpty();

        // Only hide search bar & filter button if no pois at all
        // Can't check if filtered pois is empty as they may be empty due to the filters
        if (Managers.Feature.getFeatureInstance(MainMapFeature.class)
                .customPois
                .get()
                .isEmpty()) {
            searchInput.visible = false;
            filterButton.visible = false;
        }

        // No filtered pois
        if (pois.isEmpty()) {
            return;
        }

        // Sort pois
        if (sortOrder != null) {
            sortPois();
        }

        // Starting Y position for the pois
        int row = (int) ((int) (dividedHeight * HEADER_HEIGHT) + (dividedHeight / 2f));

        int currentPoi;

        // Render manager widgets
        for (int i = 0; i < maxPoisToDisplay; i++) {
            // Get the poi to render
            currentPoi = i + scrollOffset;

            // If user has less pois than maxPoisToDisplay, make sure we don't try and get a poi out of range
            if (currentPoi > pois.size() - 1) {
                break;
            }

            CustomPoi poi = pois.get(currentPoi);

            PoiManagerWidget poiWidget = new PoiManagerWidget(
                    (int) (dividedWidth * 12),
                    row,
                    (int) (dividedWidth * 38),
                    20,
                    poi,
                    this,
                    dividedWidth,
                    selectionMode,
                    selectedPois.contains(poi));

            // Each widget height is 20, add 20 for Y position of next widget
            row += 20;

            poiManagerWidgets.add(poiWidget);

            this.addRenderableWidget(poiWidget);
        }
    }

    private void sortPois() {
        // Sort pois, ignore case and for null Y's, treat them as 0
        switch (sortOrder) {
            case ICON_ASC -> pois.sort(Comparator.comparing(CustomPoi::getIcon));
            case ICON_DESC -> pois.sort(Comparator.comparing(CustomPoi::getIcon).reversed());
            case NAME_ASC -> pois.sort(Comparator.comparing(CustomPoi::getName, String.CASE_INSENSITIVE_ORDER));
            case NAME_DESC -> pois.sort(Comparator.comparing(CustomPoi::getName, String.CASE_INSENSITIVE_ORDER)
                    .reversed());
            case X_ASC -> pois.sort(
                    Comparator.comparing(poi -> poi.getLocation().getX()));
            case X_DESC -> pois.sort(
                    Comparator.comparing(poi -> poi.getLocation().getX(), Comparator.reverseOrder()));
            case Y_ASC -> pois.sort(Comparator.comparing(
                    poi -> poi.getLocation().getY().orElse(null), Comparator.nullsFirst(Comparator.naturalOrder())));
            case Y_DESC -> pois.sort(Comparator.comparing(
                    poi -> poi.getLocation().getY().orElse(null), Comparator.nullsLast(Comparator.reverseOrder())));
            case Z_ASC -> pois.sort(
                    Comparator.comparing(poi -> poi.getLocation().getZ()));
            case Z_DESC -> pois.sort(
                    Comparator.comparing(poi -> poi.getLocation().getZ(), Comparator.reverseOrder()));
        }
    }

    private void toggleMarkers(boolean addMarkers) {
        if (addMarkers) {
            selectedPois.forEach(poi -> Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(
                    poi.getLocation().asLocation(), poi.getIcon(), poi.getColor(), poi.getColor()));
        } else {
            selectedPois.forEach(poi -> Models.Marker.USER_WAYPOINTS_PROVIDER.removeLocation(
                    poi.getLocation().asLocation()));
        }
    }

    private void toggleSelectAll(boolean select) {
        selectionMode = select;

        upButton.visible = select;
        downButton.visible = select;
        deleteSelectedButton.active = select;

        Component tooltip;

        if (select) {
            selectedPois = pois;

            tooltip = Component.translatable(
                    "screens.wynntils.poiManagementGui.exportSelected.tooltip", selectedPois.size());
        } else {
            selectedPois.clear();

            tooltip = Component.translatable("screens.wynntils.poiManagementGui.exportAll.tooltip");
        }

        exportButton.setTooltip(Tooltip.create(tooltip));

        populatePois();
    }

    private boolean toggleSortOrder(PoiSortType sortType) {
        PoiSortOrder newOrder = null;
        boolean selected = true;

        // Update the sort order of pois, first click is ascending, second descending and third will reset
        switch (sortType) {
            case ICON -> {
                if (sortOrder == null) {
                    newOrder = PoiSortOrder.ICON_ASC;
                } else if (sortOrder == PoiSortOrder.ICON_ASC) {
                    newOrder = PoiSortOrder.ICON_DESC;
                }
            }
            case NAME -> {
                if (sortOrder == null) {
                    newOrder = PoiSortOrder.NAME_ASC;
                } else if (sortOrder == PoiSortOrder.NAME_ASC) {
                    newOrder = PoiSortOrder.NAME_DESC;
                }
            }
            case X -> {
                if (sortOrder == null) {
                    newOrder = PoiSortOrder.X_ASC;
                } else if (sortOrder == PoiSortOrder.X_ASC) {
                    newOrder = PoiSortOrder.X_DESC;
                }
            }
            case Y -> {
                if (sortOrder == null) {
                    newOrder = PoiSortOrder.Y_ASC;
                } else if (sortOrder == PoiSortOrder.Y_ASC) {
                    newOrder = PoiSortOrder.Y_DESC;
                }
            }
            case Z -> {
                if (sortOrder == null) {
                    newOrder = PoiSortOrder.Z_ASC;
                } else if (sortOrder == PoiSortOrder.Z_ASC) {
                    newOrder = PoiSortOrder.Z_DESC;
                }
            }
            default -> {
                selected = false;
            }
        }

        sortOrder = newOrder;

        // If the selected sort button is now the active sort, first 2 clicks true, 3rd false
        return newOrder != null && selected;
    }

    private void updateAllUsedIcons() {
        // Get all icons from all pois, default their "active" boolean to true.
        filteredIcons = Managers.Feature.getFeatureInstance(MainMapFeature.class).customPois.get().stream()
                .map(CustomPoi::getIcon)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        texture -> filteredIcons.getOrDefault(texture, true),
                        (existing, replacement) -> existing,
                        () -> new EnumMap<>(Texture.class)));

        // Only show filter button if there is more than 1 used icon
        filterButton.visible = filteredIcons.size() > 1;
    }

    private boolean searchMatches(String poiName) {
        return StringUtils.partialMatch(poiName, searchInput.getTextBoxInput());
    }

    private void updateSelectedPoiPositions(int direction) {
        // Get selected pois in the same order they are in pois
        List<CustomPoi> orderedPois =
                pois.stream().filter(poi -> selectedPois.contains(poi)).collect(Collectors.toList());

        // If we are shifting pois down the list needs to be reversed
        if (direction == 1) {
            Collections.reverse(orderedPois);
        }

        for (CustomPoi selectedPoi : orderedPois) {
            updatePoiPosition(selectedPoi, direction);
        }
    }

    private void deleteSelectedPois() {
        HiddenConfig<List<CustomPoi>> customPois = Managers.Feature.getFeatureInstance(MainMapFeature.class).customPois;

        for (CustomPoi poi : selectedPois) {
            deletePoi(poi);
        }

        McUtils.sendMessageToClient(
                Component.translatable("screens.wynntils.poiManagementGui.deletedPois", selectedPois.size())
                        .withStyle(ChatFormatting.GREEN));

        if (customPois.get().isEmpty()) {
            selectAllButton.active = false;
            filterButton.visible = false;
            iconSortButton.visible = false;
            nameSortButton.visible = false;
            xSortButton.visible = false;
            ySortButton.visible = false;
            zSortButton.visible = false;
        }

        // Scroll up 1 poi
        if (scrollOffset == Math.max(0, pois.size() - maxPoisToDisplay)) {
            scroll(-1);
        }

        // If any pois were selected, deselect them all as they were deleted
        toggleSelectAll(false);
    }

    private void importFromClipboard() {
        String clipboard = McUtils.mc().keyboardHandler.getClipboard();

        CustomPoi[] customPois;
        try {
            customPois = Managers.Json.GSON.fromJson(clipboard, CustomPoi[].class);
        } catch (JsonSyntaxException e) {
            McUtils.sendErrorToClient(I18n.get("screens.wynntils.poiManagementGui.import.error"));
            return;
        }

        if (customPois == null) {
            McUtils.sendErrorToClient(I18n.get("screens.wynntils.poiManagementGui.import.error"));
            return;
        }

        HiddenConfig<List<CustomPoi>> customPoiConfig =
                Managers.Feature.getFeatureInstance(MainMapFeature.class).customPois;
        List<CustomPoi> existingPois = new ArrayList<>(customPoiConfig.get());

        // Only add pois that don't already exist
        List<CustomPoi> poisToAdd = Stream.of(customPois)
                .filter(newPoi -> !existingPois.contains(newPoi))
                .toList();

        existingPois.addAll(poisToAdd);
        customPoiConfig.setValue(existingPois);
        customPoiConfig.touched();
        Managers.Feature.getFeatureInstance(MainMapFeature.class).updateWaypoints();

        // Enable search and filter after importing new pois
        if (!customPoiConfig.get().isEmpty()) {
            searchInput.visible = true;
            filterButton.visible = true;
        }

        updateAllUsedIcons();

        populatePois();

        McUtils.sendMessageToClient(
                Component.translatable("screens.wynntils.poiManagementGui.import.success", poisToAdd.size())
                        .withStyle(ChatFormatting.GREEN));
    }

    private void exportToClipboard() {
        List<CustomPoi> poisToExport = selectedPois.isEmpty()
                ? Managers.Feature.getFeatureInstance(MainMapFeature.class)
                        .customPois
                        .get()
                : selectedPois;

        McUtils.mc()
                .keyboardHandler
                .setClipboard(poisToExport.stream()
                        .map(Managers.Json.GSON::toJson)
                        .toList()
                        .toString());

        McUtils.sendMessageToClient(
                Component.translatable("screens.wynntils.poiManagementGui.exportedPois", poisToExport.size())
                        .withStyle(ChatFormatting.GREEN));
    }

    private void undoDelete() {
        HiddenConfig<List<CustomPoi>> allPois = Managers.Feature.getFeatureInstance(MainMapFeature.class).customPois;

        // Potentially a poi that was deleted had been imported so don't want a duplicate
        if (!allPois.get().contains(deletedPois.getLast())) {
            allPois.get().add(deletedIndexes.getLast(), deletedPois.getLast());

            allPois.touched();
            Managers.Feature.getFeatureInstance(MainMapFeature.class).updateWaypoints();

            // Scroll up 1 poi if not already at the top of the list
            scrollOffset = Math.max(scrollOffset - 1, 0);

            updateAllUsedIcons();

            populatePois();
        }

        deletedIndexes.removeLast();
        deletedPois.removeLast();

        undoDeleteButton.active = !deletedIndexes.isEmpty();
    }

    public enum PoiSortType {
        ICON,
        NAME,
        X,
        Y,
        Z
    }

    private enum PoiSortOrder {
        ICON_ASC,
        ICON_DESC,
        NAME_ASC,
        NAME_DESC,
        X_ASC,
        X_DESC,
        Y_ASC,
        Y_DESC,
        Z_ASC,
        Z_DESC
    }
}
