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
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.UseItemEvent;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.items.game.HorseItem;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.COMBAT)
public class HorseMountFeature extends Feature {
    private static final ResourceLocation HORSE_WHISTLE_ID =
            ResourceLocation.fromNamespaceAndPath("wynntils", "horse.whistle");
    private static final SoundEvent HORSE_WHISTLE_SOUND = SoundEvent.createVariableRangeEvent(HORSE_WHISTLE_ID);

    private static final int SEARCH_RADIUS = 6; // Furthest blocks away from which we can interact with a horse
    private static final int SUMMON_ATTEMPTS = 8;
    private static final int SUMMON_DELAY_TICKS = 6;

    private static final List<StyledText> HORSE_ERROR_MESSAGES = List.of(
            StyledText.fromString("§4There is no room for a horse."),
            StyledText.fromString("§dYour horse is scared to come out right now, too many mobs are nearby."),
            StyledText.fromString("§4You cannot interact with your horse at the moment."),
            StyledText.fromString("§4You cannot use your horse here!"),
            StyledText.fromString("§4Your horse spawn was disabled (in vanish)!"),
            StyledText.fromString("§4You can not use a horse while in war."));

    private int prevItem = -1;
    private boolean alreadySetPrevItem = false;
    private boolean cancelMountingHorse = false;

    @RegisterKeyBind
    private final KeyBind mountHorseKeyBind = new KeyBind("Mount Horse", GLFW.GLFW_KEY_R, true, this::mountHorse);

    @Persisted
    public final Config<Boolean> guaranteedMount = new Config<>(true);

    @Persisted
    public final Config<Boolean> playWhistle = new Config<>(true);

    @SubscribeEvent
    public void onUseItem(UseItemEvent event) {
        if (!guaranteedMount.get()) return;

        ItemStack itemStack = McUtils.player().getMainHandItem();
        Optional<HorseItem> horseItemOpt = Models.Item.asWynnItem(itemStack, HorseItem.class);
        if (horseItemOpt.isEmpty()) return;

        mountHorse();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) // this needs to run before ChatRedirectFeature cancels the event
    public void onChatReceived(ChatMessageReceivedEvent e) {
        StyledText message = e.getOriginalStyledText();

        if (HORSE_ERROR_MESSAGES.contains(message)) {
            cancelMountingHorse = true;
        }
    }

    private void mountHorse() {
        if (!Models.WorldState.onWorld()) return;

        LocalPlayer player = McUtils.player();
        if (player.getVehicle() != null) {
            postHorseErrorMessage(MountHorseStatus.ALREADY_RIDING);
            return;
        }

        AbstractHorse horse = Models.Horse.searchForHorseNearby(player, SEARCH_RADIUS);
        if (horse == null) { // Horse has not spawned, we should do that
            int horseInventorySlot = Models.Horse.findHorseSlotNum();
            if (horseInventorySlot > 8 || horseInventorySlot == -1) {
                postHorseErrorMessage(MountHorseStatus.NO_HORSE);
                return;
            }
            trySummonAndMountHorse(horseInventorySlot, SUMMON_ATTEMPTS);
        } else { // Horse already exists, mount it
            mountHorse(horse);
        }
    }

    /** Horse should be nearby when this is called */
    private void mountHorse(Entity horse) {
        if (playWhistle.get()) {
            McUtils.playSoundAmbient(HORSE_WHISTLE_SOUND);
        }

        // swap to soul points to avoid right click problems
        int prevItem = McUtils.inventory().selected;
        int nonConflictingSlot = findNonConflictingSlot();
        if (nonConflictingSlot == -1) {
            postHorseErrorMessage(MountHorseStatus.CONFLICTING_SLOTS);
            return;
        }

        McUtils.sendPacket(new ServerboundSetCarriedItemPacket(nonConflictingSlot));
        McUtils.sendPacket(ServerboundInteractPacket.createInteractionPacket(horse, false, InteractionHand.MAIN_HAND));
        McUtils.sendPacket(new ServerboundSetCarriedItemPacket(prevItem));
    }

    /** Finds a hotbar slot where the item held allows us to safely mount a horse */
    private int findNonConflictingSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = McUtils.inventory().getItem(i);

            // empty hand
            if (itemStack.isEmpty()) {
                return i;
            }

            // horse item
            Optional<HorseItem> horseItemOpt = Models.Item.asWynnItem(itemStack, HorseItem.class);
            if (horseItemOpt.isPresent()) {
                return i;
            }

            // weapons
            Optional<GearItem> gearItemOpt = Models.Item.asWynnItem(itemStack, GearItem.class);
            if (gearItemOpt.isPresent() && gearItemOpt.get().getGearType().isWeapon()) {
                return i;
            }
        }

        return -1;
    }

    private void trySummonAndMountHorse(int horseInventorySlot, int attempts) {
        if (attempts <= 0) {
            postHorseErrorMessage(MountHorseStatus.NO_HORSE);
            return;
        }

        if (cancelMountingHorse) {
            McUtils.sendPacket(new ServerboundSetCarriedItemPacket(prevItem));
            cancelMountingHorse = false;
            return;
        }

        if (!alreadySetPrevItem) {
            prevItem = McUtils.inventory().selected;
            alreadySetPrevItem = true;
        }

        Managers.TickScheduler.scheduleLater(
                () -> {
                    LocalPlayer player = McUtils.player();
                    if (player == null) return;

                    AbstractHorse horse = Models.Horse.searchForHorseNearby(player, SEARCH_RADIUS);
                    if (horse != null) { // Horse successfully summoned
                        McUtils.sendPacket(new ServerboundSetCarriedItemPacket(prevItem));
                        alreadySetPrevItem = false;
                        mountHorse(horse);
                        return;
                    }
                    McUtils.sendPacket(new ServerboundSetCarriedItemPacket(horseInventorySlot));
                    McUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(
                            InteractionHand.MAIN_HAND, id, player.getXRot(), player.getYRot()));

                    trySummonAndMountHorse(horseInventorySlot, attempts - 1);
                },
                SUMMON_DELAY_TICKS);
    }

    private void postHorseErrorMessage(MountHorseStatus status) {
        Managers.Notification.queueMessage(
                Component.translatable(status.getTcString()).withStyle(ChatFormatting.DARK_RED));
    }

    private enum MountHorseStatus {
        NO_HORSE("feature.wynntils.horseMount.noHorse"),
        ALREADY_RIDING("feature.wynntils.horseMount.alreadyRiding"),
        CONFLICTING_SLOTS("feature.wynntils.horseMount.conflictingSlots");

        private final String tcString;

        MountHorseStatus(String tcString) {
            this.tcString = tcString;
        }

        private String getTcString() {
            return this.tcString;
        }
    }
}
