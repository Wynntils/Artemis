/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.utilities;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.models.npcdialogue.event.NpcDialogueProcessingEvent;
import com.wynntils.services.translation.TranslationService;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@StartDisabled
@ConfigCategory(Category.UTILITIES)
public class TranslationFeature extends Feature {
    @Persisted
    public final Config<String> languageName = new Config<>("");

    @Persisted
    public final Config<Boolean> translateTrackedQuest = new Config<>(true);

    @Persisted
    public final Config<Boolean> translateNpc = new Config<>(true);

    @Persisted
    public final Config<Boolean> translateInfo = new Config<>(true);

    @Persisted
    public final Config<Boolean> translatePlayerChat = new Config<>(false);

    @Persisted
    public final Config<Boolean> keepOriginal = new Config<>(true);

    @Persisted
    public final Config<TranslationService.TranslationServices> translationService =
            new Config<>(TranslationService.TranslationServices.GOOGLEAPI);

    @SubscribeEvent
    public void onChat(ChatMessageReceivedEvent e) {
        if (languageName.get().isEmpty()) return;

        if (e.getRecipientType() != RecipientType.INFO && !translatePlayerChat.get()) return;
        if (e.getRecipientType() == RecipientType.INFO && !translateInfo.get()) return;

        StyledText originalText = e.getStyledText();
        String codedString = wrapCoding(originalText);
        Services.Translation.getTranslator(translationService.get())
                .translate(List.of(codedString), languageName.get(), translatedMsgList -> {
                    StyledText messageToSend;
                    if (!translatedMsgList.isEmpty()) {
                        String result = translatedMsgList.getFirst();
                        messageToSend = unwrapCoding(result, originalText);
                    } else {
                        if (keepOriginal.get()) return;

                        // We failed to get a translation; send the original message so it's not lost
                        messageToSend = originalText;
                    }
                    McUtils.mc().doRunTask(() -> McUtils.sendMessageToClient(messageToSend.getComponent()));
                });
        if (!keepOriginal.get()) {
            e.setCanceled(true);
        }
    }

    // Translation should be the last post-processing step
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onNpcDialogue(NpcDialogueProcessingEvent.Pre event) {
        if (!translateNpc.get()) return;
        if (languageName.get().isEmpty()) return;

        event.addProcessingStep(future -> future.thenCompose(styledTexts -> {
            if (styledTexts.isEmpty()) return CompletableFuture.completedFuture(styledTexts);

            CompletableFuture<List<StyledText>> translationFuture = new CompletableFuture<>();

            CompletableFuture.runAsync(() -> {
                try {
                    Services.Translation.getTranslator(translationService.get())
                            .translate(
                                    styledTexts.stream().map(this::wrapCoding).toList(),
                                    languageName.get(),
                                    translatedMsgList -> {
                                        List<StyledText> translatedComponents = new ArrayList<>();

                                        // Add the original message if requested
                                        if (keepOriginal.get()) {
                                            translatedComponents.addAll(styledTexts);
                                        }

                                        // Add the translated message
                                        for (int i = 0; i < translatedMsgList.size(); i++) {
                                            String result = translatedMsgList.get(i);
                                            StyledText originalText = styledTexts.get(i);

                                            StyledText messageToSend = unwrapCoding(result, originalText);
                                            translatedComponents.add(messageToSend);
                                        }

                                        translationFuture.complete(translatedComponents);
                                    });
                } catch (Exception e) {
                    WynntilsMod.error("Failed to translate NPC dialogue.", e);
                    translationFuture.complete(styledTexts);
                }
            });

            return translationFuture;
        }));
    }

    private StyledText unwrapCoding(String codedTranslatedString, StyledText originalText) {
        // Some translated text (e.g. from pt_br) contains special characters.
        // These will need to be stripped or converted, which is not ideal but better than nothing.
        // Note about special characters:
        // - Á is a full-screen black character, which is used in animations.
        // - À is a common white-space character, used in the chat.

        // Note about wrapping:
        // The wrapping is done to prevent the translation service from translating the color codes.
        // - §x is used for color codes
        // - §[x] is used for click events
        // - §<x> is used for hover events

        return StyledText.fromModifiedString(
                codedTranslatedString
                        .replaceAll("\\{ ?§ ?([0-9a-fklmnor]) ?\\}", "§$1")
                        .replaceAll("\\[ ?§ ?([0-9]+) ?\\]", "§[$1]")
                        .replaceAll("\\< ?§ ?([0-9]+) ?\\>", "§<$1>")
                        .replace('Á', 'A')
                        .replace('À', 'A'),
                originalText);
    }

    private String wrapCoding(StyledText origCoded) {
        return origCoded
                .getString(PartStyle.StyleType.INCLUDE_EVENTS)
                .replaceAll("(§[0-9a-fklmnor])", "{$1}")
                .replaceAll("§\\[([0-9]+)\\]", "[§$1]")
                .replaceAll("§<([0-9]+)>", "<§$1>");
    }
}
