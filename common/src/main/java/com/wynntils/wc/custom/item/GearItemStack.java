/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.custom.item;

import com.wynntils.core.webapi.WebManager;
import com.wynntils.core.webapi.profiles.item.DamageType;
import com.wynntils.core.webapi.profiles.item.ItemProfile;
import com.wynntils.core.webapi.profiles.item.MajorIdentification;
import com.wynntils.core.webapi.profiles.item.RequirementType;
import com.wynntils.features.user.tooltips.ItemStatInfoFeature;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.StringUtils;
import com.wynntils.wc.objects.ItemIdentificationContainer;
import com.wynntils.wc.objects.Powder;
import com.wynntils.wc.utils.IdentificationOrderer;
import com.wynntils.wc.utils.WynnItemUtils;
import com.wynntils.wc.utils.WynnUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;

public class GearItemStack extends WynnItemStack {

    private static final Pattern ITEM_TIER =
            Pattern.compile("(?<Quality>Normal|Unique|Rare|Legendary|Fabled|Mythic|Set) Item(?: \\[(?<Rolls>\\d+)])?");

    private static final Component ID_PLACEHOLDER = new TextComponent("ID_PLACEHOLDER");

    private ItemProfile itemProfile;
    private boolean isPerfect;
    private boolean isDefective;
    private float overallPercentage;
    private boolean hasNew;

    private boolean isGuideStack;
    private boolean isChatItem;

    private Component customName;

    private List<ItemIdentificationContainer> identifications;
    private List<Powder> powders;
    private int rerolls;

    private List<Component> percentTooltip;
    private List<Component> rangeTooltip;
    private List<Component> rerollTooltip;

    public GearItemStack(ItemStack stack) {
        super(stack);

        // get item profile
        if (WebManager.getItemsMap() == null || !WebManager.getItemsMap().containsKey(itemName)) return;
        itemProfile = WebManager.getItemsMap().get(itemName);

        // identification parsing & tooltip creation
        identifications = new ArrayList<>();

        List<Component> lore = ComponentUtils.stripDuplicateBlank(getOriginalTooltip());
        lore.remove(0); // remove item name

        List<Component> baseTooltip = new ArrayList<>();

        boolean hasIds = false;
        boolean endOfIDs = false;
        for (int i = 0; i < lore.size(); i++) {
            Component loreLine = lore.get(i);
            String unformattedLoreLine = WynnUtils.normalizeBadString(loreLine.getString());

            if (unformattedLoreLine.equals("Set Bonus:")) {
                endOfIDs = true;
            }

            if (endOfIDs) continue;

            if (unformattedLoreLine.contains("] Powder Slots")) {
                powders = Powder.findPowders(unformattedLoreLine);
                baseTooltip.add(loreLine);
                continue;
            }

            Matcher rerollMatcher = ITEM_TIER.matcher(unformattedLoreLine);
            if (rerollMatcher.find()) {
                baseTooltip.add(loreLine);

                if (rerollMatcher.group("Rolls") == null) continue;
                rerolls = Integer.parseInt(rerollMatcher.group("Rolls"));
                continue;
            }

            ItemIdentificationContainer idContainer = WynnItemUtils.identificationFromLore(loreLine, itemProfile);
            if (idContainer == null) { // not an ID line
                baseTooltip.add(loreLine);
                continue;
            }

            // if we've reached this point, we have an id
            if (!hasIds) {
                hasIds = true;
                baseTooltip.add(ID_PLACEHOLDER);
                baseTooltip.add(new TextComponent(""));
            }

            identifications.add(idContainer);
        }

        if (!identifications.isEmpty()) {
            baseTooltip = ComponentUtils.stripDuplicateBlank(baseTooltip);
        }

        constructTooltips(baseTooltip);

        // overall percent & name
        parseIDs();
    }

    public GearItemStack(ItemProfile itemProfile) {
        super(itemProfile.getItemInfo().asItemStack());

        this.itemProfile = itemProfile;
        isGuideStack = true;

        CompoundTag tag = this.getOrCreateTag();
        tag.putBoolean("Unbreakable", true);
        if (itemProfile.getItemInfo().isArmorColorValid())
            tag.putInt("color", itemProfile.getItemInfo().getArmorColorAsInt());
        this.setTag(tag);

        customName = new TextComponent(itemProfile.getDisplayName())
                .withStyle(itemProfile.getTier().getChatFormatting());

        List<Component> baseTooltip = constructBaseTooltip();
        identifications = WynnItemUtils.identificationsFromProfile(itemProfile);
        constructTooltips(baseTooltip);
    }

