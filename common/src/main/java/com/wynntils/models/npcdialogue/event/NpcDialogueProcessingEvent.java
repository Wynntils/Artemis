/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.npcdialogue.event;

import com.wynntils.core.text.StyledText;
import com.wynntils.models.npcdialogue.type.NpcDialogue;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;

/**
 * Event that is fired at different stages during while processing an NPC dialogue.
 */
public abstract class NpcDialogueProcessingEvent extends Event {
    /**
     * Event that is fired before processing an NPC dialogue.
     * This event can be used to add any pre-processing to the dialogue.
     */
    public static class Pre extends NpcDialogueProcessingEvent {
        private final NpcDialogue dialogue;

        // This is a future that can be used to add any post-processing to the dialogue.
        // This is used for things like translation, transcription, etc.
        private CompletableFuture<List<StyledText>> postProcessedDialogueFuture;

        public Pre(NpcDialogue dialogue) {
            this.dialogue = dialogue;
            this.postProcessedDialogueFuture = CompletableFuture.completedFuture(dialogue.currentDialogue());
        }

        public void addProcessingStep(
                Function<CompletableFuture<List<StyledText>>, CompletableFuture<List<StyledText>>> processingStep) {
            postProcessedDialogueFuture = processingStep.apply(postProcessedDialogueFuture);
        }

        public NpcDialogue getDialogue() {
            return dialogue;
        }
    }

    public static class Post extends NpcDialogueProcessingEvent {
        private final NpcDialogue dialogue;
        private final List<StyledText> postProcessedDialogue;
        private final List<Component> postProcessedDialogueComponent;

        public Post(NpcDialogue dialogue, List<StyledText> postProcessedDialogue) {
            this.dialogue = dialogue;
            this.postProcessedDialogue = postProcessedDialogue;
            this.postProcessedDialogueComponent = postProcessedDialogue.stream()
                    .map(StyledText::getComponent)
                    .map(c -> (Component) c)
                    .toList();
        }

        public NpcDialogue getDialogue() {
            return dialogue;
        }

        public List<StyledText> getPostProcessedDialogue() {
            return postProcessedDialogue;
        }

        public List<Component> getPostProcessedDialogueComponent() {
            return postProcessedDialogueComponent;
        }
    }
}
