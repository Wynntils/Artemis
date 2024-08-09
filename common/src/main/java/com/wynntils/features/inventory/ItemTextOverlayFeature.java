/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.HotbarSlotRenderEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.models.dungeon.type.Dungeon;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.WynnItemData;
import com.wynntils.models.items.items.game.AmplifierItem;
import com.wynntils.models.items.items.game.AspectItem;
import com.wynntils.models.items.items.game.DungeonKeyItem;
import com.wynntils.models.items.items.game.EmeraldPouchItem;
import com.wynntils.models.items.items.game.GatheringToolItem;
import com.wynntils.models.items.items.game.HorseItem;
import com.wynntils.models.items.items.game.PotionItem;
import com.wynntils.models.items.items.game.PowderItem;
import com.wynntils.models.items.items.game.TeleportScrollItem;
import com.wynntils.models.items.items.gui.SeaskipperDestinationItem;
import com.wynntils.models.items.items.gui.SkillPointItem;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.type.TextShadow;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.INVENTORY)
public class ItemTextOverlayFeature extends Feature {
    @Persisted
    public final Config<Boolean> amplifierTierEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> amplifierTierRomanNumerals = new Config<>(true);

    @Persisted
    public final Config<TextShadow> amplifierTierShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> aspectEnabled = new Config<>(true);

    @Persisted
    public final Config<TextShadow> aspectShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> dungeonKeyEnabled = new Config<>(true);

    @Persisted
    public final Config<TextShadow> dungeonKeyShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> emeraldPouchTierEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> emeraldPouchTierRomanNumerals = new Config<>(true);

    @Persisted
    public final Config<TextShadow> emeraldPouchTierShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> gatheringToolTierEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> gatheringToolTierRomanNumerals = new Config<>(true);

    @Persisted
    public final Config<TextShadow> gatheringToolTierShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> horseTierEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> horseTierRomanNumerals = new Config<>(true);

    @Persisted
    public final Config<TextShadow> horseTierShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> hotbarTextOverlayEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> inventoryTextOverlayEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> powderTierEnabled = new Config<>(true);

    @Persisted
    public final Config<Boolean> powderTierRomanNumerals = new Config<>(true);

    @Persisted
    public final Config<TextShadow> powderTierShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> skillIconEnabled = new Config<>(true);

    @Persisted
    public final Config<TextShadow> skillIconShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> teleportScrollEnabled = new Config<>(true);

