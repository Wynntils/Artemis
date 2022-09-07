/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.managers.Model;
import com.wynntils.mc.event.ChatPacketReceivedEvent;
import com.wynntils.utils.StringUtils;
import com.wynntils.wynn.event.ActionBarMessageUpdateEvent;
import com.wynntils.wynn.objects.Powder;
import com.wynntils.wynn.utils.WynnUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ActionBarModel extends Model {
    private static final Pattern ACTIONBAR_PATTERN =
            StringUtils.compileCCRegex("§❤ ([0-9]+)/([0-9]+)§ +(.+?) +§✺ ([0-9]+)/([0-9]+)");
    private static final Pattern POWDER_CHARGE_PATTERN = Pattern.compile("§.+([✤✦❉✹❋]+) (\\d+)%");
    private static final Pattern COORDINATES_PATTERN = Pattern.compile("§7(-?\\d+)§f .+§(-?\\d+) (-?\\d+)");

    private static Component previousMessage = null;

    private static float powderSpecialCharge = -1;
    private static Powder powderSpecialType = null;
    private static int currentHealth = -1;
    private static int maxHealth = -1;
    private static int currentMana = -1;
    private static int maxMana = -1;

    /** Needed for all Models */
    public static void init() {}

    @SubscribeEvent
    public static void onActionBarUpdate(ChatPacketReceivedEvent e) {
        if (!WynnUtils.onWorld() || e.getType() != ChatType.GAME_INFO) return;

        String actionBar = e.getMessage().getString();

        Matcher matcher = ACTIONBAR_PATTERN.matcher(actionBar);
        if (!matcher.matches()) return;

        currentHealth = Integer.parseInt(matcher.group(1));
        maxHealth = Integer.parseInt(matcher.group(2));
        currentMana = Integer.parseInt(matcher.group(4));
        maxMana = Integer.parseInt(matcher.group(5));

        if (previousMessage != null && actionBar.equals(previousMessage.getString())) return;

        String centerActionString = matcher.group(3);

        Matcher powderChargeMatcher = POWDER_CHARGE_PATTERN.matcher(centerActionString);
        if (powderChargeMatcher.matches()) {
            char symbol = powderChargeMatcher.group(1).charAt(0);
            String amountStr = powderChargeMatcher.group(2);
            powderSpecialCharge = Integer.parseInt(amountStr);
            powderSpecialType = Powder.getFromSymbol(symbol);
        } else if (COORDINATES_PATTERN.matcher(centerActionString).matches()) { // This only happens if charge is lost.
            powderSpecialCharge = -1;
            powderSpecialType = null;
        }

        ActionBarMessageUpdateEvent.ActionText actionText =
                new ActionBarMessageUpdateEvent.ActionText(centerActionString);
        ActionBarMessageUpdateEvent.HealthText healthText =
                new ActionBarMessageUpdateEvent.HealthText("§c❤ " + currentHealth + "/" + maxHealth);
        ActionBarMessageUpdateEvent.ManaText manaText =
                new ActionBarMessageUpdateEvent.ManaText("§b✺ " + currentMana + "/" + maxMana);

        WynntilsMod.getEventBus().post(actionText);
        WynntilsMod.getEventBus().post(healthText);
        WynntilsMod.getEventBus().post(manaText);

        MutableComponent modified = new TextComponent(healthText.getMessage())
                .append("    ")
                .append(actionText.getMessage())
                .append("    ")
                .append(manaText.getMessage());

        previousMessage = modified;

        e.setMessage(modified);
    }

    public static int getCurrentHealth() {
        return currentHealth;
    }

    public static int getMaxHealth() {
        return maxHealth;
    }

    public static int getCurrentMana() {
        return currentMana;
    }

    public static int getMaxMana() {
        return maxMana;
    }

    public static float getPowderSpecialCharge() {
        return powderSpecialCharge;
    }

    public static Powder getPowderSpecialType() {
        return powderSpecialType;
    }
}
