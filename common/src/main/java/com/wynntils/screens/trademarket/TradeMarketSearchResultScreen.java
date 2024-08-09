/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.trademarket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.handlers.wrappedscreen.WrappedScreen;
import com.wynntils.handlers.wrappedscreen.type.WrappedScreenInfo;
import com.wynntils.screens.base.TextboxScreen;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.WynntilsContainerScreen;
import com.wynntils.screens.base.widgets.BasicTexturedButton;
import com.wynntils.screens.base.widgets.ItemFilterUIButton;
import com.wynntils.screens.base.widgets.ItemSearchHelperWidget;
import com.wynntils.screens.base.widgets.ItemSearchWidget;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.trademarket.widgets.PresetButton;
import com.wynntils.services.itemfilter.type.ItemProviderType;
import com.wynntils.services.itemfilter.type.ItemSearchQuery;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

public class TradeMarketSearchResultScreen extends WynntilsContainerScreen<ChestMenu> implements WrappedScreen {
    // Constants
    private static final int FAKE_CONTAINER_ID = 454545;
    private static final ResourceLocation CONTAINER_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int SCROLL_AREA_HEIGHT = 110;
    private static final int ITEMS_PER_PAGE = 54;

    // Info
    private final TradeMarketSearchResultHolder holder;
    private final WrappedScreenInfo wrappedScreenInfo;

    // Widgets
    private ItemSearchWidget itemSearchWidget;
    private BasicTexturedButton sortOptionsButton;

    // State
    private Component currentState = Component.empty();

    // Scrolling
    private int scrollOffset = 0;
    private boolean holdingScrollbar = false;

    protected TradeMarketSearchResultScreen(WrappedScreenInfo wrappedScreenInfo, TradeMarketSearchResultHolder holder) {
        super(
                ChestMenu.sixRows(FAKE_CONTAINER_ID, McUtils.inventory()),
                McUtils.inventory(),
                Component.literal("Trade Market Search Result Wrapped Screen"));

        // Make our screen display according to the number of items, at the correct y position
        this.imageHeight = 114 + this.getMenu().getRowCount() * 18;
        this.inventoryLabelY = this.imageHeight - 94;

        this.holder = holder;
        this.wrappedScreenInfo = wrappedScreenInfo;
    }

    @Override
    public WrappedScreenInfo getWrappedScreenInfo() {
        return wrappedScreenInfo;
    }

