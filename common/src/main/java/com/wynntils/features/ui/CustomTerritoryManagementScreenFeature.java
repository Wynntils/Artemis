/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.wrappedscreen.event.WrappedScreenOpenEvent;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.models.containers.containers.GuildTerritoriesContainer;
import com.wynntils.screens.territorymanagement.TerritoryManagementScreen;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import com.wynntils.utils.wynn.InventoryUtils;
import java.util.regex.Pattern;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.UI)
public class CustomTerritoryManagementScreenFeature extends Feature {
    private static final Pattern TERRITORY_MANAGE_ITEM_PATTERN = Pattern.compile("§e§lTerritories \\[.+\\]");
    private static final Pattern MANAGE_TITLE_PATTERN = Pattern.compile(".+: Manage");
    private static final Pattern BACK_BUTTON_PATTERN = Pattern.compile("§7§lBack");
    private static final int COMPASS_INVENTORY_SLOT = 42;
    private static final int GUILD_MANAGEMENT_SLOT = 26;
    private static final int TERRITORY_MANAGEMENT_SLOT = 14;

    @RegisterKeyBind
    private final KeyBind openTerritoryMenu =
            new KeyBind("Open Territory Menu", GLFW.GLFW_KEY_U, true, this::updateTerritoryMenu);

    @Persisted
    private final Config<ShiftBehavior> shiftBehaviorConfig = new Config<>(ShiftBehavior.DISABLED_IF_SHIFT_HELD);

    @Persisted
    public final Storage<Boolean> screenHighlightLegend = new Storage<>(true);

    @Persisted
    public final Storage<Boolean> screenTerritoryProductionTooltip = new Storage<>(true);

    private boolean shiftClickedTerritoryItem = false;

    private boolean customScreenOpened = false;
    private boolean openTerritoryManagement = false;

    @SubscribeEvent
    public void onWrappedScreenOpen(WrappedScreenOpenEvent event) {
        if (event.getWrappedScreenClass() != TerritoryManagementScreen.class) return;

        boolean shouldOpen = false;

        switch (shiftBehaviorConfig.get()) {
            case NONE -> {
                shouldOpen = true;
            }
            case ENABLED_IF_SHIFT_HELD -> {
                if (shiftClickedTerritoryItem) {
                    shouldOpen = true;
                }
            }
            case DISABLED_IF_SHIFT_HELD -> {
                if (!shiftClickedTerritoryItem) {
                    shouldOpen = true;
                }
            }
        }

        if (shouldOpen) {
            event.setOpenScreen(true);
            customScreenOpened = true;
        }
    }

    @SubscribeEvent
    public void onScreenClosed(ScreenClosedEvent event) {
        // Reset the flag when the screen is closed
        customScreenOpened = false;
        openTerritoryManagement = false;
    }

    @SubscribeEvent
    public void onScreenInit(ScreenInitEvent event) {
        if (StyledText.fromComponent(event.getScreen().getTitle()).matches(MANAGE_TITLE_PATTERN)) {
            // We might have opened a different screen,
            // but ScreenClosedEvent did not fire (as the screen was overridden).
            customScreenOpened = false;
        }
    }

    @SubscribeEvent
    public void onContainerClick(ContainerClickEvent event) {
        if (StyledText.fromComponent(event.getItemStack().getHoverName()).matches(TERRITORY_MANAGE_ITEM_PATTERN)) {
            shiftClickedTerritoryItem = KeyboardUtils.isShiftDown();
            return;
        }

        if (!customScreenOpened) return;
        if (Models.Container.getCurrentContainer() instanceof GuildTerritoriesContainer) return;

        ItemStack itemStack = event.getItemStack();
        StyledText itemName = StyledText.fromComponent(itemStack.getHoverName());

        if (itemName.matches(BACK_BUTTON_PATTERN)) {
            // We clicked the back button,
            // but we want to arrive at the territory management screen,
            // not the guild management screen. So, we click the corresponding item.
            openTerritoryManagement = true;
        }
    }

    @SubscribeEvent
    public void onMenuOpenPre(MenuEvent.MenuOpenedEvent.Pre event) {
        if (!openTerritoryManagement) return;

        openTerritoryManagement = false;

        // We cannot use ContainerModel here, as it is too early in the event chain.
        StyledText title = StyledText.fromComponent(event.getTitle());
        if (title.matches(MANAGE_TITLE_PATTERN)) {
            event.setCanceled(true);

            AbstractContainerMenu container = event.getMenuType().create(event.getContainerId(), McUtils.inventory());
            ContainerUtils.clickOnSlot(TERRITORY_MANAGEMENT_SLOT, event.getContainerId(), 0, container.getItems());
        } else if (title.equalsString(Models.Container.CHARACTER_INFO_NAME)) {
            event.setCanceled(true);

            // We still need to do a second click to open the territory management screen
            openTerritoryManagement = true;

            AbstractContainerMenu container = event.getMenuType().create(event.getContainerId(), McUtils.inventory());
            ContainerUtils.clickOnSlot(GUILD_MANAGEMENT_SLOT, event.getContainerId(), 0, container.getItems());
        }
    }

    private void updateTerritoryMenu() {
        openTerritoryManagement = true;

        if (Models.War.isWarActive()) {
            InventoryUtils.sendInventorySlotMouseClick(
                    COMPASS_INVENTORY_SLOT, InventoryUtils.MouseClickType.LEFT_CLICK);
        } else {
            Handlers.Command.sendCommandImmediately("guild manage");
        }
    }

    private enum ShiftBehavior {
        NONE,
        ENABLED_IF_SHIFT_HELD,
        DISABLED_IF_SHIFT_HELD
    }
}
