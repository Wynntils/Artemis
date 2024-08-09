/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.wrappedscreen.event.WrappedScreenOpenEvent;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.screens.trademarket.TradeMarketSearchResultScreen;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class CustomTradeMarketResultScreenFeature extends Feature {
    private static final String SEARCH_ITEM_TITLE = "§a§lSearch";

    @Persisted
    private final Config<ShiftBehavior> shiftBehaviorConfig = new Config<>(ShiftBehavior.DISABLED_IF_SHIFT_HELD);

    private boolean shiftClickedSearchItem = false;

    @SubscribeEvent
    public void onWrappedScreenOpen(WrappedScreenOpenEvent event) {
        if (event.getWrappedScreenClass() != TradeMarketSearchResultScreen.class) return;

        boolean shouldOpen = false;

        switch (shiftBehaviorConfig.get()) {
            case NONE -> {
                shouldOpen = true;
            }
            case ENABLED_IF_SHIFT_HELD -> {
                if (shiftClickedSearchItem) {
                    shouldOpen = true;
                }
            }
            case DISABLED_IF_SHIFT_HELD -> {
                if (!shiftClickedSearchItem) {
                    shouldOpen = true;
                }
            }
        }

        if (shouldOpen) {
            event.setOpenScreen(true);
        }
    }

    @SubscribeEvent
    public void onContainerClick(ContainerClickEvent event) {
        if (!StyledText.fromComponent(event.getItemStack().getHoverName()).equalsString(SEARCH_ITEM_TITLE)) return;

        shiftClickedSearchItem = KeyboardUtils.isShiftDown();
    }

    @SubscribeEvent
    public void onRenderTooltip(ItemTooltipRenderEvent.Pre event) {
        if (shiftBehaviorConfig.get() == ShiftBehavior.NONE) return;
        if (McUtils.mc().screen == null) return;

        if (!Models.TradeMarket.isFilterScreen(McUtils.mc().screen.getTitle())) {
            return;
        }

        if (!StyledText.fromComponent(event.getItemStack().getHoverName()).equalsString(SEARCH_ITEM_TITLE)) return;

        ItemStack itemStack = event.getItemStack();

        List<Component> tooltips = new ArrayList<>(event.getTooltips());

        MutableComponent component =
                switch (shiftBehaviorConfig.get()) {
                    case NONE -> Component.empty();
                    case ENABLED_IF_SHIFT_HELD -> (Component.translatable(
                            "feature.wynntils.customTradeMarketResultScreen.shiftToEnable"));
                    case DISABLED_IF_SHIFT_HELD -> (Component.translatable(
                            "feature.wynntils.customTradeMarketResultScreen.shiftToDisable"));
                };

        tooltips.add(Component.empty());
        tooltips.add(component.withStyle(ChatFormatting.GREEN));

        event.setTooltips(tooltips);
    }

    private enum ShiftBehavior {
        NONE,
        ENABLED_IF_SHIFT_HELD,
        DISABLED_IF_SHIFT_HELD
    }
}
