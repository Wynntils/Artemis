/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.utilities;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.config.HiddenConfig;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UTILITIES)
public class PerCharacterGuildContributionFeature extends Feature {
    private static final Pattern CONTRIBUTION_PATTERN =
            Pattern.compile("§3You will now contribute §b(\\d+)%§3 of your XP to §b.*§3.");

    @Persisted
    private final HiddenConfig<Map<String, Integer>> characterContributions = new HiddenConfig<>(new TreeMap<>());

    @Persisted
    public final Config<Boolean> hideContributionMessage = new Config<>(true);

    private boolean waitingForCommandResponse = false;

    @SubscribeEvent
    public void onCharacterChange(CharacterUpdateEvent e) {
        if (Models.Guild.getGuildName().isEmpty()) return;

        int amountToContribute = characterContributions.get().getOrDefault(Models.Character.getId(), -1);

        if (amountToContribute != -1) {
            waitingForCommandResponse = true;
            Handlers.Command.queueCommand("guild xp " + amountToContribute);
        }
    }

    @SubscribeEvent
    public void onChatReceived(ChatMessageReceivedEvent event) {
        if (Models.Guild.getGuildName().isEmpty()) return;
        if (!Models.Character.hasCharacter()) return;

        StyledText message = event.getOriginalStyledText();

        Matcher contributionMatcher = message.getMatcher(CONTRIBUTION_PATTERN);

        if (contributionMatcher.matches()) {
            event.setCanceled(true);

            if (waitingForCommandResponse) {
                waitingForCommandResponse = false;

                if (!hideContributionMessage.get()) {
                    sendContributionMessage(characterContributions.get().get(Models.Character.getId()));
                }

                return;
            }

            int contributionAmount = Integer.parseInt(contributionMatcher.group(1));

            if (contributionAmount < 0 || contributionAmount > 100) return;

            characterContributions.get().put(Models.Character.getId(), contributionAmount);
            characterContributions.touched();

            sendContributionMessage(contributionAmount);
        }
    }

    private void sendContributionMessage(int contributionAmount) {
        MutableComponent contributionMessage = Component.literal(ChatFormatting.DARK_AQUA + "You will now contribute "
                + ChatFormatting.AQUA
                + contributionAmount + "%" + ChatFormatting.DARK_AQUA
                + " of your XP to " + ChatFormatting.AQUA + Models.Guild.getGuildName() + ChatFormatting.DARK_AQUA
                + ".\n");

        contributionMessage.append(Component.literal("This will only apply to your current character.\n")
                .withStyle(ChatFormatting.DARK_AQUA));

        contributionMessage.append(Component.literal("Click here")
                .withStyle(ChatFormatting.AQUA)
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/wynntils feature disable " + this.getShortName()))));

        contributionMessage.append(
                Component.literal(" to disable this functionality.").withStyle(ChatFormatting.DARK_AQUA));

        McUtils.sendMessageToClient(contributionMessage);
    }
}