    /** Chat item constructor - used when decoding an encoded chat string */
    public GearItemStack(
            ItemProfile itemProfile,
            List<ItemIdentificationContainer> identifications,
            List<Powder> powders,
            int rerolls) {
        super(itemProfile.getItemInfo().asItemStack());

        this.itemProfile = itemProfile;
        this.identifications = identifications;
        this.powders = powders;
        this.rerolls = rerolls;
        isChatItem = true;

        CompoundTag tag = this.getOrCreateTag();
        tag.putBoolean("Unbreakable", true);
        if (itemProfile.getItemInfo().isArmorColorValid())
            tag.putInt("color", itemProfile.getItemInfo().getArmorColorAsInt());
        this.setTag(tag);

        customName = new TextComponent(itemProfile.getDisplayName())
                .withStyle(itemProfile.getTier().getChatFormatting());

        parseIDs();
        List<Component> baseTooltip = constructBaseTooltip();
        constructTooltips(baseTooltip);
    }

    public ItemProfile getItemProfile() {
        return itemProfile;
    }

    public float getOverallPercentage() {
        return overallPercentage;
    }

    public boolean hasNew() {
        return hasNew;
    }

    public List<ItemIdentificationContainer> getIdentifications() {
        return identifications;
    }

    public List<ItemIdentificationContainer> getOrderedIdentifications() {
        return IdentificationOrderer.INSTANCE.orderIdentifications(identifications);
    }

    public List<Powder> getPowders() {
        return powders;
    }

    public int getRerolls() {
        return rerolls;
    }

    @Override
    public Component getHoverName() {
        if (isGuideStack || isChatItem) return customName;

        /*
         * This math was originally based off Avaritia code.
         * Special thanks for Morpheus1101 and SpitefulFox
         * Avaritia Repo: https://github.com/Morpheus1101/Avaritia
         */
        if (ItemStatInfoFeature.perfect && isPerfect) {
            MutableComponent newName = new TextComponent("").withStyle(ChatFormatting.BOLD);

            String name = "Perfect " + itemName;

            long time = System.currentTimeMillis();
            for (int i = 0; i < name.length(); i++) {
                int cycle = 1000;
                Style color = Style.EMPTY
                        .withColor(Color.HSBtoRGB(((time + i * cycle / 7) % cycle) / (float) cycle, 0.8F, 0.8F))
                        .withItalic(false);

                newName.append(new TextComponent(String.valueOf(name.charAt(i))).setStyle(color));
            }

            return newName;
        }

        if (ItemStatInfoFeature.defective && isDefective) {
            MutableComponent newName = new TextComponent("").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED);
            newName.setStyle(newName.getStyle().withItalic(false));

            String name = "Defective " + itemName;

            boolean obfuscated = Math.random() < ItemStatInfoFeature.obfuscationChanceStart;
            StringBuilder current = new StringBuilder();

            for (int i = 0; i < name.length() - 1; i++) {
                current.append(name.charAt(i));

                float chance = MathUtils.lerp(
                        ItemStatInfoFeature.obfuscationChanceStart,
                        ItemStatInfoFeature.obfuscationChanceEnd,
                        (i + 1) / (float) (name.length() - 1));

                if (!obfuscated && Math.random() < chance) {
                    newName.append(new TextComponent(current.toString()).withStyle(Style.EMPTY.withItalic(false)));
                    current = new StringBuilder();

                    obfuscated = true;
                } else if (obfuscated && Math.random() > chance) {
                    newName.append(new TextComponent(current.toString())
                            .withStyle(Style.EMPTY.withObfuscated(true).withItalic(false)));
                    current = new StringBuilder();

                    obfuscated = false;
                }
            }

            current.append(name.charAt(name.length() - 1));

            if (obfuscated) {
                newName.append(new TextComponent(current.toString())
                        .withStyle(Style.EMPTY.withItalic(false).withObfuscated(true)));
            } else {
                newName.append(new TextComponent(current.toString()).withStyle(Style.EMPTY.withItalic(false)));
            }

            return newName;
        }

