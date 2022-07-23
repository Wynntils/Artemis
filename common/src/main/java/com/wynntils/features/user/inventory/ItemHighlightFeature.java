/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.inventory;

import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.mc.event.HotbarSlotRenderEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.mc.render.RenderUtils;
import com.wynntils.mc.render.Texture;
import com.wynntils.utils.objects.CustomColor;
import com.wynntils.wc.custom.item.WynnItemStack;
import com.wynntils.wc.custom.item.properties.ItemProperty;
import com.wynntils.wc.custom.item.properties.type.HighlightProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(stability = Stability.STABLE, category = "Inventory")
public class ItemHighlightFeature extends UserFeature {

    @Config
    public static boolean normalHighlightEnabled = true;

    @Config
    public static CustomColor normalHighlightColor = new CustomColor(255, 255, 255);

    @Config
    public static boolean uniqueHighlightEnabled = true;

    @Config
    public static CustomColor uniqueHighlightColor = new CustomColor(255, 255, 0);

    @Config
    public static boolean rareHighlightEnabled = true;

    @Config
    public static CustomColor rareHighlightColor = new CustomColor(255, 0, 255);

    @Config
    public static boolean setHighlightEnabled = true;

    @Config
    public static CustomColor setHighlightColor = new CustomColor(0, 255, 0);

    @Config
    public static boolean legendaryHighlightEnabled = true;

    @Config
    public static CustomColor legendaryHighlightColor = new CustomColor(0, 255, 255);

    @Config
    public static boolean fabledHighlightEnabled = true;

    @Config
    public static CustomColor fabledHighlightColor = new CustomColor(255, 85, 85);

    @Config
    public static boolean mythicHighlightEnabled = true;

    @Config
    public static CustomColor mythicHighlightColor = new CustomColor(76, 0, 76);

    @Config
    public static boolean craftedHighlightEnabled = true;

    @Config
    public static CustomColor craftedHighlightColor = new CustomColor(0, 138, 138);

    @Config
    public static boolean cosmeticHighlightEnabled = true;

    @Config
    public static boolean powderHighlightEnabled = true;

    @Config
    public static boolean inventoryHighlightEnabled = true;

    @Config
    public static float inventoryOpacity = 1f;

    @Config
    public static boolean hotbarHighlightEnabled = true;

    @Config
    public static float hotbarOpacity = .5f;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderSlot(SlotRenderEvent.Pre e) {
        if (!inventoryHighlightEnabled) return;

        CustomColor color = getHighlightColor(e.getSlot().getItem(), false);
        if (color == CustomColor.NONE) return;

        RenderUtils.drawTexturedRectWithColor(
                Texture.HIGHLIGHT.resource(),
                color.withAlpha(inventoryOpacity),
                e.getSlot().x - 1,
                e.getSlot().y - 1,
                200,
                18,
                18,
                Texture.HIGHLIGHT.width(),
                Texture.HIGHLIGHT.height());
    }

    @SubscribeEvent
    public void onRenderHotbarSlot(HotbarSlotRenderEvent.Pre e) {
        if (!hotbarHighlightEnabled) return;

        CustomColor color = getHighlightColor(e.getStack(), true);
        if (color == CustomColor.NONE) return;

        RenderUtils.drawRect(color.withAlpha(hotbarOpacity), e.getX(), e.getY(), 0, 16, 16);
    }

    private CustomColor getHighlightColor(ItemStack item, boolean hotbarHighlight) {
        if (!(item instanceof WynnItemStack wynnItem)) return CustomColor.NONE;

        if (!wynnItem.hasProperty(ItemProperty.HIGHLIGHT)) return CustomColor.NONE;
        HighlightProperty highlight = wynnItem.getProperty(ItemProperty.HIGHLIGHT);

        boolean contextEnabled = hotbarHighlight ? highlight.isHotbarHighlight() : highlight.isInventoryHighlight();
        if (!highlight.isHighlightEnabled() || !contextEnabled) return CustomColor.NONE;

        return highlight.getHighlightColor();
    }
}
