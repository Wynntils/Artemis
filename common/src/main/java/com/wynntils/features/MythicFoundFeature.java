/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.containers.event.MythicFoundEvent;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.items.items.game.GearBoxItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.items.game.InsulatorItem;
import com.wynntils.models.items.items.game.SimulatorItem;
import com.wynntils.utils.mc.McUtils;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

public class MythicFoundFeature extends Feature {
    private static final ResourceLocation MYTHIC_FOUND_ID =
            ResourceLocation.fromNamespaceAndPath("wynntils", "misc.mythic-found");
    private static final SoundEvent MYTHIC_FOUND_SOUND = SoundEvent.createVariableRangeEvent(MYTHIC_FOUND_ID);

    @Persisted
    public final Config<Boolean> playSound = new Config<>(true);

    @Persisted
    public final Config<Boolean> showDryStreakMessage = new Config<>(true);

    @SubscribeEvent
    public void onMythicFound(MythicFoundEvent event) {
        if (playSound.get()) {
            McUtils.playSoundAmbient(MYTHIC_FOUND_SOUND);
        }

        if (!showDryStreakMessage.get()) return;

        ItemStack itemStack = event.getMythicBoxItem();

        // Normal loot chest reward
        Optional<GearBoxItem> gearBoxItem = Models.Item.asWynnItem(itemStack, GearBoxItem.class);
        if (gearBoxItem.isPresent()) {
            if (gearBoxItem.get().getGearType() != GearType.MASTERY_TOME) {
                sendNormalDryStreakMessage(
                        StyledText.fromComponent(event.getMythicBoxItem().getHoverName()));
            }
            return;
        }

        // Lootrun rewards
        boolean validLootrunMythic = false;
        Optional<GearItem> gearItem = Models.Item.asWynnItem(itemStack, GearItem.class);
        if (gearItem.isPresent()) {
            validLootrunMythic = true;
        }

        Optional<InsulatorItem> insulatorItem = Models.Item.asWynnItem(itemStack, InsulatorItem.class);
        if (insulatorItem.isPresent()) {
            validLootrunMythic = true;
        }

        Optional<SimulatorItem> simulatorItem = Models.Item.asWynnItem(itemStack, SimulatorItem.class);
        if (simulatorItem.isPresent()) {
            validLootrunMythic = true;
        }

        if (validLootrunMythic) {
            sendLootrunDryStreakMessage(
                    StyledText.fromComponent(event.getMythicBoxItem().getHoverName()));
        }
    }

    private void sendLootrunDryStreakMessage(StyledText itemName) {
        McUtils.sendMessageToClient(Component.literal("Dry streak broken! Found an ")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(itemName.getComponent())
                .append(Component.literal(" after ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(Models.Lootrun.dryPulls.get() + " pulls")
                                .withStyle(ChatFormatting.GOLD)))
                .append(Component.literal(" without a mythic.").withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    private static void sendNormalDryStreakMessage(StyledText itemName) {
        McUtils.sendMessageToClient(Component.literal("Dry streak broken! Found an ")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(itemName.getComponent())
                .append(Component.literal(" in chest ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal("#" + Models.LootChest.getOpenedChestCount())
                                .withStyle(ChatFormatting.GOLD)))
                .append(Component.literal(" after ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(String.valueOf(Models.LootChest.getDryCount()))
                                .withStyle(ChatFormatting.DARK_PURPLE)))
                .append(Component.literal(" dry chests and "))
                .append(Component.literal(String.valueOf(Models.LootChest.getDryBoxes()))
                        .withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" dry boxes.")));
    }
}
