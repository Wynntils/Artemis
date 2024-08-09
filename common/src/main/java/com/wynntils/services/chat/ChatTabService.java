/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.chat;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Service;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.chat.ChatTabsFeature;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.mc.event.ClientsideMessageEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;

public final class ChatTabService extends Service {
    private ChatComponent fallbackChat;

    private ChatTab focusedTab = null;

    private final Map<ChatTab, ChatComponent> chatTabData = new ConcurrentHashMap<>();
    private final Map<ChatTab, Boolean> unreadMessages = new ConcurrentHashMap<>();

    public ChatTabService() {
        super(List.of());
    }

    public List<ChatTab> getChatTabs() {
        return Managers.Feature.getFeatureInstance(ChatTabsFeature.class)
                .chatTabs
                .get();
    }

    public Stream<ChatTab> getTabs() {
        return getChatTabs().stream();
    }

    public ChatTab getTab(int index) {
        return getChatTabs().get(index);
    }

    public int getTabCount() {
        return getChatTabs().size();
    }

    public boolean isTabListEmpty() {
        return getTabCount() == 0;
    }

    public void addTab(int insertIndex, ChatTab chatTab) {
        getChatTabs().add(insertIndex, chatTab);
        Managers.Feature.getFeatureInstance(ChatTabsFeature.class).chatTabs.touched();
    }

    public void removeTab(ChatTab chatTab) {
        getChatTabs().remove(chatTab);
        Managers.Feature.getFeatureInstance(ChatTabsFeature.class).chatTabs.touched();
    }

    public int getTabIndex(ChatTab edited) {
        return getChatTabs().indexOf(edited);
    }

    public int getNextFocusedTab() {
        return (getTabIndex(getFocusedTab()) + 1) % getTabCount();
    }

    public int getPreviousFocusedTab() {
        int tabIndex = getTabIndex(getFocusedTab());
        return (tabIndex - 1 + getTabCount()) % getTabCount();
    }

    public void refocusFirstTab() {
        if (!isTabListEmpty()) {
            setFocusedTab(0);
        }
    }

    public void resetFocusedTab() {
        setFocusedTab(null);
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.NOT_CONNECTED) {
            chatTabData.clear();
            unreadMessages.clear();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        chatTabData.values().forEach(ChatComponent::tick);
    }

    public void setFocusedTab(int index) {
        setFocusedTab(getTab(index));
    }

    public void setFocusedTab(ChatTab focused) {
        focusedTab = focused;

        if (focusedTab == null) {
            if (fallbackChat == null) {
                fallbackChat = new ChatComponent(McUtils.mc());
            }

            McUtils.mc().gui.chat = fallbackChat;
        } else {
            chatTabData.putIfAbsent(focusedTab, new ChatComponent(McUtils.mc()));
            unreadMessages.put(focusedTab, false);
            McUtils.mc().gui.chat = chatTabData.get(focusedTab);
        }
    }

    public ChatTab getFocusedTab() {
        return focusedTab;
    }

    public boolean hasUnreadMessages(ChatTab tab) {
        return unreadMessages.getOrDefault(tab, false);
    }

    public void matchMessage(ClientsideMessageEvent event) {
        // Firstly, find the FIRST matching tab with high priority
        for (ChatTab chatTab : getChatTabs()) {
            if (!chatTab.isConsuming()) continue;

            if (matchMessageFromEvent(chatTab, event)) {
                addMessageToTab(chatTab, event.getStyledText().getComponent());
                return;
            }
        }

        // Secondly, match ALL tabs with low priority
        for (ChatTab chatTab : getChatTabs()) {
            if (chatTab.isConsuming()) continue;

            if (matchMessageFromEvent(chatTab, event)) {
                addMessageToTab(chatTab, event.getStyledText().getComponent());
            }
        }
    }

    public void matchMessage(ChatMessageReceivedEvent event) {
        // Firstly, find the FIRST matching tab with high priority
        for (ChatTab chatTab : getChatTabs()) {
            if (!chatTab.isConsuming()) continue;

            if (matchMessageFromEvent(chatTab, event)) {
                addMessageToTab(chatTab, event.getStyledText().getComponent());
                return;
            }
        }

        // Secondly, match ALL tabs with low priority
        for (ChatTab chatTab : getChatTabs()) {
            if (chatTab.isConsuming()) continue;

            if (matchMessageFromEvent(chatTab, event)) {
                addMessageToTab(chatTab, event.getStyledText().getComponent());
            }
        }
    }

    private void addMessageToTab(ChatTab tab, Component message) {
        chatTabData.putIfAbsent(tab, new ChatComponent(McUtils.mc()));

        try {
            chatTabData.get(tab).addMessage(message);
        } catch (Throwable t) {
            MutableComponent warning = Component.literal(
                            "<< WARNING: A chat message was lost due to a crash in a mod other than Wynntils. See log for details. >>")
                    .withStyle(ChatFormatting.RED);
            chatTabData.get(tab).addMessage(warning);
            // We have seen many issues with badly written mods that inject into addMessage, and
            // throws exceptions. Instead of considering it a Wynntils crash, dump it to the log and
            // ignore it. We can't resend the message to the chat, since that could cause an infinite loop,
            // but the log should be fine.
            WynntilsMod.warn("Another mod has caused an exception in ChatComponent.addMessage()");
            WynntilsMod.warn("The message that could not be displayed is:"
                    + StyledText.fromComponent(message).getString());
            WynntilsMod.warn("This is not a Wynntils bug. Here is the exception that we caught.", t);
        }

        if (focusedTab != tab) {
            unreadMessages.put(tab, true);
        }
    }

    private boolean matchMessageFromEvent(ChatTab chatTab, ChatMessageReceivedEvent event) {
        if (chatTab.getFilteredTypes() != null
                && !chatTab.getFilteredTypes().isEmpty()
                && !chatTab.getFilteredTypes().contains(event.getRecipientType())) {
            return false;
        }

        Optional<Pattern> regex = chatTab.getCustomRegex();
        return regex.isEmpty() || event.getOriginalStyledText().matches(regex.get());
    }

    private boolean matchMessageFromEvent(ChatTab chatTab, ClientsideMessageEvent event) {
        if (chatTab.getFilteredTypes() != null
                && !chatTab.getFilteredTypes().isEmpty()
                && !chatTab.getFilteredTypes().contains(RecipientType.CLIENTSIDE)) {
            return false;
        }

        Optional<Pattern> regex = chatTab.getCustomRegex();
        if (regex.isEmpty()) return true;

        return event.getOriginalStyledText().matches(regex.get());
    }

    /**
     * Sends a chat message respecting chat tab autocommand settings.
     * If no chat tab is actively selected, the message will be sent normally.
     * Since autocommands are still commands, they will be queued just like any other command.
     * They are also subject to the same ratelimits on Wynncraft.
     * @param message The message to send.
     */
    public void sendChat(String message) {
        if (message.isBlank()) return;

        if (getFocusedTab() == null) {
            McUtils.sendChat(message);
            return;
        }

        String autoCommand = getFocusedTab().getAutoCommand();
        if (autoCommand != null && !autoCommand.isBlank()) {
            autoCommand = autoCommand.startsWith("/") ? autoCommand.substring(1) : autoCommand;
            Handlers.Command.sendCommandImmediately(autoCommand + " " + message);
        } else {
            McUtils.sendChat(message);
        }
    }
}
