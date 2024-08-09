/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterCommand;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.items.WynnItem;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

@StartDisabled
@ConfigCategory(Category.DEBUG)
public class LogItemInfoFeature extends Feature {
    @RegisterKeyBind
    private final KeyBind logItemInfoKeyBind = new KeyBind(
            "Log Item Info", GLFW.GLFW_KEY_INSERT, true, this::onLogItemInfoPress, this::onLogItemInfoInventoryPress);

    @RegisterCommand
    private final LiteralCommandNode<CommandSourceStack> commandNode =
            Commands.literal("show").executes(this::showCommand).build();

    private void onLogItemInfoPress() {
        logItem(McUtils.player().getItemBySlot(EquipmentSlot.MAINHAND));
    }

    private void onLogItemInfoInventoryPress(Slot hoveredSlot) {
        if (hoveredSlot == null) return;
        logItem(hoveredSlot.getItem());
    }

    private void logItem(ItemStack itemStack) {
        String description = getDescription(itemStack);

        WynntilsMod.info(description);
        McUtils.sendMessageToClient(Component.literal(description).withStyle(ChatFormatting.AQUA));
    }

    private int showCommand(CommandContext<CommandSourceStack> context) {
        String description = getDescription(McUtils.player().getItemBySlot(EquipmentSlot.MAINHAND));

        WynntilsMod.info(description);
        context.getSource().sendSuccess(() -> Component.literal(description).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static String getDescription(ItemStack itemStack) {
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(itemStack);
        String wynnItemDesc = wynnItemOpt.isPresent() ? wynnItemOpt.get().toString() : "<N/A>";

        return "[Logging Item]\nName: "
                + StyledText.fromComponent(itemStack.getHoverName()) + "\nLore:\n"
                + StyledText.join("\n", LoreUtils.getLore(itemStack)) + "\nItem Type: "
                + itemStack.getItem() + "\nDamage Value: "
                + itemStack.getDamageValue() + "\nWynn Item: "
                + wynnItemDesc + "\nNBT: "
                + itemStack.getComponentsPatch().toString().replace('§', '&') + "\nGlint: "
                + itemStack.hasFoil();
    }
}
