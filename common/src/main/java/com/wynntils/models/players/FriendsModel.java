/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.players;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.MessageType;
import com.wynntils.models.players.event.FriendsEvent;
import com.wynntils.models.players.event.HadesRelationsUpdateEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.services.hades.event.HadesEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class FriendsModel extends Model {
    // 󏿼󏿿󏿾 is for the first line
    // 󏿼󐀆  is for the other lines
    private static final String FRIEND_PREFIX_REGEX =
            "(?:(?:§a)?(?:\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE|\uDAFF\uDFFC\uE001\uDB00\uDC06)\\s)?";

    // region Friend Regexes
    /*
    Regexes should be named with this format:
    FRIEND_ACTION_(DETAIL)
    where:
    FRIEND should be the first word
    ACTION should be something like ADD, LIST, etc.
    DETAIL (optional) should be a descriptor if necessary
     */

    private static final Pattern FRIEND_LIST =
            Pattern.compile(FRIEND_PREFIX_REGEX + ".+'s? friends \\(.+\\): (.*)", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern FRIEND_LIST_FAIL_1 =
            Pattern.compile(FRIEND_PREFIX_REGEX + "We couldn't find any friends\\.");
    private static final Pattern FRIEND_LIST_FAIL_2 =
            Pattern.compile(FRIEND_PREFIX_REGEX + "Try typing §6/friend add Username§e!");
    private static final Pattern FRIEND_REMOVE_MESSAGE_PATTERN =
            Pattern.compile(FRIEND_PREFIX_REGEX + "(.+) has been removed from your friends!");
    private static final Pattern FRIEND_ADD_MESSAGE_PATTERN =
            Pattern.compile(FRIEND_PREFIX_REGEX + "(.+) has been added to your friends!");

    // Test in FriendsModel_ONLINE_FRIENDS_HEADER
    private static final Pattern ONLINE_FRIENDS_HEADER = Pattern.compile(FRIEND_PREFIX_REGEX + "Online Friends:");
    // Test in FriendsModel_ONLINE_FRIEND
    private static final Pattern ONLINE_FRIEND =
            Pattern.compile(FRIEND_PREFIX_REGEX + "§2 - §a(\\w{1,16})§2 \\[Server: §aWC(\\d{1,3})§2]");

    // Test in FriendsModel_JOIN_PATTERN
    private static final Pattern JOIN_PATTERN = Pattern.compile(
            FRIEND_PREFIX_REGEX
                    + "(?<username>\\w{1,16})§2 has logged into server §aWC(?<server>\\d{1,3})§2 as §aan? (?<class>[A-Z][a-z]+)");
    // Test in FriendsModel_LEAVE_PATTERN
    private static final Pattern LEAVE_PATTERN =
            Pattern.compile(FRIEND_PREFIX_REGEX + "(?<username>\\w{1,16}) left the game\\.");
    // endregion

    private static final int REQUEST_RATELIMIT = 250;

    private ListStatus friendMessageStatus = ListStatus.IDLE;
    private long lastFriendRequest = 0;

    private ListStatus onlineMessageStatus = ListStatus.IDLE;
    private long lastOnlineRequest = 0;

    private Set<String> friends;
    private Map<String, Integer> onlineFriends = new HashMap<>(); // <username, server>

    public FriendsModel() {
        super(List.of());

        resetData();
    }

    @SubscribeEvent
    public void onAuth(HadesEvent.Authenticated event) {
        if (!Models.WorldState.onWorld()) return;

        requestData();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.WORLD) {
            requestData();
        } else {
            resetData();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatReceived(ChatMessageReceivedEvent event) {
        if (event.getMessageType() != MessageType.FOREGROUND) return;

        StyledText styledText = event.getOriginalStyledText();
        String unformatted = styledText.getStringWithoutFormatting();

        Matcher joinMatcher = styledText.getMatcher(JOIN_PATTERN);
        if (joinMatcher.matches()) {
            String username = joinMatcher.group("username");
            int server = Integer.parseInt(joinMatcher.group("server"));

            onlineFriends.put(username, server);
            WynntilsMod.postEvent(new FriendsEvent.Joined(username, server));
            return;
        }

        Matcher leaveMatcher = styledText.getMatcher(LEAVE_PATTERN);
        if (leaveMatcher.matches()) {
            String username = leaveMatcher.group("username");

            onlineFriends.remove(username);
            WynntilsMod.postEvent(new FriendsEvent.Left(username));
            return;
        }

        if (tryParseFriendMessages(styledText)) return;

        if (friendMessageStatus == ListStatus.EXPECTING) {
            if (tryParseFriendList(unformatted) || tryParseNoFriendList(styledText)) {
                event.setCanceled(true);
                friendMessageStatus = ListStatus.IDLE;
                return;
            }

            // Skip first message of two, but still expect more messages
            if (styledText.getMatcher(FRIEND_LIST_FAIL_1).matches()) {
                event.setCanceled(true);
                return;
            }
        }

        if (onlineMessageStatus == ListStatus.EXPECTING
                && styledText.getMatcher(ONLINE_FRIENDS_HEADER).matches()) {
            // List of online friends is sent in multiple messages
            // When we detect the first message indicating the start of the friends list, we set a flag
            onlineMessageStatus = ListStatus.PROCESSING;
            onlineFriends.clear();
            event.setCanceled(true);
            return;
        }
        if (onlineMessageStatus == ListStatus.PROCESSING) {
            // If this flag is set, the next messages should be the list of online friends
            // But as soon as the matcher fails, we know we've reached the end of the list
            Matcher onlineFriendMatcher = styledText.getMatcher(ONLINE_FRIEND);
            if (onlineFriendMatcher.matches()) {
                String username = onlineFriendMatcher.group(1);
                int server = Integer.parseInt(onlineFriendMatcher.group(2));

                onlineFriends.put(username, server);
                WynntilsMod.info("Friend " + username + " is online on WC" + server);
                event.setCanceled(true);
                return;
            } else {
                onlineMessageStatus = ListStatus.IDLE;
                WynntilsMod.postEvent(new FriendsEvent.OnlineListed());
            }
        }
    }

    private boolean tryParseNoFriendList(StyledText styledText) {
        if (styledText.getMatcher(FRIEND_LIST_FAIL_2).matches()) {
            WynntilsMod.info("Friend list is empty.");
            return true;
        }

        return false;
    }

    private boolean tryParseFriendMessages(StyledText styledText) {
        Matcher matcher = styledText.getMatcher(FRIEND_REMOVE_MESSAGE_PATTERN);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player has removed friend: " + player);

            friends.remove(player);
            WynntilsMod.postEvent(new HadesRelationsUpdateEvent.FriendList(
                    Set.of(player), HadesRelationsUpdateEvent.ChangeType.REMOVE));
            WynntilsMod.postEvent(new FriendsEvent.Removed(player));
            return true;
        }

        matcher = styledText.getMatcher(FRIEND_ADD_MESSAGE_PATTERN);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player has added friend: " + player);

            friends.add(player);
            WynntilsMod.postEvent(
                    new HadesRelationsUpdateEvent.FriendList(Set.of(player), HadesRelationsUpdateEvent.ChangeType.ADD));
            WynntilsMod.postEvent(new FriendsEvent.Added(player));
            return true;
        }

        return false;
    }

    private boolean tryParseFriendList(String unformatted) {
        Matcher matcher = FRIEND_LIST.matcher(unformatted);
        if (!matcher.matches()) return false;

        String[] friendList = matcher.group(1)
                .replaceAll(FRIEND_PREFIX_REGEX, "")
                .replaceAll("\\n", "")
                .split(", ");

        friends = Arrays.stream(friendList).collect(Collectors.toSet());
        WynntilsMod.postEvent(
                new HadesRelationsUpdateEvent.FriendList(friends, HadesRelationsUpdateEvent.ChangeType.RELOAD));
        WynntilsMod.postEvent(new FriendsEvent.Listed());

        WynntilsMod.info("Successfully updated friend list, user has " + friendList.length + " friends.");
        return true;
    }

    private void resetData() {
        friends = new HashSet<>();
        onlineFriends = new HashMap<>();

        WynntilsMod.postEvent(
                new HadesRelationsUpdateEvent.FriendList(friends, HadesRelationsUpdateEvent.ChangeType.RELOAD));
    }

    /**
     * Sends "/friend list" to Wynncraft and waits for the response.
     * (!) Skips if the last request was less than 250ms ago.
     * When the response is received, friends will be updated.
     */
    public void requestData() {
        if (McUtils.player() == null) return;

        if (System.currentTimeMillis() - lastFriendRequest > REQUEST_RATELIMIT) {
            friendMessageStatus = ListStatus.EXPECTING;
            lastFriendRequest = System.currentTimeMillis();
            Handlers.Command.queueCommand("friend list");
        } else {
            WynntilsMod.info("Skipping friend list request because it was requested very recently.");
        }

        if (System.currentTimeMillis() - lastOnlineRequest > REQUEST_RATELIMIT) {
            onlineMessageStatus = ListStatus.EXPECTING;
            lastOnlineRequest = System.currentTimeMillis();
            Handlers.Command.queueCommand("friend online");
        } else {
            WynntilsMod.info("Skipping online friend list request because it was requested very recently.");
        }
    }

    public boolean isFriend(String playerName) {
        return friends.contains(playerName);
    }

    public Set<String> getFriends() {
        return Collections.unmodifiableSet(friends);
    }

    public Map<String, Integer> getOnlineFriends() {
        return Collections.unmodifiableMap(onlineFriends);
    }

    private enum ListStatus {
        IDLE,
        EXPECTING,
        PROCESSING
    }
}
