/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.statuseffects;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.PlayerInfoFooterChangedEvent;
import com.wynntils.models.statuseffects.event.StatusEffectsChangedEvent;
import com.wynntils.models.statuseffects.type.StatusEffect;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class StatusEffectModel extends Model {
    /**
     * CG1 is the color and symbol used for the effect, and the strength modifier string (e.g. "79%")
     * NCG1 is for strength modifiers without a decimal, and the % sign
     * NCG2 is the decimal point and second \d+ option for strength modifiers with a decimal
     * CG2 is the actual name of the effect
     * CG3 is the duration string (eg. "1:23")
     * Note: Buffs like "+190 Main Attack Damage" will have the +190 be considered as part of the name.
     * Buffs like "17% Frenzy" will have the 17% be considered as part of the prefix.
     * This is because the 17% in Frenzy (and certain other buffs) can change, but the static scroll buffs cannot.
     * <p>
     *
     * <p>Originally taken from: <a href="https://github.com/Wynntils/Wynntils/pull/615">Legacy</a>
     */
    /*
     * current regex:
     * <prefix>         captures any characters before the §7 colour-indicator.
     * <modifier>       optionally captures effects' modifiers' values (+100, -20, 5)
     * <modifierSuffix> optionally captures the suffixes of the modifiers (/5s, /3s, %)
     * <name>           captures the effects name (Strength, and even +Lightweight)
     * <timer>          captures the effects duration ( (xx:xx) or (**:**) ).
     *
     * */

    // Test in StatusEffectModel_STATUS_EFFECT_PATTERN
    private static final Pattern STATUS_EFFECT_PATTERN = Pattern.compile(
            "(?<prefix>.+?)§7\\s?(?<modifier>(\\-|\\+)?([\\-\\.\\d]+))?(?<modifierSuffix>((\\/\\d+s)|%)?)?\\s?(?<name>\\+?['a-zA-Z\\/\\s]+?)\\s(?<timer>§[84a]\\((.+?)\\))");

    private static final StyledText STATUS_EFFECTS_TITLE = StyledText.fromString("§d§lStatus Effects");

    private List<StatusEffect> statusEffects = List.of();

    public StatusEffectModel() {
        super(List.of());
    }

    public List<StatusEffect> getStatusEffects() {
        return statusEffects;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldStateChanged(WorldStateEvent e) {
        statusEffects = List.of();
        WynntilsMod.postEvent(new StatusEffectsChangedEvent());
    }

    @SubscribeEvent
    public void onTabListCustomization(PlayerInfoFooterChangedEvent event) {
        StyledText footer = event.getFooter();

        if (footer.isEmpty()) {
            if (!statusEffects.isEmpty()) {
                statusEffects = List.of(); // No timers, get rid of them
                WynntilsMod.postEvent(new StatusEffectsChangedEvent());
            }
            return;
        }

        if (!footer.startsWith(STATUS_EFFECTS_TITLE)) return;

        List<StatusEffect> newStatusEffects = new ArrayList<>();

        StyledText[] effects = footer.split("\\s{2}"); // Effects are split up by 2 spaces
        for (StyledText effect : effects) {
            StyledText trimmedEffect = effect.trim();
            if (trimmedEffect.isEmpty()) continue;

            Matcher m = trimmedEffect.getMatcher(STATUS_EFFECT_PATTERN);
            if (!m.find()) continue;

            String color = ChatFormatting.GRAY.toString();

            StyledText prefix = StyledText.fromString(m.group("prefix").trim());
            StyledText name = StyledText.fromString(color + m.group("name").trim());
            StyledText displayedTime = StyledText.fromString(m.group("timer").trim());

            String modifierGroup = m.group("modifier");
            StyledText modifier =
                    modifierGroup == null ? StyledText.EMPTY : StyledText.fromString(color + modifierGroup.trim());
            String modifierSuffixGroup = m.group("modifierSuffix");

            StyledText modifierSuffix = modifierSuffixGroup == null
                    ? StyledText.EMPTY
                    : StyledText.fromString(color + modifierSuffixGroup.trim());

            newStatusEffects.add(new StatusEffect(name, modifier, modifierSuffix, displayedTime, prefix));
        }

        statusEffects = newStatusEffects;
        WynntilsMod.postEvent(new StatusEffectsChangedEvent());
    }
}
