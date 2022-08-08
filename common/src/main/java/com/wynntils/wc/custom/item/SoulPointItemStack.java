/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.custom.item;

import com.wynntils.features.user.SoulPointTimerFeature;
import com.wynntils.mc.utils.McUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class SoulPointItemStack extends WynnItemStack {
    private final List<Component> tooltip;

    public SoulPointItemStack(ItemStack stack) {
        super(stack);

        tooltip = getOriginalTooltip();
    }

    @Override
    public List<Component> getTooltipLines(Player player, TooltipFlag isAdvanced) {
        if (SoulPointTimerFeature.INSTANCE.isEnabled()) {
            List<Component> copy = new ArrayList<>(tooltip);

            copy.add(new TextComponent(" "));

            int rawSecondsUntilSoulPoint = getTicksTillNextSoulPoint() / 20;
            int minutesUntilSoulPoint = rawSecondsUntilSoulPoint / 60;
            int secondsUntilSoulPoint = rawSecondsUntilSoulPoint % 60;

            copy.add(new TranslatableComponent(
                    "feature.wynntils.soulPointTimer.lore",
                    minutesUntilSoulPoint,
                    String.format("%02d", secondsUntilSoulPoint)));

            return copy;
        }

        return tooltip;
    }

    /**
     * @return Time in game ticks (1/20th of a second, 50ms) until next soul point
     *     <p>-1 if unable to determine
     */
    private static int getTicksTillNextSoulPoint() {
        if (McUtils.mc().level == null) return -1;

        return 24000 - (int) (McUtils.mc().level.getDayTime() % 24000);
    }
}