    @Persisted
    public final Config<TextShadow> teleportScrollShadow = new Config<>(TextShadow.OUTLINE);

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Post e) {
        if (!inventoryTextOverlayEnabled.get()) return;

        drawTextOverlay(e.getPoseStack(), e.getSlot().getItem(), e.getSlot().x, e.getSlot().y, false);
    }

    @SubscribeEvent
    public void onRenderHotbarSlot(HotbarSlotRenderEvent.Post e) {
        if (!hotbarTextOverlayEnabled.get()) return;

        drawTextOverlay(e.getPoseStack(), e.getItemStack(), e.getX(), e.getY(), true);
    }

    private void drawTextOverlay(PoseStack poseStack, ItemStack itemStack, int slotX, int slotY, boolean hotbar) {
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(itemStack);
        if (wynnItemOpt.isEmpty()) return;

        WynnItem wynnItem = wynnItemOpt.get();
        TextOverlayInfo overlayProperty =
                wynnItem.getData().getOrCalculate(WynnItemData.OVERLAY_KEY, () -> calculateOverlay(wynnItem));
        if (overlayProperty == null) return;

        if (!overlayProperty.isTextOverlayEnabled()) return;

        TextOverlay textOverlay = overlayProperty.getTextOverlay();
        if (textOverlay == null) {
            WynntilsMod.error(overlayProperty + "'s textOverlay was null.");
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0, 0, 300); // items are drawn at z300, so text has to be as well
        poseStack.scale(textOverlay.scale(), textOverlay.scale(), 1f);
        float x = (slotX + textOverlay.xOffset()) / textOverlay.scale();
        float y = (slotY + textOverlay.yOffset()) / textOverlay.scale();
        FontRenderer.getInstance().renderText(poseStack, x, y, textOverlay.task());
        poseStack.popPose();
    }

    private TextOverlayInfo calculateOverlay(WynnItem wynnItem) {
        if (wynnItem instanceof AmplifierItem amplifierItem) {
            return new AmplifierOverlay(amplifierItem);
        }
        if (wynnItem instanceof AspectItem aspectItem) {
            return new AspectOverlay(aspectItem);
        }
        if (wynnItem instanceof DungeonKeyItem dungeonKeyItem) {
            return new DungeonKeyOverlay(dungeonKeyItem);
        }
        if (wynnItem instanceof EmeraldPouchItem emeraldPouchItem) {
            return new EmeraldPouchOverlay(emeraldPouchItem);
        }
        if (wynnItem instanceof GatheringToolItem gatheringToolItem) {
            return new GatheringToolOverlay(gatheringToolItem);
        }
        if (wynnItem instanceof HorseItem horseItem) {
            return new HorseOverlay(horseItem);
        }
        if (wynnItem instanceof PowderItem powderItem) {
            return new PowderOverlay(powderItem);
        }
        if (wynnItem instanceof SeaskipperDestinationItem seaskipperDestinationItem) {
            return new SeaskipperDestinationOverlay(seaskipperDestinationItem);
        }
        if (wynnItem instanceof SkillPointItem skillPointItem) {
            return new SkillPointOverlay(skillPointItem);
        }
        if (wynnItem instanceof PotionItem potionItem && potionItem.getType().getSkill() != null) {
            return new SkillPotionOverlay(potionItem);
        }
        if (wynnItem instanceof TeleportScrollItem teleportScrollItem) {
            return new TeleportScrollOverlay(teleportScrollItem);
        }

        return null;
    }

    private String valueToString(int value, boolean useRomanNumerals) {
        return useRomanNumerals ? MathUtils.toRoman(value) : String.valueOf(value);
    }

    private interface TextOverlayInfo {
        TextOverlay getTextOverlay();

        boolean isTextOverlayEnabled();
    }

    private final class AspectOverlay implements TextOverlayInfo {
        private static final CustomColor TIER_1_HIGHLIGHT_COLOR =
                CustomColor.fromChatFormatting(ChatFormatting.DARK_GRAY);
        private static final CustomColor TIER_2_HIGHLIGHT_COLOR = new CustomColor(205, 127, 50);
        private static final CustomColor TIER_3_HIGHLIGHT_COLOR = new CustomColor(192, 192, 192);
        private static final CustomColor TIER_4_HIGHLIGHT_COLOR = new CustomColor(255, 215, 0);

        private final AspectItem item;

        private AspectOverlay(AspectItem item) {
            this.item = item;
        }

        @Override
        public TextOverlay getTextOverlay() {
            CustomColor highlightColor =
                    switch (item.getAspectTier()) {
                        case 2 -> TIER_2_HIGHLIGHT_COLOR;
                        case 3 -> TIER_3_HIGHLIGHT_COLOR;
                        case 4 -> TIER_4_HIGHLIGHT_COLOR;
                        default -> TIER_1_HIGHLIGHT_COLOR;
                    };

            TextRenderSetting style =
                    TextRenderSetting.DEFAULT.withCustomColor(highlightColor).withTextShadow(aspectShadow.get());

            return new TextOverlay(
                    new TextRenderTask(item.getClassType().getName().substring(0, 2), style), -1, 1, 0.75f);
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return aspectEnabled.get();
        }
    }

    private final class AmplifierOverlay implements TextOverlayInfo {
        private static final CustomColor HIGHLIGHT_COLOR = new CustomColor(0, 255, 255);

        private final AmplifierItem item;

        private AmplifierOverlay(AmplifierItem item) {
            this.item = item;
        }

        @Override
        public TextOverlay getTextOverlay() {
            String text = valueToString(item.getTier(), amplifierTierRomanNumerals.get());
            TextRenderSetting style = TextRenderSetting.DEFAULT
                    .withCustomColor(HIGHLIGHT_COLOR)
                    .withTextShadow(amplifierTierShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), -1, 1, 0.75f);
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return amplifierTierEnabled.get();
        }
    }

    private final class DungeonKeyOverlay implements TextOverlayInfo {
        private static final CustomColor STANDARD_COLOR = CustomColor.fromChatFormatting(ChatFormatting.GOLD);
        private static final CustomColor CORRUPTED_COLOR = CustomColor.fromChatFormatting(ChatFormatting.DARK_RED);
        private static final CustomColor REMOVED_COLOR = CustomColor.fromChatFormatting(ChatFormatting.GRAY);

        private final DungeonKeyItem item;

        private DungeonKeyOverlay(DungeonKeyItem item) {
            this.item = item;
        }

        @Override
        public TextOverlay getTextOverlay() {
            Dungeon dungeon = item.getDungeon();
            String text = dungeon.getInitials();

            CustomColor textColor;
            if (item.isCorrupted()) {
                textColor = dungeon.isCorruptedRemoved() ? REMOVED_COLOR : CORRUPTED_COLOR;
            } else {
                textColor = dungeon.isRemoved() ? REMOVED_COLOR : STANDARD_COLOR;
            }

            TextRenderSetting style =
                    TextRenderSetting.DEFAULT.withCustomColor(textColor).withTextShadow(dungeonKeyShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), -1, 1, 1f);
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return dungeonKeyEnabled.get();
        }
    }

    private final class EmeraldPouchOverlay implements TextOverlayInfo {
        private static final CustomColor HIGHLIGHT_COLOR = CustomColor.fromChatFormatting(ChatFormatting.GREEN);

        private final EmeraldPouchItem item;

        private EmeraldPouchOverlay(EmeraldPouchItem item) {
            this.item = item;
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return emeraldPouchTierEnabled.get();
        }

        @Override
        public TextOverlay getTextOverlay() {
            String text = valueToString(item.getTier(), emeraldPouchTierRomanNumerals.get());
            TextRenderSetting style = TextRenderSetting.DEFAULT
                    .withCustomColor(HIGHLIGHT_COLOR)
                    .withTextShadow(emeraldPouchTierShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), -1, 1, 0.9f);
        }
    }

    private final class GatheringToolOverlay implements TextOverlayInfo {
        private final GatheringToolItem item;

        private GatheringToolOverlay(GatheringToolItem item) {
            this.item = item;
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return gatheringToolTierEnabled.get();
        }

        @Override
        public TextOverlay getTextOverlay() {
            String text = valueToString(item.getTier(), gatheringToolTierRomanNumerals.get());
            TextRenderSetting style = TextRenderSetting.DEFAULT
                    .withCustomColor(CustomColor.fromChatFormatting(ChatFormatting.DARK_AQUA))
                    .withTextShadow(gatheringToolTierShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), -1, 1, 0.9f);
        }
    }

    private final class HorseOverlay implements TextOverlayInfo {
        private final HorseItem item;

        private HorseOverlay(HorseItem item) {
            this.item = item;
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return horseTierEnabled.get();
        }

        @Override
        public TextOverlay getTextOverlay() {
            String text = valueToString(item.getTier().getNumeral(), horseTierRomanNumerals.get());
            TextRenderSetting style = TextRenderSetting.DEFAULT
                    .withCustomColor(CustomColor.fromChatFormatting(ChatFormatting.DARK_AQUA))
                    .withTextShadow(horseTierShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), -1, 1, 0.9f);
        }
    }

    private final class PowderOverlay implements TextOverlayInfo {
        private final PowderItem item;

        private PowderOverlay(PowderItem item) {
            this.item = item;
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return powderTierEnabled.get();
        }

        @Override
        public TextOverlay getTextOverlay() {
            CustomColor highlightColor = item.getPowderProfile().element().getColor();

            String text = valueToString(item.getTier(), powderTierRomanNumerals.get());
            TextRenderSetting style =
                    TextRenderSetting.DEFAULT.withCustomColor(highlightColor).withTextShadow(powderTierShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), -1, 1, 0.75f);
        }
    }

    private final class SeaskipperDestinationOverlay implements TextOverlayInfo {
        private static final CustomColor CITY_COLOR = CustomColor.fromChatFormatting(ChatFormatting.AQUA);

        private final SeaskipperDestinationItem item;

        private SeaskipperDestinationOverlay(SeaskipperDestinationItem item) {
            this.item = item;
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return teleportScrollEnabled.get();
        }

        @Override
        public TextOverlay getTextOverlay() {
            String text = item.getShorthand();
            TextRenderSetting style =
                    TextRenderSetting.DEFAULT.withCustomColor(CITY_COLOR).withTextShadow(teleportScrollShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), 0, 0, 1f);
        }
    }

    private final class SkillPointOverlay implements TextOverlayInfo {
        private final SkillPointItem item;

        private SkillPointOverlay(SkillPointItem item) {
            this.item = item;
        }

        @Override
        public ItemTextOverlayFeature.TextOverlay getTextOverlay() {
            Skill skill = item.getSkill();

            String text = skill.getSymbol();
            TextRenderSetting style = TextRenderSetting.DEFAULT
                    .withCustomColor(CustomColor.fromChatFormatting(skill.getColorCode()))
                    .withTextShadow(skillIconShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), -1, 1, 0.9f);
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return skillIconEnabled.get();
        }
    }

    private final class SkillPotionOverlay implements TextOverlayInfo {
        private final PotionItem item;

        private SkillPotionOverlay(PotionItem item) {
            this.item = item;
        }

        @Override
        public TextOverlay getTextOverlay() {
            Skill skill = item.getType().getSkill();

            String text = skill.getSymbol();
            TextRenderSetting style = TextRenderSetting.DEFAULT
                    .withCustomColor(CustomColor.fromChatFormatting(skill.getColorCode()))
                    .withTextShadow(skillIconShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), -1, 1, 0.9f);
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return skillIconEnabled.get();
        }
    }

    private final class TeleportScrollOverlay implements TextOverlayInfo {
        private static final CustomColor CITY_COLOR = CustomColor.fromChatFormatting(ChatFormatting.AQUA);
        private static final CustomColor DUNGEON_COLOR = CustomColor.fromChatFormatting(ChatFormatting.GOLD);
        private static final CustomColor OUT_OF_CHARGES_COLOR = CustomColor.fromChatFormatting(ChatFormatting.RED);

        private final TeleportScrollItem item;

        private TeleportScrollOverlay(TeleportScrollItem item) {
            this.item = item;
        }

        @Override
        public boolean isTextOverlayEnabled() {
            return teleportScrollEnabled.get();
        }

        @Override
        public TextOverlay getTextOverlay() {
            CustomColor textColor;
            if (item.getRemainingCharges() <= 0) {
                textColor = OUT_OF_CHARGES_COLOR;
            } else if (item.isDungeon()) {
                textColor = DUNGEON_COLOR;
            } else {
                textColor = CITY_COLOR;
            }

            String text = item.getDestination();
            TextRenderSetting style =
                    TextRenderSetting.DEFAULT.withCustomColor(textColor).withTextShadow(teleportScrollShadow.get());

            return new TextOverlay(new TextRenderTask(text, style), 0, 0, 1f);
        }
    }

    /**
     * Describes an item's text overlay, with its color, position relative to the item's slot, and text scale.
     */
    private record TextOverlay(TextRenderTask task, int xOffset, int yOffset, float scale) {}
}
