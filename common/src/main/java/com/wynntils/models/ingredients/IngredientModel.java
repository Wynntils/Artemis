/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.ingredients;

import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.net.event.NetResultProcessedEvent;
import com.wynntils.models.ingredients.type.IngredientInfo;
import com.wynntils.models.wynnitem.WynnItemModel;
import com.wynntils.models.wynnitem.type.ItemObtainInfo;
import com.wynntils.models.wynnitem.type.ItemObtainType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.neoforged.bus.api.SubscribeEvent;

public class IngredientModel extends Model {
    private static final Map<ChatFormatting, Integer> TIER_COLOR_CODES = Map.of(
            ChatFormatting.DARK_GRAY, 0,
            ChatFormatting.YELLOW, 1,
            ChatFormatting.LIGHT_PURPLE, 2,
            ChatFormatting.AQUA, 3);
    private final IngredientInfoRegistry ingredientInfoRegistry = new IngredientInfoRegistry();

    public IngredientModel(WynnItemModel wynnItem) {
        super(List.of(wynnItem));

        // We do not explicitly load the ingredient DB here,
        // but when all of it's dependencies are loaded,
        // the NetResultProcessedEvent will trigger the load.
    }

    @Override
    public void reloadData() {
        ingredientInfoRegistry.loadData();
    }

    @SubscribeEvent
    public void onDataLoaded(NetResultProcessedEvent.ForUrlId event) {
        UrlId urlId = event.getUrlId();
        if (urlId == UrlId.DATA_STATIC_ITEM_OBTAIN || urlId == UrlId.DATA_STATIC_MATERIAL_CONVERSION) {
            // We need both material conversio  and obtain info to be able to load the ingredient DB
            if (!Models.WynnItem.hasObtainInfo()) return;
            if (!Models.WynnItem.hasMaterialConversionInfo()) return;

            ingredientInfoRegistry.loadData();
            return;
        }
    }

    public int getTierFromColorCode(String tierColor) {
        return TIER_COLOR_CODES.getOrDefault(ChatFormatting.getByCode(tierColor.charAt(0)), 0);
    }

    public IngredientInfo getIngredientInfoFromName(String ingredientName) {
        return ingredientInfoRegistry.getFromDisplayName(ingredientName);
    }

    public List<ItemObtainInfo> getObtainInfo(IngredientInfo ingredientInfo) {
        List<ItemObtainInfo> obtainInfo = Models.WynnItem.getObtainInfo(ingredientInfo.name());
        if (obtainInfo == null) {
            return List.of(new ItemObtainInfo(ItemObtainType.UNKNOWN, Optional.empty()));
        }
        return obtainInfo;
    }

    public Stream<IngredientInfo> getAllIngredientInfos() {
        return ingredientInfoRegistry.getIngredientInfoStream();
    }
}
