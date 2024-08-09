/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ContainerRenderEvent;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.models.containers.containers.ScrapMenuContainer;
import com.wynntils.models.containers.type.SearchableContainerProperty;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.INVENTORY)
public class HightlightDuplicateCosmeticsFeature extends Feature {
    @Persisted
    public final Config<HighlightCondition> highlightCondition = new Config<>(HighlightCondition.BOTH);

    @Persisted
    public final Config<CustomColor> hoveredHighlightColor = new Config<>(CommonColors.BLUE);

    @Persisted
    public final Config<CustomColor> selectedHighlightColor = new Config<>(CommonColors.ORANGE);

    private static final Component ADD_REWARD_TEXT = Component.literal("§7Click on a reward to add it");
    // The colored glass panes on the new reward screen
    private static final Component DECORATIVE_ITEM_NAMES = Component.literal("§f");
    private static final Component RETURN_TEXT = Component.literal("§7Return to Scrap Menu");
    private static final int FINAL_SLOT = 53;
    private static final int RETURN_SLOT = 18;
    private static final List<Integer> SELECTED_COSMETIC_SLOTS = List.of(1, 2, 3, 4, 5);

    private Component hoveredCosmetic;
    private Set<Component> selectedCosmetics = new HashSet<>();
    private SearchableContainerProperty scrapMenu = null;

    @SubscribeEvent
    public void onScreenInit(ScreenInitEvent event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!(screen.getMenu() instanceof ChestMenu)) return;

        if (Models.Container.getCurrentContainer() instanceof ScrapMenuContainer scrapMenuContainer) {
            scrapMenu = scrapMenuContainer;
        }
    }

    @SubscribeEvent
    public void onScreenClose(ScreenClosedEvent e) {
        selectedCosmetics = new HashSet<>();
        hoveredCosmetic = null;
        scrapMenu = null;
    }

    @SubscribeEvent
    public void onContainerRender(ContainerRenderEvent event) {
        if (scrapMenu == null) return;

        // No need to get hovered cosmetic if only displaying selected cosmetics
        if (highlightCondition.get() == HighlightCondition.SELECTED) return;

        Slot hoveredSlot = event.getHoveredSlot();
        Component finalItem =
                event.getScreen().getMenu().slots.get(FINAL_SLOT).getItem().getHoverName();

        // If not hovering a cosmetic item
        if (hoveredSlot == null
                || hoveredSlot.getItem().getItem() == Items.AIR
                || finalItem.equals(DECORATIVE_ITEM_NAMES)) {
            hoveredCosmetic = null;
            return;
        }

        hoveredCosmetic = hoveredSlot.getItem().getHoverName();
    }

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Pre e) {
        if (scrapMenu == null) return;

        // Don't highlight the cosmetics in the selected slots
        if (scrapMenu.getBounds().getSlots().contains(e.getSlot().index)) {
            Component hoverName = e.getSlot().getItem().getHoverName();
            boolean isSelected = selectedCosmetics.contains(hoverName);
            boolean isHovered = hoverName.equals(hoveredCosmetic);
            HighlightCondition condition = highlightCondition.get();

            if ((isSelected && condition != HighlightCondition.HOVER)
                    || (isHovered && condition != HighlightCondition.SELECTED)) {
                CustomColor color = isSelected ? selectedHighlightColor.get() : hoveredHighlightColor.get();
                RenderUtils.drawArc(e.getPoseStack(), color, e.getSlot().x, e.getSlot().y, 200, 1f, 6, 8);
            }
        }
    }

    @SubscribeEvent
    public void onContainerSetContent(ContainerSetContentEvent.Post event) {
        if (scrapMenu == null) return;

        // Silverbull subscribers have the first 2 slots filled automatically
        List<Integer> selectedSlots = Models.Character.isSilverbullSubscriber()
                ? SELECTED_COSMETIC_SLOTS.subList(2, 5)
                : SELECTED_COSMETIC_SLOTS;

        selectedCosmetics = new HashSet<>();

        // Cosmetics have been scrapped and new cosmetic is being displayed, clear selected
        if (event.getItems().get(RETURN_SLOT).getHoverName().equals(RETURN_TEXT)) {
            return;
        }

        // Update the selected cosmetics
        for (int slot : selectedSlots) {
            Component selectedCosmetic = event.getItems().get(slot).getHoverName();

            if (selectedCosmetic.equals(ADD_REWARD_TEXT)) {
                // If the first slot does not have a cosmetic then none are selected
                if (slot == selectedSlots.getFirst()) {
                    selectedCosmetics = new HashSet<>();
                }

                break;
            }

            selectedCosmetics.add(selectedCosmetic);
        }
    }

    private enum HighlightCondition {
        HOVER,
        SELECTED,
        BOTH
    }
}
