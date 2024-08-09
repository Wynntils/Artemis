/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.guides.tome;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.handlers.tooltip.impl.identifiable.IdentifiableTooltipBuilder;
import com.wynntils.models.items.WynnItemData;
import com.wynntils.models.items.items.game.TomeItem;
import com.wynntils.models.rewards.type.TomeInfo;
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

public class GuideTomeItemStack extends GuideItemStack {
    private final TomeInfo tomeInfo;
    private final MutableComponent name;
    private List<Component> generatedTooltip;

    public GuideTomeItemStack(TomeInfo tomeInfo) {
        super(tomeInfo.metaInfo().material().itemStack(), new TomeItem(tomeInfo, null), tomeInfo.name());

        this.tomeInfo = tomeInfo;
        this.name = Component.literal(tomeInfo.name()).withStyle(tomeInfo.tier().getChatFormatting());
        this.generatedTooltip = List.of();
    }

    @Override
    public Component getHoverName() {
        return name;
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext context, Player player, TooltipFlag flag) {
        List<Component> tooltipLines = new ArrayList<>(generatedTooltip);

        appendObtainInfo(tooltipLines, tomeInfo.metaInfo().obtainInfo());

        tooltipLines.add(Component.empty());
        if (Services.Favorites.isFavorite(this)) {
            tooltipLines.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.unfavorite")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltipLines.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.favorite")
                    .withStyle(ChatFormatting.GREEN));
        }

        return tooltipLines;
    }

    public TomeInfo getTomeInfo() {
        return tomeInfo;
    }

    public void buildTooltip() {
        IdentifiableTooltipBuilder tooltipBuilder =
                Handlers.Tooltip.buildNew(new TomeItem(tomeInfo, null), true, false);
        this.generatedTooltip = tooltipBuilder.getTooltipLines(Models.Character.getClassType());

        // Force ItemStatInfoFeature to recreate its cache
        Optional<TomeItem> tomeItemOpt = Models.Item.asWynnItem(this, TomeItem.class);
        if (tomeItemOpt.isEmpty()) return;
        tomeItemOpt.get().getData().clear(WynnItemData.TOOLTIP_KEY);
    }
}
