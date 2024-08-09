/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.territorymanagement;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.wrappedscreen.WrappedScreenHolder;
import com.wynntils.handlers.wrappedscreen.type.WrappedScreenInfo;
import com.wynntils.mc.event.ContainerSetSlotEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.items.items.gui.TerritoryItem;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.TerritoryConnectionType;
import com.wynntils.models.territories.type.TerritoryUpgrade;
import com.wynntils.screens.territorymanagement.highlights.TerritoryBonusEffectHighlighter;
import com.wynntils.screens.territorymanagement.highlights.TerritoryHighlighter;
import com.wynntils.screens.territorymanagement.highlights.TerritoryTypeHighlighter;
import com.wynntils.screens.territorymanagement.type.TerritoryColor;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.wynn.ContainerUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class TerritoryManagementHolder extends WrappedScreenHolder<TerritoryManagementScreen> {
    private static final String SELECT_TERRITORIES_TITLE = "Select Territories";
    private static final Pattern TITLE_PATTERN =
            Pattern.compile("(?:.+: Territories)|(?:" + SELECT_TERRITORIES_TITLE + ")");
    private static final int ITEMS_PER_CONTAINER_PAGE = 45;
    private static final int ITEMS_PER_PAGE = 35;
    private static final int ITEMS_PER_ROW = 7;

    private static final Pattern NEXT_PAGE_PATTERN = Pattern.compile("§a§lNext Page");
    private static final Pattern PREVIOUS_PAGE_PATTERN = Pattern.compile("§a§lPrevious Page");
    private static final int NEXT_PAGE_SLOT = 27;
    private static final int PREVIOUS_PAGE_SLOT = 9;

    private static final int APPLY_BUTTON_SLOT = 0;
    private static final int APPLY_DISABLED_DAMAGE = 17;
    private static final int APPLY_UNCONFIRMED_DAMAGE = 19;
    private static final int APPLY_CONFIRMED_DAMAGE = 20;

    private static final int REQUEST_LOAD_DELAY = 5;
    private static final int SELECTION_MODE_LOAD_DELAY = 20;
    // If a change is not loaded after this delay, force the next step
    private static final int FORCED_LOAD_DELAY = 20;

    // Highlighters
    private final List<TerritoryHighlighter> highlighters = new ArrayList<>();

    // Screen
    private TerritoryManagementScreen wrappedScreen;

    // Page loading
    private int currentPage;
    private int requestedPage;
    private int loadedItems;
    private long nextRequestTicks;
    private long lastItemLoadedTicks;
    private boolean initialLoadFinished;

    // Territory data
    private Int2ObjectSortedMap<Pair<ItemStack, TerritoryItem>> territories = new Int2ObjectAVLTreeMap<>();
    private Map<TerritoryItem, TerritoryConnectionType> territoryConnections = Map.of();

    // Mode-specific data
    private boolean selectionMode;
    private String territoryToBeClicked;
    private Set<String> selectedTerritories = new HashSet<>();
    // Note: It seems like Wynn only accepts 1 territory click "request" at a time, if you do more than one,
    //       the server will only process the first one. So, we need to queue the clicks.
    private int currentClick;
    private long lastClickTicks;

    public TerritoryManagementHolder() {
        // Register the highlighters
        registerHighlighter(new TerritoryBonusEffectHighlighter());
        registerHighlighter(new TerritoryTypeHighlighter(this));
    }

    @SubscribeEvent
    public void onSetSlot(ContainerSetSlotEvent.Post event) {
        if (event.getContainerId() != wrappedScreen.getWrappedScreenInfo().containerId()) return;

        // Otherwise, update the slot directly
        int slot = event.getSlot();
        if (slot % 9 < 2) return;
        if (slot >= ITEMS_PER_CONTAINER_PAGE) return;

        loadedItems++;

        ItemStack itemStack = event.getItemStack();
        int absSlot = getAbsoluteSlot(slot);

        // If there is a click in progress,
        // check if it is the response to the current click
        if (absSlot == currentClick) {
            currentClick = -1;
            lastClickTicks = Integer.MAX_VALUE;
        }

        Optional<TerritoryItem> territoryItemOpt = Models.Item.asWynnItem(itemStack, TerritoryItem.class);
        if (territoryItemOpt.isEmpty()) {
            territories.remove(absSlot);
        } else {
            TerritoryItem territoryItem = territoryItemOpt.get();
            territories.put(absSlot, Pair.of(itemStack, territoryItem));

            if (!isSinglePage()) {
                // There is cases where we need to do clicking:
                // Normal mode:
                // 1. The territory item is marked to be clicked

                if (Objects.equals(territoryToBeClicked, territoryItem.getName())) {
                    clickOnTerritory(absSlot);
                }
            }
        }

        lastItemLoadedTicks = McUtils.player().tickCount;

        updateRenderedItems();

        // Reset the requested page, after loading the page
        if (loadedItems >= ITEMS_PER_PAGE) {
            requestedPage = -1;
            loadedItems = 0;

            // Wait before the next request, while the page is being loaded
            nextRequestTicks =
                    McUtils.player().tickCount + (selectionMode ? SELECTION_MODE_LOAD_DELAY : REQUEST_LOAD_DELAY);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (McUtils.player().tickCount >= lastItemLoadedTicks + FORCED_LOAD_DELAY) {
            // Force the next page load
            WynntilsMod.warn("Forcing the next page load, as the previous one did not finish properly.");
            requestedPage = -1;
            loadedItems = 0;
            nextRequestTicks = McUtils.player().tickCount;
            lastItemLoadedTicks = Integer.MAX_VALUE;
        }

        if (McUtils.player().tickCount >= lastClickTicks + FORCED_LOAD_DELAY) {
            // Force the next click
            WynntilsMod.warn("Forcing the next click, as the previous one did not finish properly.");
            currentClick = -1;
            lastClickTicks = Integer.MAX_VALUE;
        }

        // If we are in selection mode, and there are no changes needed, return
        // If the initial load has not finished, we need to proceed anyway
        if (initialLoadFinished && selectionMode && !shouldChangePageForSelection()) {
            tryClickNextSelection();
            return;
        }

        // Try to cycle through the pages, if there are more than one
        if (McUtils.player().tickCount < nextRequestTicks) return;

        // If there are no more pages to load, or the selection has changed, return
        if (isSinglePage()) return;

        // If we already have a requested page, return
        if (requestedPage != -1) return;

        boolean forwardPage = StyledText.fromComponent(wrappedScreen
                        .getWrappedScreenInfo()
                        .containerMenu()
                        .getItems()
                        .get(NEXT_PAGE_SLOT)
                        .getHoverName())
                .matches(NEXT_PAGE_PATTERN);

        if (forwardPage) {
            requestedPage = currentPage + 1;
        } else {
            requestedPage = currentPage - 1;

            // Initial load finished when we start going backwards
            // (Note: this won't always work in non-selection mode,
            // as the back button can open the menu on the second page)
            initialLoadFinished = true;
        }

        // Proceed to do the requests for the next page
        doNextRequest();

        nextRequestTicks = Integer.MAX_VALUE;
    }

    @Override
    protected Pattern getReplacedScreenTitlePattern() {
        return TITLE_PATTERN;
    }

    @Override
    protected TerritoryManagementScreen createWrappedScreen(WrappedScreenInfo wrappedScreenInfo) {
        return new TerritoryManagementScreen(wrappedScreenInfo, this);
    }

    @Override
    protected void setWrappedScreen(TerritoryManagementScreen wrappedScreen) {
        this.wrappedScreen = wrappedScreen;

        // This should have already been done in the constructor,
        // but just in case
        reset();

        // Update the selection mode
        selectionMode = StyledText.fromComponent(
                        wrappedScreen.getWrappedScreenInfo().screen().getTitle())
                .equalsString(SELECT_TERRITORIES_TITLE, PartStyle.StyleType.NONE);
    }

    @Override
    protected void reset() {
        currentPage = 0;
        requestedPage = -1;
        loadedItems = 0;
        nextRequestTicks = Integer.MAX_VALUE;
        lastItemLoadedTicks = Integer.MAX_VALUE;
        initialLoadFinished = false;
        territories = new Int2ObjectAVLTreeMap<>();
        territoryConnections = new HashMap<>();
        selectionMode = false;
        territoryToBeClicked = null;
        selectedTerritories = new HashSet<>();
        currentClick = -1;
        lastClickTicks = Integer.MAX_VALUE;
    }

    public TerritoryColor getTerritoryColor(TerritoryItem territoryItem) {
        CustomColor borderColor = CustomColor.NONE;
        List<CustomColor> backgroundColors = new ArrayList<>();

        for (TerritoryHighlighter highlighter : highlighters) {
            Optional<CustomColor> highlighterBorderColor = highlighter.getBorderColor(territoryItem);

            if (highlighterBorderColor.isPresent() && borderColor == CustomColor.NONE) {
                borderColor = highlighterBorderColor.get();
            }

            backgroundColors.addAll(highlighter.getBackgroundColors(territoryItem));
        }

        return new TerritoryColor(borderColor, backgroundColors);
    }

    public Collection<Pair<ItemStack, TerritoryItem>> territoryItems() {
        // Return an unique collection of the territories, by checking territory item names
        List<String> territoryNames = territories.values().stream()
                .map(Pair::b)
                .map(TerritoryItem::getName)
                .distinct()
                .toList();

        return territoryNames.stream()
                .map(name -> territories.values().stream()
                        .filter(pair -> pair.b().getName().equals(name))
                        .findFirst()
                        .orElseThrow())
                .toList();
    }

    public Map<TerritoryItem, TerritoryConnectionType> territoryConnections() {
        return territoryConnections;
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public int getCountForConnectionType(TerritoryConnectionType territoryConnectionType) {
        return (int) territoryConnections.values().stream()
                .filter(type -> type == territoryConnectionType)
                .count();
    }

    public int getCountForUpgrade(TerritoryUpgrade upgrade) {
        return (int) territories.values().stream()
                .map(Pair::b)
                .map(TerritoryItem::getUpgrades)
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .filter(upgrade::equals)
                .count();
    }

    public int getOverallProductionForResource(GuildResource resource) {
        return territories.values().stream()
                .map(Pair::b)
                .map(TerritoryItem::getProduction)
                .mapToInt(production -> production.getOrDefault(resource, 0))
                .sum();
    }

    public CappedValue getOverallStorageForResource(GuildResource resource) {
        return territories.values().stream()
                .map(Pair::b)
                .map(TerritoryItem::getStorage)
                .map(storage -> storage.getOrDefault(resource, new CappedValue(0, 0)))
                .reduce((cappedValue1, cappedValue2) -> new CappedValue(
                        cappedValue1.current() + cappedValue2.current(), cappedValue1.max() + cappedValue2.max()))
                .orElse(new CappedValue(0, 0));
    }

    public long getOverallUsageForResource(GuildResource resource) {
        return territories.values().stream()
                .map(Pair::b)
                .map(TerritoryItem::getUpgrades)
                .flatMap(upgrades -> upgrades.entrySet().stream())
                .filter(entry -> entry.getKey().getCostResource() == resource)
                .map(entry -> entry.getKey().getLevels()[entry.getValue()])
                .mapToLong(TerritoryUpgrade.Level::cost)
                .sum();
    }

    public void territoryItemClicked(TerritoryItem territoryItem) {
        if (selectionMode) {
            if (selectedTerritories.contains(territoryItem.getName())) {
                selectedTerritories.remove(territoryItem.getName());
            } else {
                selectedTerritories.add(territoryItem.getName());
            }
        } else {
            if (!Models.War.isWarActive()) {
                Handlers.Command.sendCommandImmediately("gu territory " + territoryItem.getName());
                return;
            }

            territoryToBeClicked = territoryItem.getName();
        }

        // Click on the territory item to select/deselect it
        int absSlot = getAbsoluteSlotForItem(territoryItem);

        int itemPage = absSlot / ITEMS_PER_PAGE;

        if (absSlot == -1) {
            WynntilsMod.warn("Could not find the slot for the territory item when trying to click it: "
                    + territoryItem.getName());
            return;
        }

        if (selectionMode && currentClick != -1) {
            // If there is a click in progress, we need to wait for it to finish
            return;
        }

        // Otherwise, set the current click to the item
        currentClick = absSlot;
        lastClickTicks = McUtils.player().tickCount;

        // If the item is not on the current page, we'll eventually go to the page
        if (currentPage != itemPage) return;

        clickOnTerritory(getRelativeSlot(currentClick));
    }

    public Texture getApplyButtonTexture() {
        // Should not be called when not in selection mode
        if (!selectionMode) return Texture.CHECKMARK_GREEN;

        ItemStack applyButton =
                wrappedScreen.getWrappedScreenInfo().containerMenu().getItems().get(APPLY_BUTTON_SLOT);

        if (applyButton.getDamageValue() == APPLY_DISABLED_DAMAGE) {
            return Texture.CHECKMARK_GRAY;
        } else if (applyButton.getDamageValue() == APPLY_UNCONFIRMED_DAMAGE) {
            return Texture.CHECKMARK_YELLOW;
        } else if (applyButton.getDamageValue() == APPLY_CONFIRMED_DAMAGE) {
            return Texture.CHECKMARK_GREEN;
        } else {
            WynntilsMod.warn("Unknown apply button damage value: " + applyButton.getDamageValue());
            return Texture.CHECKMARK_GREEN;
        }
    }

    private void doNextRequest() {
        NonNullList<ItemStack> items =
                wrappedScreen.getWrappedScreenInfo().containerMenu().getItems();

        // We are already at the requested page
        // This shouldn't happen, but return just in case
        if (currentPage == requestedPage) {
            WynntilsMod.warn("Already at the requested page. Current page: " + currentPage + ", Requested page: "
                    + requestedPage);
            return;
        }

        if (requestedPage > currentPage) {
            currentPage++;

            ContainerUtils.clickOnSlot(
                    NEXT_PAGE_SLOT,
                    wrappedScreen.getWrappedScreenInfo().containerId(),
                    GLFW.GLFW_MOUSE_BUTTON_LEFT,
                    items);
            return;
        }

        if (requestedPage < currentPage) {
            // If there is a previous page, but we are on page 0,
            // we need to reset our knowledge of the menu
            // This can happen as back buttons sometimes open the menu on the second page,
            // and when a two-paged menu turns into a single-paged one
            if (currentPage == 0) {
                territories.clear();
                territoryConnections.clear();
            } else {
                currentPage--;
            }

            ContainerUtils.clickOnSlot(
                    PREVIOUS_PAGE_SLOT,
                    wrappedScreen.getWrappedScreenInfo().containerId(),
                    GLFW.GLFW_MOUSE_BUTTON_LEFT,
                    items);
            return;
        }
    }

    private boolean shouldChangePageForSelection() {
        Set<String> currentlySelectedTerritories = territories.values().stream()
                .map(Pair::b)
                .filter(TerritoryItem::isSelected)
                .map(TerritoryItem::getName)
                .collect(Collectors.toUnmodifiableSet());

        // Check if there are any changes needed,
        // but only on different pages than the current one
        // (otherwise, we don't need to switch pages)
        List<String> territoriesOnCurrentPage =
                getItemsOnPage(currentPage).stream().map(TerritoryItem::getName).toList();

        // Check for changes needed on the current page
        Set<String> selectedTerritoriesOnCurrentPage = territories.values().stream()
                .map(Pair::b)
                .filter(TerritoryItem::isSelected)
                .map(TerritoryItem::getName)
                .filter(territoriesOnCurrentPage::contains)
                .collect(Collectors.toUnmodifiableSet());

        Set<String> territoriesToBeSelectedOnCurrentPage = selectedTerritories.stream()
                .filter(territoriesOnCurrentPage::contains)
                .collect(Collectors.toUnmodifiableSet());

        // If we still have work to do on the current page, return false
        if (!selectedTerritoriesOnCurrentPage.equals(territoriesToBeSelectedOnCurrentPage)) {
            return false;
        }

        // Check for changes needed on different pages
        Set<String> selectedTerritoriesOnDifferentPages = territories.values().stream()
                .map(Pair::b)
                .filter(TerritoryItem::isSelected)
                .map(TerritoryItem::getName)
                .filter(name -> !territoriesOnCurrentPage.contains(name))
                .collect(Collectors.toUnmodifiableSet());

        Set<String> territoriesToBeSelectedOnDifferentPages = selectedTerritories.stream()
                .filter(territory -> !territoriesOnCurrentPage.contains(territory))
                .collect(Collectors.toUnmodifiableSet());

        // If there are any changes needed, return true
        return !selectedTerritoriesOnDifferentPages.equals(territoriesToBeSelectedOnDifferentPages);
    }

    private void tryClickNextSelection() {
        // A click is in progress, wait for it to finish
        if (currentClick != -1) return;

        int nextSelection = getNextSelectionInQueue();

        if (nextSelection == -1) {
            // If there are no more selections, we are done
            return;
        }

        currentClick = nextSelection;
        lastClickTicks = McUtils.player().tickCount;
        clickOnTerritory(getRelativeSlot(currentClick));
    }

    private void clickOnTerritory(int slot) {
        ContainerUtils.clickOnSlot(
                slot,
                wrappedScreen.getWrappedScreenInfo().containerId(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                wrappedScreen.getWrappedScreenInfo().containerMenu().getItems());
    }

    private boolean isSinglePage() {
        NonNullList<ItemStack> items =
                wrappedScreen.getWrappedScreenInfo().containerMenu().getItems();
        return items.get(NEXT_PAGE_SLOT).isEmpty()
                && items.get(PREVIOUS_PAGE_SLOT).isEmpty()
                && territories.size() <= ITEMS_PER_PAGE;
    }

    private int getNextSelectionInQueue() {
        if (selectedTerritories.isEmpty()) return -1;

        List<TerritoryItem> itemsOnPage = getItemsOnPage(currentPage);

        for (TerritoryItem territoryItem : itemsOnPage) {
            // The territory item is not selected, but it should be
            if (!territoryItem.isSelected() && selectedTerritories.contains(territoryItem.getName())) {
                return getAbsoluteSlotForItem(territoryItem);
            }

            // The territory item is selected, but it should not be
            if (territoryItem.isSelected() && !selectedTerritories.contains(territoryItem.getName())) {
                return getAbsoluteSlotForItem(territoryItem);
            }
        }

        return -1;
    }

    // Returns the actual slot of the territory items, starting from one
    // (a page fits 35 items, as the first two rows are reserved for the page navigation)
    private int getAbsoluteSlot(int slot) {
        // Both the argument and the return value are 0-indexed
        return currentPage * ITEMS_PER_PAGE + slot / 9 * ITEMS_PER_ROW + slot % 9 - 2;
    }

    private int getRelativeSlot(int absSlot) {
        return (absSlot + (absSlot / ITEMS_PER_ROW + 1) * 2) - currentPage * (ITEMS_PER_PAGE + 10);
    }

    private int getAbsoluteSlotForItem(TerritoryItem territoryItem) {
        return territories.int2ObjectEntrySet().stream()
                .filter(entry -> entry.getValue().b().getName().equals(territoryItem.getName()))
                .mapToInt(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
    }

    private List<TerritoryItem> getItemsOnPage(int page) {
        return territories.int2ObjectEntrySet().stream()
                .filter(entry -> entry.getKey() / ITEMS_PER_PAGE == page)
                .map(Map.Entry::getValue)
                .map(Pair::b)
                .collect(Collectors.toList());
    }

    private void updateRenderedItems() {
        territoryConnections = Models.Territory.getTerritoryConnections(
                territories.values().stream().map(Pair::b).collect(Collectors.toList()));

        // Update the rendered territory items
        wrappedScreen.updateTerritoryItems();
    }

    private void registerHighlighter(TerritoryHighlighter highlighter) {
        highlighters.add(highlighter);
    }
}
