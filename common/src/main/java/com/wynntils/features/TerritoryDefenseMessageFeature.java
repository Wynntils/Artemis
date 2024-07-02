/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.InventoryMouseClickedEvent;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UNCATEGORIZED)
public class TerritoryDefenseMessageFeature extends Feature {
    private static final Pattern ATTACK_SCREEN_TITLE = Pattern.compile("Attacking: (.+)");
    private static final Pattern TERRITORY_DEFENSE_PATTERN = Pattern.compile("Territory Defences: (.+)");
    private static final Pattern TOO_MANY_ATTACKS =
            Pattern.compile("§cYou can only attack \\w+ ?territor(?:y|ies) at a time\\.");

    @SubscribeEvent
    public void onInventoryClick(InventoryMouseClickedEvent event) {
        if (event.getHoveredSlot() == null || McUtils.mc().screen == null) return;
        Matcher titleMatcher =
                ATTACK_SCREEN_TITLE.matcher(McUtils.mc().screen.getTitle().getString());
        if (!titleMatcher.matches()) return;

        ItemStack itemStack = event.getHoveredSlot().getItem();

        Matcher tooManyAttacksMatcher =
                TOO_MANY_ATTACKS.matcher(LoreUtils.getStringLore(itemStack).getString());
        if (tooManyAttacksMatcher.find()) return; // invalid attack, don't send defence info

        for (Component tooltipLine : LoreUtils.getTooltipLines(itemStack)) {
            Matcher matcher = StyledText.fromComponent(tooltipLine)
                    .getMatcher(TERRITORY_DEFENSE_PATTERN, PartStyle.StyleType.NONE);
            if (matcher.matches()) {
                String message = I18n.get(
                        "feature.wynntils.territoryDefenseMessage.message", titleMatcher.group(1), matcher.group(1));
                Handlers.Command.sendCommandImmediately("g " + message);
                return;
            }
        }
    }
}