        // besides special case of perfect/defective, use name already set
        return customName != null ? customName : super.getHoverName();
    }

    @Override
    public List<Component> getTooltipLines(Player player, TooltipFlag flag) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getHoverName());

        if (isGuideStack) {
            tooltip.addAll(rangeTooltip);
            return tooltip;
        }

        if (isChatItem) {
            tooltip.add(new TextComponent("From chat")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .withStyle(ChatFormatting.ITALIC));

            tooltip.addAll(percentTooltip);
            return tooltip;
        }

        if (GLFW.glfwGetKey(McUtils.mc().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) == 1) {
            tooltip.addAll(rangeTooltip);
        } else if (GLFW.glfwGetKey(McUtils.mc().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) == 1) {
            tooltip.addAll(rerollTooltip);
        } else {
            tooltip.addAll(percentTooltip);
        }

        return tooltip;
    }

    private void parseIDs() {
        overallPercentage = -1f;
        hasNew = identifications.stream().anyMatch(ItemIdentificationContainer::isNew);
        DoubleSummaryStatistics percents = identifications.stream()
                .filter(Predicate.not(ItemIdentificationContainer::isFixed))
                .mapToDouble(ItemIdentificationContainer::percent)
                .summaryStatistics();
        int idAmount = (int) percents.getCount();
        float percentTotal = (float) percents.getSum();

        MutableComponent name;
        if (customName == null) {
            name = new TextComponent(WynnUtils.normalizeBadString(ComponentUtils.getFormatted(getHoverName())));
        } else {
            name = customName.copy();
        }

        if (hasNew) {
            name.append(new TextComponent(" [NEW]").withStyle(ChatFormatting.GOLD));
        } else if (idAmount > 0) {
            overallPercentage = percentTotal / idAmount;

            // check for perfect/0% items
            isPerfect = overallPercentage >= 100d;
            isDefective = overallPercentage == 0;

            name.append(WynnItemUtils.getPercentageTextComponent(overallPercentage));
        }

        customName = name;
    }

    private void constructTooltips(List<Component> baseTooltip) {
        int idIndex = baseTooltip.indexOf(ID_PLACEHOLDER);
        baseTooltip.remove(ID_PLACEHOLDER);

        percentTooltip = new ArrayList<>(baseTooltip);
        rangeTooltip = new ArrayList<>(baseTooltip);
        rerollTooltip = new ArrayList<>(baseTooltip);

        if (!identifications.isEmpty() && idIndex != -1) {
            Map<String, Component> percentMap = identifications.stream()
                    .collect(Collectors.toMap(
                            ItemIdentificationContainer::shortIdName, ItemIdentificationContainer::percentLoreLine));
            Map<String, Component> rangeMap = identifications.stream()
                    .collect(Collectors.toMap(
                            ItemIdentificationContainer::shortIdName, ItemIdentificationContainer::rangeLoreLine));
            Map<String, Component> rerollMap = identifications.stream()
                    .collect(Collectors.toMap(
                            ItemIdentificationContainer::shortIdName, ItemIdentificationContainer::rerollLoreLine));

            Collection<Component> orderedPercents;
            Collection<Component> orderedRanges;
            Collection<Component> orderedRerolls;

            if (ItemStatInfoFeature.reorderIdentifications || isGuideStack) {
                orderedPercents = IdentificationOrderer.INSTANCE.orderComponents(
                        percentMap, ItemStatInfoFeature.groupIdentifications);
                orderedRanges = IdentificationOrderer.INSTANCE.orderComponents(
                        rangeMap, ItemStatInfoFeature.groupIdentifications);
                orderedRerolls = IdentificationOrderer.INSTANCE.orderComponents(
                        rerollMap, ItemStatInfoFeature.groupIdentifications);
            } else {
                orderedPercents = percentMap.values();
                orderedRanges = rangeMap.values();
                orderedRerolls = rerollMap.values();
            }

            percentTooltip.addAll(idIndex, orderedPercents);
            rangeTooltip.addAll(idIndex, orderedRanges);
            rerollTooltip.addAll(idIndex, orderedRerolls);
        }
    }

    private List<Component> constructBaseTooltip() {
        List<Component> baseTooltip = new ArrayList<>();

        // attack speed
        if (itemProfile.getAttackSpeed() != null)
            baseTooltip.add(new TextComponent(itemProfile.getAttackSpeed().asLore()));

        baseTooltip.add(new TextComponent(""));

        // elemental damages
        if (itemProfile.getDamageTypes().size() > 0) {
            Map<DamageType, String> damages = itemProfile.getDamages();
            for (Map.Entry<DamageType, String> entry : damages.entrySet()) {
                DamageType type = entry.getKey();
                MutableComponent damage = new TextComponent(type.getSymbol() + " " + type).withStyle(type.getColor());
                damage.append(new TextComponent(" Damage: " + entry.getValue())
                        .withStyle(
                                type == DamageType.NEUTRAL
                                        ? type.getColor()
                                        : ChatFormatting.GRAY)); // neutral is all gold
                baseTooltip.add(damage);
            }

            baseTooltip.add(new TextComponent(""));
        }

        // elemental defenses
        if (itemProfile.getDefenseTypes().size() > 0) {
            int health = itemProfile.getHealth();
            if (health != 0) {
                MutableComponent healthComp =
                        new TextComponent("❤ Health: " + health).withStyle(ChatFormatting.DARK_RED);
                baseTooltip.add(healthComp);
            }

            Map<DamageType, Integer> defenses = itemProfile.getElementalDefenses();
            for (Map.Entry<DamageType, Integer> entry : defenses.entrySet()) {
                DamageType type = entry.getKey();
                MutableComponent defense = new TextComponent(type.getSymbol() + " " + type).withStyle(type.getColor());
                defense.append(new TextComponent(" Defence: " + entry.getValue()).withStyle(ChatFormatting.GRAY));
                baseTooltip.add(defense);
            }

            baseTooltip.add(new TextComponent(""));
        }

        // requirements
        if (itemProfile.hasRequirements()) {
            Map<RequirementType, String> requirements = itemProfile.getRequirements();
            for (Map.Entry<RequirementType, String> entry : requirements.entrySet()) {
                RequirementType type = entry.getKey();
                MutableComponent requirement = new TextComponent("✔ ").withStyle(ChatFormatting.GREEN);
                requirement.append(new TextComponent(type.asLore() + entry.getValue()).withStyle(ChatFormatting.GRAY));
                baseTooltip.add(requirement);
            }

            baseTooltip.add(new TextComponent(""));
        }

        // ids
        if (itemProfile.getStatuses().size() > 0) {
            baseTooltip.add(ID_PLACEHOLDER);
            baseTooltip.add(new TextComponent(""));
        }

        // major ids
        if (itemProfile.getMajorIds() != null && itemProfile.getMajorIds().size() > 0) {
            for (MajorIdentification majorId : itemProfile.getMajorIds()) {
                Stream.of(StringUtils.wrapTextBySize(majorId.asLore(), 150))
                        .forEach(c -> baseTooltip.add(new TextComponent(c).withStyle(ChatFormatting.DARK_AQUA)));
            }
            baseTooltip.add(new TextComponent(""));
        }

        // powder slots
        if (itemProfile.getPowderAmount() > 0) {
            if (isGuideStack || powders == null) {
                baseTooltip.add(new TextComponent("[" + itemProfile.getPowderAmount() + " Powder Slots]")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                MutableComponent powderLine = new TextComponent(
                                "[" + powders.size() + "/" + itemProfile.getPowderAmount() + "] Powder Slots ")
                        .withStyle(ChatFormatting.GRAY);
                if (powders.size() > 0) {
                    MutableComponent powderList = new TextComponent("[");
                    for (Powder p : powders) {
                        String symbol = p.getColoredSymbol();
                        if (powderList.getSiblings().size() > 0) symbol = " " + symbol;
                        powderList.append(new TextComponent(symbol));
                    }
                    powderList.append(new TextComponent("]"));
                    powderLine.append(powderList);
                }
                baseTooltip.add(powderLine);
            }
        }

        // tier & rerolls
        MutableComponent tier = itemProfile.getTier().asLore().copy();
        if (rerolls > 1) {
            tier.append(" [" + rerolls + "]");
        }
        baseTooltip.add(tier);

        // untradable
        if (itemProfile.getRestriction() != null) {
            baseTooltip.add(new TextComponent(StringUtils.capitalizeFirst(itemProfile.getRestriction() + " Item"))
                    .withStyle(ChatFormatting.RED));
        }

        return baseTooltip;
    }
}
