/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.commands.CommandBase;
import com.wynntils.core.managers.UpdateManager;
import com.wynntils.mc.utils.McUtils;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;

public class UpdateCommand extends CommandBase {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getBaseCommandBuilder() {
        return Commands.literal("update").executes(this::update);
    }

    private int update(CommandContext<CommandSourceStack> context) {
        if (WynntilsMod.isDevelopmentEnvironment()) {
            context.getSource()
                    .sendFailure(new TranslatableComponent("feature.wynntils.updates.error.development")
                            .withStyle(ChatFormatting.DARK_RED));
            WynntilsMod.error("Development environment detected, cannot update!");
            return 0;
        }

        CompletableFuture.runAsync(() -> {
            WynntilsMod.info("Attempting to fetch Wynntils update.");
            CompletableFuture<UpdateManager.UpdateResult> completableFuture = UpdateManager.tryUpdate();

            completableFuture.whenComplete((result, throwable) -> {
                switch (result) {
                    case SUCCESSFUL -> McUtils.sendMessageToClient(
                            new TranslatableComponent("feature.wynntils.updates.result.successful")
                                    .withStyle(ChatFormatting.DARK_GREEN));
                    case ERROR -> McUtils.sendMessageToClient(
                            new TranslatableComponent("feature.wynntils.updates.result.error")
                                    .withStyle(ChatFormatting.DARK_RED));
                    case ALREADY_ON_LATEST -> McUtils.sendMessageToClient(
                            new TranslatableComponent("feature.wynntils.updates.result.latest")
                                    .withStyle(ChatFormatting.YELLOW));
                    case UPDATE_PENDING -> McUtils.sendMessageToClient(
                            new TranslatableComponent("feature.wynntils.updates.result.pending")
                                    .withStyle(ChatFormatting.YELLOW));
                }
            });
        });

        context.getSource()
                .sendSuccess(
                        new TranslatableComponent("feature.wynntils.updates.checking").withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }
}
