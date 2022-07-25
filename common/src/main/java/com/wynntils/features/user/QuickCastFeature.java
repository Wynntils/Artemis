/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyHolder;
import com.wynntils.mc.event.ClientTickEvent;
import com.wynntils.mc.utils.ItemUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wc.utils.WynnUtils;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(stability = FeatureInfo.Stability.EXPERIMENTAL)
public class QuickCastFeature extends UserFeature {

    @RegisterKeyBind
    private final KeyHolder castFirstSpell =
            new KeyHolder("Cast 1st Spell", GLFW.GLFW_KEY_Z, "Wynntils", true, QuickCastFeature::castFirstSpell);

    @RegisterKeyBind
    private final KeyHolder castSecondSpell =
            new KeyHolder("Cast 2nd Spell", GLFW.GLFW_KEY_X, "Wynntils", true, QuickCastFeature::castSecondSpell);

    @RegisterKeyBind
    private final KeyHolder castThirdSpell =
            new KeyHolder("Cast 3rd Spell", GLFW.GLFW_KEY_C, "Wynntils", true, QuickCastFeature::castThirdSpell);

    @RegisterKeyBind
    private final KeyHolder castFourthSpell =
            new KeyHolder("Cast 4th Spell", GLFW.GLFW_KEY_V, "Wynntils", true, QuickCastFeature::castFourthSpell);

    private static final Pattern INCORRECT_CLASS_PATTERN =
            Pattern.compile("§✖§ Class Req: (.+)".replace("§", "(?:§[0-9a-fklmnor])*"));
    private static final Pattern LVL_MIN_NOT_REACHED_PATTERN =
            Pattern.compile("§✖§ (.+) Min: ([0-9]+)".replace("§", "(?:§[0-9a-fklmnor])*"));
    private static final Packet<?> RIGHT_CLICK_PACKET = new ServerboundUseItemPacket(InteractionHand.MAIN_HAND);
    private static final Packet<?> LEFT_CLICK_PACKET = new ServerboundSwingPacket(InteractionHand.MAIN_HAND);
    private static final Queue<Packet<?>> SPELL_PACKET_QUEUE = new LinkedList<>();
    private static final boolean RIGHT = true;
    private static final boolean LEFT = false;

    public static void castFirstSpell() {
        tryCastSpell(RIGHT, LEFT, RIGHT);
    }

    public static void castSecondSpell() {
        tryCastSpell(RIGHT, RIGHT, RIGHT);
    }

    public static void castThirdSpell() {
        tryCastSpell(RIGHT, LEFT, LEFT);
    }

    public static void castFourthSpell() {
        tryCastSpell(RIGHT, RIGHT, LEFT);
    }

    private static void tryCastSpell(boolean a, boolean b, boolean c) {
        ItemStack heldItem = McUtils.player().getItemInHand(InteractionHand.MAIN_HAND);

        if (!ItemUtils.isWeapon(heldItem)) {
            McUtils.sendMessageToClient(
                    new TextComponent("You can only quick-cast with a weapon!").withStyle(ChatFormatting.RED));
            return;
        }

        List<String> loreLines = ItemUtils.getLore(heldItem);

        for (String lore : loreLines) {
            Matcher matcher = INCORRECT_CLASS_PATTERN.matcher(lore);
            if (matcher.matches()) {
                McUtils.sendMessageToClient(new TextComponent("You can't use " + matcher.group(1) + "-type weapons!")
                        .withStyle(ChatFormatting.RED));
                return;
            }
        }

        for (String lore : loreLines) {
            Matcher matcher = LVL_MIN_NOT_REACHED_PATTERN.matcher(lore);
            if (matcher.matches()) {
                McUtils.sendMessageToClient(new TextComponent("You need to reach " + matcher.group(1) + " "
                                + matcher.group(2) + " before using this weapon.")
                        .withStyle(ChatFormatting.RED));
                return;
            }
        }

        // TODO: Check for in-progress spells

        // TODO: Actually check for archer class
        boolean isSpellInverted = true;
        castSpellSequence(Stream.of(a, b, c).map(x -> isSpellInverted != x).toList());
    }

    static int time = 0;

    @SubscribeEvent
    public static void onTick(ClientTickEvent e) {
        if (!WynnUtils.onWorld() || e.getTickPhase() != ClientTickEvent.Phase.END) return;
        time++;
        if (time % 2 == 0 || SPELL_PACKET_QUEUE.isEmpty()) return;
        McUtils.sendPacket(SPELL_PACKET_QUEUE.poll());
    }

    private static void castSpellSequence(List<Boolean> spell) {
        spell.stream().map(x -> x ? RIGHT_CLICK_PACKET : LEFT_CLICK_PACKET).forEach(SPELL_PACKET_QUEUE::add);
    }
}
