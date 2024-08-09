/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.chat;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handler;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.mod.event.WynncraftConnectionEvent;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.MessageType;
import com.wynntils.handlers.chat.type.NpcDialogueType;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.mc.event.ChatPacketReceivedEvent;
import com.wynntils.mc.event.MobEffectEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.StyledTextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * The responsibility of this class is to act as the first gateway for incoming
 * chat messages from Wynncraft. Chat messages in vanilla comes in three types,
 * CHAT, SYSTEM and GAME_INFO. The latter is the "action bar", and is handled
 * elsewhere. However, starting with Minecraft 1.19, Wynncraft will send all chat
 * messages as SYSTEM, so we will ignore the CHAT type.
 * <p>
 * Using the regexp patterns in RecipientType, we classify the incoming messages
 * according to if they are sent to the guild, party, global chat, etc. Messages
 * that do not match any of these categories are called "info" messages, and are
 * typically automated responses or announcements. Messages that do match any other
 * category, are sent by other users (what could really be termed "chat"). The one
 * exception is guild messages, which can also be e.g. WAR announcements.
 * (Unfortunately, there is no way to distinguish these from chat sent by a build
 * member named "WAR", or "INFO", or..., so if these need to be separated, it has
 * to happen in a later stage).
 * <p>
 * The final problem this class needs to resolve is how Wynncraft handles NPC
 * dialogs. When you enter a NPC dialog, Wynncraft start sending "screens" once a
 * second or so, which is multi-line messages that repeat the chat history, and add
 * the NPC dialog at the end. This way, the vanilla client will always show the NPC
 * dialog, so it is a clever hack in that respect. But it makes our life harder. We
 * solve this by detecting when a multiline "screen" happens, look for the last
 * real chat message we received, and splits of the rest as the "newLines". These
 * are in turn examined, since they can contain the actual NPC dialog, or they can
 * contain new chat messages sent while the user is in the NPC dialog.
 * <p>
 * These new chat messages are the real problematic thing here. They are
 * differently formatted to be gray and tuned-down, which makes the normal regexp
 * matching fail. They are also sent as pure strings with formatting codes, instead
 * of Components as normal one-line chats are. This mean things like hover and
 * onClick information is lost. (There is nothing we can do about this, it is a
 * Wynncraft limitation.) We send out these chat messages one by one, as they would
 * have appeared if we were not in a NPC dialog, but we tag them as BACKGROUND to
 * signal that formatting is different.
 * <p>
 * In a normal vanilla setting, the last "screen" that Wynncraft sends out, the
 * messages are re-colored to have their normal colors restored (hover and onClick
 * as still missing, though). Currently, we do not handle this, since it would mean
 * sending out information that already sent chat lines would need to be updated to
 * a different formatting. This could be done, but requires extra logic, and most
 * importantly, a way to update already printed chat lines.
 */
public final class ChatHandler extends Handler {
    // Test in ChatHandler_NPC_CONFIRM_PATTERN
    private static final Pattern NPC_CONFIRM_PATTERN =
            Pattern.compile("^ *§[47]Press §[cf](SNEAK|SHIFT) §[47]to continue$");

    // Test in ChatHandler_NPC_SELECT_PATTERN
    private static final Pattern NPC_SELECT_PATTERN =
            Pattern.compile("^ *§[47cf](Select|CLICK) §[47cf]an option (§[47])?to continue$");

    private static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("^\\s*(§r|À+)?\\s*$");
    private static final long SLOWDOWN_PACKET_TICK_DELAY = 20;
    private static final int CHAT_SCREEN_TICK_DELAY = 1;

    private String lastRealChat = null;

    // This is used to detect when the lastRealChat message
    // is actually a confirmationless dialogue, but not a standard one,
    // and we can't parse it properly. That makes it be the last "real" chat,
    // so when we receive the clear screen, we think that all the messages are new.
    // By keeping track of the last two real chats, we can detect this case.
    private String oneBeforeLastRealChat = null;

    private long lastSlowdownApplied = 0;
    private List<StyledText> lastScreenNpcDialogue = List.of();
    private StyledText lastConfirmationlessDialogue = null;
    private List<StyledText> delayedDialogue;
    private NpcDialogueType delayedType;
    private long chatScreenTicks = 0;
    private List<StyledText> collectedLines = new ArrayList<>();

