/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.guides.gear;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.handlers.tooltip.impl.identifiable.IdentifiableTooltipBuilder;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.items.WynnItemData;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.screens.guides.GuideItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public final class GuideGearItemStack extends GuideItemStack {
    private final GearInfo gearInfo;
    private final MutableComponent name;
    private List<Component> generatedTooltip;

    public GuideGearItemStack(GearInfo gearInfo) {
        super(gearInfo.metaInfo().material().itemStack(), new GearItem(gearInfo, null), gearInfo.name());

        this.gearInfo = gearInfo;
        this.name = Component.literal(gearInfo.name()).withStyle(gearInfo.tier().getChatFormatting());
        this.generatedTooltip = List.of();
    }

    @Override
    public Component getHoverName() {
        return name;
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext context, Player player, TooltipFlag flag) {
        List<Component> tooltipLines = new ArrayList<>(generatedTooltip);

        appendObtainInfo(tooltipLines, gearInfo.metaInfo().obtainInfo());

        tooltipLines.add(Component.empty());
        if (Services.Favorites.isFavorite(this)) {
            tooltipLines.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.unfavorite")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltipLines.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.favorite")
                    .withStyle(ChatFormatting.GREEN));
        }
        tooltipLines.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.open")
                .withStyle(ChatFormatting.RED));

        return tooltipLines;
    }

    public GearInfo getGearInfo() {
        return gearInfo;
    }

    public void buildTooltip() {
        IdentifiableTooltipBuilder tooltipBuilder =
                Handlers.Tooltip.buildNew(new GearItem(gearInfo, null), true, false);
        this.generatedTooltip = tooltipBuilder.getTooltipLines(Models.Character.getClassType());

        // Force ItemStatInfoFeature to recreate its cache
        Optional<GearItem> gearItemOpt = Models.Item.asWynnItem(this, GearItem.class);
        if (gearItemOpt.isEmpty()) return;
        gearItemOpt.get().getData().clear(WynnItemData.TOOLTIP_KEY);
    }
}
