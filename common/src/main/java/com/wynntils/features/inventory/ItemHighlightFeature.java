/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.HotbarSlotRenderEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.WynnItemData;
import com.wynntils.models.items.items.game.EmeraldPouchItem;
import com.wynntils.models.items.items.game.IngredientItem;
import com.wynntils.models.items.items.game.MaterialItem;
import com.wynntils.models.items.items.game.PowderItem;
import com.wynntils.models.items.items.gui.CosmeticItem;
import com.wynntils.models.items.properties.GearTierItemProperty;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.INVENTORY)
public class ItemHighlightFeature extends Feature {
    @Persisted
    public final Config<Boolean> normalHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> normalHighlightColor = new Config<>(new CustomColor(255, 255, 255));

    @Persisted
    public final Config<Boolean> uniqueHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> uniqueHighlightColor = new Config<>(new CustomColor(255, 255, 0));

    @Persisted
    public final Config<Boolean> rareHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> rareHighlightColor = new Config<>(new CustomColor(255, 0, 255));

    @Persisted
    public final Config<Boolean> setHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> setHighlightColor = new Config<>(new CustomColor(0, 255, 0));

    @Persisted
    public final Config<Boolean> legendaryHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> legendaryHighlightColor = new Config<>(new CustomColor(0, 255, 255));

    @Persisted
    public final Config<Boolean> fabledHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> fabledHighlightColor = new Config<>(new CustomColor(255, 85, 85));

    @Persisted
    public final Config<Boolean> mythicHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> mythicHighlightColor = new Config<>(new CustomColor(76, 0, 76));

    @Persisted
    public final Config<Boolean> craftedHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> craftedHighlightColor = new Config<>(new CustomColor(0, 138, 138));

    @Persisted
    public final Config<Boolean> zeroStarIngredientHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> zeroStarIngredientHighlightColor = new Config<>(new CustomColor(102, 102, 102));

    @Persisted
    public final Config<Boolean> oneStarIngredientHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> oneStarIngredientHighlightColor = new Config<>(new CustomColor(255, 247, 153));

    @Persisted
    public final Config<Boolean> twoStarIngredientHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> twoStarIngredientHighlightColor = new Config<>(new CustomColor(255, 255, 0));

    @Persisted
    public final Config<Boolean> threeStarIngredientHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> threeStarIngredientHighlightColor = new Config<>(new CustomColor(230, 77, 0));

    @Persisted
    public final Config<Boolean> oneStarMaterialHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> oneStarMaterialHighlightColor = new Config<>(new CustomColor(255, 247, 153));

    @Persisted
    public final Config<Boolean> twoStarMaterialHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> twoStarMaterialHighlightColor = new Config<>(new CustomColor(255, 255, 0));

    @Persisted
    public final Config<Boolean> threeStarMaterialHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<CustomColor> threeStarMaterialHighlightColor = new Config<>(new CustomColor(230, 77, 0));

    @Persisted
    public final Config<Boolean> cosmeticHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> powderHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> emeraldPouchHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> inventoryHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<Float> inventoryOpacity = new Config<>(1f);

    @Persisted
    public final Config<Boolean> hotbarHighlightEnabled = new Config<>(true);

    @Persisted
    public final Config<Float> hotbarOpacity = new Config<>(.5f);

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderSlot(SlotRenderEvent.Pre e) {
        if (!inventoryHighlightEnabled.get()) return;

        CustomColor color = getHighlightColor(e.getSlot().getItem(), false);
        if (color == CustomColor.NONE) return;

        RenderSystem.enableDepthTest();
        RenderUtils.drawTexturedRectWithColor(
                e.getPoseStack(),
                Texture.HIGHLIGHT.resource(),
                color.withAlpha(inventoryOpacity.get()),
                e.getSlot().x - 1,
                e.getSlot().y - 1,
                200,
                18,
                18,
                Texture.HIGHLIGHT.width(),
                Texture.HIGHLIGHT.height());
        RenderSystem.disableDepthTest();
    }

    @SubscribeEvent
    public void onRenderHotbarSlot(HotbarSlotRenderEvent.Pre e) {
        if (!hotbarHighlightEnabled.get()) return;

        CustomColor color = getHighlightColor(e.getItemStack(), true);
        if (color == CustomColor.NONE) return;

        RenderUtils.drawRect(e.getPoseStack(), color.withAlpha(hotbarOpacity.get()), e.getX(), e.getY(), 0, 16, 16);
    }

    private CustomColor getHighlightColor(ItemStack itemStack, boolean hotbarHighlight) {
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(itemStack);
        if (wynnItemOpt.isEmpty()) return CustomColor.NONE;

        WynnItem wynnItem = wynnItemOpt.get();
        HighlightInfo highlight = wynnItem.getData()
                .getOrCalculate(WynnItemData.HIGHLIGHT_KEY, () -> calculateHighlightInfo(wynnItemOpt.get()));
        if (highlight == null) return CustomColor.NONE;

        if (!highlight.isHighlightEnabled()) return CustomColor.NONE;

        return highlight.getHighlightColor();
    }

