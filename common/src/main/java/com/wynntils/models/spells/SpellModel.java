/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.spells;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Model;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.actionbar.event.ActionBarUpdatedEvent;
import com.wynntils.handlers.item.event.ItemRenamedEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.spells.actionbar.matchers.SpellSegmentMatcher;
import com.wynntils.models.spells.actionbar.segments.SpellSegment;
import com.wynntils.models.spells.event.SpellEvent;
import com.wynntils.models.spells.type.SpellDirection;
import com.wynntils.models.spells.type.SpellFailureReason;
import com.wynntils.models.spells.type.SpellType;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.neoforged.bus.api.SubscribeEvent;

public class SpellModel extends Model {
    private static final Pattern SPELL_CAST = Pattern.compile("^§7(.*) spell cast! §3\\[§b-([0-9]+) ✺§3\\]$");
    private static final int SPELL_COST_RESET_TICKS = 60;

    private SpellDirection[] lastSpell = SpellDirection.NO_SPELL;
    private String lastBurstSpellName = "";
    private String lastSpellName = "";
    private int repeatedBurstSpellCount = 0;
    private int repeatedSpellCount = 0;
    private int ticksSinceCastBurst = 0;
    private int ticksSinceCast = 0;

    public SpellModel() {
        super(List.of());

        Handlers.ActionBar.registerSegment(new SpellSegmentMatcher());
        Handlers.Item.registerKnownMarkerNames(getKnownMarkerNames());
    }

    private List<Pattern> getKnownMarkerNames() {
        List<Pattern> knownMarkerNames = new ArrayList<>();
        knownMarkerNames.add(SPELL_CAST);
        knownMarkerNames.addAll(Arrays.stream(SpellFailureReason.values())
                .map(s -> Pattern.compile(s.getMessage().getString()))
                .toList());
        return knownMarkerNames;
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onItemRenamed(ItemRenamedEvent event) {
        StyledText msg = event.getNewName();
        SpellFailureReason failureReason = SpellFailureReason.fromMsg(msg);
        if (failureReason != null) {
            WynntilsMod.postEvent(new SpellEvent.Failed(failureReason));
            return;
        }

        Matcher spellMatcher = msg.getMatcher(SPELL_CAST);
        if (spellMatcher.matches()) {
            SpellType spellType = SpellType.fromName(spellMatcher.group(1));
            int manaCost = Integer.parseInt(spellMatcher.group(2));
            WynntilsMod.postEvent(new SpellEvent.Cast(spellType, manaCost));
        }
    }

    @SubscribeEvent
    public void onActionBarUpdate(ActionBarUpdatedEvent event) {
        event.runIfPresent(SpellSegment.class, this::updateFromSpellSegment);
    }

    @SubscribeEvent
    public void onSpellCast(SpellEvent.Cast e) {
        ticksSinceCastBurst = 0;
        ticksSinceCast = 0;

        if (e.getSpellType().getName().equals(lastBurstSpellName)) {
            repeatedBurstSpellCount++;
        } else {
            repeatedBurstSpellCount = 1;
        }
        if (e.getSpellType().getName().equals(lastSpellName)) {
            repeatedSpellCount++;
        } else {
            repeatedSpellCount = 1;
        }

        lastBurstSpellName = e.getSpellType().getName();
        lastSpellName = e.getSpellType().getName();
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        if (!lastBurstSpellName.isEmpty()) {
            ticksSinceCastBurst++;
        }
        if (!lastSpellName.isEmpty()) {
            ticksSinceCast++;
        }

        if (ticksSinceCastBurst >= SPELL_COST_RESET_TICKS) {
            lastBurstSpellName = "";
            repeatedBurstSpellCount = 0;
            ticksSinceCastBurst = 0;
        }
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent e) {
        lastBurstSpellName = "";
        lastSpellName = "";
        repeatedBurstSpellCount = 0;
        repeatedSpellCount = 0;
        ticksSinceCastBurst = 0;
        ticksSinceCast = 0;
    }

    public String getLastBurstSpellName() {
        return lastBurstSpellName;
    }

    public String getLastSpellName() {
        return lastSpellName;
    }

    public int getRepeatedBurstSpellCount() {
        return repeatedBurstSpellCount;
    }

    public int getRepeatedSpellCount() {
        return repeatedSpellCount;
    }

    public int getTicksSinceCastBurst() {
        return ticksSinceCastBurst;
    }

    public int getTicksSinceCast() {
        return ticksSinceCast;
    }

    private void updateFromSpellSegment(SpellSegment spellSegment) {
        // noop if the spell state hasn't changed
        if (Arrays.equals(spellSegment.getDirections(), lastSpell)) return;
        lastSpell = spellSegment.getDirections();

        WynntilsMod.postEvent(new SpellEvent.Partial(lastSpell));

        if (lastSpell.length == 3) {
            WynntilsMod.postEvent(new SpellEvent.Completed(lastSpell, SpellType.fromSpellDirectionArray(lastSpell)));
        }
    }
}
