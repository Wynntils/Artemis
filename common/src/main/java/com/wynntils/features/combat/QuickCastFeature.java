/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.spells.event.SpellEvent;
import com.wynntils.models.spells.type.SpellDirection;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ItemUtils;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.COMBAT)
public class QuickCastFeature extends Feature {
    @RegisterKeyBind
    private final KeyBind castFirstSpell = new KeyBind("Cast 1st Spell", GLFW.GLFW_KEY_Z, true, this::castFirstSpell);

    @RegisterKeyBind
    private final KeyBind castSecondSpell = new KeyBind("Cast 2nd Spell", GLFW.GLFW_KEY_X, true, this::castSecondSpell);

    @RegisterKeyBind
    private final KeyBind castThirdSpell = new KeyBind("Cast 3rd Spell", GLFW.GLFW_KEY_C, true, this::castThirdSpell);

    @RegisterKeyBind
    private final KeyBind castFourthSpell = new KeyBind("Cast 4th Spell", GLFW.GLFW_KEY_V, true, this::castFourthSpell);

    @Persisted
    private final Config<Integer> rightClickTickDelay = new Config<>(3);

    @Persisted
    private final Config<Boolean> safeCasting = new Config<>(true);

    @Persisted
    private final Config<Integer> spellCooldown = new Config<>(0);

    private static final Pattern INCORRECT_CLASS_PATTERN = compileCCRegex("§✖§ Class Req: (.+)");
    private static final Pattern LVL_MIN_NOT_REACHED_PATTERN = compileCCRegex("§✖§ (.+) Min: ([0-9]+)");

    private SpellDirection[] spellInProgress = SpellDirection.NO_SPELL;

    private static final Queue<SpellDirection> SPELL_PACKET_QUEUE = new LinkedList<>();

    private int packetCountdown = 0;
    private int spellCountdown = 0;
    private int lastSelectedSlot = 0;

    @SubscribeEvent
    public void onSpellSequenceUpdate(SpellEvent.Partial e) {
        updateSpell(e.getSpellDirectionArray());
    }

    private void updateSpell(SpellDirection[] spell) {
        if (Arrays.equals(spellInProgress, spell)) return;
        if (spell.length == 3) {
            spellInProgress = SpellDirection.NO_SPELL;
            spellCountdown = 0;
        } else {
            spellInProgress = spell;
            spellCountdown = 40;
        }
    }

    private void castFirstSpell() {
        tryCastSpell(SpellUnit.PRIMARY, SpellUnit.SECONDARY, SpellUnit.PRIMARY);
    }

    private void castSecondSpell() {
        tryCastSpell(SpellUnit.PRIMARY, SpellUnit.PRIMARY, SpellUnit.PRIMARY);
    }

    private void castThirdSpell() {
        tryCastSpell(SpellUnit.PRIMARY, SpellUnit.SECONDARY, SpellUnit.SECONDARY);
    }

    private void castFourthSpell() {
        tryCastSpell(SpellUnit.PRIMARY, SpellUnit.PRIMARY, SpellUnit.SECONDARY);
    }

    private void tryCastSpell(SpellUnit a, SpellUnit b, SpellUnit c) {
        if (!SPELL_PACKET_QUEUE.isEmpty()) {
            sendCancelReason(Component.translatable("feature.wynntils.quickCast.anotherInProgress"));
            return;
        }

        ItemStack heldItem = McUtils.player().getItemInHand(InteractionHand.MAIN_HAND);

        if (!ItemUtils.isWeapon(heldItem)) {
            sendCancelReason(Component.translatable("feature.wynntils.quickCast.notAWeapon"));
            return;
        }

        List<StyledText> loreLines = LoreUtils.getLore(heldItem);

        boolean isArcher = false;
        for (StyledText lore : loreLines) {
            if (lore.contains("Archer/Hunter")) isArcher = true;
            Matcher matcher = lore.getMatcher(INCORRECT_CLASS_PATTERN);
            if (!matcher.matches()) continue;
            sendCancelReason(Component.translatable("feature.wynntils.quickCast.classMismatch", matcher.group(1)));
            return;
        }

        for (StyledText lore : loreLines) {
            Matcher matcher = lore.getMatcher(LVL_MIN_NOT_REACHED_PATTERN);
            if (!matcher.matches()) continue;
            sendCancelReason(Component.translatable(
                    "feature.wynntils.quickCast.levelRequirementNotReached", matcher.group(1), matcher.group(2)));
            return;
        }

        boolean isSpellInverted = isArcher;
        List<SpellDirection> spell = Stream.of(a, b, c)
                .map(x -> (x == SpellUnit.PRIMARY) != isSpellInverted ? SpellDirection.RIGHT : SpellDirection.LEFT)
                .toList();

        if (safeCasting.get()) {
            for (int i = 0; i < spellInProgress.length; ++i) {
                if (spellInProgress[i] != spell.get(i)) {
                    sendCancelReason(Component.translatable("feature.wynntils.quickCast.incompatibleInProgress"));
                    return;
                }
            }
        }

        lastSelectedSlot = McUtils.inventory().selected;
        List<SpellDirection> remainder = spell.subList(spellInProgress.length, spell.size());
        SPELL_PACKET_QUEUE.addAll(remainder);
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        if (!Models.WorldState.onWorld()) return;

        // Clear spell after the 40 tick timeout period
        if (spellCountdown > 0) {
            spellCountdown--;
            if (spellCountdown <= 0) {
                spellInProgress = SpellDirection.NO_SPELL;
            }
        }

        if (packetCountdown > 0) {
            packetCountdown--;
        }

        if (packetCountdown > 0) return;
        if (SPELL_PACKET_QUEUE.isEmpty()) return;

        int currSelectedSlot = McUtils.inventory().selected;
        boolean slotChanged = currSelectedSlot != lastSelectedSlot;

        if (slotChanged) {
            McUtils.sendPacket(new ServerboundSetCarriedItemPacket(lastSelectedSlot));
        }

        // Right clicks need a tick delay between them (2-3 ticks), left clicks don't
        boolean didRightClick = false;
        do {
            SpellDirection spellDirection = SPELL_PACKET_QUEUE.poll();
            spellDirection.getSendPacketRunnable().run();

            if (spellDirection == SpellDirection.RIGHT) {
                didRightClick = true;
            }
        } while (!SPELL_PACKET_QUEUE.isEmpty()
                && (!didRightClick || SPELL_PACKET_QUEUE.peek() != SpellDirection.RIGHT));

        if (slotChanged) {
            McUtils.sendPacket(new ServerboundSetCarriedItemPacket(currSelectedSlot));
        }

        // Right clicks need a tick delay between them (2-3 ticks), left clicks don't
        // So, we add a delay between right clicks, and new casts
        packetCountdown = didRightClick ? rightClickTickDelay.get() : 0;
        if (SPELL_PACKET_QUEUE.isEmpty()) {
            packetCountdown = Math.max(packetCountdown, spellCooldown.get());
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldStateEvent e) {
        SPELL_PACKET_QUEUE.clear();
        spellInProgress = SpellDirection.NO_SPELL;
    }

    private static void sendCancelReason(MutableComponent reason) {
        Managers.Notification.queueMessage(reason.withStyle(ChatFormatting.RED));
    }

    private static Pattern compileCCRegex(String regex) {
        return Pattern.compile(regex.replace("§", "(?:§[0-9a-fklmnor])*"));
    }

    public enum SpellUnit {
        PRIMARY,
        SECONDARY
    }
}
