/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.tooltip.TooltipBuilder;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.properties.CraftedItemProperty;
import com.wynntils.models.items.properties.IdentifiableItemProperty;
import com.wynntils.models.items.properties.NamedItemProperty;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

public class FakeItemStack extends ItemStack {
    private final WynnItem wynnItem;
    private final String source;

    public FakeItemStack(WynnItem wynnItem, ItemStack itemStack, String source) {
        super(itemStack.getItem(), 1);
        this.applyComponents(itemStack.getComponentsPatch());

        if (wynnItem instanceof NamedItemProperty namedItemProperty) {
            Handlers.Item.updateItem(this, wynnItem, StyledText.fromString(namedItemProperty.getName()));
        }

        this.wynnItem = wynnItem;
        this.source = source;
    }

    public FakeItemStack(GearItem gearItem, String source) {
        this(gearItem, gearItem.getItemInfo().metaInfo().material().itemStack(), source);
    }

    // This should be only used by chat items, so the item does not matter
    public FakeItemStack(WynnItem wynnItem, String source) {
        this(wynnItem, new ItemStack(Items.STONE), source);
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext context, Player player, TooltipFlag isAdvanced) {
        TooltipBuilder tooltipBuilder = null;
        if (wynnItem instanceof IdentifiableItemProperty<?, ?> identifiableItem) {
            tooltipBuilder = Handlers.Tooltip.buildNew(identifiableItem, false, true);
        } else if (wynnItem instanceof CraftedItemProperty craftedItemProperty) {
            tooltipBuilder = Handlers.Tooltip.buildNew(craftedItemProperty);
        }

        if (tooltipBuilder == null) return List.of();

        List<Component> tooltip = tooltipBuilder.getTooltipLines(Models.Character.getClassType());
        // Add a line describing the source of this fake stack
        tooltip.add(
                1, Component.literal(source).withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
        return tooltip;
    }
}
