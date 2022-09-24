/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigManager;
import com.wynntils.core.config.TypeOverride;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.ContainerRenderEvent;
import com.wynntils.mc.event.DropHeldItemEvent;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wynn.model.CharacterManager;
import com.wynntils.wynn.utils.WynnUtils;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class ItemLockFeature extends UserFeature {
    @RegisterKeyBind
    private final KeyBind lockSlotKeyBind =
            new KeyBind("Lock Slot", GLFW.GLFW_KEY_H, true, null, ItemLockFeature::tryChangeLockStateOnHoveredSlot);

    @Config(visible = false)
    private static Map<Integer, Set<Integer>> classSlotLockMap = new HashMap<>();

    @TypeOverride
    private static final Type classSlotLockMapType = new TypeToken<HashMap<Integer, Set<Integer>>>() {}.getType();

    @Config
    public static boolean blockAllActionsOnLockedItems = false;

    @SubscribeEvent
    public void onContainerRender(ContainerRenderEvent event) {
        AbstractContainerScreen<?> abstractContainerScreen = event.getScreen();

        CharacterManager.CharacterInfo characterInfo = WynnUtils.getCharacterInfo();

        if (characterInfo == null) return;

        for (Integer slotId : classSlotLockMap.getOrDefault(characterInfo.getId(), Set.of())) {
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
        if (!(McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen)) return;
        if (!blockAllActionsOnLockedItems && event.getClickType() != ClickType.THROW) return;

        CharacterManager.CharacterInfo characterInfo = WynnUtils.getCharacterInfo();

        if (characterInfo == null) return;

        Optional<Slot> slotOptional = abstractContainerScreen.getMenu().slots.stream()
                .filter(slot -> slot.container instanceof Inventory && slot.getItem() == event.getItemStack())
                .findFirst();

        if (slotOptional.isEmpty()) {
            return;
        }

        if (classSlotLockMap
                .getOrDefault(characterInfo.getId(), Set.of())
                .contains(slotOptional.get().getContainerSlot())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrop(DropHeldItemEvent event) {
        CharacterManager.CharacterInfo characterInfo = WynnUtils.getCharacterInfo();

        if (characterInfo == null) return;
        ItemStack selected = McUtils.inventory().getSelected();
        Optional<Slot> heldItemSlot = McUtils.inventoryMenu().slots.stream()
                .filter(slot -> slot.getItem() == selected)
                .findFirst();
        if (heldItemSlot.isEmpty()) return;

        if (classSlotLockMap
                .getOrDefault(characterInfo.getId(), Set.of())
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

    private static void tryChangeLockStateOnHoveredSlot(Slot hoveredSlot) {
        if (hoveredSlot == null || !(hoveredSlot.container instanceof Inventory)) return;

        CharacterManager.CharacterInfo characterInfo = WynnUtils.getCharacterInfo();
        if (characterInfo == null) return;

        classSlotLockMap.putIfAbsent(characterInfo.getId(), new HashSet<>());

        Set<Integer> classSet = classSlotLockMap.get(characterInfo.getId());

        if (classSet.contains(hoveredSlot.getContainerSlot())) {
            classSet.remove(hoveredSlot.getContainerSlot());
        } else {
            classSet.add(hoveredSlot.getContainerSlot());
        }

        ConfigManager.saveConfig();
    }
}