    @Override
    protected void doInit() {
        super.doInit();

        // This is screen.topPos and screen.leftPos, but they are not calculated yet when this is called
        int renderX = (width - imageWidth) / 2;
        int renderY = (height - imageHeight) / 2 - 22;

        itemSearchWidget = new ItemSearchWidget(
                renderX,
                renderY,
                155,
                20,
                ItemProviderType.normalTypes(),
                true,
                (query) -> {
                    saveSearchFilter(query);
                    reloadElements();
                },
                (TextboxScreen) this);
        this.addRenderableWidget(itemSearchWidget);

        // Set the last search filter, if we opened a new screen
        // On reloads, this should not change anything
        itemSearchWidget.setTextBoxInput(Models.TradeMarket.getLastSearchFilter());

        this.addRenderableWidget(new ItemFilterUIButton(
                renderX + 157,
                renderY,
                itemSearchWidget,
                this,
                true,
                Arrays.stream(ItemProviderType.values()).toList()));

        WynntilsButton backButton = new BasicTexturedButton(
                renderX - Texture.CONTAINER_SIDEBAR.width() / 2 - 2,
                renderY + Texture.CONTAINER_SIDEBAR.height(),
                Texture.ARROW_LEFT_ICON.width(),
                Texture.ARROW_LEFT_ICON.height(),
                Texture.ARROW_LEFT_ICON,
                (button) -> holder.goBackToSearch(),
                List.of(Component.translatable("screens.wynntils.tradeMarketSearchResult.backToSearch")
                        .withStyle(ChatFormatting.BOLD)));
        this.addRenderableWidget(backButton);

        WynntilsButton loadMoreButton = new BasicTexturedButton(
                renderX - Texture.CONTAINER_SIDEBAR.width() / 2 - 2,
                renderY + Texture.CONTAINER_SIDEBAR.height() - 17,
                Texture.SMALL_ADD_ICON.width(),
                Texture.SMALL_ADD_ICON.height(),
                Texture.SMALL_ADD_ICON,
                (button) -> holder.loadNextPageBatch(),
                List.of(Component.translatable(
                                "screens.wynntils.tradeMarketSearchResult.loadNextBatch", holder.getPageLoadBatchSize())
                        .withStyle(ChatFormatting.BOLD)));
        this.addRenderableWidget(loadMoreButton);

        sortOptionsButton = new BasicTexturedButton(
                renderX - Texture.CONTAINER_SIDEBAR.width() / 2 - 2,
                renderY + Texture.CONTAINER_SIDEBAR.height() - 34,
                Texture.SORT.width(),
                Texture.SORT.height(),
                Texture.SORT,
                holder::changeSortingMode,
                holder.getSortingItemTooltip());
        this.addRenderableWidget(sortOptionsButton);

        // Add preset buttons
        for (int i = 0; i < 4; i++) {
            this.addRenderableWidget(new PresetButton(
                    renderX - Texture.CONTAINER_SIDEBAR.width() / 2 - 2,
                    renderY + 25 + i * (Texture.PRESET.height() + 2),
                    Texture.PRESET.width(),
                    Texture.PRESET.height(),
                    i,
                    this));
        }
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        updateItems();

        super.doRender(guiGraphics, mouseX, mouseY, partialTick);
        renderScrollButton(poseStack);

        renderables.forEach(c -> c.render(guiGraphics, mouseX, mouseY, partialTick));

        // Render item tooltip
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render tooltip for hovered widget
        for (GuiEventListener child : children()) {
            if (child instanceof TooltipProvider tooltipProvider && child.isMouseOver(mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(
                        FontRenderer.getInstance().getFont(), tooltipProvider.getTooltipLines(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(
                FontRenderer.getInstance().getFont(),
                this.currentState,
                this.titleLabelX,
                this.titleLabelY,
                CommonColors.TITLE_GRAY.asInt(),
                false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        PoseStack poseStack = guiGraphics.pose();

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Container
        RenderUtils.drawTexturedRect(
                poseStack, CONTAINER_BACKGROUND, x, y, this.imageWidth, this.menu.getRowCount() * 18 + 17, 256, 256);

        // Inventory
        RenderUtils.drawTexturedRect(
                poseStack,
                CONTAINER_BACKGROUND,
                x,
                y + this.menu.getRowCount() * 18 + 17,
                0,
                this.imageWidth,
                96,
                0,
                126,
                this.imageWidth,
                96,
                256,
                256);

        // Scrollbar
        RenderUtils.drawTexturedRect(poseStack, Texture.SCROLLBAR_BACKGROUND, x + this.imageWidth - 7, y);

        // Sidebar
        RenderUtils.drawTexturedRect(
                poseStack, Texture.CONTAINER_SIDEBAR, x - Texture.CONTAINER_SIDEBAR.width() + 7, y);
    }

    private void renderScrollButton(PoseStack poseStack) {
        float renderX =
                (this.width - this.imageWidth) / 2 + this.imageWidth + Texture.SCROLLBAR_BACKGROUND.width() / 2 - 14;
        float renderY = (this.height - this.imageHeight) / 2
                + Texture.SCROLLBAR_BUTTON.height() / 2
                + MathUtils.map(scrollOffset, 0, getMaxScrollOffset(), 0, SCROLL_AREA_HEIGHT);

        RenderUtils.drawTexturedRect(poseStack, Texture.SCROLLBAR_BUTTON, renderX, renderY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scrollBarRenderX =
                (this.width - this.imageWidth) / 2 + this.imageWidth + Texture.SCROLLBAR_BACKGROUND.width() / 2 - 14;
        float scrollBarRenderY = (this.height - this.imageHeight) / 2
                + Texture.SCROLLBAR_BUTTON.height() / 2
                + MathUtils.map(scrollOffset, 0, getMaxScrollOffset(), 0, SCROLL_AREA_HEIGHT);

        if (mouseX >= scrollBarRenderX
                && mouseX <= scrollBarRenderX + Texture.SCROLLBAR_BUTTON.width()
                && mouseY >= scrollBarRenderY
                && mouseY <= scrollBarRenderY + Texture.SCROLLBAR_BUTTON.height()) {
            holdingScrollbar = true;
            return true;
        }

        if (hoveredSlot != null) {
            holder.clickOnItem(hoveredSlot.getItem());
            return true;
        }

        // Item helper widget needs special handling because it is overlaid on the search box
        for (GuiEventListener child : children()) {
            if (child instanceof ItemSearchHelperWidget) {
                if (child.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!holdingScrollbar) return false;

        int renderY = (this.height - this.imageHeight) / 2;
        int scrollAreaStartY = renderY + 14;

        int newValue = Math.round(MathUtils.map(
                (float) mouseY, scrollAreaStartY, scrollAreaStartY + SCROLL_AREA_HEIGHT, 0, getMaxScrollOffset()));

        scroll(newValue - scrollOffset);

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        holdingScrollbar = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Signum so we only scroll 1 item at a time
        double scrollValue = -Math.signum(deltaY);
        scroll((int) scrollValue);

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    public void setSearchQuery(String query) {
        itemSearchWidget.setTextBoxInput(query);
    }

    public ItemSearchQuery getSearchQuery() {
        return itemSearchWidget.getSearchQuery();
    }

    public void onSortingModeChanged() {
        sortOptionsButton.setTooltip(holder.getSortingItemTooltip());
    }

    private void scroll(int delta) {
        int maxValue = getMaxScrollOffset();

        scrollOffset = MathUtils.clamp(scrollOffset + delta, 0, maxValue);
    }

    private int getMaxScrollOffset() {
        int maxItemOffset = Math.max(0, holder.getFilteredItems().size() - 54);
        int maxValue = maxItemOffset / 9 + (maxItemOffset % 9 > 0 ? 1 : 0);
        return maxValue;
    }

    private void updateItems() {
        List<ItemStack> filteredItems = holder.getFilteredItems();

        // Reset all items
        for (int i = 0; i < 54; i++) {
            this.menu.setItem(i, 0, ItemStack.EMPTY);
        }

        // Set items with filters and sorting, with scroll offset
        int itemIndex = scrollOffset * 9;
        int currentSlot = 0;
        while (itemIndex < filteredItems.size() && currentSlot < ITEMS_PER_PAGE) {
            ItemStack itemStack = filteredItems.get(itemIndex);
            this.menu.setItem(currentSlot, 0, itemStack);

            itemIndex++;
            currentSlot++;
        }
    }

    protected void setCurrentState(Component currentState) {
        this.currentState = currentState;
    }

    private void reloadElements() {
        holder.updateDisplayItems(itemSearchWidget.getSearchQuery());
        scrollOffset = 0;
    }

    private void saveSearchFilter(ItemSearchQuery query) {
        Models.TradeMarket.setLastSearchFilter(query.queryString());
    }
}
