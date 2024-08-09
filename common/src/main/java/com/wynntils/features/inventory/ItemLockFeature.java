/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.config.HiddenConfig;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.ContainerRenderEvent;
import com.wynntils.mc.event.DropHeldItemEvent;
import com.wynntils.models.containers.type.FullscreenContainerProperty;
import com.wynntils.models.items.items.game.MultiHealthPotionItem;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.INVENTORY)
public class ItemLockFeature extends Feature {
    @RegisterKeyBind
    private final KeyBind lockSlotKeyBind =
            new KeyBind("Lock Slot", GLFW.GLFW_KEY_H, true, null, this::tryChangeLockStateOnHoveredSlot);

    @Persisted
    public final HiddenConfig<Map<String, Set<Integer>>> classSlotLockMap = new HiddenConfig<>(new TreeMap<>());

    @Persisted
    public final Config<Boolean> blockAllActionsOnLockedItems = new Config<>(false);

    @Persisted
    public final Config<Boolean> allowClickOnEmeraldPouchInBlockingMode = new Config<>(true);

    @Persisted
    public final Config<Boolean> allowClickOnMultiHealthPotionsInBlockingMode = new Config<>(true);

    @SubscribeEvent
    public void onContainerRender(ContainerRenderEvent event) {
        AbstractContainerScreen<?> abstractContainerScreen = event.getScreen();

        // Don't render lock on full screen containers
        if (Models.Container.getCurrentContainer() instanceof FullscreenContainerProperty) return;

        for (Integer slotId : classSlotLockMap.get().getOrDefault(Models.Character.getId(), new TreeSet<>())) {
            Optional<Slot> lockedSlot = abstractContainerScreen.getMenu().slots.stream()
                    .filter(slot -> slot.container instanceof Inventory && slot.getContainerSlot() == slotId)
                    .findFirst();

            if (lockedSlot.isEmpty()) {
                continue;
            }

            renderLockedSlot(event.getPoseStack(), abstractContainerScreen, lockedSlot.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInventoryClickEvent(ContainerClickEvent event) {
        // Don't lock fullscreen container slots
        if (!(McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen)
                || Models.Container.getCurrentContainer() instanceof FullscreenContainerProperty) return;
        if (!blockAllActionsOnLockedItems.get() && event.getClickType() != ClickType.THROW) return;

        // We have to match slot.index here, because the event slot number is an index as well
        Optional<Slot> slotOptional = abstractContainerScreen.getMenu().slots.stream()
                .filter(slot -> slot.container instanceof Inventory && slot.index == event.getSlotNum())
                .findFirst();

        if (slotOptional.isEmpty()) {
            return;
        }

        // We want to allow opening emerald pouches and deleting potions even if locked
        // Right click is used to perform these actions, left click picks them up
        // So only allow right click actions
        if (event.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (allowClickOnEmeraldPouchInBlockingMode.get()
                    && Models.Emerald.isEmeraldPouch(slotOptional.get().getItem())) {
                return;
            }

            if (allowClickOnMultiHealthPotionsInBlockingMode.get()
                    && Models.Item.asWynnItem(slotOptional.get().getItem(), MultiHealthPotionItem.class)
                            .isPresent()) {
                return;
            }
        }

        if (classSlotLockMap
                .get()
                .getOrDefault(Models.Character.getId(), new TreeSet<>())
                .contains(slotOptional.get().getContainerSlot())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrop(DropHeldItemEvent event) {
        ItemStack selected = McUtils.inventory().getSelected();
        Optional<Slot> heldItemSlot = McUtils.inventoryMenu().slots.stream()
                .filter(slot -> slot.getItem() == selected)
                .findFirst();
        if (heldItemSlot.isEmpty()) return;

        if (classSlotLockMap
                .get()
                .getOrDefault(Models.Character.getId(), new TreeSet<>())
                .contains(heldItemSlot.get().getContainerSlot())) {
            event.setCanceled(true);
        }
    }

    private void renderLockedSlot(PoseStack poseStack, AbstractContainerScreen<?> containerScreen, Slot lockedSlot) {
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.ITEM_LOCK.resource(),
                ((containerScreen.leftPos + lockedSlot.x)) + 12,
                ((containerScreen.topPos + lockedSlot.y)) - 4,
                400,
                8,
                8,
                Texture.ITEM_LOCK.width() / 2,
                Texture.ITEM_LOCK.height() / 2);
    }

    private void tryChangeLockStateOnHoveredSlot(Slot hoveredSlot) {
        if (hoveredSlot == null || !(hoveredSlot.container instanceof Inventory)) return;

        classSlotLockMap.get().putIfAbsent(Models.Character.getId(), new TreeSet<>());

        Set<Integer> classSet = classSlotLockMap.get().get(Models.Character.getId());

        if (classSet.contains(hoveredSlot.getContainerSlot())) {
            classSet.remove(hoveredSlot.getContainerSlot());
        } else {
            classSet.add(hoveredSlot.getContainerSlot());
        }

        classSlotLockMap.touched();
    }
}
