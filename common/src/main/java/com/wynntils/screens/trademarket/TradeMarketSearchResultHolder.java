/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.trademarket;

import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.wrappedscreen.WrappedScreenHolder;
import com.wynntils.handlers.wrappedscreen.type.WrappedScreenInfo;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.ContainerSetSlotEvent;
import com.wynntils.services.itemfilter.type.ItemSearchQuery;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import com.wynntils.utils.wynn.ItemUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class TradeMarketSearchResultHolder extends WrappedScreenHolder<TradeMarketSearchResultScreen> {
    // Patterns
    private static final Pattern TITLE_PATTERN = Pattern.compile("Search Results");
    private static final Pattern PAGE_PATTERN = Pattern.compile("§f§lPage (\\d+)§a (:?[<>]§2[<>]§a)+[<>]");

    // Constants
    private static final int EXPECTED_ITEMS_PER_PAGE = 42;
    private static final int LAST_ITEM_SLOT = 54;
    private static final int PAGE_BATCH_SIZE = 10;

    // Slots
    private static final int PREVIOUS_PAGE_SLOT = 26;
    private static final int NEXT_PAGE_SLOT = 35;
    private static final int BACK_TO_SEARCH_SLOT = 8;
    private static final int SORT_MODE_SLOT = 53;

    // Screen
    private TradeMarketSearchResultScreen wrappedScreen;

    // Query / actions
    private int requestedPage = -1;
    private ItemStack requestedItem;
    private PageLoadingMode pageLoadingMode = PageLoadingMode.NONE;
    private QueuedAction queuedAction;

    // Page info
    private int currentPage = 0;
    private int parsedCurrentPage = 0;
    private boolean initialPageLoadRequested = false;
    private boolean allPagesLoaded = false;

    // Items
    private Map<Integer, Int2ObjectSortedMap<ItemStack>> itemMap = new TreeMap<>();
    private int pageItemCount = 0;

    private List<ItemStack> filteredItems = new ArrayList<>();
    private ItemStack sortingButtonItem = ItemStack.EMPTY;

    @SubscribeEvent
    public void onContainerSetContent(ContainerSetContentEvent.Pre event) {
        if (pageLoadingMode == PageLoadingMode.NONE || requestedPage == -1) return;

        WrappedScreenInfo wrappedScreenInfo = wrappedScreen.getWrappedScreenInfo();
        if (event.getContainerId() != wrappedScreenInfo.containerId()) return;

        // We only use set slot events to get the items,
        // but we don't want the items to be set on our custom screen
        event.setCanceled(true);

        // Reset the empty item count,
        // set content packets mean we are on a new page
        pageItemCount = 0;

        if (event.getItems().size() >= SORT_MODE_SLOT) {
            sortingButtonItem = event.getItems().get(SORT_MODE_SLOT);
            wrappedScreen.onSortingModeChanged();
            return;
        }
    }

    @SubscribeEvent
    public void onContainerSetSlot(ContainerSetSlotEvent.Pre event) {
        if (pageLoadingMode == PageLoadingMode.NONE || requestedPage == -1) return;

        WrappedScreenInfo wrappedScreenInfo = wrappedScreen.getWrappedScreenInfo();
        if (event.getContainerId() != wrappedScreenInfo.containerId()) return;

        int slot = event.getSlot();

        // We don't want the items to be set on our custom screen
        event.setCanceled(true);

        ItemStack itemStack = event.getItemStack();

        handleSetItem(slot, itemStack);
        updateDisplayItems(wrappedScreen.getSearchQuery());
    }

    @Override
    protected Pattern getReplacedScreenTitlePattern() {
        return TITLE_PATTERN;
    }

    @Override
    protected TradeMarketSearchResultScreen createWrappedScreen(WrappedScreenInfo wrappedScreenInfo) {
        return new TradeMarketSearchResultScreen(wrappedScreenInfo, this);
    }

    @Override
    protected void setWrappedScreen(TradeMarketSearchResultScreen wrappedScreen) {
        this.wrappedScreen = wrappedScreen;

        // Expect to load the first page only, decide what to do later
        pageLoadingMode = PageLoadingMode.LOAD_ITEMS;
        requestedPage = 0;
    }

    @Override
    protected void reset() {
        requestedPage = -1;
        requestedItem = null;
        pageLoadingMode = PageLoadingMode.NONE;
        queuedAction = null;
        allPagesLoaded = false;
        currentPage = 0;
        parsedCurrentPage = 0;
        initialPageLoadRequested = false;
        itemMap = new TreeMap<>();
        pageItemCount = 0;
        filteredItems = new ArrayList<>();
        sortingButtonItem = ItemStack.EMPTY;

        this.wrappedScreen = null;
    }

    public void clickOnItem(ItemStack clickedItem) {
        // When writing this, items only update on page change,
        // so if we don't switch pages, we can just click on the item
        ObjectSortedSet<Int2ObjectMap.Entry<ItemStack>> currentPageEntries =
                itemMap.getOrDefault(currentPage, new Int2ObjectAVLTreeMap<>()).int2ObjectEntrySet();
        for (Int2ObjectMap.Entry<ItemStack> entry : currentPageEntries) {
            if (ItemUtils.isItemEqual(entry.getValue(), clickedItem)) {
                // Item found on the current page, click on it
                WrappedScreenInfo wrappedScreenInfo = wrappedScreen.getWrappedScreenInfo();
                ContainerUtils.clickOnSlot(
                        entry.getIntKey(),
                        wrappedScreenInfo.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT,
                        wrappedScreenInfo.containerMenu().getItems());

                return;
            }
        }

        // Check if the item is on a later or earlier page
        for (int i = 0; i < itemMap.size(); i++) {
            // Skip the current page
            if (i == currentPage) continue;

            boolean foundItem = false;

            Int2ObjectSortedMap<ItemStack> itemsOnPage = itemMap.get(i);

            for (ItemStack itemStack : itemsOnPage.values()) {
                if (ItemUtils.isItemEqual(itemStack, clickedItem)) {
                    // Item found on a later page, load the pages until the last one and click on it
                    foundItem = true;
                    break;
                }
            }

            if (foundItem) {
                runOrQueueAction(new QueuedAction(PageLoadingMode.CLICK_ITEM, i, clickedItem));
                return;
            }
        }

        wrappedScreen.setCurrentState(Component.literal("Couldn't find item"));
    }

    public void goBackToSearch() {
        WrappedScreenInfo wrappedScreenInfo = wrappedScreen.getWrappedScreenInfo();
        ContainerUtils.clickOnSlot(
                BACK_TO_SEARCH_SLOT,
                wrappedScreenInfo.containerId(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                wrappedScreenInfo.containerMenu().getItems());
    }

    public void loadNextPageBatch() {
        if (allPagesLoaded) return;

        runOrQueueAction(new QueuedAction(PageLoadingMode.LOAD_ITEMS, itemMap.size() - 1 + PAGE_BATCH_SIZE, null));
    }

    public void changeSortingMode(int mouseButton) {
        WrappedScreenInfo wrappedScreenInfo = wrappedScreen.getWrappedScreenInfo();
        ContainerUtils.clickOnSlot(
                SORT_MODE_SLOT,
                wrappedScreenInfo.containerId(),
                mouseButton,
                wrappedScreenInfo.containerMenu().getItems());

        // Reset the item map
        itemMap = new TreeMap<>();
        pageItemCount = 0;

        // Set the page loading mode so we expect the items to be loaded
        // Note: At the time of writing this, changing the sorting mode
        //       triggers a screen reopen, meaning we reset the state,
        //       so it is up to the holder to figure out how to load pages
        pageLoadingMode = PageLoadingMode.LOAD_ITEMS;
        requestedPage = 0;

        // Reset the queued action
        queuedAction = null;
    }

    public List<Component> getSortingItemTooltip() {
        return LoreUtils.getTooltipLines(sortingButtonItem);
    }

    public void updateDisplayItems(ItemSearchQuery searchQuery) {
        filteredItems.clear();

        Stream<ItemStack> items = itemMap.values().stream()
                .map(map -> map.values().toArray(new ItemStack[0]))
                .flatMap(Stream::of);

        List<ItemStack> matchingItems = Services.ItemFilter.filterAndSort(searchQuery, items.toList());

        filteredItems.addAll(matchingItems);
    }

    public List<ItemStack> getFilteredItems() {
        return filteredItems;
    }

    public int getPageLoadBatchSize() {
        return PAGE_BATCH_SIZE;
    }

    private void handleSetItem(int slot, ItemStack itemStack) {
        // Update the sorting mode tooltip
        if (slot == SORT_MODE_SLOT) {
            sortingButtonItem = itemStack;
            wrappedScreen.onSortingModeChanged();
            return;
        }

        // Try to update the current page,
        // the result screen might not open on the first page
        if (slot == PREVIOUS_PAGE_SLOT || slot == NEXT_PAGE_SLOT) {
            StyledText itemName = StyledText.fromComponent(itemStack.getHoverName());
            Matcher matcher = itemName.getMatcher(PAGE_PATTERN);
            if (matcher.matches()) {
                int pageItemNumber = Integer.parseInt(matcher.group(1)) - 1;
                parsedCurrentPage = slot == PREVIOUS_PAGE_SLOT ? pageItemNumber + 1 : pageItemNumber - 1;
            }

            return;
        }

        // We only care about the slots that are in the "search results" area
        if (slot % 9 >= 7 || slot >= LAST_ITEM_SLOT) return;

        // If we have found an empty item, we count them and check if we have the expected amount
        Int2ObjectSortedMap<ItemStack> currentItems =
                itemMap.computeIfAbsent(this.currentPage, k -> new Int2ObjectAVLTreeMap<>());

        boolean emptyItem = isEmptyItem(itemStack);
        if (!emptyItem) {
            // Update item in slot, when changing pages,
            // items can change
            currentItems.put(slot, itemStack);
        } else {
            // Remove the item from the map if it was there
            currentItems.remove(slot);
        }

        pageItemCount++;

        switch (pageLoadingMode) {
            case LOAD_ITEMS -> pageLoadedWhileLoadingItems(currentItems);
            case CLICK_ITEM -> pageLoadedWhileSelectingItem(currentItems, slot, itemStack);
        }
    }

    private void pageLoadedWhileLoadingItems(Int2ObjectSortedMap<ItemStack> currentItems) {
        wrappedScreen.setCurrentState(Component.literal("Loading page " + (currentPage + 1) + "..."));

        // We only go to the next page if we have the expected amount of items
        if (pageItemCount != EXPECTED_ITEMS_PER_PAGE) return;

        // Decide what to do after the first page is loaded
        if (!initialPageLoadRequested) {
            initialPageLoadRequested = true;

            if (parsedCurrentPage == 0) {
                // We are on the first page, we don't need to do anything special
                loadItemsUntilPage(PAGE_BATCH_SIZE, false);

                // Continue loading the pages as usual
            } else {
                // We are not on the first page, special case

                // Sync the item map and current page info
                itemMap.put(parsedCurrentPage, itemMap.remove(currentPage));
                currentPage = parsedCurrentPage;

                // Load the pages until the first page
                loadItemsUntilPage(1, true);
                return;
            }
        }

        // If we have air items on the page, we reached the end
        if (currentItems.size() < EXPECTED_ITEMS_PER_PAGE) {
            wrappedScreen.setCurrentState(Component.literal("All pages loaded"));

            allPagesLoaded = true;

            startNextQueuedAction();

            return;
        }

        if (this.currentPage == requestedPage) {
            wrappedScreen.setCurrentState(Component.literal((itemMap.size()) + " pages loaded"));

            startNextQueuedAction();

            return;
        }

        goToNextPage();
    }

    private void pageLoadedWhileSelectingItem(
            Int2ObjectSortedMap<ItemStack> currentItems, int slot, ItemStack itemStack) {
        wrappedScreen.setCurrentState(Component.empty()
                .append(Component.literal("Clicking on "))
                .append(requestedItem.getHoverName())
                .append(Component.literal("...")));

        // If we found the item, click on it
        if (ItemUtils.isItemEqual(itemStack, requestedItem)) {
            // Loaded the item, click on it
            WrappedScreenInfo wrappedScreenInfo = wrappedScreen.getWrappedScreenInfo();
            ContainerUtils.clickOnSlot(
                    slot,
                    wrappedScreenInfo.containerId(),
                    GLFW.GLFW_MOUSE_BUTTON_LEFT,
                    wrappedScreenInfo.containerMenu().getItems());

            startNextQueuedAction();

            return;
        } else if (pageItemCount == EXPECTED_ITEMS_PER_PAGE) {
            if (currentPage == requestedPage) {
                wrappedScreen.setCurrentState(Component.literal("Couldn't click on item"));

                startNextQueuedAction();

                return;
            }

            // We couldn't find the item on the page, go to the next one
            goToNextPage();
        }
    }

    private void goToNextPage() {
        if (requestedPage == -1 || currentPage == requestedPage) return;

        pageItemCount = 0;

        int clickSlot;
        if (this.currentPage < requestedPage) {
            clickSlot = NEXT_PAGE_SLOT;
            this.currentPage++;
        } else {
            clickSlot = PREVIOUS_PAGE_SLOT;
            this.currentPage--;
        }

        WrappedScreenInfo wrappedScreenInfo = wrappedScreen.getWrappedScreenInfo();
        ContainerUtils.clickOnSlot(
                clickSlot,
                wrappedScreenInfo.containerId(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                wrappedScreenInfo.containerMenu().getItems());
    }

    private void startNextQueuedAction() {
        pageLoadingMode = PageLoadingMode.NONE;
        requestedPage = -1;
        runQueuedAction();
    }

    private void runQueuedAction() {
        if (queuedAction == null) return;

        runOrQueueAction(queuedAction);
        queuedAction = null;
    }

    private void runOrQueueAction(QueuedAction action) {
        if (pageLoadingMode == PageLoadingMode.NONE) {
            pageLoadingMode = action.pageLoadingMode();
            requestedPage = action.requestedPage();
            requestedItem = action.requestedItemStack();

            goToNextPage();
        } else if (queuedAction == null) {
            queuedAction = action;
        }
        // We only allow one queued action, ignore this call
    }

    private void loadItemsUntilPage(int page, boolean forcePageLoad) {
        pageLoadingMode = PageLoadingMode.LOAD_ITEMS;

        // 0 based indexing
        requestedPage = page - 1;

        if (forcePageLoad) {
            goToNextPage();
        }
    }

    private boolean isEmptyItem(ItemStack itemStack) {
        List<StyledText> loreTag = LoreUtils.getLore(itemStack);
        return itemStack.getItem() == Items.SNOW && (loreTag == null || loreTag.isEmpty());
    }

    public record QueuedAction(PageLoadingMode pageLoadingMode, int requestedPage, ItemStack requestedItemStack) {}

    public enum PageLoadingMode {
        LOAD_ITEMS,
        CLICK_ITEM,
        NONE
    }
}
