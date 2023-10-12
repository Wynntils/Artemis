/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.itemfilter.statproviders;

import com.wynntils.core.components.Models;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.WynnItemCache;
import com.wynntils.models.trademarket.type.TradeMarketPriceInfo;
import com.wynntils.services.itemfilter.type.ItemStatProvider;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public class AmountStatProvider extends ItemStatProvider<Integer> {
    @Override
    public List<Integer> getValue(ItemStack itemStack, WynnItem wynnItem) {
        TradeMarketPriceInfo priceInfo = wynnItem.getCache().getOrCalculate(WynnItemCache.EMERALD_PRICE_KEY, () -> {
            TradeMarketPriceInfo calculatedInfo = Models.TradeMarket.calculateItemPriceInfo(itemStack);
            return calculatedInfo;
        });

        if (priceInfo == TradeMarketPriceInfo.EMPTY) {
            return List.of();
        }

        return List.of(priceInfo.amount());
    }
}
