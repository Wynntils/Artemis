/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.InventoryKeyPressEvent;
import com.wynntils.mc.event.MouseScrollEvent;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.models.containers.containers.personal.AccountBankContainer;
import com.wynntils.models.containers.containers.personal.CharacterBankContainer;
import com.wynntils.models.containers.containers.personal.IslandBlockBankContainer;
import com.wynntils.models.containers.containers.personal.PersonalBlockBankContainer;
import com.wynntils.models.containers.containers.personal.PersonalStorageContainer;
import com.wynntils.screens.container.widgets.PersonalStorageUtilitiesWidget;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.INVENTORY)
public class PersonalStorageUtilitiesFeature extends Feature {
    private static final int STORAGE_TYPE_SLOT = 47;
    private static final Pattern PAGE_PATTERN = Pattern.compile("§7- §f.*§8 Page (\\d+)");

    private boolean quickJumping = false;
    private int currentPage = 1;
    private int lastPage = 21;
    private int pageDestination = 1;
    private PersonalStorageContainer storageContainer;
    private PersonalStorageUtilitiesWidget widget;

    @SubscribeEvent
    public void onScreenInit(ScreenInitEvent event) {
        if (Models.Bank.getStorageContainerType() == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!(Models.Container.getCurrentContainer() instanceof PersonalStorageContainer container)) return;

        storageContainer = container;

        lastPage = Models.Bank.getFinalPage();

        currentPage = Models.Bank.getCurrentPage();

        // This is screen.topPos and screen.leftPos, but they are not calculated yet when this is called
        int renderX = (screen.width - screen.imageWidth) / 2;
        int renderY = (screen.height - screen.imageHeight) / 2;

        widget = screen.addRenderableWidget(
                new PersonalStorageUtilitiesWidget(renderX - 108, renderY, storageContainer, this, screen));
    }

    @SubscribeEvent
    public void onScreenClose(ScreenClosedEvent e) {
        pageDestination = 1;
        quickJumping = false;
    }

    @SubscribeEvent
    public void onContainerSetContent(ContainerSetContentEvent.Pre event) {
        if (Models.Bank.getStorageContainerType() == null) return;
        // onScreenInit is not called when changing pages so we have to update current and last page here
        currentPage = Models.Bank.getCurrentPage();
        lastPage = Models.Bank.getFinalPage();

        // Mods such as Flow still render the widget after we close the screen so name has to be
        // set here instead of being retrieved in the widgets render method
        widget.updatePageName();

        if (!quickJumping) return;

        if (pageDestination > lastPage) {
            quickJumping = false;
            pageDestination = currentPage;
        } else if (pageDestination != currentPage) {
            jumpToDestination(pageDestination);
        } else if (pageDestination == currentPage) {
            quickJumping = false;
        }
    }

    @SubscribeEvent
    public void onSlotClicked(ContainerClickEvent e) {
        if (Models.Bank.getStorageContainerType() == null) return;

        // Swapping between character and account bank and island and personal storage does not call onScreenClose so
        // quick jumps need to be reset here
        if (Models.Container.getCurrentContainer() instanceof AccountBankContainer
                || Models.Container.getCurrentContainer() instanceof CharacterBankContainer
                || Models.Container.getCurrentContainer() instanceof IslandBlockBankContainer
                || Models.Container.getCurrentContainer() instanceof PersonalBlockBankContainer) {
            if (e.getSlotNum() == STORAGE_TYPE_SLOT) {
                pageDestination = 1;
                quickJumping = false;
            }
        }

        Models.Bank.toggleEditingName(false);
        widget.removeEditInput();
    }

    @SubscribeEvent
    public void onInventoryKeyPress(InventoryKeyPressEvent event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_ENTER) return;
        if (!Models.Bank.isEditingName()) return;

        Models.Bank.saveCurrentPageName(widget.getName());
        widget.removeEditInput();
    }

    @SubscribeEvent
    public void onScroll(MouseScrollEvent event) {
        if (!Models.Bank.isEditingName()) return;

        // Scrolling with ContainerScrollFeature doesn't call ContainerClickEvent so toggle editing here
        Models.Bank.toggleEditingName(false);
        widget.removeEditInput();
    }

    public void jumpToDestination(int destination) {
        quickJumping = true;
        pageDestination = destination;

        if (currentPage == pageDestination || pageDestination > lastPage) return;

        int pageDifference = pageDestination - currentPage;

        switch (pageDifference) {
            case 1 -> {
                if (currentPage != lastPage) {
                    clickNextPage();
                }
            }
            case -1 -> clickPreviousPage();
            default -> {
                if (!tryToQuickJump()) {
                    if (currentPage > pageDestination) {
                        clickPreviousPage();
                    } else if (currentPage != lastPage) {
                        clickNextPage();
                    }
                }
            }
        }
    }

    private boolean tryToQuickJump() {
        int target;

        if (storageContainer.getQuickJumpDestinations().contains(pageDestination)) {
            target = storageContainer.getQuickJumpDestinations().indexOf(pageDestination);
        } else {
            int closest = storageContainer.getQuickJumpDestinations().getFirst();

            for (int destination : storageContainer.getQuickJumpDestinations()) {
                if (Math.abs(pageDestination - destination) < Math.abs(pageDestination - closest)) {
                    closest = destination;
                }
            }

            target = storageContainer.getQuickJumpDestinations().indexOf(closest);
        }

        if (pageDestination >= currentPage
                && currentPage >= storageContainer.getQuickJumpDestinations().get(target)) {
            return false;
        }

        // Next page button is available to press
        if (currentPage != lastPage) {
            ItemStack nextButton = McUtils.containerMenu().getItems().get(storageContainer.getNextItemSlot());

            for (StyledText line : LoreUtils.getLore(nextButton)) {
                Matcher pageMatcher = line.getMatcher(PAGE_PATTERN);

                if (pageMatcher.matches()
                        && Integer.parseInt(pageMatcher.group(1))
                                == storageContainer.getQuickJumpDestinations().get(target)) {
                    ContainerUtils.pressKeyOnSlot(
                            storageContainer.getNextItemSlot(),
                            storageContainer.getContainerId(),
                            target,
                            McUtils.containerMenu().getItems());
                    return true;
                }
            }
        } else { // Use previous button
            ItemStack previousButton = McUtils.containerMenu().getItems().get(storageContainer.getPreviousItemSlot());

            for (StyledText line : LoreUtils.getLore(previousButton)) {
                Matcher pageMatcher = line.getMatcher(PAGE_PATTERN);

                if (pageMatcher.matches()
                        && Integer.parseInt(pageMatcher.group(1))
                                == storageContainer.getQuickJumpDestinations().get(target)) {
                    ContainerUtils.pressKeyOnSlot(
                            storageContainer.getPreviousItemSlot(),
                            storageContainer.getContainerId(),
                            target,
                            McUtils.containerMenu().getItems());
                    return true;
                }
            }
        }

        return false;
    }

    private void clickNextPage() {
        ContainerUtils.clickOnSlot(
                storageContainer.getNextItemSlot(),
                storageContainer.getContainerId(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                McUtils.containerMenu().getItems());
    }

    private void clickPreviousPage() {
        ContainerUtils.clickOnSlot(
                storageContainer.getPreviousItemSlot(),
                storageContainer.getContainerId(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                McUtils.containerMenu().getItems());
    }
}
