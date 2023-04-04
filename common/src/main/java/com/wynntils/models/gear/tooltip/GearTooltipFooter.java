/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear.tooltip;

import com.wynntils.core.text.StyledText;
import com.wynntils.models.elements.type.Powder;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearInstance;
import com.wynntils.models.gear.type.GearMajorId;
import com.wynntils.models.gear.type.GearRestrictions;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.RenderedStringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class GearTooltipFooter {
    private static final int PIXEL_WIDTH = 150;

    public static List<Component> buildTooltip(GearInfo gearInfo, GearInstance gearInstance) {
        List<Component> footer = new ArrayList<>();

        // major ids
        // FIXME: This is not the format Wynncraft uses. The major ID name should be followed
        // by the lore directly on the same line.
        // To fix this, we need a version af wrapTextBySize() that can take in a Component.
        // For now, print the name of the major ID on a separate line.
        if (!gearInfo.fixedStats().majorIds().isEmpty()) {
            for (GearMajorId majorId : gearInfo.fixedStats().majorIds()) {
                footer.add(Component.literal("+" + majorId.name() + ": ").withStyle(ChatFormatting.AQUA));
                Stream.of(RenderedStringUtils.wrapTextBySize(majorId.lore(), PIXEL_WIDTH))
                        .forEach(c -> footer.add(c.asComponent().withStyle(ChatFormatting.DARK_AQUA)));
            }
        }

        footer.add(Component.literal(""));

        // powder slots
        if (gearInfo.powderSlots() > 0) {
            if (gearInstance == null) {
                footer.add(Component.literal("[" + gearInfo.powderSlots() + " Powder Slots]")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                MutableComponent powderLine = Component.literal(
                                "[" + gearInstance.powders().size() + "/" + gearInfo.powderSlots() + "] Powder Slots ")
                        .withStyle(ChatFormatting.GRAY);
                if (!gearInstance.powders().isEmpty()) {
                    MutableComponent powderList = Component.literal("[");
                    for (Powder p : gearInstance.powders()) {
                        String symbol = p.getColoredSymbol();
                        if (!powderList.getSiblings().isEmpty()) symbol = " " + symbol;
                        powderList.append(Component.literal(symbol));
                    }
                    powderList.append(Component.literal("]"));
                    powderLine.append(powderList);
                }
                footer.add(powderLine);
            }
        }

        // tier & rerolls
        GearTier gearTier = gearInfo.tier();
        MutableComponent tier = Component.literal(gearTier.getName() + " Item").withStyle(gearTier.getChatFormatting());
        if (gearInstance != null && gearInstance.rerolls() > 1) {
            tier.append(" [" + gearInstance.rerolls() + "]");
        }
        footer.add(tier);

        // restrictions (untradable, quest item)
        if (gearInfo.metaInfo().restrictions() != GearRestrictions.NONE) {
            footer.add(Component.literal(StringUtils.capitalizeFirst(
                            gearInfo.metaInfo().restrictions().getDescription()))
                    .withStyle(ChatFormatting.RED));
        }

        // lore
        Optional<StyledText> lore = gearInfo.metaInfo().lore();
        if (lore.isPresent()) {
            Stream.of(RenderedStringUtils.wrapTextBySize(lore.get(), PIXEL_WIDTH))
                    .forEach(c -> footer.add(c.asComponent().withStyle(ChatFormatting.DARK_GRAY)));
        }

        return footer;
    }
}
