/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.utils;

import com.wynntils.core.WynntilsMod;
import com.wynntils.mc.event.TitleSetTextEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpellManager {
    private static final Pattern SPELL_PATTERN = Pattern.compile(
            "§([LR]|Right|Left)§-§([LR?]|Right|Left)§-§([LR?]|Right|Left)§".replace("§", "(?:§[0-9a-fklmnor])*"));
    public static final boolean[] NO_SPELL = new boolean[0];
    public static boolean[] lastSpell = NO_SPELL;

    private static boolean[] getSpellFromString(String string) {
        Matcher spellMatcher = SPELL_PATTERN.matcher(string);
        if (!spellMatcher.matches()) return null;

        int size = 1;
        for (; size < 3; ++size) {
            if (spellMatcher.group(size + 1).equals("?")) break;
        }

        boolean[] spell = new boolean[size];
        for (int i = 0; i < size; ++i) {
            spell[i] = spellMatcher.group(i + 1).charAt(0) == 'R';
        }

        return spell;
    }

    public static void tryUpdateSpell(String text) {
        boolean[] spell = getSpellFromString(text);
        if (spell == null) return;
        lastSpell = spell.length == 3 ? NO_SPELL : spell;
    }

    @SubscribeEvent
    public static void onTitleUpdate(TitleSetTextEvent e) {
        if (!WynnUtils.onWorld()) return;
        tryUpdateSpell(e.getComponent().getString());
    }

    public static void init() {
        WynntilsMod.getEventBus().register(SpellManager.class);
    }

    private static boolean[] getLastSpell() {
        return lastSpell;
    }
}
