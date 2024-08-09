/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.overlays.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.CustomNameProperty;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.persisted.Translatable;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.OverlayGroupHolder;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.overlays.CustomBarsOverlayFeature;
import com.wynntils.features.overlays.InfoBoxFeature;
import com.wynntils.overlays.custombars.CustomBarOverlayBase;
import com.wynntils.overlays.infobox.InfoBoxOverlay;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.widgets.SearchWidget;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.base.widgets.WynntilsCheckbox;
import com.wynntils.screens.overlays.placement.OverlayManagementScreen;
import com.wynntils.screens.overlays.selection.widgets.OverlayButton;
import com.wynntils.screens.overlays.selection.widgets.OverlayOptionsButton;
import com.wynntils.screens.settings.widgets.ConfigTile;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class OverlaySelectionScreen extends WynntilsScreen {
    private static final float SCROLL_FACTOR = 10f;
    private static final int CONFIG_MASK_TOP_Y = 25;
    private static final int CONFIG_MASK_BOTTOM_Y = 197;
    private static final int CONFIGS_PER_PAGE = 4;
    private static final int MAX_OVERLAYS_PER_PAGE = 8;

    // Collections
    private List<Overlay> overlayList = new ArrayList<>();
    private List<OverlayButton> overlays = new ArrayList<>();
    private List<OverlayOptionsButton> optionButtons = new ArrayList<>();
    private List<WynntilsButton> configs = new ArrayList<>();

    // Renderables
    private final SearchWidget searchWidget;
    private Button exitPreviewButton;
    private OverlayOptionsButton allButton;
    private OverlayOptionsButton builtInButton;
    private OverlayOptionsButton customButton;
    private OverlayOptionsButton selectedFilterButton;
    private TextInputBoxWidget focusedTextInput;
    private WynntilsCheckbox renderOverlaysCheckbox;

    // UI size, positions, etc
    private boolean draggingOverlayScroll = false;
    private boolean draggingConfigScroll = false;
    private float configScrollY;
    private float overlayScrollY;
    private float translationX;
    private float translationY;
    private int configScrollOffset = 0;
    private int overlayScrollOffset = 0;

    // Overlay display
    private boolean renderPreview = false;
    private boolean showOverlays = false;
    private FilterType filterType = FilterType.ALL;
    private Overlay selectedOverlay;

    private OverlaySelectionScreen() {
        super(Component.translatable("screens.wynntils.overlaySelection.name"));

        searchWidget = new SearchWidget(
                7,
                5,
                120,
                20,
                (s) -> {
                    overlayScrollOffset = 0;
                    populateOverlays();
                },
                this);

        setFocusedTextInput(searchWidget);
    }

    public static Screen create() {
        return new OverlaySelectionScreen();
    }

    @Override
    protected void doInit() {
        translationX = (this.width - Texture.OVERLAY_SELECTION_GUI.width()) / 2f;
        translationY = (this.height - Texture.OVERLAY_SELECTION_GUI.height()) / 2f;
        addOptionButtons();

        // region Preview renderables
        exitPreviewButton = this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.overlaySelection.exitPreview"),
                        (button) -> togglePreview(false))
                .pos((Texture.OVERLAY_SELECTION_GUI.width() / 2) - 40, (int) (this.height - 25 - translationY))
                .size(80, 20)
                .tooltip(Tooltip.create(Component.translatable("screens.wynntils.overlaySelection.exitPreviewTooltip")))
                .build());

        renderOverlaysCheckbox = this.addRenderableWidget(new WynntilsCheckbox(
                (Texture.OVERLAY_SELECTION_GUI.width() / 2) - 70,
                (int) (this.height - 70 - translationY),
                20,
                Component.translatable("screens.wynntils.overlaySelection.showOverlays"),
                showOverlays,
                120,
                (c, b) -> showOverlays = b,
                ComponentUtils.wrapTooltips(
                        List.of(Component.translatable("screens.wynntils.overlaySelection.showOverlaysTooltip")),
                        150)));
        // endregion

        togglePreview(renderPreview);

        this.addRenderableWidget(searchWidget);
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(translationX, translationY, 0);

        // Adjust mouse for posestack translation
        int adjustedMouseX = mouseX - (int) translationX;
        int adjustedMouseY = mouseY - (int) translationY;

        // When not rendering a preview of the selected overlay
        if (!renderPreview) {
            RenderUtils.drawTexturedRect(poseStack, Texture.OVERLAY_SELECTION_GUI, 0, 0);

            searchWidget.render(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);

            renderWidgets(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);

            renderTooltips(guiGraphics, mouseX, mouseY);

            if (selectedOverlay != null) {
                String textToRender = selectedOverlay.getTranslatedName();

                if (selectedOverlay instanceof CustomNameProperty customNameProperty) {
                    if (!customNameProperty.getCustomName().get().isEmpty()) {
                        textToRender = customNameProperty.getCustomName().get();
                    }
                }

                FontRenderer.getInstance()
                        .renderAlignedTextInBox(
                                poseStack,
                                StyledText.fromString(textToRender),
                                146,
                                338,
                                4,
                                24,
                                200,
                                CommonColors.WHITE,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.MIDDLE,
                                TextShadow.NORMAL);
            } else {
                FontRenderer.getInstance()
                        .renderAlignedTextInBox(
                                poseStack,
                                StyledText.fromComponent(
                                        Component.translatable("screens.wynntils.overlaySelection.unselectedOverlay")),
                                146,
                                338,
                                67,
                                160,
                                200,
                                CommonColors.WHITE,
                                HorizontalAlignment.CENTER,
                                VerticalAlignment.TOP,
                                TextShadow.NORMAL);
            }

            if (overlayList.size() > MAX_OVERLAYS_PER_PAGE) {
                renderOverlayScroll(poseStack);
            }

            if (selectedOverlay != null
                    && selectedOverlay.getVisibleConfigOptions().size() > CONFIGS_PER_PAGE) {
                renderConfigScroll(poseStack);
            }

            poseStack.popPose();
        } else {
            // Render options, these need to be rendered before the posestack translation is reverted
            renderOverlaysCheckbox.render(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);
            exitPreviewButton.render(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);

            // Revert the posestack translation and render the preview with an outline
            poseStack.popPose();

            // We don't have a delta tracker here, so we'll just use a zero delta tracker
            selectedOverlay.renderPreview(poseStack, guiGraphics.bufferSource(), DeltaTracker.ZERO, McUtils.window());

            RenderUtils.drawRectBorders(
                    poseStack,
                    CommonColors.RED,
                    selectedOverlay.getRenderX(),
                    selectedOverlay.getRenderY(),
                    selectedOverlay.getRenderX() + selectedOverlay.getWidth(),
                    selectedOverlay.getRenderY() + selectedOverlay.getHeight(),
                    1,
                    1);
        }
    }

    @Override
    public void added() {
        searchWidget.opened();
        super.added();
    }

    @Override
    public void onClose() {
        super.onClose();

        renderPreview = false;
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        double adjustedMouseX = mouseX - translationX;
        double adjustedMouseY = mouseY - translationY;

        if (!renderPreview) {
            if (!draggingOverlayScroll && overlayList.size() > MAX_OVERLAYS_PER_PAGE) {
                if (MathUtils.isInside(
                        (int) adjustedMouseX,
                        (int) adjustedMouseY,
                        132,
                        132 + Texture.SCROLL_BUTTON.width(),
                        (int) overlayScrollY,
                        (int) (overlayScrollY + Texture.SCROLL_BUTTON.height()))) {
                    draggingOverlayScroll = true;

                    return true;
                }
            }

            if (!draggingConfigScroll
                    && selectedOverlay != null
                    && selectedOverlay.getVisibleConfigOptions().size() > CONFIGS_PER_PAGE) {
                if (MathUtils.isInside(
                        (int) adjustedMouseX,
                        (int) adjustedMouseY,
                        344,
                        344 + Texture.SCROLL_BUTTON.width(),
                        (int) configScrollY,
                        (int) (configScrollY + Texture.SCROLL_BUTTON.height()))) {
                    draggingConfigScroll = true;

                    return true;
                }
            }
        }

        for (GuiEventListener listener : getWidgetsForIteration().toList()) {
            if (listener.isMouseOver(adjustedMouseX, adjustedMouseY)) {
                // Buttons have a slight bit rendered underneath the background but we don't want that part to be
                // clickable
                if (listener instanceof OverlayOptionsButton) {
                    if (MathUtils.isInside(
                            (int) adjustedMouseX,
                            (int) adjustedMouseY,
                            0,
                            Texture.OVERLAY_SELECTION_GUI.width(),
                            0,
                            Texture.OVERLAY_SELECTION_GUI.height())) {
                        return false;
                    }
                }

                listener.mouseClicked(adjustedMouseX, adjustedMouseY, button);
            }
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double adjustedMouseY = mouseY - translationY;

        if (draggingOverlayScroll) {
            int scrollAreaStartY = 24 + 10;
            int scrollAreaHeight = MAX_OVERLAYS_PER_PAGE * 21 - Texture.SCROLL_BUTTON.height();

            int newOffset = Math.round(MathUtils.map(
                    (float) adjustedMouseY,
                    scrollAreaStartY,
                    scrollAreaStartY + scrollAreaHeight,
                    0,
                    getMaxOverlayScrollOffset()));

            newOffset = Math.max(0, Math.min(newOffset, getMaxOverlayScrollOffset()));

            scrollOverlays(newOffset);

            return true;
        } else if (draggingConfigScroll) {
            int scrollAreaStartY = 24 + 10;
            int scrollAreaHeight = CONFIGS_PER_PAGE * 43 - Texture.SCROLL_BUTTON.height();

            int newOffset = Math.round(MathUtils.map(
                    (float) adjustedMouseY,
                    scrollAreaStartY,
                    scrollAreaStartY + scrollAreaHeight,
                    0,
                    getMaxConfigScrollOffset()));

            newOffset = Math.max(0, Math.min(newOffset, getMaxConfigScrollOffset()));

            scrollConfigs(newOffset);

            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double adjustedMouseX = mouseX - translationX;
        double adjustedMouseY = mouseY - translationY;

        for (GuiEventListener listener : getWidgetsForIteration().toList()) {
            if (listener.isMouseOver(adjustedMouseX, adjustedMouseY)) {
                listener.mouseReleased(adjustedMouseX, adjustedMouseY, button);
            }
        }

        draggingOverlayScroll = false;
        draggingConfigScroll = false;

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double adjustedMouseX = mouseX - translationX;
        int scrollAmount = (int) (-deltaY * SCROLL_FACTOR);

        if (!renderPreview) {
            // When the mouse is to the left of the config area or no selected overlay, scroll overlays.
            // Otherwise scroll the configs
            if (adjustedMouseX < 145 || selectedOverlay == null) {
                int newOffset = Math.max(0, Math.min(overlayScrollOffset + scrollAmount, getMaxOverlayScrollOffset()));
                scrollOverlays(newOffset);
            } else if (selectedOverlay != null
                    && selectedOverlay.getVisibleConfigOptions().size() > CONFIGS_PER_PAGE) {
                int newOffset = Math.max(0, Math.min(configScrollOffset + scrollAmount, getMaxConfigScrollOffset()));
                scrollConfigs(newOffset);
            }
        }

        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return focusedTextInput != null && focusedTextInput.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // If rendering a preview and esc is pressed, then return to the selection menu.
            // Otherwise, close the screen
            if (renderPreview) {
                togglePreview(false);
            } else {
                this.onClose();
            }

            return true;
        }

        for (OverlayButton overlayButton : overlays) {
            if (overlayButton.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        return focusedTextInput != null && focusedTextInput.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public TextInputBoxWidget getFocusedTextInput() {
        return focusedTextInput;
    }

    @Override
    public void setFocusedTextInput(TextInputBoxWidget focusedTextInput) {
        this.focusedTextInput = focusedTextInput;
    }

    public boolean configOptionContains(Config<?> config) {
        return !searchWidget.getTextBoxInput().isEmpty()
                && StringUtils.containsIgnoreCase(config.getDisplayName(), searchWidget.getTextBoxInput());
    }

    public void populateOverlays() {
        for (AbstractWidget widget : overlays) {
            this.removeWidget(widget);
        }

        overlays = new ArrayList<>();

        // Get all overlays, sorted by parent feature a-z, then a-z for each overlay in that feature.
        // Filter to only include overlays matching search query.
        overlayList = Managers.Overlay.getOverlays().stream()
                .sorted(Overlay::compareTo)
                .filter(this::searchMatches)
                .toList();

        // If not in the "All" filter, then only show overlays that are built-in, or custom bars/info boxes
        if (filterType == FilterType.BUILT_IN) {
            overlayList = overlayList.stream()
                    .filter(overlay ->
                            !(overlay instanceof CustomBarOverlayBase) && !(overlay instanceof InfoBoxOverlay))
                    .toList();
        } else if (filterType == FilterType.CUSTOM) {
            overlayList = overlayList.stream()
                    .filter(overlay -> (overlay instanceof CustomBarOverlayBase) || (overlay instanceof InfoBoxOverlay))
                    .toList();
        }

        if (!overlayList.contains(selectedOverlay)) {
            // Don't deselect custom overlays as they will always fail the above check
            if (filterType == FilterType.BUILT_IN
                    || !(selectedOverlay instanceof CustomBarOverlayBase
                            || selectedOverlay instanceof InfoBoxOverlay)) {
                setSelectedOverlay(null);
            }
        }

        int yPos = 31;
        for (Overlay value : overlayList) {
            overlays.add(new OverlayButton(7, yPos, 120, 18, value, this));

            yPos += 21;
        }

        if (selectedOverlay != null) {
            Stream<Overlay> overlaysList = Managers.Feature.getFeatures().stream()
                    .map(Managers.Overlay::getFeatureOverlays)
                    .flatMap(Collection::stream);

            Overlay newSelected = overlaysList
                    .filter(overlay -> overlay.getJsonName().equals(selectedOverlay.getJsonName()))
                    .findFirst()
                    .orElse(null);

            setSelectedOverlay(newSelected);
        }

        scrollOverlays(overlayScrollOffset);
    }

    public void setSelectedOverlay(Overlay selectedOverlay) {
        if (this.selectedOverlay != null) {
            for (OverlayButton overlayButton : overlays) {
                if (overlayButton.getOverlay() == this.selectedOverlay) {
                    overlayButton.hideEditInput();
                    break;
                }
            }
        }

        this.selectedOverlay = selectedOverlay;

        configScrollOffset = 0;

        populateConfigs();
        addOptionButtons();
    }

    private void deleteOverlay() {
        int overlayId = -1;

        if (selectedOverlay instanceof InfoBoxOverlay infoBoxOverlay) {
            overlayId = infoBoxOverlay.getId();
        } else if (selectedOverlay instanceof CustomBarOverlayBase customBarOverlay) {
            overlayId = customBarOverlay.getId();
        }

        if (overlayId == -1) return;

        // Get the group holder
        OverlayGroupHolder overlayGroupHolder = getGroupHolder();

        if (overlayGroupHolder == null) {
            WynntilsMod.error("Failed to delete, overlay group not found for overlay " + selectedOverlay.getJsonName());
            return;
        }

        // Delete the overlay
        Managers.Overlay.removeIdFromOverlayGroup(overlayGroupHolder, overlayId);

        // Reload config
        Managers.Config.loadConfigOptions(true, false);
        Managers.Config.saveConfig();
        Managers.Config.reloadConfiguration();

        selectedOverlay = null;
        populateConfigs();

        overlayScrollOffset = 0;
        populateOverlays();
        addOptionButtons();
    }

    private OverlayGroupHolder getGroupHolder() {
        // Get the parent feature of the overlay
        Feature feature = selectedOverlay instanceof InfoBoxOverlay
                ? Managers.Feature.getFeatureInstance(InfoBoxFeature.class)
                : Managers.Feature.getFeatureInstance(CustomBarsOverlayFeature.class);

        // Loop through holders, if holder contains this overlay then that is the one
        for (OverlayGroupHolder group : Managers.Overlay.getFeatureOverlayGroups(feature)) {
            if (group.getOverlays().contains(selectedOverlay)) {
                return group;
            }
        }

        return null;
    }

    public Overlay getSelectedOverlay() {
        return selectedOverlay;
    }

    public boolean renderingPreview() {
        return renderPreview;
    }

    public boolean shouldShowOverlays() {
        return showOverlays;
    }

    public float getTranslationX() {
        return translationX;
    }

    public float getTranslationY() {
        return translationY;
    }

    public int getConfigMaskTopY() {
        return CONFIG_MASK_TOP_Y;
    }

    public int getConfigMaskBottomY() {
        return CONFIG_MASK_BOTTOM_Y;
    }

    private void populateConfigs() {
        configs = new ArrayList<>();

        if (selectedOverlay == null) return;

        // Get all config options for the selected overlay
        List<Config<?>> configsOptions = selectedOverlay.getVisibleConfigOptions().stream()
                .sorted(Comparator.comparing(config -> !Objects.equals(config.getFieldName(), "userEnabled")))
                .toList();

        int renderY = 25;

        for (Config<?> config : configsOptions) {
            configs.add(new ConfigTile(148, renderY, 188, 41, this, config));

            renderY += 43;
        }

        scrollConfigs(configScrollOffset);
    }

    private void togglePreview(boolean enabled) {
        // If no overlay and preview mode trying to be enabled, return
        if (selectedOverlay == null && enabled) return;

        renderPreview = enabled;

        // Toggle visibility of buttons
        for (OverlayOptionsButton optionsButton : optionButtons) {
            optionsButton.visible = !enabled;
        }

        exitPreviewButton.visible = enabled;

        // Either clear or repopulate the overlay/config lists
        if (enabled) {
            overlays = new ArrayList<>();
            configs = new ArrayList<>();
        } else {
            populateOverlays();
            populateConfigs();
        }
    }

    private void scrollOverlays(int newOffset) {
        overlayScrollOffset = newOffset;

        for (OverlayButton overlay : overlays) {
            int newY = 31 + (overlays.indexOf(overlay) * 21) - overlayScrollOffset;

            overlay.setY(newY);
            overlay.visible = newY >= (31 - 21) && newY <= (31 + (MAX_OVERLAYS_PER_PAGE + 1) * 21);
        }
    }

    private int getMaxOverlayScrollOffset() {
        return (overlayList.size() - MAX_OVERLAYS_PER_PAGE) * 21;
    }

    private void scrollConfigs(int newOffset) {
        configScrollOffset = newOffset;

        for (WynntilsButton config : configs) {
            int newY = 25 + (configs.indexOf(config) * 43) - configScrollOffset;

            config.setY(newY);
            config.visible = newY >= (25 - 43) && newY <= (25 + (CONFIGS_PER_PAGE + 1) * 43);
        }
    }

    private int getMaxConfigScrollOffset() {
        return (selectedOverlay.getVisibleConfigOptions().size() - CONFIGS_PER_PAGE) * 43;
    }

    private void addInfoBox() {
        // Get the info box feature
        Feature infoBoxFeature = Managers.Feature.getFeatureInstance(InfoBoxFeature.class);

        // Loop through group holders
        for (OverlayGroupHolder group : Managers.Overlay.getFeatureOverlayGroups(infoBoxFeature)) {
            // If the parent feature of the group is the info box feature
            if (group.getParent() == infoBoxFeature) {
                // Get the ID of the new info box
                int id = Managers.Overlay.extendOverlayGroup(group);

                // Reload config
                Managers.Config.loadConfigOptions(true, false);
                Managers.Config.saveConfig();
                Managers.Config.reloadConfiguration();

                // Repopulate overlay list
                populateOverlays();

                // Set the new info box as the selected overlay
                setSelectedOverlay(group.getOverlays().getLast());

                McUtils.sendMessageToClient(Component.translatable(
                                "screens.wynntils.overlaySelection.createdOverlay",
                                group.getOverlayClass().getSimpleName(),
                                group.getFieldName(),
                                id)
                        .withStyle(ChatFormatting.GREEN));
                return;
            }
        }
    }

    private void setSelectedFilter(FilterType newFilter) {
        selectedFilterButton.setIsSelected(false);

        // Set which buttons is selected to change its texture
        switch (newFilter) {
            case BUILT_IN -> {
                builtInButton.setIsSelected(true);
                selectedFilterButton = builtInButton;
            }
            case CUSTOM -> {
                customButton.setIsSelected(true);
                selectedFilterButton = customButton;
            }
            default -> {
                allButton.setIsSelected(true);
                selectedFilterButton = allButton;
            }
        }

        // Update the filter type and repopulate the overlays list
        filterType = newFilter;
        overlayScrollOffset = 0;

        populateOverlays();
    }

    private boolean searchMatches(Translatable translatable) {
        // For info boxes and custom bars, we want to search for their custom name if given
        // if there is no match, then check the translated name
        if (translatable instanceof CustomNameProperty customNameProperty) {
            if (StringUtils.partialMatch(customNameProperty.getCustomName().get(), searchWidget.getTextBoxInput())) {
                return true;
            }
        }

        return StringUtils.partialMatch(translatable.getTranslatedName(), searchWidget.getTextBoxInput());
    }

    private Stream<GuiEventListener> getWidgetsForIteration() {
        return Stream.concat(
                children.stream(),
                Stream.concat(optionButtons.stream(), Stream.concat(overlays.stream(), configs.stream())));
    }

    private void addOptionButtons() {
        optionButtons = new ArrayList<>();

        // region Add Overlay buttons
        optionButtons.add(new OverlayOptionsButton(
                (Texture.OVERLAY_SELECTION_GUI.width() / 2) - 130,
                -(Texture.PAPER_BUTTON_TOP.height() / 2) + 4,
                Texture.PAPER_BUTTON_TOP.width(),
                Texture.PAPER_BUTTON_TOP.height() / 2,
                StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.addInfoBox")),
                (button) -> addInfoBox(),
                List.of(Component.translatable("screens.wynntils.overlaySelection.addInfoBoxTooltip")),
                Texture.PAPER_BUTTON_TOP,
                false));

        optionButtons.add(new OverlayOptionsButton(
                (Texture.OVERLAY_SELECTION_GUI.width() / 2) + 10,
                -(Texture.PAPER_BUTTON_TOP.height() / 2) + 4,
                Texture.PAPER_BUTTON_TOP.width(),
                Texture.PAPER_BUTTON_TOP.height() / 2,
                StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.addCustomBar")),
                (button) -> McUtils.mc().setScreen(CustomBarSelectionScreen.create(this)),
                List.of(Component.translatable("screens.wynntils.overlaySelection.addCustomBarTooltip")),
                Texture.PAPER_BUTTON_TOP,
                false));
        // endregion

        // region Filter buttons
        allButton = new OverlayOptionsButton(
                -(Texture.PAPER_BUTTON_LEFT.width()) + 4,
                8,
                Texture.PAPER_BUTTON_LEFT.width(),
                Texture.PAPER_BUTTON_LEFT.height() / 2,
                StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.all")),
                (button) -> setSelectedFilter(FilterType.ALL),
                List.of(Component.translatable("screens.wynntils.overlaySelection.allTooltip")),
                Texture.PAPER_BUTTON_LEFT,
                filterType == FilterType.ALL);

        optionButtons.add(allButton);

        builtInButton = new OverlayOptionsButton(
                -(Texture.PAPER_BUTTON_LEFT.width()) + 4,
                12 + Texture.PAPER_BUTTON_LEFT.height() / 2,
                Texture.PAPER_BUTTON_LEFT.width(),
                Texture.PAPER_BUTTON_LEFT.height() / 2,
                StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.builtIn")),
                (button) -> setSelectedFilter(FilterType.BUILT_IN),
                List.of(Component.translatable("screens.wynntils.overlaySelection.builtInTooltip")),
                Texture.PAPER_BUTTON_LEFT,
                filterType == FilterType.BUILT_IN);

        optionButtons.add(builtInButton);

        customButton = new OverlayOptionsButton(
                -(Texture.PAPER_BUTTON_LEFT.width()) + 4,
                16 + (Texture.PAPER_BUTTON_LEFT.height() / 2) * 2,
                Texture.PAPER_BUTTON_LEFT.width(),
                Texture.PAPER_BUTTON_LEFT.height() / 2,
                StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.custom")),
                (button) -> setSelectedFilter(FilterType.CUSTOM),
                List.of(Component.translatable("screens.wynntils.overlaySelection.customTooltip")),
                Texture.PAPER_BUTTON_LEFT,
                filterType == FilterType.CUSTOM);

        optionButtons.add(customButton);

        switch (filterType) {
            case BUILT_IN -> selectedFilterButton = builtInButton;
            case CUSTOM -> selectedFilterButton = customButton;
            default -> selectedFilterButton = allButton;
        }
        // endregion

        // region Delete overlay button
        OverlayOptionsButton deleteButton = new OverlayOptionsButton(
                -(Texture.PAPER_BUTTON_LEFT.width()) + 4,
                28 + (Texture.PAPER_BUTTON_LEFT.height() / 2) * 5,
                Texture.PAPER_BUTTON_LEFT.width(),
                Texture.PAPER_BUTTON_LEFT.height() / 2,
                StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.delete")),
                (button) -> deleteOverlay(),
                List.of(Component.translatable("screens.wynntils.overlaySelection.deleteTooltip")),
                Texture.PAPER_BUTTON_LEFT,
                false);

        optionButtons.add(deleteButton);
        deleteButton.visible = selectedOverlay != null
                && (selectedOverlay instanceof InfoBoxOverlay || selectedOverlay instanceof CustomBarOverlayBase);
        // endregion

        // region Edit buttons
        optionButtons.add(new OverlayOptionsButton(
                (Texture.OVERLAY_SELECTION_GUI.width() / 2) - 100,
                Texture.OVERLAY_SELECTION_GUI.height() - 4,
                Texture.PAPER_BUTTON_BOTTOM.width(),
                Texture.PAPER_BUTTON_BOTTOM.height() / 2,
                StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.freeMove")),
                (button) -> {
                    Managers.Config.saveConfig();
                    McUtils.mc().setScreen(OverlayManagementScreen.create(this));
                },
                List.of(Component.translatable("screens.wynntils.overlaySelection.freeMoveTooltip")),
                Texture.PAPER_BUTTON_BOTTOM,
                false));

        optionButtons.add(new OverlayOptionsButton(
                (Texture.OVERLAY_SELECTION_GUI.width() / 2) - 30,
                Texture.OVERLAY_SELECTION_GUI.height() - 4,
                Texture.PAPER_BUTTON_BOTTOM.width(),
                Texture.PAPER_BUTTON_BOTTOM.height() / 2,
                StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.close")),
                (button) -> onClose(),
                List.of(Component.translatable("screens.wynntils.overlaySelection.closeTooltip")),
                Texture.PAPER_BUTTON_BOTTOM,
                false));

        optionButtons.add(new OverlayOptionsButton(
                (Texture.OVERLAY_SELECTION_GUI.width() / 2) + 40,
                Texture.OVERLAY_SELECTION_GUI.height() - 4,
                Texture.PAPER_BUTTON_BOTTOM.width(),
                Texture.PAPER_BUTTON_BOTTOM.height() / 2,
                StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.save")),
                (button) -> {
                    Managers.Config.saveConfig();
                    onClose();
                },
                List.of(Component.translatable("screens.wynntils.overlaySelection.saveTooltip")),
                Texture.PAPER_BUTTON_BOTTOM,
                false));

        // Add the two buttons that should only be visible when an overlay is selected
        if (selectedOverlay != null) {
            optionButtons.add(new OverlayOptionsButton(
                    (Texture.OVERLAY_SELECTION_GUI.width() / 2) - 170,
                    Texture.OVERLAY_SELECTION_GUI.height() - 4,
                    Texture.PAPER_BUTTON_BOTTOM.width(),
                    Texture.PAPER_BUTTON_BOTTOM.height() / 2,
                    StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.preview")),
                    (button) -> togglePreview(true),
                    List.of(Component.translatable("screens.wynntils.overlaySelection.previewTooltip")),
                    Texture.PAPER_BUTTON_BOTTOM,
                    false));

            optionButtons.add(new OverlayOptionsButton(
                    (Texture.OVERLAY_SELECTION_GUI.width() / 2) + 110,
                    Texture.OVERLAY_SELECTION_GUI.height() - 4,
                    Texture.PAPER_BUTTON_BOTTOM.width(),
                    Texture.PAPER_BUTTON_BOTTOM.height() / 2,
                    StyledText.fromComponent(Component.translatable("screens.wynntils.overlaySelection.edit")),
                    (button) -> {
                        if (selectedOverlay != null) {
                            Managers.Config.saveConfig();
                            McUtils.mc().setScreen(OverlayManagementScreen.create(this, selectedOverlay));
                        }
                    },
                    List.of(Component.translatable("screens.wynntils.overlaySelection.editTooltip")),
                    Texture.PAPER_BUTTON_BOTTOM,
                    false));
        }
        // endregion
    }

    private void renderWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (OverlayOptionsButton optionsButton : optionButtons) {
            optionsButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        RenderUtils.createRectMask(guiGraphics.pose(), 6, 30, 122, MAX_OVERLAYS_PER_PAGE * 21);

        for (AbstractWidget widget : overlays) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        RenderUtils.clearMask();

        RenderUtils.createRectMask(guiGraphics.pose(), 148, 25, 188, CONFIGS_PER_PAGE * 43);

        for (AbstractWidget widget : configs) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        RenderUtils.clearMask();
    }

    private void renderOverlayScroll(PoseStack poseStack) {
        overlayScrollY = 24
                + MathUtils.map(
                        overlayScrollOffset,
                        0,
                        getMaxOverlayScrollOffset(),
                        0,
                        177 - Texture.CONFIG_BOOK_SCROLL_BUTTON.height());

        RenderUtils.drawTexturedRect(poseStack, Texture.SCROLL_BUTTON, 132, overlayScrollY);
    }

    private void renderConfigScroll(PoseStack poseStack) {
        configScrollY = 24
                + MathUtils.map(
                        configScrollOffset,
                        0,
                        getMaxConfigScrollOffset(),
                        0,
                        177 - Texture.CONFIG_BOOK_SCROLL_BUTTON.height());

        RenderUtils.drawTexturedRect(poseStack, Texture.SCROLL_BUTTON, 344, configScrollY);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int adjustedMouseX = mouseX - (int) translationX;
        int adjustedMouseY = mouseY - (int) translationY;

        // The option buttons have a slight bit rendered underneath the background, we don't want to render the tooltip
        // when hovering that bit.
        if (MathUtils.isInside(
                adjustedMouseX,
                adjustedMouseY,
                0,
                Texture.OVERLAY_SELECTION_GUI.width(),
                0,
                Texture.OVERLAY_SELECTION_GUI.height())) return;

        for (GuiEventListener child : optionButtons) {
            if (child instanceof TooltipProvider tooltipProvider && child.isMouseOver(adjustedMouseX, adjustedMouseY)) {
                guiGraphics.renderComponentTooltip(
                        FontRenderer.getInstance().getFont(),
                        tooltipProvider.getTooltipLines(),
                        adjustedMouseX,
                        adjustedMouseY);
                break;
            }
        }
    }

    private enum FilterType {
        ALL,
        BUILT_IN,
        CUSTOM
    }
}
