/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ChatScreenKeyTypedEvent;
import com.wynntils.mc.event.ChatScreenSendEvent;
import com.wynntils.mc.event.CommandSentEvent;
import com.wynntils.mc.event.EditBoxInsertEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.models.wynnalphabet.WynnAlphabet;
import com.wynntils.screens.transcription.widgets.WynnAlphabetButton;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.CHAT)
public class InputTranscriptionFeature extends Feature {
    @Persisted
    public final Config<Boolean> transcriptionButtons = new Config<>(true);

    private static final int MAX_CHAT_LENGTH = 256;
    // Numbers higher than this will be replaced with "∞"
    private static final int MAX_TRANSCRIPTABLE_NUMBER = 5000;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    @SubscribeEvent
    public void onScreenInit(ScreenInitEvent event) {
        if (!transcriptionButtons.get()) return;

        if (event.getScreen() instanceof ChatScreen chatScreen) {
            chatScreen.width -= 45;
            int xOffset = chatScreen.width + 1;

            addAlphabetButton(chatScreen, xOffset, WynnAlphabet.DEFAULT);
            xOffset += 15;

            addAlphabetButton(chatScreen, xOffset, WynnAlphabet.WYNNIC);
            xOffset += 15;

            addAlphabetButton(chatScreen, xOffset, WynnAlphabet.GAVELLIAN);
        }
    }

    @SubscribeEvent
    public void onEditBoxInsert(EditBoxInsertEvent event) {
        if (!(McUtils.mc().screen instanceof ChatScreen chatScreen)) return;
        if (!transcriptionButtons.get()) return;
        if (event.getTextToWrite().isBlank()) return;

        WynnAlphabet selectedAlphabet = Models.WynnAlphabet.getSelectedAlphabet();

        if (selectedAlphabet == WynnAlphabet.DEFAULT) return;

        // Can't use parseInt here as '０', '１', '２' are used for '.', '!', '?' in Wynnic
        Matcher numMatcher = NUMBER_PATTERN.matcher(event.getTextToWrite());

        if (numMatcher.matches()) {
            if (selectedAlphabet != WynnAlphabet.WYNNIC) return;

            handleTypedNumber(
                    Integer.parseInt(event.getTextToWrite()),
                    Models.WynnAlphabet.getWynnicNumbers(),
                    event,
                    chatScreen);
        } else {
            List<Character> replacementList = selectedAlphabet == WynnAlphabet.GAVELLIAN
                    ? Models.WynnAlphabet.getGavellianCharacters()
                    : Models.WynnAlphabet.getWynnicCharacters();

            handleTypedCharacter(event.getTextToWrite(), replacementList, event, chatScreen);
        }
    }

    @SubscribeEvent
    public void onChatScreenKeyTyped(ChatScreenKeyTypedEvent event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_BACKSPACE) return;
        if (!(McUtils.mc().screen instanceof ChatScreen chatScreen)) return;
        if (!transcriptionButtons.get()) return;
        if (!chatScreen.input.getHighlighted().isBlank()) return;

        String beforeCursor = chatScreen.input.getValue().substring(0, chatScreen.input.getCursorPosition());
        String afterCursor = chatScreen.input.getValue().substring(chatScreen.input.getCursorPosition());

        if (beforeCursor.isBlank()
                || !Models.WynnAlphabet.getWynnicNumbers().contains(beforeCursor.charAt(beforeCursor.length() - 1)))
            return;

        String currentNumsStr = getWynnicNumBeforeCursor(beforeCursor);

        int wynnicNum = Models.WynnAlphabet.wynnicNumToInt(currentNumsStr);

        String backspacedNum = String.valueOf(wynnicNum);

