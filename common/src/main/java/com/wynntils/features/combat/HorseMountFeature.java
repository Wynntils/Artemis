/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.features.Feature;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.mc.event.UseItemEvent;
import com.wynntils.models.items.items.game.HorseItem;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.InventoryUtils;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.COMBAT)
public class HorseMountFeature extends Feature {
    private static final int SEARCH_RADIUS = 6; // Furthest blocks away from which we can interact with a horse
    private static final int SUMMON_ATTEMPTS = 8;
    private static final int SUMMON_DELAY_TICKS = 6;

    private int prevItem = -1;
    private boolean alreadySetPrevItem = false;

    @RegisterKeyBind
    private final KeyBind mountHorseKeyBind = new KeyBind("Mount Horse", GLFW.GLFW_KEY_R, true, this::mountHorse);

    @RegisterConfig
    public final Config<Boolean> guaranteedMount = new Config<>(true);

    @SubscribeEvent
    public void onUseItem(UseItemEvent event) {
        if (!guaranteedMount.get()) return;

        ItemStack itemStack = McUtils.player().getMainHandItem();
        Optional<HorseItem> horseItemOpt = Models.Item.asWynnItem(itemStack, HorseItem.class);
        if (horseItemOpt.isEmpty()) return;

        mountHorse();
        event.setCanceled(true);
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
            trySummonAndMountHorse(horseInventorySlot, SUMMON_ATTEMPTS, player);
        } else { // Horse already exists, mount it
            mountHorse(horse);
        }
    }

    /** Horse should be nearby when this is called */
    private void mountHorse(Entity horse) {
        // swap to soul points to avoid right click problems
        int prevItem = McUtils.inventory().selected;
        McUtils.sendPacket(new ServerboundSetCarriedItemPacket(InventoryUtils.SOUL_POINTS_SLOT_NUM));
        McUtils.sendPacket(ServerboundInteractPacket.createInteractionPacket(horse, false, InteractionHand.MAIN_HAND));
        McUtils.sendPacket(new ServerboundSetCarriedItemPacket(prevItem));
    }

    private void trySummonAndMountHorse(int horseInventorySlot, int attempts, LocalPlayer player) {
        if (attempts <= 0) {
            postHorseErrorMessage(MountHorseStatus.NO_HORSE);
            return;
        }

        if (!alreadySetPrevItem) {
            prevItem = McUtils.inventory().selected;
            alreadySetPrevItem = true;
        }

        Managers.TickScheduler.scheduleLater(
                () -> {
                    AbstractHorse horse = Models.Horse.searchForHorseNearby(player, SEARCH_RADIUS);
                    if (horse != null) { // Horse successfully summoned
                        McUtils.sendPacket(new ServerboundSetCarriedItemPacket(prevItem));
                        alreadySetPrevItem = false;
                        mountHorse(horse);
                        return;
                    }
                    McUtils.sendPacket(new ServerboundSetCarriedItemPacket(horseInventorySlot));
                    McUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id));

                    trySummonAndMountHorse(horseInventorySlot, attempts - 1, player);
                },
                SUMMON_DELAY_TICKS);
    }

    private void postHorseErrorMessage(MountHorseStatus status) {
        Managers.Notification.queueMessage(
                Component.translatable(status.getTcString()).withStyle(ChatFormatting.DARK_RED));
    }

    private enum MountHorseStatus {
        NO_HORSE("feature.wynntils.horseMount.noHorse"),
        ALREADY_RIDING("feature.wynntils.horseMount.alreadyRiding");

        private final String tcString;

        MountHorseStatus(String tcString) {
            this.tcString = tcString;
        }

        private String getTcString() {
            return this.tcString;
        }
    }
}
