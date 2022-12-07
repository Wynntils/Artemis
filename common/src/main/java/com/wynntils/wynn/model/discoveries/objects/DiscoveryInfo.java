/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.discoveries.objects;

import com.wynntils.core.webapi.TerritoryManager;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.mc.utils.ItemUtils;
import com.wynntils.wynn.model.CharacterManager;
import com.wynntils.wynn.objects.profiles.DiscoveryProfile;
import com.wynntils.wynn.objects.profiles.TerritoryProfile;
import com.wynntils.wynn.utils.WynnUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;

public class DiscoveryInfo {
    private final String name;
    private final DiscoveryType type;
    private final String description;
    private final int minLevel;
    private final TerritoryProfile guildTerritory;
    private final boolean discovered;
    private final List<String> requirements;
    private List<Component> displayLore = null;

    public DiscoveryInfo(DiscoveryProfile discoveryProfile) {
        this.name = discoveryProfile.getName();
        this.type = DiscoveryType.valueOf(discoveryProfile.getType().toUpperCase(Locale.ROOT));
        this.description = "";
        this.minLevel = discoveryProfile.getLevel();
        this.guildTerritory = TerritoryManager.getTerritories().get(name);
        this.discovered = false;
        this.requirements = discoveryProfile.getRequirements();
    }

    private DiscoveryInfo(String name, DiscoveryType type, String description, int minLevel) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.minLevel = minLevel;
        this.guildTerritory = TerritoryManager.getTerritories().get(name);
        this.discovered = true;
        this.requirements = List.of();
    }

    public static DiscoveryInfo parseFromItemStack(ItemStack itemStack) {
        List<String> lore = ItemUtils.getLore(itemStack);
        if (lore.isEmpty()) {
            return null;
        }

        String name = WynnUtils.normalizeBadString(ComponentUtils.getCoded(itemStack.getHoverName()));
        int minLevel = Integer.parseInt(lore.get(0).replace("§a✔§r§7 Combat Lv. Min: §r§f", ""));

        DiscoveryType type;
        if (name.charAt(1) == DiscoveryType.WORLD.getColor().getChar()) {
            type = DiscoveryType.WORLD;
        } else if (name.charAt(1) == DiscoveryType.TERRITORY.getColor().getChar()) {
            type = DiscoveryType.TERRITORY;
        } else if (name.charAt(1) == DiscoveryType.SECRET.getColor().getChar()) {
            type = DiscoveryType.SECRET;
        } else {
            return null;
        }

        StringBuilder descriptionBuilder = new StringBuilder();
        for (int i = 2; i < lore.size(); i++) {
            descriptionBuilder.append(ComponentUtils.stripFormatting(lore.get(i)));
        }
        String description = descriptionBuilder.toString();

        return new DiscoveryInfo(ComponentUtils.stripFormatting(name), type, description, minLevel);
    }

    private List<Component> generateLore() {
        displayLore = new ArrayList<>();

        displayLore.add(new TextComponent(name).withStyle(type.getColor()).withStyle(ChatFormatting.BOLD));

        if (CharacterManager.getCharacterInfo().getLevel() >= minLevel) {
            displayLore.add(new TextComponent("✔")
                    .withStyle(ChatFormatting.GREEN)
                    .append(new TextComponent(" Combat Lv. Min: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(new TextComponent(String.valueOf(minLevel)).withStyle(ChatFormatting.WHITE))));
        } else {
            displayLore.add(new TextComponent("✘")
                    .withStyle(ChatFormatting.RED)
                    .append(new TextComponent(" Combat Lv. Min: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(new TextComponent(String.valueOf(minLevel)).withStyle(ChatFormatting.WHITE))));
        }

        displayLore.add(TextComponent.EMPTY);

        if (discovered) {
            displayLore.add(new TextComponent("Discovered").withStyle(ChatFormatting.GREEN));
        } else {
            displayLore.add(new TextComponent("Not Discovered").withStyle(ChatFormatting.RED));
        }

        if (!description.isEmpty()) {
            displayLore.add(TextComponent.EMPTY);
            displayLore.addAll(ComponentUtils.wrapTooltips(
                    List.of(new TextComponent(description).withStyle(ChatFormatting.GRAY)), 300));
        }

        return displayLore;
    }

    public List<Component> getLore() {
        return displayLore == null ? displayLore = generateLore() : displayLore;
    }

    public String getName() {
        return name;
    }

    public DiscoveryType getType() {
        return type;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public TerritoryProfile getGuildTerritory() {
        return guildTerritory;
    }

    public List<String> getRequirements() {
        return requirements;
    }
}
