/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.utilities;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.models.npcdialogue.event.NpcDialogueProcessingEvent;
import com.wynntils.models.wynnalphabet.WynnAlphabet;
import com.wynntils.models.wynnalphabet.type.TranscribeCondition;
import com.wynntils.utils.colors.ColorChatFormatting;
import com.wynntils.utils.type.IterationDecision;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Style;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UTILITIES)
public class TranscribeMessagesFeature extends Feature {
    @Persisted
    public final Config<Boolean> transcribeChat = new Config<>(true);

    @Persisted
    public final Config<Boolean> transcribeNpcs = new Config<>(true);

    @Persisted
    public final Config<TranscribeCondition> transcribeCondition = new Config<>(TranscribeCondition.ALWAYS);

    @Persisted
    public final Config<Boolean> showTooltip = new Config<>(false);

    @Persisted
    public final Config<Boolean> coloredTranscriptions = new Config<>(true);

    @Persisted
    public final Config<ColorChatFormatting> gavellianColor = new Config<>(ColorChatFormatting.LIGHT_PURPLE);

    @Persisted
    public final Config<ColorChatFormatting> wynnicColor = new Config<>(ColorChatFormatting.DARK_GREEN);

    private static final Pattern END_OF_HEADER_PATTERN = Pattern.compile(".*[\\]:]\\s?");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ChatMessageReceivedEvent event) {
        if (!transcribeChat.get()) return;
        if (!Models.WynnAlphabet.hasWynnicOrGavellian(event.getStyledText().getString())) return;

        boolean transcribeWynnic = Models.WynnAlphabet.shouldTranscribe(transcribeCondition.get(), WynnAlphabet.WYNNIC);
        boolean transcribeGavellian =
                Models.WynnAlphabet.shouldTranscribe(transcribeCondition.get(), WynnAlphabet.GAVELLIAN);

        if (!transcribeWynnic && !transcribeGavellian) return;

        StyledText styledText = event.getStyledText();

        StyledText modified = getStyledTextWithTranscription(styledText, transcribeWynnic, transcribeGavellian, false);

        if (styledText.equals(modified)) return;

        event.setMessage(modified);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onNpcDialogue(NpcDialogueProcessingEvent.Pre event) {
        if (!transcribeNpcs.get()) return;

        boolean transcribeWynnic = Models.WynnAlphabet.shouldTranscribe(transcribeCondition.get(), WynnAlphabet.WYNNIC);
        boolean transcribeGavellian =
                Models.WynnAlphabet.shouldTranscribe(transcribeCondition.get(), WynnAlphabet.GAVELLIAN);

        if (!transcribeWynnic && !transcribeGavellian) return;

        event.addProcessingStep(future ->
                future.thenApply(styledTexts -> transcribeText(styledTexts, transcribeWynnic, transcribeGavellian)));
    }

    private List<StyledText> transcribeText(
            List<StyledText> styledTexts, boolean transcribeWynnic, boolean transcribeGavellian) {
        // If there are no Wynnic or Gavellian characters, return the original text
        if (styledTexts.stream()
                .noneMatch(text -> Models.WynnAlphabet.hasWynnicOrGavellian(text.getStringWithoutFormatting()))) {
            return styledTexts;
        }

        // Transcribe each styled text
        return styledTexts.stream()
                .map(styledText ->
                        getStyledTextWithTranscription(styledText, transcribeWynnic, transcribeGavellian, true))
                .toList();
    }

    private StyledText getStyledTextWithTranscription(
            StyledText original, boolean transcribeWynnic, boolean transcribeGavellian, boolean npcDialogue) {
        if (!transcribeWynnic && !transcribeGavellian) return original;

        StyledText transcribedStyledText = original;

        if (transcribeWynnic) {
            // Wynnic numbers are transcribed first
            transcribedStyledText = transcribeStyledText(
                    transcribedStyledText,
                    Models.WynnAlphabet::getWynnicNumberMatcher,
                    (partToBeTranslated) -> Models.WynnAlphabet.transcribeMessageFromWynnAlphabet(
                            partToBeTranslated,
                            WynnAlphabet.WYNNIC,
                            coloredTranscriptions.get(),
                            wynnicColor.get().getChatFormatting(),
                            !npcDialogue && showTooltip.get()));

            // Wynnic characters are transcribed second
            transcribedStyledText = transcribeStyledText(
                    transcribedStyledText,
                    Models.WynnAlphabet::getWynnicCharacterMatcher,
                    (partToBeTranslated) -> Models.WynnAlphabet.transcribeMessageFromWynnAlphabet(
                            partToBeTranslated,
                            WynnAlphabet.WYNNIC,
                            coloredTranscriptions.get(),
                            wynnicColor.get().getChatFormatting(),
                            !npcDialogue && showTooltip.get()));
        }

        if (transcribeGavellian) {
            // Lastly, Gavellian characters are transcribed
            transcribedStyledText = transcribeStyledText(
                    transcribedStyledText,
                    Models.WynnAlphabet::getGavellianCharacterMatcher,
                    (partToBeTranslated) -> Models.WynnAlphabet.transcribeMessageFromWynnAlphabet(
                            partToBeTranslated,
                            WynnAlphabet.GAVELLIAN,
                            coloredTranscriptions.get(),
                            gavellianColor.get().getChatFormatting(),
                            !npcDialogue && showTooltip.get()));
        }

        return transcribedStyledText;
    }

    private StyledText transcribeStyledText(
            StyledText original,
            Function<String, Matcher> matcherFunction,
            Function<StyledTextPart, StyledTextPart> transcriptorFunction) {
        return original.iterateBackwards((part, changes) -> {
            String partText = part.getString(null, PartStyle.StyleType.NONE);
            String transcriptedText = partText;

            if (END_OF_HEADER_PATTERN.matcher(partText).matches()) {
                return IterationDecision.BREAK;
            }

            List<StyledTextPart> newParts = new ArrayList<>();

            newParts = translatePartUsingMatcher(part, matcherFunction, transcriptorFunction, newParts);

            // If the part was not changed, we don't need to add it to the changes
            if (newParts.isEmpty()) {
                return IterationDecision.CONTINUE;
            }

            changes.remove(part);
            changes.addAll(newParts);

            return IterationDecision.CONTINUE;
        });
    }

    private List<StyledTextPart> translatePartUsingMatcher(
            StyledTextPart part,
            Function<String, Matcher> matcherFunction,
            Function<StyledTextPart, StyledTextPart> transcriptorFunction,
            List<StyledTextPart> newParts) {
        String partText = part.getString(null, PartStyle.StyleType.NONE);

        Matcher matcher = matcherFunction.apply(partText);

        while (matcher.find()) {
            StyledTextPart partToBeTranslated =
                    new StyledTextPart(matcher.group(), part.getPartStyle().getStyle(), null, Style.EMPTY);
            StyledTextPart transcribedPart = transcriptorFunction.apply(partToBeTranslated);

            if (matcher.start() > 0) {
                String preText = partText.substring(0, matcher.start());

                // Optimization: If the preText is blank, we use the same color as the part to be translated,
                //               so we reduce the number of color codes in the string output
                Style style = preText.isBlank()
                        ? part.getPartStyle()
                                .getStyle()
                                .withColor(transcribedPart
                                        .getPartStyle()
                                        .getStyle()
                                        .getColor())
                        : part.getPartStyle().getStyle();

                newParts.add(new StyledTextPart(preText, style, null, Style.EMPTY));
            }

            newParts.add(transcribedPart);

            partText = partText.substring(matcher.end());
            matcher = matcherFunction.apply(partText);
        }

        if (!partText.isEmpty()) {
            newParts.add(new StyledTextPart(partText, part.getPartStyle().getStyle(), null, Style.EMPTY));
        }

        return newParts;
    }
}
