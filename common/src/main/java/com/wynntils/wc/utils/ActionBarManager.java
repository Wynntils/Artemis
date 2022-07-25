/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.utils;

import com.wynntils.core.WynntilsMod;
import com.wynntils.mc.event.ChatReceivedEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.ChatType;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ActionBarManager {

    private static final Pattern ACTIONBAR_PATTERN =
            Pattern.compile("§❤ ([0-9]+)/([0-9]+)§ +(.+?) +§✺ ([0-9]+)/([0-9]+)".replace("§", "(?:§[0-9a-fklmnor])*"));

    private static String previousActionBar = null;

    private static int currentHealth = -1;
    private static int maxHealth = -1;
    private static int currentMana = -1;
    private static int maxMana = -1;

    @SubscribeEvent
    public static void onActionBarUpdate(ChatReceivedEvent e) {
        if (!WynnUtils.onWorld() || e.getType() != ChatType.GAME_INFO) return;

        String actionBar = e.getMessage().getString();
        // don't parse unchanged actionbar
        if (actionBar.equals(previousActionBar)) return;
        previousActionBar = actionBar;

        Matcher matcher = ACTIONBAR_PATTERN.matcher(actionBar);
        if (!matcher.matches()) return;

        currentHealth = Integer.parseInt(matcher.group(1));
        maxHealth = Integer.parseInt(matcher.group(2));
        currentMana = Integer.parseInt(matcher.group(4));
        maxMana = Integer.parseInt(matcher.group(5));

        String centerText = matcher.group(3);
        SpellManager.tryUpdateSpell(centerText);
    }

    public static void init() {
        WynntilsMod.getEventBus().register(ActionBarManager.class);
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
}
