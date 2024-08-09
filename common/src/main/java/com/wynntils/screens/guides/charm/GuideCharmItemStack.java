/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.guides.charm;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.handlers.tooltip.impl.identifiable.IdentifiableTooltipBuilder;
import com.wynntils.models.items.WynnItemData;
import com.wynntils.models.items.items.game.CharmItem;
import com.wynntils.models.rewards.type.CharmInfo;
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

public class GuideCharmItemStack extends GuideItemStack {
    private final CharmInfo charmInfo;
    private final MutableComponent name;
    private List<Component> generatedTooltip;

    public GuideCharmItemStack(CharmInfo charmInfo) {
        super(charmInfo.metaInfo().material().itemStack(), new CharmItem(charmInfo, null), charmInfo.name());

        this.charmInfo = charmInfo;
        this.name =
                Component.literal(charmInfo.name()).withStyle(charmInfo.tier().getChatFormatting());
        this.generatedTooltip = List.of();
    }

    @Override
    public Component getHoverName() {
        return name;
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext context, Player player, TooltipFlag flag) {
        List<Component> tooltipLines = new ArrayList<>(generatedTooltip);

        appendObtainInfo(tooltipLines, charmInfo.metaInfo().obtainInfo());

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

    public CharmInfo getCharmInfo() {
        return charmInfo;
    }

    public void buildTooltip() {
        IdentifiableTooltipBuilder tooltipBuilder =
                Handlers.Tooltip.buildNew(new CharmItem(charmInfo, null), true, false);
        this.generatedTooltip = tooltipBuilder.getTooltipLines(Models.Character.getClassType());

        // Force ItemStatInfoFeature to recreate its cache
        Optional<CharmItem> charmItemOpt = Models.Item.asWynnItem(this, CharmItem.class);
        if (charmItemOpt.isEmpty()) return;
        charmItemOpt.get().getData().clear(WynnItemData.TOOLTIP_KEY);
    }
}
