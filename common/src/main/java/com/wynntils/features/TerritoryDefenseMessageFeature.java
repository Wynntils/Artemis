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
import com.wynntils.mc.event.SoundPlayedEvent;
import com.wynntils.models.territories.type.GuildResourceValues;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UNCATEGORIZED)
public class TerritoryDefenseMessageFeature extends Feature {
    private static final Pattern ATTACK_SCREEN_TITLE = Pattern.compile("Attacking: (.+)");
    private static final Pattern TERRITORY_DEFENSE_PATTERN = Pattern.compile("Territory Defences: (.+)");
    private static final String DEFENSE_MESSAGE = "g %s defense is %s";
    private static final ResourceLocation ATTACK_SOUND =
            ResourceLocation.fromNamespaceAndPath("minecraft", "entity.ender_dragon.growl");
    // 2 seconds for the server to respond to an attack command
    private static final long MESSAGE_TIMEOUT = 2000;
    private final Queue<QueuedTerritory> queuedTerritories = new LinkedList<>();

    @SubscribeEvent
    public void onInventoryClick(InventoryMouseClickedEvent event) {
        if (event.getHoveredSlot() == null || McUtils.mc().screen == null) return;
        Matcher titleMatcher =
                ATTACK_SCREEN_TITLE.matcher(McUtils.mc().screen.getTitle().getString());
        if (!titleMatcher.matches()) return;

        ItemStack itemStack = event.getHoveredSlot().getItem();

        for (Component tooltipLine : LoreUtils.getTooltipLines(itemStack)) {
            Matcher matcher = StyledText.fromComponent(tooltipLine)
                    .getMatcher(TERRITORY_DEFENSE_PATTERN, PartStyle.StyleType.NONE);
            if (matcher.matches()) {
                // intentionally not localized to match Wynncraft language
                queuedTerritories.add(new QueuedTerritory(
                        titleMatcher.group(1),
                        System.currentTimeMillis(),
                        GuildResourceValues.fromString(matcher.group(1))));
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackSound(SoundPlayedEvent event) {
        if (queuedTerritories.isEmpty()
                || !event.getSoundInstance().getLocation().equals(ATTACK_SOUND)) {
            return;
        }

        while (!queuedTerritories.isEmpty()) {
            // remove all expired messages, then send the first one
            QueuedTerritory queuedTerritory = queuedTerritories.poll();
            if (System.currentTimeMillis() - queuedTerritory.timestamp() < MESSAGE_TIMEOUT) {
                Handlers.Command.sendCommandImmediately(DEFENSE_MESSAGE.formatted(
                        queuedTerritory.territory(), queuedTerritory.defense().getAsString()));
                break;
            }
        }
    }

    private record QueuedTerritory(String territory, long timestamp, GuildResourceValues defense) {}
}
