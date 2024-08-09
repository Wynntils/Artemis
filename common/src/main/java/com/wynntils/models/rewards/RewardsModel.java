/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.rewards;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.net.event.NetResultProcessedEvent;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.items.game.CharmItem;
import com.wynntils.models.items.items.game.TomeItem;
import com.wynntils.models.rewards.type.CharmInfo;
import com.wynntils.models.rewards.type.CharmInstance;
import com.wynntils.models.rewards.type.TomeInfo;
import com.wynntils.models.rewards.type.TomeInstance;
import com.wynntils.models.wynnitem.WynnItemModel;
import com.wynntils.models.wynnitem.parsing.WynnItemParseResult;
import com.wynntils.models.wynnitem.parsing.WynnItemParser;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

public class RewardsModel extends Model {
    private final TomeInfoRegistry tomeInfoRegistry = new TomeInfoRegistry();
    private final CharmInfoRegistry charmInfoRegistry = new CharmInfoRegistry();

    public RewardsModel(WynnItemModel wynnItemModel) {
        super(List.of(wynnItemModel));

        // We do not explicitly load the ingredient DB here,
        // but when all of it's dependencies are loaded,
        // the NetResultProcessedEvent will trigger the load.
    }

    @Override
    public void reloadData() {
        tomeInfoRegistry.reloadData();
        charmInfoRegistry.reloadData();
    }

    @SubscribeEvent
    public void onDataLoaded(NetResultProcessedEvent.ForUrlId event) {
        UrlId urlId = event.getUrlId();
        if (urlId == UrlId.DATA_STATIC_ITEM_OBTAIN || urlId == UrlId.DATA_STATIC_MATERIAL_CONVERSION) {
            // We need both material conversio  and obtain info to be able to load the ingredient DB
            if (!Models.WynnItem.hasObtainInfo()) return;
            if (!Models.WynnItem.hasMaterialConversionInfo()) return;

            tomeInfoRegistry.reloadData();
            charmInfoRegistry.reloadData();
            return;
        }
    }

    public CharmInfo getCharmInfoFromDisplayName(String name) {
        return charmInfoRegistry.getFromDisplayName(name);
    }

    public TomeInfo getTomeInfoFromDisplayName(String name) {
        return tomeInfoRegistry.getFromDisplayName(name);
    }

    public Stream<CharmInfo> getAllCharmInfos() {
        return charmInfoRegistry.getAllCharmInfos();
    }

    public Stream<TomeInfo> getAllTomeInfos() {
        return tomeInfoRegistry.getAllTomeInfos();
    }

    public ItemAnnotation fromCharmItemStack(ItemStack itemStack, StyledText name, String displayName, String type) {
        GearTier tier = GearTier.fromStyledText(name);

        CharmInfo charmInfo = charmInfoRegistry.getFromDisplayName(name.getStringWithoutFormatting());
        if (charmInfo == null) {
            WynntilsMod.warn("Could not find charm info for " + name.getStringWithoutFormatting());
            return null;
        }

        WynnItemParseResult result = WynnItemParser.parseItemStack(itemStack, charmInfo.getVariableStatsMap());
        if (result.tier() != charmInfo.tier()) {
            WynntilsMod.warn("Tier for " + charmInfo.name() + " is reported as " + result.tier());
        }

        return new CharmItem(charmInfo, CharmInstance.create(result.rerolls(), charmInfo, result.identifications()));
    }

    public TomeItem fromTomeItemStack(ItemStack itemStack, StyledText name) {
        GearTier gearTier = GearTier.fromStyledText(name);

        TomeInfo tomeInfo = tomeInfoRegistry.getFromDisplayName(name.getStringWithoutFormatting());
        if (tomeInfo == null) {
            WynntilsMod.warn("Could not find tome info for " + name.getStringWithoutFormatting());
            return null;
        }

        WynnItemParseResult result = WynnItemParser.parseItemStack(itemStack, tomeInfo.getVariableStatsMap());
        if (result.tier() != tomeInfo.tier()) {
            WynntilsMod.warn("Tier for " + tomeInfo.name() + " is reported as " + result.tier());
        }

        return new TomeItem(tomeInfo, TomeInstance.create(result.rerolls(), tomeInfo, result.identifications()));
    }
}
