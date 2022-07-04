/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.mc.event.ContainerCloseEvent;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wc.utils.ContainerUtils;
import com.wynntils.wc.utils.WynnItemMatchers;
import com.wynntils.wc.utils.WynnUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(stability = Stability.STABLE)
public class MythicBlockerFeature extends UserFeature {

    @SubscribeEvent
    public void onChestCloseAttempt(ContainerCloseEvent.Pre e) {
        if (!WynnUtils.onWorld()) return;
        if (!ContainerUtils.isLootOrRewardChest(McUtils.screen())) return;

        NonNullList<ItemStack> items = ContainerUtils.getItems(McUtils.screen());
        for (int i = 0; i < 27; i++) {
            ItemStack stack = items.get(i);
            if (WynnItemMatchers.isMythic(stack)) {
                McUtils.sendMessageToClient(new TranslatableComponent("feature.wynntils.mythicBlocker.closingBlocked")
                        .withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }
        }
    }
}
