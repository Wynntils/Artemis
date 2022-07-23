/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.inventory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.mc.event.HotbarSlotRenderEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.mc.render.FontRenderer;
import com.wynntils.wc.custom.item.WynnItemStack;
import com.wynntils.wc.custom.item.properties.ItemProperty;
import com.wynntils.wc.custom.item.properties.type.TextOverlayProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(category = "Inventory")
public class ItemTextOverlayFeature extends UserFeature {

    @Config
    public static boolean powderTierEnabled = true;

    @Config
    public static boolean powderTierRomanNumerals = true;

    @Config
    public static FontRenderer.TextShadow powderTierShadow = FontRenderer.TextShadow.OUTLINE;

    @Config
    public static boolean teleportScrollEnabled = true;

    @Config
    public static FontRenderer.TextShadow teleportScrollShadow = FontRenderer.TextShadow.OUTLINE;

    @Config
    public static boolean dungeonKeyEnabled = true;

    @Config
    public static FontRenderer.TextShadow dungeonKeyShadow = FontRenderer.TextShadow.OUTLINE;

    @Config
    public static boolean amplifierTierEnabled = true;

    @Config
    public static boolean amplifierTierRomanNumerals = true;

    @Config
    public static FontRenderer.TextShadow amplifierTierShadow = FontRenderer.TextShadow.OUTLINE;

    @Config
    public static boolean consumableChargeEnabled = true;

    @Config
    public static FontRenderer.TextShadow consumableChargeShadow = FontRenderer.TextShadow.NORMAL;

    @Config
    public static boolean inventoryTextOverlayEnabled = true;

    @Config
    public static boolean hotbarTextOverlayEnabled = true;

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Post e) {
        if (!inventoryTextOverlayEnabled) return;

        drawTextOverlay(e.getSlot().getItem(), e.getSlot().x, e.getSlot().y, false);
    }

    @SubscribeEvent
    public void onRenderHotbarSlot(HotbarSlotRenderEvent.Post e) {
        if (!hotbarTextOverlayEnabled) return;

        drawTextOverlay(e.getStack(), e.getX(), e.getY(), true);
    }

    public void drawTextOverlay(ItemStack item, int slotX, int slotY, boolean hotbar) {
        if (!(item instanceof WynnItemStack wynnItem)) return;
        if (!wynnItem.hasProperty(ItemProperty.TEXT_OVERLAY)) return;

        for (TextOverlayProperty overlayProperty : wynnItem.getProperties(ItemProperty.TEXT_OVERLAY)) {
            boolean contextEnabled = hotbar ? overlayProperty.isHotbarText() : overlayProperty.isInventoryText();
            if (!overlayProperty.isTextOverlayEnabled() || !contextEnabled) continue; // not enabled or wrong context

            TextOverlayProperty.TextOverlay textOverlay = overlayProperty.getTextOverlay();

            PoseStack poseStack = new PoseStack();
            poseStack.translate(0, 0, 300); // items are drawn at z300, so text has to be as well
            poseStack.scale(textOverlay.scale(), textOverlay.scale(), 1f);
            float x = (slotX + textOverlay.xOffset()) / textOverlay.scale();
            float y = (slotY + textOverlay.yOffset()) / textOverlay.scale();
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            textOverlay.text(),
                            x,
                            y,
                            textOverlay.color(),
                            FontRenderer.TextAlignment.LEFT_ALIGNED,
                            textOverlay.shadow());
        }
    }
}
