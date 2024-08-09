/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.npcdialogue;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.overlays.NpcDialogueFeature;
import com.wynntils.handlers.chat.type.NpcDialogueType;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.npcdialogue.event.NpcDialogueProcessingEvent;
import com.wynntils.models.npcdialogue.event.NpcDialogueRemoved;
import com.wynntils.models.npcdialogue.type.NpcDialogue;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import net.neoforged.bus.api.SubscribeEvent;

public class NpcDialogueModel extends Model {
    private static final Pattern NEW_QUEST_STARTED = Pattern.compile("^§6§lNew Quest Started: §e§l(.*)$");

    private final Set<Feature> dialogExtractionDependents = new HashSet<>();

    private NpcDialogue currentDialogue = NpcDialogue.EMPTY;

    // If we translate a confirmationless dialogue, then we need to change this list.
    // However, translation is ran as a futrue, so it is not guranteed that only one thread will access this list at a
    // time.
    private ConcurrentLinkedQueue<NpcDialogue> confirmationlessDialogues = new ConcurrentLinkedQueue<>();

    public NpcDialogueModel() {
        super(List.of());
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent e) {
        currentDialogue = NpcDialogue.EMPTY;
        confirmationlessDialogues = new ConcurrentLinkedQueue<>();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        long now = System.currentTimeMillis();
        confirmationlessDialogues.removeIf(dialogue -> now >= dialogue.removeTime());
    }

    public void addNpcDialogExtractionDependent(Feature feature) {
        dialogExtractionDependents.add(feature);
    }

    public boolean isNpcDialogExtractionRequired() {
        return !dialogExtractionDependents.isEmpty()
                && dialogExtractionDependents.stream().allMatch(Feature::isEnabled);
    }

    public long calculateMessageReadTime(List<StyledText> msg) {
        // FIXME: Remove this feature-model dependency when we have model configs
        NpcDialogueFeature feature = Managers.Feature.getFeatureInstance(NpcDialogueFeature.class);

        int words = StyledText.join(" ", msg).split(" ").length;
        long delay = feature.dialogAutoProgressDefaultTime.get()
                + ((long) words * feature.dialogAutoProgressAdditionalTimePerWord.get());
        return delay;
    }

    public boolean isInDialogue() {
        return currentDialogue != NpcDialogue.EMPTY && !currentDialogue.isEmpty();
    }

    public NpcDialogue getCurrentDialogue() {
        return currentDialogue;
    }

    public List<NpcDialogue> getConfirmationlessDialogues() {
        return List.copyOf(confirmationlessDialogues);
    }

    public void handleDialogue(List<StyledText> chatMessage, boolean protectedDialogue, NpcDialogueType type) {
        // Print dialogue to the system log
        WynntilsMod.info("[NPC] Type: " + (chatMessage.isEmpty() ? "<empty> " : "")
                + (protectedDialogue ? "<protected> " : "") + type);
        chatMessage.forEach(s -> WynntilsMod.info("[NPC] " + (s.isEmpty() ? "<empty>" : s)));

        // The same message can be repeating before we have finished removing the old
        // Just remove the old and add the new with an updated remove time
        // It can also happen that a confirmationless dialogue turn into a normal
        // dialogue after a while (the "Press SHIFT..." text do not appear immediately)
        confirmationlessDialogues.removeIf(d -> d.currentDialogue().equals(chatMessage));

        // If the message is "NONE", set an empty dialogue
        if (type == NpcDialogueType.NONE) {
            NpcDialogue oldDialogue = currentDialogue;
            currentDialogue = NpcDialogue.EMPTY;
            WynntilsMod.postEvent(new NpcDialogueRemoved(oldDialogue));
            return;
        }

        // If the message is the same as the current one, and the mode is "normal", don't update it
        // (ChatHandler already filters duplicates, but there are rare cases where the same dialogue is refreshed after
        // a while)
        if (type == NpcDialogueType.NORMAL && currentDialogue.currentDialogue().equals(chatMessage)) return;

        NpcDialogue dialogue = new NpcDialogue(
                chatMessage,
                type,
                protectedDialogue,
                System.currentTimeMillis(),
                System.currentTimeMillis() + calculateMessageReadTime(chatMessage));

        if (type == NpcDialogueType.CONFIRMATIONLESS) {
            confirmationlessDialogues.add(dialogue);
        } else {
            currentDialogue = dialogue;
        }

        if (!chatMessage.isEmpty()
                && chatMessage.getFirst().getMatcher(NEW_QUEST_STARTED).find()) {
            // TODO: Show nice banner notification instead
            // but then we'd also need to confirm it with a sneak
            Managers.Notification.queueMessage(chatMessage.getFirst());
        }

        NpcDialogueProcessingEvent.Pre event = new NpcDialogueProcessingEvent.Pre(dialogue);
        WynntilsMod.postEvent(event);

        // Hook after post-processing steps
        event.addProcessingStep(future -> future.whenComplete((styledTexts, throwable) -> {
            if (throwable != null) {
                WynntilsMod.error("Failed to process NPC dialogue.", throwable);
                return;
            }

            // Should never happen, but just in case
            if (styledTexts.isEmpty()) {
                WynntilsMod.warn("Dialogue processing returned an empty list.");
                WynntilsMod.postEvent(new NpcDialogueProcessingEvent.Post(dialogue, dialogue.currentDialogue()));
                return;
            }

            // If the dialogue is the same as the old one, don't do anything
            if (dialogue.currentDialogue().equals(styledTexts)) {
                WynntilsMod.postEvent(new NpcDialogueProcessingEvent.Post(dialogue, styledTexts));
                return;
            }

            styledTexts.forEach(styledText -> WynntilsMod.info("[Translated NPC] " + styledText.getString()));

            // Update the dialogue with the new one
            NpcDialogue newDialogue = new NpcDialogue(
                    styledTexts,
                    dialogue.dialogueType(),
                    dialogue.isProtected(),
                    dialogue.addTime(),
                    dialogue.removeTime());

            // Update the current dialogue
            if (dialogue == currentDialogue) {
                currentDialogue = newDialogue;
            } else {
                // Update the confirmationless dialogues, if the old dialogue is still in the list
                if (confirmationlessDialogues.remove(dialogue)) {
                    confirmationlessDialogues.add(newDialogue);
                }
            }

            Managers.TickScheduler.scheduleNextTick(() -> {
                NpcDialogueProcessingEvent.Post postEvent =
                        new NpcDialogueProcessingEvent.Post(newDialogue, styledTexts);
                WynntilsMod.postEvent(postEvent);
            });
        }));
    }
}