    private HighlightInfo calculateHighlightInfo(WynnItem wynnItem) {
        if (wynnItem instanceof CosmeticItem cosmeticItem) {
            return new CosmeticHighlight(cosmeticItem);
        }
        if (wynnItem instanceof GearTierItemProperty gearItem) {
            return new GearHighlight(gearItem);
        }
        if (wynnItem instanceof IngredientItem ingredientItem) {
            return new IngredientHighlight(ingredientItem);
        }
        if (wynnItem instanceof MaterialItem materialItem) {
            return new MaterialHighlight(materialItem);
        }
        if (wynnItem instanceof PowderItem powderItem) {
            return new PowderHighlight(powderItem);
        }
        if (wynnItem instanceof EmeraldPouchItem emeraldPouchItem) {
            return new EmeraldPouchHighlight(emeraldPouchItem);
        }

        return null;
    }

    private interface HighlightInfo {
        CustomColor getHighlightColor();

        boolean isHighlightEnabled();
    }

    private final class CosmeticHighlight implements HighlightInfo {
        private final CosmeticItem item;

        private CosmeticHighlight(CosmeticItem item) {
            this.item = item;
        }

        @Override
        public boolean isHighlightEnabled() {
            return cosmeticHighlightEnabled.get();
        }

        @Override
        public CustomColor getHighlightColor() {
            return item.getHighlightColor();
        }
    }

    private final class GearHighlight implements HighlightInfo {
        private final GearTierItemProperty item;

        private GearHighlight(GearTierItemProperty item) {
            this.item = item;
        }

        @Override
        public boolean isHighlightEnabled() {
            return switch (item.getGearTier()) {
                case NORMAL -> normalHighlightEnabled.get();
                case UNIQUE -> uniqueHighlightEnabled.get();
                case RARE -> rareHighlightEnabled.get();
                case SET -> setHighlightEnabled.get();
                case LEGENDARY -> legendaryHighlightEnabled.get();
                case FABLED -> fabledHighlightEnabled.get();
                case MYTHIC -> mythicHighlightEnabled.get();
                case CRAFTED -> craftedHighlightEnabled.get();
            };
        }

        @Override
        public CustomColor getHighlightColor() {
            return switch (item.getGearTier()) {
                case NORMAL -> normalHighlightColor.get();
                case UNIQUE -> uniqueHighlightColor.get();
                case RARE -> rareHighlightColor.get();
                case SET -> setHighlightColor.get();
                case LEGENDARY -> legendaryHighlightColor.get();
                case FABLED -> fabledHighlightColor.get();
                case MYTHIC -> mythicHighlightColor.get();
                case CRAFTED -> craftedHighlightColor.get();
            };
        }
    }

    private final class IngredientHighlight implements HighlightInfo {
        private final IngredientItem item;

        private IngredientHighlight(IngredientItem item) {
            this.item = item;
        }

        @Override
        public CustomColor getHighlightColor() {
            return switch (item.getQualityTier()) {
                case 0 -> zeroStarIngredientHighlightColor.get();
                case 1 -> oneStarIngredientHighlightColor.get();
                case 2 -> twoStarIngredientHighlightColor.get();
                case 3 -> threeStarIngredientHighlightColor.get();
                default -> CustomColor.NONE;
            };
        }

        @Override
        public boolean isHighlightEnabled() {
            return switch (item.getQualityTier()) {
                case 0 -> zeroStarIngredientHighlightEnabled.get();
                case 1 -> oneStarIngredientHighlightEnabled.get();
                case 2 -> twoStarIngredientHighlightEnabled.get();
                case 3 -> threeStarIngredientHighlightEnabled.get();
                default -> false;
            };
        }
    }

    private final class MaterialHighlight implements HighlightInfo {
        private final MaterialItem item;

        private MaterialHighlight(MaterialItem item) {
            this.item = item;
        }

        @Override
        public CustomColor getHighlightColor() {
            return switch (item.getQualityTier()) {
                case 1 -> oneStarMaterialHighlightColor.get();
                case 2 -> twoStarMaterialHighlightColor.get();
                case 3 -> threeStarMaterialHighlightColor.get();
                default -> CustomColor.NONE;
            };
        }

        @Override
        public boolean isHighlightEnabled() {
            return switch (item.getQualityTier()) {
                case 1 -> oneStarMaterialHighlightEnabled.get();
                case 2 -> twoStarMaterialHighlightEnabled.get();
                case 3 -> threeStarMaterialHighlightEnabled.get();
                default -> false; // should not happen
            };
        }
    }

    private final class PowderHighlight implements HighlightInfo {
        private final PowderItem item;

        private PowderHighlight(PowderItem item) {
            this.item = item;
        }

        @Override
        public boolean isHighlightEnabled() {
            return powderHighlightEnabled.get();
        }

        @Override
        public CustomColor getHighlightColor() {
            return item.getPowderProfile().element().getColor();
        }
    }

    private final class EmeraldPouchHighlight implements HighlightInfo {
        private EmeraldPouchHighlight(EmeraldPouchItem item) {}

        @Override
        public boolean isHighlightEnabled() {
            return emeraldPouchHighlightEnabled.get();
        }

        @Override
        public CustomColor getHighlightColor() {
            return CustomColor.fromChatFormatting(ChatFormatting.GREEN);
        }
    }
}