    @SubscribeEvent
    public void onConnectionChange(WynncraftConnectionEvent.Connected event) {
        // Reset chat handler
        collectedLines = new ArrayList<>();
        chatScreenTicks = 0;
        lastRealChat = null;
        oneBeforeLastRealChat = null;
        lastSlowdownApplied = 0;
        lastScreenNpcDialogue = List.of();
        lastConfirmationlessDialogue = null;
        delayedDialogue = null;
        delayedType = NpcDialogueType.NONE;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (collectedLines.isEmpty()) return;

        // Tick event runs after the chat packets, with the same tick number
        // as the chat packets. This means we can allow equality here.
        long ticks = McUtils.mc().level.getGameTime();
        if (ticks >= chatScreenTicks + CHAT_SCREEN_TICK_DELAY) {
            // Send the collected screen lines
            processCollectedChatScreen();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerChatReceived(ChatPacketReceivedEvent.Player event) {
        if (shouldSeparateNPC()) {
            handleWithSeparation(event);
        } else {
            handleIncomingChatLine(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSystemChatReceived(ChatPacketReceivedEvent.System event) {
        if (shouldSeparateNPC()) {
            handleWithSeparation(event);
        } else {
            handleIncomingChatLine(event);
        }
    }

    @SubscribeEvent
    public void onStatusEffectUpdate(MobEffectEvent.Update event) {
        if (event.getEntity() != McUtils.player()) return;

        if (event.getEffect().equals(MobEffects.MOVEMENT_SLOWDOWN.value())
                && event.getEffectAmplifier() == 3
                && event.getEffectDurationTicks() == 32767) {
            if (delayedDialogue != null) {
                List<StyledText> dialogue = delayedDialogue;
                delayedDialogue = null;

                handleNpcDialogue(dialogue, delayedType, true);
            } else {
                lastSlowdownApplied = McUtils.mc().level.getGameTime();
            }
        }
    }

    @SubscribeEvent
    public void onStatusEffectRemove(MobEffectEvent.Remove event) {
        if (event.getEntity() != McUtils.player()) return;

        if (event.getEffect().equals(MobEffects.MOVEMENT_SLOWDOWN.value())) {
            lastSlowdownApplied = 0;
        }
    }

    private void handleIncomingChatLine(ChatPacketReceivedEvent event) {
        StyledText styledText = StyledText.fromComponent(event.getMessage());

        // This is a normal one line chat, or we pass a chat screen through
        StyledText updatedMessage = postChatLine(styledText, MessageType.FOREGROUND);

        if (updatedMessage == null) {
            event.setCanceled(true);
        } else if (!updatedMessage.equals(styledText)) {
            event.setMessage(updatedMessage.getComponent());
        }
    }

    private void handleWithSeparation(ChatPacketReceivedEvent event) {
        StyledText styledText = StyledText.fromComponent(event.getMessage());

        long currentTicks = McUtils.mc().level.getGameTime();

        List<StyledText> lines = StyledTextUtils.splitInLines(styledText);

        // It is a multi-line screen if it is parsed to be multiple lines,
        // or if it is empty and sent in the same tick (with some fuzziness) as the current screen
        if (lines.size() > 1 || (styledText.isEmpty() && (currentTicks <= chatScreenTicks + CHAT_SCREEN_TICK_DELAY))) {
            // This is a "chat screen" message, which is a multi-line message

            // Allow ticks to be equal, since we want to
            // collect all lines in this tick and the next one
            if (currentTicks <= chatScreenTicks + CHAT_SCREEN_TICK_DELAY) {
                // We are collecting lines, so add to the current collection
                collectedLines.addAll(lines);
            } else {
                // Start a new collection
                if (chatScreenTicks != 0) {
                    // Send the old before starting a new. We should not really end up here since this should
                    // be done in the tick handler.
                    processCollectedChatScreen();
                }

                collectedLines = new ArrayList<>(lines);
                chatScreenTicks = currentTicks;
            }

            // For all those cases, we will collect the lines and thus need to cancel the event
            event.setCanceled(true);
        } else {
            if (chatScreenTicks != 0) {
                // We got a normal line while collecting chat screen lines. This means the screen is
                // done, and we should process it first.
                processCollectedChatScreen();
            }

            // Process this as a normal line
            handleIncomingChatLine(event);
        }
    }

    private void processCollectedChatScreen() {
        List<StyledText> lines = new ArrayList<>(collectedLines);

        // Reset screen line collection
        collectedLines = new ArrayList<>();
        chatScreenTicks = 0;

        // From now on, we'll work on reversed lists, so the message that should
        // have been closest to the bottom is now on top.
        Collections.reverse(lines);

        LinkedList<StyledText> newLines = new LinkedList<>();
        if (lastRealChat == null) {
            // If we have no history, all lines are to be considered new
            lines.forEach(newLines::addLast);
        } else {
            // Figure out what's new since last chat message
            for (StyledText line : lines) {
                String plainText = line.getStringWithoutFormatting();
                if (plainText.equals(lastRealChat)) break;
                if (plainText.equals(oneBeforeLastRealChat)) {
                    // We've not found the last chat message, but we have found the one before that
                    // This means that the last chat message was a confirmationless dialogue
                    lastRealChat = oneBeforeLastRealChat;
                    oneBeforeLastRealChat = null;

                    break;
                }
                newLines.addLast(line);
            }
        }

        if (newLines.isEmpty()) {
            // No new lines has appeared since last registered chat line.
            // We could just have a dialog that disappeared, so we must signal this
            handleNpcDialogue(List.of(), NpcDialogueType.NONE, false);
            return;
        }

        boolean expectedConfirmationlessDialogue = false;

        if (newLines.getLast().getString().isEmpty()) {
            if (newLines.size() == 2) {
                // This should happen as a frequent case. It is a confirmationless dialogue.
                // Two new lines are supposed to be received:
                // - One empty line
                // - One line with a (confirmationless) dialogue
                // The dialogue line itself still can be full of À characters (aka. match EMPTY_LINE_PATTERN).
                // In this case, it is a "preparation" screen for the dialog,
                // that should be sent in place of the empty line, in the upcoming packets.
                // Currently, we just ignore this line.

                if (newLines.getFirst().matches(EMPTY_LINE_PATTERN)) {
                    // Both the first and the last line are empty, we expect a dialogue screen in the next packet batch
                    WynntilsMod.info("[NPC] - Confirmationless dialogue preparation screen detected");
                    // Nothing to do here, as both lines are empty
                    return;
                }

                WynntilsMod.info("[NPC] - Line expected to be a confirmationless dialogue: " + newLines.getFirst());
                expectedConfirmationlessDialogue = true;
            } else if (newLines.size() == 4) {
                // This should happen as a special case.
                // Receiving 4 lines can result in two different scenarios:
                // - We received a normal dialogue, with the last line being the control message
                //   (Press SHIFT/Select an option to continue)
                // - We received a **temporary** confirmationless dialogue,
                //   with the last line being the control message, but it not being sent yet, making the line empty
                //   In this case, we will soon receive the control message, as a single line

                // If the first and second line is empty, the third line is a dialogue, and the fourth line is empty,
                // we can assume that the fourth line is the control message,
                // and the third line is a temporary confirmationless dialogue
                if (newLines.get(0).matches(EMPTY_LINE_PATTERN)
                        && newLines.get(1).matches(EMPTY_LINE_PATTERN)
                        && !newLines.get(2).matches(EMPTY_LINE_PATTERN)
                        && newLines.get(3).matches(EMPTY_LINE_PATTERN)) {
                    // The third line is a temporary confirmationless dialogue
                    WynntilsMod.info("[NPC] - Temporary confirmationless dialogue detected");
                    expectedConfirmationlessDialogue = true;

                    // Remove the first two empty lines
                    newLines.removeFirst();
                    newLines.removeFirst();
                }
            }

            // Wynncraft add an empty line before the NPC dialog; remove it
            newLines.removeLast();
        }

        // Now what to do with the new lines we found?
        processNewLines(newLines, expectedConfirmationlessDialogue);
    }

    private void processNewLines(LinkedList<StyledText> newLines, boolean expectedConfirmationlessDialogue) {
        // We have new lines added to the bottom of the chat screen. They are either a dialogue,
        // or new background chat messages. Separate them in two parts
        LinkedList<StyledText> newChatLines = new LinkedList<>();
        LinkedList<StyledText> dialogue = new LinkedList<>();

        StyledText firstText = newLines.getFirst();
        boolean isNpcConfirm = firstText.find(NPC_CONFIRM_PATTERN);
        boolean isNpcSelect = firstText.find(NPC_SELECT_PATTERN);

        if (isNpcConfirm || isNpcSelect) {
            // This is an NPC dialogue screen.
            // First remove the "Press SHIFT/Select an option to continue" trailer.
            newLines.removeFirst();

            // If this happens, the "Press SHIFT/Select an option to continue" got appended to the last dialogue
            // NOTE: Currently, we do nothing in this case, as it seems to work without any issues
            //       In the future, additional handling for converting temporary confirmationless dialogues
            //       to normal dialogues may be needed
            if (newLines.isEmpty()) {
                WynntilsMod.info("[NPC] - Control message appended to the last dialogue");
                return;
            }

            if (newLines.getFirst().getString().isEmpty()) {
                // After this we assume a blank line
                newLines.removeFirst();
            } else {
                WynntilsMod.warn("Malformed dialog [#1]: " + newLines.getFirst());
            }

            boolean dialogDone = false;
            // This need to be false if we are to look for options
            boolean optionsFound = !isNpcSelect;

            // Separate the dialog part from any potential new "real" chat lines
            for (StyledText line : newLines) {
                if (!dialogDone) {
                    if (line.find(EMPTY_LINE_PATTERN)) {
                        if (!optionsFound) {
                            // First part of the dialogue found
                            optionsFound = true;
                            dialogue.push(line);
                        } else {
                            dialogDone = true;
                        }
                        // Intentionally throw away this line
                    } else {
                        dialogue.push(line);
                    }
                } else {
                    // If there is anything after the dialogue, it is new chat lines
                    if (!line.find(EMPTY_LINE_PATTERN)) {
                        newChatLines.push(line);
                    }
                }
            }
        } else if (expectedConfirmationlessDialogue) {
            if (newLines.size() != 1) {
                WynntilsMod.warn("New lines has an unexpected dialogue count [#1]: " + newLines);
            }

            // This is a confirmationless dialogue
            handleNpcDialogue(List.of(newLines.getFirst()), NpcDialogueType.CONFIRMATIONLESS, false);

            // If we expect a confirmationless dialogue, we should only have one line,
            // so we don't have to do any separation logic
            return;
        } else {
            // After a NPC dialogue screen, Wynncraft sends a "clear screen" with line of ÀÀÀ...
            // We just ignore that part. Also, remove empty lines or lines with just the §r code
            while (!newLines.isEmpty() && newLines.getFirst().find(EMPTY_LINE_PATTERN)) {
                newLines.removeFirst();
            }

            // But we may also handle new messages during the NPC dialogue screen here
            // If so, we need to separate the repeated dialogue and the new chat lines
            // The repeated dialogue starts with an empty line, followed by the actual dialogue

            // Reverse back the list, so it's in the order it was received
            Collections.reverse(newLines);

            // Add the lines to the new chat lines, until we find an empty line
            // If an empty line is found, check to see if it's followed by
            // either a confirmationless or a normal dialogue
            // If so, the rest of the lines are dialogues, so ignore them
            // If not, continue adding the lines to the new chat lines, and check for empty lines again,
            // if any are found
            while (!newLines.isEmpty()) {
                StyledText line = newLines.removeFirst();
                if (line.find(EMPTY_LINE_PATTERN)) {
                    if (newLines.isEmpty()) {
                        // If there are no more lines, we can't do anything
                        break;
                    }

                    StyledText nextLine = newLines.getFirst();
                    if (nextLine.equals(lastConfirmationlessDialogue)) {
                        // The rest of the lines is a re-sent confirmationless dialogue
                        if (newLines.size() > 1) {
                            // There should not be any more lines after this
                            WynntilsMod.warn("Unexpected lines after a confirmationless dialogue: " + newLines);
                        }

                        break;
                    }

                    // Check if the following lines match the last NPC screen dialogue
                    // Otherwise, treat them as new chat lines
                    for (StyledText dialogueLine : lastScreenNpcDialogue) {
                        if (newLines.isEmpty()) {
                            // If there are no more lines, we can't do anything
                            break;
                        }

                        StyledText nextDialogueLine = newLines.getFirst();
                        if (!nextDialogueLine.equals(dialogueLine)) {
                            // If the next line does not match the dialogue line, it's a new chat line
                            break;
                        }

                        // If the next line matches the dialogue line, remove it
                        newLines.removeFirst();
                    }

                    // If we have removed all the lines, we don't need to do anything more
                    if (newLines.isEmpty()) {
                        break;
                    }
                }

                // This was not found to be a dialogue line, so add it to the new chat lines
                newChatLines.addLast(line);
            }
        }

        // Register all new chat lines
        newChatLines.forEach(this::handleFakeChatLine);

        // Handle the dialogue, if any
        handleScreenNpcDialog(dialogue, isNpcSelect);
    }

    private void handleScreenNpcDialog(List<StyledText> dialogues, boolean isSelection) {
        if (dialogues.isEmpty()) {
            // dialog could be the empty list, this means the last dialog is removed
            handleNpcDialogue(dialogues, NpcDialogueType.NONE, false);
            return;
        }

        NpcDialogueType type = isSelection ? NpcDialogueType.SELECTION : NpcDialogueType.NORMAL;

        if (McUtils.mc().level.getGameTime() <= lastSlowdownApplied + SLOWDOWN_PACKET_TICK_DELAY) {
            // This is a "protected" dialogue if we have gotten slowdown effect just prior to the chat message
            // This is the normal case
            handleNpcDialogue(dialogues, type, true);
            return;
        }

        // Maybe this should be a protected dialogue but packets came in the wrong order.
        // Wait a tick for slowdown, and then send the event
        delayedDialogue = dialogues;
        delayedType = type;
        Managers.TickScheduler.scheduleNextTick(() -> {
            if (delayedDialogue != null) {
                List<StyledText> dialogToSend = delayedDialogue;
                delayedDialogue = null;
                // If we got here, then we did not get the slowdown effect, otherwise we would
                // have sent the dialogue already
                handleNpcDialogue(dialogToSend, delayedType, false);
            }
        });
    }

    private void handleFakeChatLine(StyledText styledText) {
        // This is a normal, single line chat, sent in the background
        if (styledText.isEmpty()) return;

        // But it can weirdly enough actually also be a foreground NPC chat message, or
        // a game message; similar to a dialogue but not uttered by an NPC.
        RecipientType recipientType = getRecipientType(styledText, MessageType.FOREGROUND);
        if (recipientType == RecipientType.NPC) {
            // In this case, do *not* save this as last chat, since it will soon disappear
            // from history!
            handleNpcDialogue(List.of(styledText), NpcDialogueType.CONFIRMATIONLESS, false);
            return;
        }

        StyledText updatedMessage = postChatLine(styledText, MessageType.BACKGROUND);
        // If the message is canceled, we do not need to cancel any packets,
        // just don't send out the chat message
        if (updatedMessage == null) return;

        // Otherwise emulate a normal incoming chat message
        McUtils.sendMessageToClient(updatedMessage.getComponent());
    }

    /**
     * Return a "massaged" version of the message, or null if we should cancel the
     * message entirely.
     */
    private StyledText postChatLine(StyledText styledText, MessageType messageType) {
        String plainText = styledText.getStringWithoutFormatting();
        if (!plainText.isBlank()) {
            // We store the unformatted string version to be able to compare between
            // foreground and background versions
            oneBeforeLastRealChat = lastRealChat;
            lastRealChat = plainText;
        }

        // Normally § codes are stripped from the log; need this to be able to debug chat formatting
        WynntilsMod.info("[CHAT] " + styledText.getString().replace("§", "&"));
        RecipientType recipientType = getRecipientType(styledText, messageType);

        if (recipientType == RecipientType.NPC) {
            if (shouldSeparateNPC()) {
                handleNpcDialogue(List.of(styledText), NpcDialogueType.CONFIRMATIONLESS, false);
                // We need to cancel the original chat event, if any
                return null;
            } else {
                // Reclassify this as a INFO type for the chat
                recipientType = RecipientType.INFO;
            }
        }

        ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(styledText, messageType, recipientType);
        WynntilsMod.postEvent(event);
        if (event.isCanceled()) return null;
        return event.getStyledText();
    }

    private void handleNpcDialogue(List<StyledText> dialogue, NpcDialogueType type, boolean isProtected) {
        if (type == NpcDialogueType.NONE) {
            // Ignore any delayed dialogues, since they are now obsolete
            delayedDialogue = null;
        }

        // Confirmationless dialogues bypass the lastScreenNpcDialogue check
        if (type == NpcDialogueType.CONFIRMATIONLESS) {
            if (dialogue.size() != 1) {
                WynntilsMod.warn("Confirmationless dialogues should only have one line: " + dialogue);
            }

            // Store the last confirmationless dialogue, but it may be repeated,
            // so we need to check that it's not duplicated when a message is sent during the dialogue
            lastConfirmationlessDialogue = dialogue.getFirst();
        } else {
            if (lastScreenNpcDialogue.equals(dialogue)) return;

            lastScreenNpcDialogue = dialogue;
        }

        Models.NpcDialogue.handleDialogue(dialogue, isProtected, type);
    }

    private RecipientType getRecipientType(StyledText codedMessage, MessageType messageType) {
        // Check if message match a recipient category
        for (RecipientType recipientType : RecipientType.values()) {
            if (recipientType.matchPattern(codedMessage, messageType)) {
                return recipientType;
            }
        }

        // If no specific recipient matched, it is an "info" message
        return RecipientType.INFO;
    }

    private boolean shouldSeparateNPC() {
        return Models.NpcDialogue.isNpcDialogExtractionRequired();
    }
}
