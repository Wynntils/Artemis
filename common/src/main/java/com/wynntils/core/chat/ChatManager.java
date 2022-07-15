/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.chat;

import com.wynntils.core.WynntilsMod;
import com.wynntils.mc.event.ChatReceivedEvent;
import com.wynntils.wc.utils.WynnUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ChatManager {
    private static final Pattern INFO_BAR_PATTERN = Pattern.compile("§c❤ (\\d+)\\/(\\d+)§0 .* §b✺ (\\d+)\\/(\\d+)");

    public static void init() {
        WynntilsMod.getEventBus().register(ChatManager.class);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChatReceived(ChatReceivedEvent e) {
        if (!WynnUtils.onWorld()) return;

        switch (e.getType()) {
            case GAME_INFO -> parseGameInfo(e.getMessage());
            case SYSTEM -> parseSystemMessage(e.getMessage());
            case CHAT -> parseChatMessage(e.getMessage());
        }
    }

    private static void parseChatMessage(Component message) {
        // this is a real chat message, or wynncraft stuff like
        // " [Info] Link your Wynncraft forum account with "/forum" and get a stylish white username in chat! Visit
        // wynncraft.com/forumlink for more!"
    }

    private static void parseSystemMessage(Component message) {
        // this is a wynncraft response, e.g.
        // "/toggle
        // [swears/blood/insults/autojoin/music/vet/war/guildjoin/attacksound/rpwarning/100/sb/autotracking/pouchmsg/combatbar/ghosts/popups/guildpopups/friendpopups/beacon/outlines/bombbell/pouchpickup/queststartbeacon]"
    }

    private static void parseGameInfo(Component message) {
        // Game info is the message above the health bar
        // On Wynncraft it contain your health, position and mana
        String str = message.getString();
        Matcher m = INFO_BAR_PATTERN.matcher(str);
        if (m.find()) {
            int currentHealth = Integer.parseInt(m.group(1));
            int maxHealth = Integer.parseInt(m.group(2));
            int currentMana = Integer.parseInt(m.group(3));
            int maxMana = Integer.parseInt(m.group(4));
            WynntilsMod.getEventBus().post(new InfoBarUpdateEvent(currentHealth, maxHealth, currentMana, maxMana));
        }
    }
}
