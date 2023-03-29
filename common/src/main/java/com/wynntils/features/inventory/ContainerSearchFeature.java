/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.features.Feature;
import com.wynntils.mc.event.ContainerCloseEvent;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.ContainerSetSlotEvent;
import com.wynntils.mc.event.InventoryKeyPressEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.mc.extension.ScreenExtension;
import com.wynntils.models.containers.type.SearchableContainerType;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.WynnItemCache;
import com.wynntils.screens.base.widgets.SearchWidget;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.INVENTORY)
public class ContainerSearchFeature extends Feature {
    @RegisterConfig
    public final Config<Boolean> filterInBank = new Config<>(true);

    @RegisterConfig
    public final Config<Boolean> filterInMiscBucket = new Config<>(true);

    @RegisterConfig
    public final Config<Boolean> filterInGuildBank = new Config<>(true);

    @RegisterConfig
    public final Config<Boolean> filterInGuildMemberList = new Config<>(true);

    @RegisterConfig
    public final Config<CustomColor> highlightColor = new Config<>(CommonColors.MAGENTA);

    // If the guild bank has lots of custom (crafted) items, it can take multiple packets and a decent amount of time
    // for Wynn to send us the entire updated inventory. During this, the inventory will be in a weird state where
    // some items are updated and some are not. We will assume that after SEARCH_DELAY_MS milliseconds, the inventory
    // is fully updated.
    // From my testing, 150 seems to work, but is not very reliable, so we will use 200
    private static final int GUILD_BANK_SEARCH_DELAY = 200;
    private long guildBankLastSearch = 0;

    private SearchWidget lastSearchWidget;
    private SearchableContainerType currentSearchableContainerType;
    private boolean autoSearching = false;

    @SubscribeEvent
    public void onScreenInit(ScreenInitEvent event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        String title = ComponentUtils.getCoded(screen.getTitle());

        // This is screen.topPos and screen.leftPos, but they are not calculated yet when this is called
        int renderX = (screen.width - screen.imageWidth) / 2;
        int renderY = (screen.height - screen.imageHeight) / 2;

        SearchableContainerType searchableContainerType = getCurrentSearchableContainerType(title);
        if (searchableContainerType == null) return;

        currentSearchableContainerType = searchableContainerType;

        addSearchWidget(screen, renderX, renderY);
    }

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Pre e) {
        ItemStack itemStack = e.getSlot().getItem();
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(itemStack);
        if (wynnItemOpt.isEmpty()) return;

        Boolean result = wynnItemOpt.get().getCache().get(WynnItemCache.SEARCHED_KEY);
        if (result == null || !result) return;

        RenderUtils.drawArc(e.getPoseStack(), highlightColor.get(), e.getSlot().x, e.getSlot().y, 200, 1f, 6, 8);
    }

    @SubscribeEvent
    public void onContainerSetContent(ContainerSetContentEvent.Post event) {
        forceUpdateSearch();

        if (autoSearching && McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen) {
            tryAutoSearch(abstractContainerScreen);
        }
    }

    @SubscribeEvent
    public void onContainerSetSlot(ContainerSetSlotEvent event) {
        forceUpdateSearch();
    }

    @SubscribeEvent
    public void onContainerClose(ContainerCloseEvent.Post event) {
        lastSearchWidget = null;
        currentSearchableContainerType = null;
        autoSearching = false;
        guildBankLastSearch = 0;
    }

    @SubscribeEvent
    public void onInventoryKeyPress(InventoryKeyPressEvent event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_ENTER) return;
        if (lastSearchWidget == null
                || currentSearchableContainerType == null
                || currentSearchableContainerType.getNextItemSlot() == -1
                || !(McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen)) return;

        autoSearching = true;
        matchItems(lastSearchWidget.getTextBoxInput(), abstractContainerScreen);

        tryAutoSearch(abstractContainerScreen);
    }

    private void tryAutoSearch(AbstractContainerScreen<?> abstractContainerScreen) {
        if (!autoSearching) return;
        if (currentSearchableContainerType == SearchableContainerType.GUILD_BANK) {
            long diff = System.currentTimeMillis() - guildBankLastSearch;
            if (diff < GUILD_BANK_SEARCH_DELAY) {
                Managers.TickScheduler.scheduleLater(
                        () -> tryAutoSearch(abstractContainerScreen),
                        (int) (GUILD_BANK_SEARCH_DELAY - diff) / 20);
                return;
            }
            guildBankLastSearch = System.currentTimeMillis();
        }

        String name = ComponentUtils.getCoded(abstractContainerScreen
                .getMenu()
                .getItems()
                .get(currentSearchableContainerType.getNextItemSlot())
                .getHoverName());

        if (!currentSearchableContainerType.getNextItemPattern().matcher(name).matches()) {
            autoSearching = false;
            return;
        }

        ContainerUtils.clickOnSlot(
                currentSearchableContainerType.getNextItemSlot(),
                abstractContainerScreen.getMenu().containerId,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                abstractContainerScreen.getMenu().getItems());
    }

    private SearchableContainerType getCurrentSearchableContainerType(String title) {
        SearchableContainerType containerType = SearchableContainerType.getContainerType(title);

        if (containerType == SearchableContainerType.BANK && filterInBank.get()) {
            return SearchableContainerType.BANK;
        }

        if (containerType == SearchableContainerType.MISC_BUCKET && filterInMiscBucket.get()) {
            return SearchableContainerType.MISC_BUCKET;
        }

        if (containerType == SearchableContainerType.GUILD_BANK && filterInGuildBank.get()) {
            return SearchableContainerType.GUILD_BANK;
        }

        if (containerType == SearchableContainerType.MEMBER_LIST && filterInGuildMemberList.get()) {
            return SearchableContainerType.MEMBER_LIST;
        }

        return null;
    }

    private void addSearchWidget(AbstractContainerScreen<?> screen, int renderX, int renderY) {
        SearchWidget searchWidget = new SearchWidget(
                renderX + screen.imageWidth - 100, renderY - 20, 100, 20, s -> matchItems(s, screen), (ScreenExtension)
                        screen);

        if (lastSearchWidget != null) {
            searchWidget.setTextBoxInput(lastSearchWidget.getTextBoxInput());
        }

        lastSearchWidget = searchWidget;

        screen.addRenderableWidget(lastSearchWidget);
    }

    private void matchItems(String searchStr, AbstractContainerScreen<?> screen) {
        String search = searchStr.toLowerCase(Locale.ROOT);

        NonNullList<ItemStack> playerItems = McUtils.inventory().items;
        for (ItemStack itemStack : screen.getMenu().getItems()) {
            Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(itemStack);
            if (wynnItemOpt.isEmpty()) return;
            if (playerItems.contains(itemStack)) continue;

            String name =
                    ComponentUtils.getUnformatted(itemStack.getHoverName()).toLowerCase(Locale.ROOT);

            boolean filtered = !search.isEmpty() && name.contains(search) && itemStack.getItem() != Items.AIR;
            wynnItemOpt.get().getCache().store(WynnItemCache.SEARCHED_KEY, filtered);
            if (filtered) {
                autoSearching = false;
            }
        }
    }

    private void forceUpdateSearch() {
        Screen screen = McUtils.mc().screen;
        if (lastSearchWidget != null && screen instanceof AbstractContainerScreen<?> abstractContainerScreen) {
            matchItems(lastSearchWidget.getTextBoxInput(), abstractContainerScreen);
        }
    }
}
