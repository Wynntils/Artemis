/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.models.players.type.GuildRank;
import com.wynntils.utils.type.IterationDecision;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@StartDisabled
@ConfigCategory(Category.CHAT)
public class GuildRankReplacementFeature extends Feature {
    private static final char STAR = '★';
    private static final char REPLACEMENT_STAR = '*';

    @Persisted
    public final Config<RankType> rankType = new Config<>(RankType.NAME);

    // Test in GuildRankReplacementFeature_GUILD_MESSAGE_PATTERN
    private static final Pattern GUILD_MESSAGE_PATTERN =
            Pattern.compile("§3\\[(?:§b)?★{0,5}(?:§3)?(?:§o)?.{1,16}(?:§r)?(?:§3)?\\]§b");

    // Test in GuildRankReplacementFeature_RECRUIT_USERNAME_PATTERN
    private static final Pattern RECRUIT_USERNAME_PATTERN = Pattern.compile("§3\\[(.{1,16})");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatMessageReceived(ChatMessageReceivedEvent e) {
        if (e.getRecipientType() != RecipientType.GUILD) return;

        StyledText originalStyledText = e.getStyledText();

        Matcher m = originalStyledText.getMatcher(GUILD_MESSAGE_PATTERN);
        if (!m.find()) return;

        StyledText modified =
                switch (rankType.get()) {
                    case NONE -> modifyByRemovingRank(originalStyledText);
                    case NAME -> modifyByAddingTextRank(originalStyledText);
                    case SMALL_STARS -> modifyByAddingSmallStarsRank(originalStyledText);
                };

        if (originalStyledText.equals(modified)) return; // no changes

        e.setMessage(modified);
    }

    private StyledText modifyByRemovingRank(StyledText styledText) {
        return styledText.iterate((part, changes) -> {
            if (part.getString(null, PartStyle.StyleType.NONE).contains(String.valueOf(STAR))) {
                changes.remove(part);
                return IterationDecision.BREAK;
            }
            return IterationDecision.CONTINUE;
        });
    }

    private StyledText modifyByAddingTextRank(StyledText styledText) {
        return styledText.iterateBackwards((part, changes) -> {
            int stars = (int) part.getString(null, PartStyle.StyleType.NONE)
                    .chars()
                    .filter(c -> c == STAR)
                    .count();

            if (stars > 0) {
                String rankName = GuildRank.values()[stars].getName();

                changes.remove(part);
                changes.add(new StyledTextPart(rankName, part.getPartStyle().getStyle(), null, Style.EMPTY));
                // Add a space to separate the rank from the username
                changes.add(new StyledTextPart(" ", part.getPartStyle().getStyle(), null, Style.EMPTY));

                return IterationDecision.BREAK;
            }

            // Check for recruit rank (0 stars)
            Matcher usernameMatcher =
                    RECRUIT_USERNAME_PATTERN.matcher(part.getString(null, PartStyle.StyleType.DEFAULT));
            if (usernameMatcher.find()) {
                String username = usernameMatcher.group(1);

                Style originalStyle = part.getPartStyle().getStyle();

                // Remove the alias formatting
                Style defaultNameStyle = originalStyle.withHoverEvent(null).withItalic(false);

                changes.remove(part);
                changes.add(new StyledTextPart("[", defaultNameStyle, null, Style.EMPTY));

                changes.add(new StyledTextPart(
                        "Recruit", defaultNameStyle.withColor(ChatFormatting.AQUA), null, Style.EMPTY));

                changes.add(
                        new StyledTextPart(" ", defaultNameStyle.withColor(ChatFormatting.AQUA), null, Style.EMPTY));

                changes.add(new StyledTextPart(
                        username, originalStyle.withColor(ChatFormatting.DARK_AQUA), null, Style.EMPTY));

                return IterationDecision.BREAK;
            }

            return IterationDecision.CONTINUE;
        });
    }

    private StyledText modifyByAddingSmallStarsRank(StyledText styledText) {
        return styledText.iterate((part, changes) -> {
            String partContent = part.getString(null, PartStyle.StyleType.NONE);
            if (partContent.contains(String.valueOf(STAR))) {
                changes.set(
                        0,
                        new StyledTextPart(
                                partContent.replaceAll(String.valueOf(STAR), String.valueOf(REPLACEMENT_STAR)),
                                part.getPartStyle().getStyle(),
                                null,
                                Style.EMPTY));
                return IterationDecision.BREAK;
            }
            return IterationDecision.CONTINUE;
        });
    }

    private enum RankType {
        NAME,
        NONE,
        SMALL_STARS
    }
}
