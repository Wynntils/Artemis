/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import com.wynntils.core.chat.ChatTab;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.config.TypeOverride;
import com.wynntils.core.features.UserFeature;
import com.wynntils.handlers.chat.RecipientType;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.ChatScreenKeyTypedEvent;
import com.wynntils.mc.event.ClientsideMessageEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.mc.event.ScreenRenderEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.screens.chattabs.widgets.ChatTabAddButton;
import com.wynntils.screens.chattabs.widgets.ChatTabButton;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class ChatTabsFeature extends UserFeature {
    public static ChatTabsFeature INSTANCE;

    @Config(visible = false)
    public List<ChatTab> chatTabs = new ArrayList<>(List.of(
            new ChatTab("All", false, null, null, null),
            new ChatTab("Global", false, null, Sets.newHashSet(RecipientType.GLOBAL), null),
            new ChatTab("Local", false, null, Sets.newHashSet(RecipientType.LOCAL), null),
            new ChatTab("Guild", false, "/g ", Sets.newHashSet(RecipientType.GUILD), null),
            new ChatTab("Party", false, "/p ", Sets.newHashSet(RecipientType.PARTY), null),
            new ChatTab("Private", false, "/r ", Sets.newHashSet(RecipientType.PRIVATE), null),
            new ChatTab("Shout", false, null, Sets.newHashSet(RecipientType.SHOUT), null)));

    @TypeOverride
    private final Type chatTabsType = new TypeToken<ArrayList<ChatTab>>() {}.getType();

    // We do this here, and not in Models.ChatTab to not introduce a feature-model dependency.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChatReceived(ChatMessageReceivedEvent event) {
        // We are already sending this message to every matching tab, so we can cancel it.
        event.setCanceled(true);

        // Firstly, find the FIRST matching tab with high priority
        for (ChatTab chatTab : chatTabs) {
            if (!chatTab.isConsuming()) continue;

            if (chatTab.matchMessageFromEvent(event)) {
                Managers.ChatTab.addMessageToTab(chatTab, event.getMessage());
                return;
            }
        }

        // Secondly, match ALL tabs with low priority
        for (ChatTab chatTab : chatTabs) {
            if (chatTab.isConsuming()) continue;

            if (chatTab.matchMessageFromEvent(event)) {
                Managers.ChatTab.addMessageToTab(chatTab, event.getMessage());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientsideChat(ClientsideMessageEvent event) {
        // We've already sent this message to every matching tab, so we can cancel it.
        event.setCanceled(true);

        // Firstly, find the FIRST matching tab with high priority
        for (ChatTab chatTab : chatTabs) {
            if (!chatTab.isConsuming()) continue;

            if (chatTab.matchMessageFromEvent(event)) {
                Managers.ChatTab.addMessageToTab(chatTab, event.getComponent());
                return;
            }
        }

        // Secondly, match ALL tabs with low priority
        for (ChatTab chatTab : chatTabs) {
            if (chatTab.isConsuming()) continue;

            if (chatTab.matchMessageFromEvent(event)) {
                Managers.ChatTab.addMessageToTab(chatTab, event.getComponent());
            }
        }
    }

    @SubscribeEvent
    public void onScreenInit(ScreenInitEvent event) {
        if (event.getScreen() instanceof ChatScreen chatScreen) {
            int xOffset = 0;

            chatScreen.addRenderableWidget(new ChatTabAddButton(xOffset + 2, chatScreen.height - 35, 12, 13));
            xOffset += 15;

            for (ChatTab chatTab : chatTabs) {
                chatScreen.addRenderableWidget(new ChatTabButton(xOffset + 2, chatScreen.height - 35, 40, 13, chatTab));
                xOffset += 43;
            }
        }
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.WORLD
                && !chatTabs.isEmpty()
                && Managers.ChatTab.getFocusedTab() == null) {
            // We joined wynn, time to override our focused tab.
            Managers.ChatTab.setFocusedTab(chatTabs.get(0));
        }
    }

    @SubscribeEvent
    public void onScreenRender(ScreenRenderEvent event) {
        if (!(event.getScreen() instanceof ChatScreen chatScreen)) return;

        // We render this twice for chat screen, but it is not heavy and this is a simple and least conflicting way of
        // rendering command suggestions on top of chat tab buttons.
        chatScreen.commandSuggestions.render(event.getPoseStack(), event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public void onChatScreenKeyTyped(ChatScreenKeyTypedEvent event) {
        // We can't use keybinds here to not conflict with TAB key's other behaviours.
        if (event.getKeyCode() != GLFW.GLFW_KEY_TAB) return;
        if (!(McUtils.mc().screen instanceof ChatScreen)) return;
        if (!KeyboardUtils.isShiftDown()) return;

        event.setCanceled(true);
        Managers.ChatTab.setFocusedTab(
                chatTabs.get((chatTabs.indexOf(Managers.ChatTab.getFocusedTab()) + 1) % chatTabs.size()));
    }

    @Override
    protected void postEnable() {
        if (chatTabs.isEmpty()) return;

        Managers.ChatTab.setFocusedTab(chatTabs.get(0));
    }

    @Override
    protected void onConfigUpdate(ConfigHolder configHolder) {
        if (!chatTabs.isEmpty()) {
            Managers.ChatTab.setFocusedTab(chatTabs.get(0));
        }

        if ((McUtils.mc().screen instanceof ChatScreen chatScreen)) {
            // Reload chat tab buttons
            chatScreen.init(
                    McUtils.mc(),
                    McUtils.mc().getWindow().getGuiScaledWidth(),
                    McUtils.mc().getWindow().getGuiScaledHeight());
        }
    }
}
