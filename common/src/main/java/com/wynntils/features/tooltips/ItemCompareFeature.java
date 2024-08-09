/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.tooltips;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.properties.GearTypeItemProperty;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.TOOLTIPS)
public class ItemCompareFeature extends Feature {
    @RegisterKeyBind
    private final KeyBind toggleCompareModeKeyBind =
            new KeyBind("Compare mode", GLFW.GLFW_KEY_KP_EQUAL, true, null, this::onCompareModeKeyPress);

    @RegisterKeyBind
    private final KeyBind compareSelectKeyBind =
            new KeyBind("Select for comparing", GLFW.GLFW_KEY_C, true, null, this::onSelectKeyPress);

    private ItemStack comparedItem = null;
    private boolean compareToEquipped = false;

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        comparedItem = null;
        compareToEquipped = false;
    }

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Pre event) {
        Slot slot = event.getSlot();
        if (slot.getItem() == comparedItem) {
            RenderUtils.drawArc(event.getPoseStack(), CommonColors.LIGHT_BLUE, slot.x, slot.y, 200, 1, 6, 8);
        }
    }

    @SubscribeEvent
    public void onItemTooltipRenderEvent(ItemTooltipRenderEvent.Post event) {
        if (McUtils.mc().screen == null
                || !(McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen)) return;

        if (abstractContainerScreen.hoveredSlot == null) {
            return;
        }

        ItemStack hoveredItemStack = abstractContainerScreen.hoveredSlot.getItem();

        if (event.getItemStack() != hoveredItemStack) {
            return;
        }

        Optional<GearTypeItemProperty> gearItemOpt =
                Models.Item.asWynnItemProperty(hoveredItemStack, GearTypeItemProperty.class);
        if (gearItemOpt.isEmpty()) return;

        ItemStack itemToCompare = null;

        // No compared item selected, try compare to equipped armor
        if (compareToEquipped) {
            List<ItemStack> armorSlots = McUtils.inventory().armor;

            Optional<ItemStack> matchingArmorItemStack = armorSlots.stream()
                    .filter(itemStack -> isMatchingType(itemStack, gearItemOpt.get()))
                    .findFirst();

            itemToCompare = matchingArmorItemStack.orElse(null);
        } else if (comparedItem != null) {
            itemToCompare = comparedItem;
        }

        if (itemToCompare == null) return;

        if (itemToCompare == hoveredItemStack) {
            return;
        }

        final PoseStack poseStack = new PoseStack();
        final int mouseX = event.getMouseX();
        final int mouseY = event.getMouseY();

        poseStack.pushPose();

        poseStack.translate(0, 0, 300);

        int toBeRenderedWidth = abstractContainerScreen.getTooltipFromItem(McUtils.mc(), itemToCompare).stream()
                .map(component -> McUtils.mc().font.width(component))
                .max(Integer::compareTo)
                .orElse(0);

        int hoveredWidth = abstractContainerScreen.getTooltipFromItem(McUtils.mc(), hoveredItemStack).stream()
                .map(component -> McUtils.mc().font.width(component))
                .max(Integer::compareTo)
                .orElse(0);

        GuiGraphics guiGraphics = event.getGuiGraphics();
        if (mouseX + toBeRenderedWidth + hoveredWidth > abstractContainerScreen.width) {
            guiGraphics.renderTooltip(
                    FontRenderer.getInstance().getFont(), itemToCompare, mouseX - toBeRenderedWidth - 10, mouseY);
        } else {
            guiGraphics.renderTooltip(
                    FontRenderer.getInstance().getFont(), itemToCompare, mouseX + hoveredWidth + 10, mouseY);
        }

        poseStack.popPose();
    }

    private boolean isMatchingType(ItemStack itemStack, GearTypeItemProperty gearItemReference) {
        Optional<GearTypeItemProperty> gearOpt = Models.Item.asWynnItemProperty(itemStack, GearTypeItemProperty.class);
        if (gearOpt.isEmpty()) return false;

        return gearOpt.get().getGearType() == gearItemReference.getGearType();
    }

    @SubscribeEvent
    public void onScreenClose(ScreenClosedEvent event) {
        compareToEquipped = false;
    }

    private void onCompareModeKeyPress(Slot hoveredSlot) {
        if (comparedItem != null) {
            comparedItem = null;
            compareToEquipped = true;
        } else {
            compareToEquipped = !compareToEquipped;
        }
    }

    private void onSelectKeyPress(Slot hoveredSlot) {
        if (hoveredSlot == null) return;

        ItemStack itemStack = hoveredSlot.getItem();
        Optional<GearItem> wynnItemOpt = Models.Item.asWynnItem(itemStack, GearItem.class);
        if (wynnItemOpt.isEmpty()) return;

        if (comparedItem == itemStack) {
            comparedItem = null;
        } else {
            comparedItem = itemStack;
            compareToEquipped = false;
        }
    }
}
