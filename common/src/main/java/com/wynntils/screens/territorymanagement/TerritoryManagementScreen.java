/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.territorymanagement;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.ui.CustomTerritoryManagementScreenFeature;
import com.wynntils.handlers.wrappedscreen.WrappedScreen;
import com.wynntils.handlers.wrappedscreen.type.WrappedScreenInfo;
import com.wynntils.models.items.items.gui.TerritoryItem;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.widgets.BasicTexturedButton;
import com.wynntils.screens.base.widgets.ItemFilterUIButton;
import com.wynntils.screens.base.widgets.ItemSearchWidget;
import com.wynntils.screens.territorymanagement.widgets.GuildOverallProductionWidget;
import com.wynntils.screens.territorymanagement.widgets.TerritoryApplyLoadoutButton;
import com.wynntils.screens.territorymanagement.widgets.TerritoryHighlightLegendWidget;
import com.wynntils.screens.territorymanagement.widgets.TerritoryWidget;
import com.wynntils.screens.territorymanagement.widgets.quickfilters.TerritoryBonusesQuickFilterWidget;
import com.wynntils.screens.territorymanagement.widgets.quickfilters.TerritoryDefenseQuickFilterWidget;
import com.wynntils.screens.territorymanagement.widgets.quickfilters.TerritoryProducesQuickFilterWidget;
import com.wynntils.screens.territorymanagement.widgets.quickfilters.TerritoryQuickFilterWidget;
import com.wynntils.screens.territorymanagement.widgets.quicksorts.TerritoryDefenseQuickSortWidget;
import com.wynntils.screens.territorymanagement.widgets.quicksorts.TerritoryOverallProductionQuickSortWidget;
import com.wynntils.screens.territorymanagement.widgets.quicksorts.TerritoryQuickSortWidget;
import com.wynntils.screens.territorymanagement.widgets.quicksorts.TerritoryTreasuryQuickSortWidget;
import com.wynntils.services.itemfilter.type.ItemProviderType;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class TerritoryManagementScreen extends WynntilsScreen implements WrappedScreen {
    // Constants
    // The render area is the area where the territories are rendered
    private static final Pair<Integer, Integer> RENDER_AREA_POSITION = new Pair<>(9, 16);
    private static final Pair<Integer, Integer> RENDER_AREA_SIZE = new Pair<>(221, 110);
    private static final int TERRITORY_SIZE = 20;
    private static final int TERRITORIES_PER_ROW = RENDER_AREA_SIZE.a() / TERRITORY_SIZE;
    private static final int BACK_BUTTON_SLOT = 18;
    private static final int APPLY_BUTTON_SLOT = 0;
    private static final int LOADOUT_BUTTON_SLOT = 36;
    private static final int QUICK_FILTER_WIDTH = 150;

    // Territory items
    private List<Pair<ItemStack, TerritoryItem>> territoryItems = new ArrayList<>();

    // Widgets
    private final List<AbstractWidget> renderAreaWidgets = new ArrayList<>();
    private final List<TerritoryQuickFilterWidget> quickFilters = new ArrayList<>();
    private final List<TerritoryQuickSortWidget> quickSorts = new ArrayList<>();
    private ItemSearchWidget itemSearchWidget;

    // Scroll fields
    // Scroll offset is measured in pixels
    private float scrollOffset = 0;
    private boolean draggingScroll = false;

    // WrappedScreen fields
    private final WrappedScreenInfo wrappedScreenInfo;
    private final TerritoryManagementHolder holder;

    public TerritoryManagementScreen(WrappedScreenInfo wrappedScreenInfo, TerritoryManagementHolder holder) {
        super(Component.literal("Territory Management"));
        this.wrappedScreenInfo = wrappedScreenInfo;
        this.holder = holder;
    }

    @Override
    public WrappedScreenInfo getWrappedScreenInfo() {
        return wrappedScreenInfo;
    }

    @Override
    protected void doInit() {
        ItemSearchWidget oldWidget = itemSearchWidget;

        itemSearchWidget = new ItemSearchWidget(
                getRenderX(),
                getRenderY() - 20,
                Texture.TERRITORY_MANAGEMENT_BACKGROUND.width() - 25,
                20,
                List.of(ItemProviderType.TERRITORY),
                true,
                (q) -> populateRenderAreaWidgets(),
                this);
        this.addRenderableWidget(itemSearchWidget);
        itemSearchWidget.setTextBoxInput(
                oldWidget == null ? "" : oldWidget.getSearchQuery().queryString());

        this.addRenderableWidget(new ItemFilterUIButton(
                getRenderX() + Texture.TERRITORY_MANAGEMENT_BACKGROUND.width() - 20,
                getRenderY() - 20,
                itemSearchWidget,
                this,
                true,
                List.of(ItemProviderType.TERRITORY)));

        // Territory Highlighter Legend
        this.addRenderableWidget(new TerritoryHighlightLegendWidget(
                getRenderX(),
                getRenderY() + Texture.TERRITORY_MANAGEMENT_BACKGROUND.height() + 5,
                Texture.TERRITORY_MANAGEMENT_BACKGROUND.width(),
                110,
                holder));

        this.addRenderableWidget(
                new GuildOverallProductionWidget(getRenderX() - 190, getRenderY() + 10, 200, 150, holder));

        // Back button in the sidebar
        this.addRenderableWidget(new BasicTexturedButton(
                getRenderX() - 20,
                getRenderY() + 5,
                Texture.ARROW_LEFT_ICON.width(),
                Texture.ARROW_LEFT_ICON.height(),
                Texture.ARROW_LEFT_ICON,
                (button) -> {
                    ContainerUtils.clickOnSlot(
                            BACK_BUTTON_SLOT,
                            wrappedScreenInfo.containerId(),
                            button,
                            wrappedScreenInfo.containerMenu().getItems());
                },
                List.of(Component.translatable("gui.back").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD)),
                false));

        // Territory production tooltip disable button
        this.addRenderableWidget(new BasicTexturedButton(
                getRenderX() - 20,
                getRenderY() + 75,
                Texture.DEFENSE_FILTER_ICON.width(),
                Texture.DEFENSE_FILTER_ICON.height(),
                Texture.DEFENSE_FILTER_ICON,
                (button) -> {
                    Storage<Boolean> screenTerritoryProductionTooltip = Managers.Feature.getFeatureInstance(
                                    CustomTerritoryManagementScreenFeature.class)
                            .screenTerritoryProductionTooltip;
                    screenTerritoryProductionTooltip.store(!screenTerritoryProductionTooltip.get());
                },
                List.of(
                        Component.translatable(
                                        "feature.wynntils.customTerritoryManagementScreen.disableTerritoryProductionTooltip")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD),
                        Component.translatable(
                                        "feature.wynntils.customTerritoryManagementScreen.territoryProductionHelper")
                                .withStyle(ChatFormatting.GRAY)),
                false));

        // Highlight legend disable button
        this.addRenderableWidget(new BasicTexturedButton(
                getRenderX() - 17,
                getRenderY() + 95,
                Texture.HELP_ICON.width(),
                Texture.HELP_ICON.height(),
                Texture.HELP_ICON,
                (button) -> {
                    Storage<Boolean> screenHighlightLegend = Managers.Feature.getFeatureInstance(
                                    CustomTerritoryManagementScreenFeature.class)
                            .screenHighlightLegend;
                    screenHighlightLegend.store(!screenHighlightLegend.get());
                },
                List.of(Component.translatable(
                                "feature.wynntils.customTerritoryManagementScreen.disableHighlightLegend")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD)),
                false));

        if (!holder.isSelectionMode()) {
            // Loadout button in the sidebar
            this.addRenderableWidget(new BasicTexturedButton(
                    getRenderX() - 20,
                    getRenderY() + 115,
                    Texture.TERRITORY_LOADOUT.width(),
                    Texture.TERRITORY_LOADOUT.height(),
                    Texture.TERRITORY_LOADOUT,
                    (button) -> ContainerUtils.clickOnSlot(
                            LOADOUT_BUTTON_SLOT,
                            wrappedScreenInfo.containerId(),
                            button,
                            wrappedScreenInfo.containerMenu().getItems()),
                    List.of(
                            Component.translatable("feature.wynntils.customTerritoryManagementScreen.loadouts")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.empty(),
                            Component.translatable(
                                            "feature.wynntils.customTerritoryManagementScreen.loadouts.description")
                                    .withStyle(ChatFormatting.GRAY),
                            Component.empty(),
                            Component.translatable(
                                            "feature.wynntils.customTerritoryManagementScreen.loadouts.clickToOpen")
                                    .withStyle(ChatFormatting.GREEN))));
        } else {
            // Apply selection button in the sidebar
            this.addRenderableWidget(new TerritoryApplyLoadoutButton(
                    getRenderX() - 20,
                    getRenderY() + 115,
                    Texture.CHECKMARK_YELLOW.width(),
                    Texture.CHECKMARK_YELLOW.height(),
                    holder::getApplyButtonTexture,
                    (button) -> ContainerUtils.clickOnSlot(
                            APPLY_BUTTON_SLOT,
                            wrappedScreenInfo.containerId(),
                            button,
                            wrappedScreenInfo.containerMenu().getItems()),
                    List.of(
                            Component.translatable("feature.wynntils.customTerritoryManagementScreen.applySelection")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.empty(),
                            Component.translatable(
                                            "feature.wynntils.customTerritoryManagementScreen.applySelection.description")
                                    .withStyle(ChatFormatting.GRAY),
                            Component.empty(),
                            Component.translatable(
                                            "feature.wynntils.customTerritoryManagementScreen.applySelection.clickToConfirm")
                                    .withStyle(ChatFormatting.GREEN))));
        }

        // Quick filters
        quickFilters.clear();

        quickFilters.add(new TerritoryBonusesQuickFilterWidget(
                getRenderX() + Texture.TERRITORY_MANAGEMENT_BACKGROUND.width() + 5,
                getRenderY() + 20,
                QUICK_FILTER_WIDTH,
                10,
                this));

        quickFilters.add(new TerritoryDefenseQuickFilterWidget(
                getRenderX() + Texture.TERRITORY_MANAGEMENT_BACKGROUND.width() + 5,
                getRenderY() + 35,
                QUICK_FILTER_WIDTH,
                10,
                this));

        quickFilters.add(new TerritoryProducesQuickFilterWidget(
                getRenderX() + Texture.TERRITORY_MANAGEMENT_BACKGROUND.width() + 5,
                getRenderY() + 50,
                QUICK_FILTER_WIDTH,
                10,
                this));

        // Quick sorts
        quickSorts.clear();

        quickSorts.add(new TerritoryTreasuryQuickSortWidget(
                getRenderX() + Texture.TERRITORY_MANAGEMENT_BACKGROUND.width() + 5,
                getRenderY() + 85,
                QUICK_FILTER_WIDTH,
                10,
                this));

        quickSorts.add(new TerritoryDefenseQuickSortWidget(
                getRenderX() + Texture.TERRITORY_MANAGEMENT_BACKGROUND.width() + 5,
                getRenderY() + 100,
                QUICK_FILTER_WIDTH,
                10,
                this));

        quickSorts.add(new TerritoryOverallProductionQuickSortWidget(
                getRenderX() + Texture.TERRITORY_MANAGEMENT_BACKGROUND.width() + 5,
                getRenderY() + 115,
                QUICK_FILTER_WIDTH,
                10,
                this));

        updateTerritoryItems();
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Screen background
        RenderUtils.drawTexturedRect(
                guiGraphics.pose(), Texture.TERRITORY_MANAGEMENT_BACKGROUND, getRenderX(), getRenderY());
        RenderUtils.drawTexturedRect(guiGraphics.pose(), Texture.TERRITORY_SIDEBAR, getRenderX() - 22, getRenderY());

        // Render title
        FontRenderer.getInstance()
                .renderText(
                        guiGraphics.pose(),
                        StyledText.fromComponent(wrappedScreenInfo.screen().getTitle()),
                        getRenderX() + 8,
                        getRenderY() + 9,
                        CommonColors.TITLE_GRAY,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NONE);

        // Render the widgets
        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);

        // Render scroll button
        renderScrollButton(guiGraphics);

        // Render quick filters
        renderQuickFiltersAndSorts(guiGraphics, mouseX, mouseY, partialTick);

        // Render widget tooltip
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render territory widgets in the render area
        RenderUtils.enableScissor(
                getRenderX() + RENDER_AREA_POSITION.a(),
                getRenderY() + RENDER_AREA_POSITION.b(),
                RENDER_AREA_SIZE.a(),
                RENDER_AREA_SIZE.b());

        //  Render area widgets need to handle the scroll offset
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -scrollOffset, 0);

        // Render the render main area widgets
        for (AbstractWidget widget : renderAreaWidgets) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        guiGraphics.pose().popPose();

        RenderUtils.disableScissor();

        // Render normal widgets
        renderables.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));
    }

    private void renderScrollButton(GuiGraphics guiGraphics) {
        float renderY = MathUtils.map(
                scrollOffset,
                0,
                getMaxScrollOffset(),
                getRenderY() + RENDER_AREA_POSITION.b(),
                getRenderY() + RENDER_AREA_POSITION.b() + RENDER_AREA_SIZE.b());
        RenderUtils.drawTexturedRect(
                guiGraphics.pose(),
                Texture.SCROLLBAR_BUTTON,
                getRenderX()
                        + RENDER_AREA_POSITION.a()
                        + RENDER_AREA_SIZE.a()
                        + 9f
                        - Texture.SCROLL_BUTTON.width() / 2f,
                renderY - Texture.SCROLLBAR_BUTTON.height() / 2f);
    }

    private void renderQuickFiltersAndSorts(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.pose().pushPose();

        guiGraphics
                .pose()
                .translate(getRenderX() + Texture.TERRITORY_MANAGEMENT_BACKGROUND.width() + 5, getRenderY(), 0);

        RenderUtils.drawRect(
                guiGraphics.pose(),
                CommonColors.BLACK.withAlpha(80),
                0,
                0,
                0,
                QUICK_FILTER_WIDTH,
                Texture.TERRITORY_MANAGEMENT_BACKGROUND.height());

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        guiGraphics.pose(),
                        StyledText.fromComponent(
                                Component.translatable("feature.wynntils.customTerritoryManagementScreen.filters")),
                        0,
                        QUICK_FILTER_WIDTH,
                        5,
                        0,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        TextShadow.OUTLINE);

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        guiGraphics.pose(),
                        StyledText.fromComponent(
                                Component.translatable("feature.wynntils.customTerritoryManagementScreen.sorts")),
                        0,
                        QUICK_FILTER_WIDTH,
                        70,
                        0,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        TextShadow.OUTLINE);

        guiGraphics.pose().popPose();

        for (TerritoryQuickFilterWidget quickFilter : quickFilters) {
            quickFilter.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        for (TerritoryQuickSortWidget quickSort : quickSorts) {
            quickSort.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        AbstractWidget hoveredWidget = getHoveredWidget(mouseX, mouseY);
        if (hoveredWidget == null) return;
        if (!(hoveredWidget instanceof TooltipProvider tooltipProvider)) return;

        List<Component> tooltipLines = tooltipProvider.getTooltipLines();
        if (tooltipLines.isEmpty()) return;

        guiGraphics.renderComponentTooltip(FontRenderer.getInstance().getFont(), tooltipLines, mouseX, mouseY);
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        // Render area widgets need to handle the scroll offset
        // Check if mouse is over the render area
        if (mouseX >= getRenderX() + RENDER_AREA_POSITION.a()
                && mouseX <= getRenderX() + RENDER_AREA_POSITION.a() + RENDER_AREA_SIZE.a()
                && mouseY >= getRenderY() + RENDER_AREA_POSITION.b()
                && mouseY <= getRenderY() + RENDER_AREA_POSITION.b() + RENDER_AREA_SIZE.b()) {
            for (AbstractWidget widget : renderAreaWidgets) {
                if (widget.isMouseOver(mouseX, mouseY + scrollOffset)) {
                    return widget.mouseClicked(mouseX, mouseY + scrollOffset, button);
                }
            }
        }

        for (TerritoryQuickFilterWidget quickFilter : quickFilters) {
            if (quickFilter.isMouseOver(mouseX, mouseY)) {
                return quickFilter.mouseClicked(mouseX, mouseY, button);
            }
        }

        for (TerritoryQuickSortWidget quickSort : quickSorts) {
            if (quickSort.isMouseOver(mouseX, mouseY)) {
                return quickSort.mouseClicked(mouseX, mouseY, button);
            }
        }

        // Check if the scroll button was clicked
        float scrollX = getRenderX()
                + RENDER_AREA_POSITION.a()
                + RENDER_AREA_SIZE.a()
                + 9f
                - Texture.SCROLL_BUTTON.width() / 2f;
        float scrollY = MathUtils.map(
                scrollOffset,
                0,
                getMaxScrollOffset(),
                getRenderY() + RENDER_AREA_POSITION.b(),
                getRenderY() + RENDER_AREA_POSITION.b() + RENDER_AREA_SIZE.b());
        if (mouseX >= scrollX
                && mouseX <= scrollX + Texture.SCROLL_BUTTON.width()
                && mouseY >= scrollY - Texture.SCROLL_BUTTON.height() / 2f
                && mouseY <= scrollY + Texture.SCROLL_BUTTON.height() / 2f) {
            draggingScroll = true;
            return true;
        }

        return super.doMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Scroll the render area
        setScrollOffset((float) (scrollOffset - Math.signum(scrollY) * 10f));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroll) {
            // Calculate the new scroll offset
            float newScrollOffset = MathUtils.map(
                    (float) mouseY,
                    getRenderY() + RENDER_AREA_POSITION.b(),
                    getRenderY() + RENDER_AREA_POSITION.b() + RENDER_AREA_SIZE.b(),
                    0,
                    getMaxScrollOffset());
            setScrollOffset(newScrollOffset);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public void updateTerritoryItems() {
        territoryItems = holder.territoryItems().stream().toList();

        populateRenderAreaWidgets();
        scrollOffset = Math.min(getMaxScrollOffset(), scrollOffset);
    }

    public void updateSearchFromQuickFilters() {
        itemSearchWidget.setTextBoxInput(Stream.concat(
                        quickFilters.stream().map(TerritoryQuickFilterWidget::getItemSearchQuery),
                        quickSorts.stream().map(TerritoryQuickSortWidget::getItemSearchQuery))
                .collect(Collectors.joining(" "))
                .trim()
                .replaceAll("\\s+", " "));
    }

    private void populateRenderAreaWidgets() {
        List<Pair<ItemStack, TerritoryItem>> filteredItems = Services.ItemFilter.filterAndSort(
                        itemSearchWidget.getSearchQuery(),
                        territoryItems.stream().map(Pair::a).toList())
                .stream()
                .map(item -> Pair.of(
                        item, Models.Item.asWynnItem(item, TerritoryItem.class).orElseThrow()))
                .toList();

        renderAreaWidgets.clear();

        int x = getRenderX() + RENDER_AREA_POSITION.a();
        int y = getRenderY() + RENDER_AREA_POSITION.b();

        // Populate the render area with territory widgets
        // (even if they are out of bounds, they will be clipped,
        // but we need to render them to be able to scroll)
        int xOffset = (RENDER_AREA_SIZE.a() - TERRITORIES_PER_ROW * TERRITORY_SIZE) / 2;
        int yOffset = 0;
        for (Pair<ItemStack, TerritoryItem> territoryItemPair : filteredItems) {
            renderAreaWidgets.add(new TerritoryWidget(
                    x + xOffset,
                    y + yOffset,
                    TERRITORY_SIZE,
                    TERRITORY_SIZE,
                    holder,
                    holder.getTerritoryColor(territoryItemPair.b()),
                    territoryItemPair.a(),
                    territoryItemPair.b()));

            xOffset += TERRITORY_SIZE;
            if (xOffset >= TERRITORY_SIZE * TERRITORIES_PER_ROW) {
                xOffset = (RENDER_AREA_SIZE.a() - TERRITORIES_PER_ROW * TERRITORY_SIZE) / 2;
                yOffset += TERRITORY_SIZE;
            }
        }
    }

    private AbstractWidget getHoveredWidget(int mouseX, int mouseY) {
        // For render area widgets, take the scroll offset into account
        for (AbstractWidget widget : renderAreaWidgets) {
            if (widget.isMouseOver(mouseX, mouseY + scrollOffset)
                    && mouseY >= getRenderY() + RENDER_AREA_POSITION.b()
                    && mouseY <= getRenderY() + RENDER_AREA_POSITION.b() + RENDER_AREA_SIZE.b()) {
                return widget;
            }
        }

        for (GuiEventListener child : children) {
            if (child instanceof TooltipProvider tooltipProvider) {
                AbstractWidget widget = (AbstractWidget) child;
                if (widget.isMouseOver(mouseX, mouseY)) {
                    return widget;
                }
            }
        }

        return null;
    }

    private void setScrollOffset(float scrollOffset) {
        this.scrollOffset = scrollOffset;
        this.scrollOffset = Math.max(0, this.scrollOffset);
        this.scrollOffset = Math.min(getMaxScrollOffset(), this.scrollOffset);
    }

    private int getMaxScrollOffset() {
        int totalHeight = TERRITORY_SIZE * ((renderAreaWidgets.size() + TERRITORIES_PER_ROW - 1) / TERRITORIES_PER_ROW);
        return Math.max(0, totalHeight - RENDER_AREA_SIZE.b());
    }

    private int getRenderX() {
        return (this.width - Texture.TERRITORY_MANAGEMENT_BACKGROUND.width()) / 2;
    }

    private int getRenderY() {
        return (this.height - Texture.TERRITORY_MANAGEMENT_BACKGROUND.height()) / 2;
    }
}
