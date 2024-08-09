/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.inventory;

import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.SetSlotEvent;
import com.wynntils.models.inventory.type.InventoryAccessory;
import com.wynntils.models.items.items.gui.IngredientPouchItem;
import com.wynntils.models.items.properties.RequirementItemProperty;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.wynn.InventoryUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

public final class InventoryModel extends Model {
    private static final String EMPTY_ACCESSORY_SLOT = "§7Accessory Slot";
    private static final int MAX_INVENTORY_SLOTS = 28;
    private static final int MAX_INGREDIENT_POUCH_SLOTS = 27;

    private final InventoryWatcher emptySlotWatcher = new InventoryWatcher(ItemStack::isEmpty);
    private final List<InventoryWatcher> watchers = new ArrayList<>(List.of(emptySlotWatcher));

    public InventoryModel() {
        super(List.of());
    }

    public CappedValue getInventorySlots() {
        return new CappedValue(MAX_INVENTORY_SLOTS - getEmptySlots(), MAX_INVENTORY_SLOTS);
    }

    public CappedValue getIngredientPouchSlots() {
        return new CappedValue(getUsedIngredientPouchSlots(), MAX_INGREDIENT_POUCH_SLOTS);
    }

    public void registerWatcher(InventoryWatcher watcher) {
        watchers.add(watcher);
        updateCache();
    }

    public void unregisterWatcher(InventoryWatcher watcher) {
        watchers.remove(watcher);
    }

    /**
     * @return List of all equipped armor, accessories, and held item that meets requirements
     */
    public List<ItemStack> getEquippedItems() {
        List<ItemStack> returnable = new ArrayList<>(McUtils.inventory().armor);
        Collections.reverse(returnable); // Reverse so that helmet is first

        for (int i : InventoryAccessory.getSlots()) {
            int baseSize = 0;
            if (McUtils.player().hasContainerOpen()) {
                // Scale according to server chest size
                // Eg. 3 row chest size = 27 (ends on i=26 since 0-index), we would get accessory slots {27, 28, 29, 30}
                baseSize = McUtils.player().containerMenu.getItems().size();
            }
            ItemStack accessory = McUtils.inventory().getItem(i + baseSize);
            if (accessory.getHoverName().getString().equals(EMPTY_ACCESSORY_SLOT)) continue;
            returnable.add(McUtils.inventory().getItem(i + baseSize));
        }

        Optional<RequirementItemProperty> wynnItem = Models.Item.asWynnItemProperty(
                McUtils.player().getItemInHand(InteractionHand.MAIN_HAND), RequirementItemProperty.class);
        if (wynnItem.isPresent() && wynnItem.get().meetsActualRequirements()) {
            returnable.add(McUtils.player().getItemInHand(InteractionHand.MAIN_HAND));
        }

        return returnable.stream().filter(itemStack -> !itemStack.isEmpty()).toList();
    }

    @SubscribeEvent
    public void onWorldChange(WorldStateEvent e) {
        if (e.getNewState() == WorldState.WORLD) {
            updateCache();
        } else {
            resetCache();
        }
    }

    @SubscribeEvent
    public void onContainerSetEvent(ContainerSetContentEvent.Post e) {
        // Only update if the container is the player inventory
        if (e.getContainerId() == McUtils.inventoryMenu().containerId) {
            updateCache();
        }
    }

    @SubscribeEvent
    public void onSlotSetEvent(SetSlotEvent.Post e) {
        // Only update if the container is the player inventory
        if (Objects.equals(e.getContainer(), McUtils.inventory())) {
            updateCache();
        }
    }

    private int getEmptySlots() {
        return emptySlotWatcher.getSlots();
    }

    private int getUsedIngredientPouchSlots() {
        ItemStack itemStack = McUtils.inventory().items.get(InventoryUtils.INGREDIENT_POUCH_SLOT_NUM);
        Optional<IngredientPouchItem> pouchItemOpt = Models.Item.asWynnItem(itemStack, IngredientPouchItem.class);

        // This should never happen
        if (pouchItemOpt.isEmpty()) return -1;

        return pouchItemOpt.get().getIngredients().size();
    }

    private void updateCache() {
        Inventory inventory = McUtils.inventory();
        if (inventory == null) return;

        watchers.forEach(watcher -> {
            int slots = 0;
            int totalCount = 0;

            for (ItemStack itemStack : inventory.items) {
                if (watcher.shouldInclude(itemStack)) {
                    slots++;
                    totalCount += itemStack.getCount();
                }
            }

            if (slots != watcher.getSlots() || totalCount != watcher.getTotalCount()) {
                watcher.updateFromModel(slots, totalCount);
            }
        });
    }

    private void resetCache() {
        watchers.forEach(watcher -> watcher.updateFromModel(0, 0));
    }
}