        if (backspacedNum.length() > 1) {
            event.setCanceled(true);

            backspacedNum = backspacedNum.substring(0, backspacedNum.length() - 1);

            int index = beforeCursor.lastIndexOf(currentNumsStr);

            beforeCursor = beforeCursor.substring(0, index);

            updateInput(beforeCursor, afterCursor, Integer.parseInt(backspacedNum), chatScreen.input);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChatScreenSend(ChatScreenSendEvent event) {
        // Parses brackets and does the transcription, then sends the new message.
        if (event.getInput().isBlank()) return;

        String message = event.getInput();
        if (!Models.WynnAlphabet.containsBrackets(message)) return;

        String updatedMessage = Models.WynnAlphabet.transcribeBracketedText(message);
        updatedMessage = updatedMessage.substring(0, Math.min(updatedMessage.length(), MAX_CHAT_LENGTH));
        if (updatedMessage.equals(message)) return; // No changes, we can pretend nothing happened.

        event.setCanceled(true);
        Services.ChatTab.sendChat(updatedMessage);

        Models.WynnAlphabet.setSelectedAlphabet(WynnAlphabet.DEFAULT);
    }

    @SubscribeEvent
    public void onCommandSent(CommandSentEvent event) {
        String command = event.getCommand();

        if (Models.WynnAlphabet.containsBrackets(command)) {
            String updatedCommand = Models.WynnAlphabet.transcribeBracketedText(command);

            updatedCommand = updatedCommand.substring(0, Math.min(updatedCommand.length(), MAX_CHAT_LENGTH));

            if (!updatedCommand.equals(command)) {
                event.setCanceled(true);
                Handlers.Command.sendCommandImmediately(updatedCommand);
            }
        }

        Models.WynnAlphabet.setSelectedAlphabet(WynnAlphabet.DEFAULT);
    }

    private void addAlphabetButton(ChatScreen chatScreen, int xOffset, WynnAlphabet alphabet) {
        chatScreen.addRenderableWidget(new WynnAlphabetButton(xOffset, chatScreen.height - 14, 12, 12, alphabet));
    }

    private void handleTypedCharacter(
            String typedChar, List<Character> replacementList, EditBoxInsertEvent event, ChatScreen chatScreen) {
        int engIndex = Models.WynnAlphabet.getEnglishCharacters()
                .indexOf(typedChar.toLowerCase(Locale.ROOT).charAt(0));

        if (engIndex == -1) return;
        if (engIndex >= replacementList.size()) return;

        String replaced = String.valueOf(replacementList.get(engIndex));

        event.setCanceled(true);

        chatScreen.input.insertText(replaced);
    }

    private void handleTypedNumber(
            int typedNumber, List<Character> replacementList, EditBoxInsertEvent event, ChatScreen chatScreen) {
        String input = chatScreen.input.getValue();

        String beforeCursor = input.substring(0, chatScreen.input.getCursorPosition());
        String afterCursor = input.substring(chatScreen.input.getCursorPosition());

        if (input.isBlank()
                || !Models.WynnAlphabet.getWynnicNumbers().contains(beforeCursor.charAt(beforeCursor.length() - 1))) {
            int numIndex = Models.WynnAlphabet.getEnglishNumbers().indexOf(typedNumber);

            if (numIndex == -1) return;

            event.setCanceled(true);

            String replaced = String.valueOf(replacementList.get(numIndex));

            chatScreen.input.insertText(replaced);

            return;
        }

        event.setCanceled(true);

        String currentNumsStr = getWynnicNumBeforeCursor(beforeCursor);

        int index = beforeCursor.lastIndexOf(currentNumsStr);

        beforeCursor = beforeCursor.substring(0, index);

        int updateNum = Models.WynnAlphabet.calculateWynnicNum(currentNumsStr, typedNumber);

        updateInput(beforeCursor, afterCursor, updateNum, chatScreen.input);
    }

    private String getWynnicNumBeforeCursor(String beforeCursor) {
        StringBuilder currentNumsStr = new StringBuilder();

        List<Character> numbers = Models.WynnAlphabet.getWynnicNumbers();

        for (int i = beforeCursor.length() - 1; i >= 0; i--) {
            if (numbers.contains(beforeCursor.charAt(i))) {
                currentNumsStr.append(beforeCursor.charAt(i));
            } else {
                break;
            }
        }

        return currentNumsStr.reverse().toString();
    }

    private void updateInput(String beforeCursor, String afterCursor, int num, EditBox chatInput) {
        String transcriptedNum = num > MAX_TRANSCRIPTABLE_NUMBER ? "∞" : Models.WynnAlphabet.intToWynnicNum(num);

        String newInput = beforeCursor + transcriptedNum + afterCursor;

        if (newInput.length() > MAX_CHAT_LENGTH) return;

        int newCursorPos = beforeCursor.length() + transcriptedNum.length();

        chatInput.setValue(newInput);

        chatInput.moveCursorTo(newCursorPos, false);
    }
}
