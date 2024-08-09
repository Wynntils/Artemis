/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.BossHealthUpdateEvent;
import com.wynntils.mc.mixin.accessors.ClientboundBossEventPacketAccessor;
import com.wynntils.utils.StringUtils;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket.AddOperation;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket.Operation;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket.UpdateNameOperation;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.COMBAT)
public class AbbreviateMobHealthFeature extends Feature {
    private static final Pattern MOB_HEALTH_PATTERN =
            Pattern.compile("(.*§[cb])(?<current>\\d+)(§.\\/(?<max>\\d+))?(§[cb4]\\s?❤.*)");

    @SubscribeEvent
    public void onHealthBarEvent(BossHealthUpdateEvent event) {
        ClientboundBossEventPacket packet = event.getPacket();
        Operation operation = ((ClientboundBossEventPacketAccessor) packet).getOperation();

        if (operation instanceof AddOperation addOperation) {
            addOperation.name = transformHealthComponent(addOperation.name);
            return;
        }

        if (operation instanceof UpdateNameOperation updateOperation) {
            updateOperation.name = transformHealthComponent(updateOperation.name);
            return;
        }
    }

    private Component transformHealthComponent(Component component) {
        String name = component.getString();
        Matcher healthMatcher = MOB_HEALTH_PATTERN.matcher(name);
        if (!healthMatcher.matches()) return component;

        int rawHealth = Integer.parseInt(healthMatcher.group("current"));
        String formattedHealth = StringUtils.integerToShortString(rawHealth).toUpperCase(Locale.ROOT);

        String maxHealthString = healthMatcher.group("max");
        if (maxHealthString != null) {
            int rawMaxHealth = Integer.parseInt(maxHealthString);
            String formattedMaxHealth = healthMatcher
                    .group(3)
                    .replace(
                            maxHealthString,
                            StringUtils.integerToShortString(rawMaxHealth).toUpperCase(Locale.ROOT));
            formattedHealth += formattedMaxHealth;
        }

        String formattedName = healthMatcher.replaceAll("$1" + formattedHealth + "$5");
        return Component.literal(formattedName);
    }
}
