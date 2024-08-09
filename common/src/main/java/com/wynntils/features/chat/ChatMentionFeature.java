/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.utils.colors.ColorChatFormatting;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.IterationDecision;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.CHAT)
public class ChatMentionFeature extends Feature {
    private static final Pattern END_OF_HEADER_PATTERN = Pattern.compile(".*[\\]:]\\s?");
    private static final Pattern NON_WORD_CHARACTERS =
            Pattern.compile("\\W"); // all non-alphanumeric-underscore characters

    @Persisted
    public final Config<Boolean> markMention = new Config<>(true);

    @Persisted
    public final Config<Boolean> dingMention = new Config<>(true);

    @Persisted
    public final Config<ColorChatFormatting> mentionColor = new Config<>(ColorChatFormatting.YELLOW);

    @Persisted
    public final Config<String> aliases = new Config<>("");

    @Persisted
    public final Config<Boolean> suppressMentionsInInfo = new Config<>(false);

    private List<Pattern> mentionPatterns;

    public ChatMentionFeature() {
        mentionPatterns = buildPattern();
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        // rebuild pattern in case it has changed
        mentionPatterns = buildPattern();
    }

    private List<Pattern> buildPattern() {
        List<Pattern> returnable = new ArrayList<>();

        List<String> splitAliases = new ArrayList<>();
        splitAliases.add(McUtils.mc().getUser().getName());
        splitAliases.addAll(Arrays.asList(this.aliases.get().split(",")));

        for (String alias : splitAliases) {
            if (alias.isEmpty()) continue;

            Matcher nonWordMatcher = NON_WORD_CHARACTERS.matcher(alias);
            if (nonWordMatcher.find()) {
                // We know there are strange characters in this alias, we need a different way to detect them
                // we cannot use word boundaries (because there are non word characters!)
                // so we will just do a simple space/start/end check
                String quotedAlias = Pattern.quote(alias);
                returnable.add(Pattern.compile("(?:\\s|^)(" + quotedAlias + ")(?:\\s|$)", Pattern.CASE_INSENSITIVE));
            } else {
                returnable.add(Pattern.compile("(?<!\\[)\\b(" + alias + ")\\b(?!:|])", Pattern.CASE_INSENSITIVE));
            }
        }
        return returnable;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChat(ChatMessageReceivedEvent e) {
        if (e.getRecipientType() == RecipientType.INFO && suppressMentionsInInfo.get()) return;

        StyledText styledText = e.getStyledText();
        StyledText modified = styledText.iterateBackwards((part, changes) -> {
            // We have reached the end of the message content,
            // we don't want to highlight our own name in our own message
            if (END_OF_HEADER_PATTERN
                    .matcher(part.getString(null, PartStyle.StyleType.NONE))
                    .matches()) {
                return IterationDecision.BREAK;
            }

            StyledTextPart partToReplace = part;
            for (Pattern pattern : mentionPatterns) {
                Matcher matcher = pattern.matcher(partToReplace.getString(null, PartStyle.StyleType.NONE));

                while (matcher.find()) {
                    String match = partToReplace.getString(null, PartStyle.StyleType.NONE);

                    String firstPart = match.substring(0, matcher.start());
                    String mentionPart = match.substring(matcher.start(), matcher.end());
                    String lastPart = match.substring(matcher.end());

                    PartStyle partStyle = partToReplace.getPartStyle();

                    StyledTextPart first = new StyledTextPart(firstPart, partStyle.getStyle(), null, Style.EMPTY);
                    StyledTextPart mention = new StyledTextPart(
                            mentionPart,
                            partStyle.getStyle().withColor(mentionColor.get().getChatFormatting()),
                            null,
                            first.getPartStyle().getStyle());
                    StyledTextPart last = new StyledTextPart(lastPart, partStyle.getStyle(), null, Style.EMPTY);

                    changes.remove(partToReplace);
                    changes.add(first);
                    changes.add(mention);
                    changes.add(last);

                    partToReplace = last;
                    matcher = pattern.matcher(lastPart);
                }
            }

            return IterationDecision.CONTINUE;
        });

        // No changes were made, there was no mention.
        if (styledText.equals(modified)) return;

        if (markMention.get()) {
            e.setMessage(modified);
        }

        if (dingMention.get()) {
            McUtils.playSoundUI(SoundEvents.NOTE_BLOCK_PLING.value());
        }
    }
}
