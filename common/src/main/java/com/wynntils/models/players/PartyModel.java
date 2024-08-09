/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.players;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.MessageType;
import com.wynntils.handlers.scoreboard.ScoreboardPart;
import com.wynntils.mc.event.SetPlayerTeamEvent;
import com.wynntils.models.players.event.HadesRelationsUpdateEvent;
import com.wynntils.models.players.event.PartyEvent;
import com.wynntils.models.players.scoreboard.PartyScoreboardPart;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.services.hades.event.HadesEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * This model handles the player's party relations.
 */
public final class PartyModel extends Model {
    // 󏿼󏿿󏿾 is for the first line
    // 󏿼󐀆  is for the other lines
    private static final String PARTY_PREFIX_REGEX =
            "(?:(?:§e)?(?:\uDAFF\uDFFC\uE005\uDAFF\uDFFF\uE002\uDAFF\uDFFE|\uDAFF\uDFFC\uE001\uDB00\uDC06)\\s)?";

    // region Party Regexes
    /*
    Regexes should be named with this format:
    PARTY_ACTION_INVOLVED_(DETAIL)
    where:
    PARTY should be the first word in the name
    ACTION should be something like JOIN, LEAVE, LIST, etc.
    INVOLVED should be the player that is affected by the action:
    - ALL should be used if the action affects all players in the party
    - LEADER should be used if the action affects the party leader
    - SELF should be used if the action affects the player
    - OTHER should be used if the action affects another player(s), but not ALL players
    DETAIL (optional) should be a descriptor if necessary
    */
    private static final Pattern PARTY_LIST_ALL = Pattern.compile(PARTY_PREFIX_REGEX + "Party members: (.*)");
    private static final Pattern PARTY_LIST_LEADER = Pattern.compile("§b(\\w{1,16})");
    private static final Pattern PARTY_LIST_SELF_FAILED =
            Pattern.compile(PARTY_PREFIX_REGEX + "You must be in a party to use this\\.");

    private static final Pattern PARTY_LEAVE_OTHER =
            Pattern.compile(PARTY_PREFIX_REGEX + "(\\w{1,16}) has left the party\\.");
    private static final Pattern PARTY_LEAVE_SELF_ALREADYLEFT =
            Pattern.compile(PARTY_PREFIX_REGEX + "You must be in a party to leave\\.");

    // This is a special case; Wynn sends the same message for when we leave a party or get kicked from a party
    private static final Pattern PARTY_LEAVE_SELF_KICK =
            Pattern.compile(PARTY_PREFIX_REGEX + "You have been removed from the party\\.");

    private static final Pattern PARTY_JOIN_OTHER =
            Pattern.compile(PARTY_PREFIX_REGEX + "(\\w{1,16}) has joined your party, say hello!");
    private static final Pattern PARTY_JOIN_OTHER_SWITCH =
            Pattern.compile(PARTY_PREFIX_REGEX + "Say hello to (\\w{1,16}) which just joined your party!");
    private static final Pattern PARTY_JOIN_SELF =
            Pattern.compile(PARTY_PREFIX_REGEX + "You have successfully joined the party\\.");

    private static final Pattern PARTY_PROMOTE_OTHER =
            Pattern.compile(PARTY_PREFIX_REGEX + "Successfully promoted (.+) to party leader!");
    private static final Pattern PARTY_PROMOTE_SELF = Pattern.compile(
            PARTY_PREFIX_REGEX + "You are now the leader of this party! Type /party for a list of commands\\.");

    private static final Pattern PARTY_DISBAND_ALL =
            Pattern.compile(PARTY_PREFIX_REGEX + "Your party has been disbanded\\.");
    private static final Pattern PARTY_DISBAND_SELF = Pattern.compile(
            PARTY_PREFIX_REGEX + "Your party has been disbanded since you were the only member remaining\\.");

    private static final Pattern PARTY_CREATE_SELF =
            Pattern.compile(PARTY_PREFIX_REGEX + "You have successfully created a party\\.");

    private static final Pattern PARTY_INVITED =
            Pattern.compile(PARTY_PREFIX_REGEX + "\\s+You have been invited to join (\\w{1,16})'s? party!");

    private static final Pattern PARTY_KICK_OTHER =
            Pattern.compile(PARTY_PREFIX_REGEX + "You have kicked the player from the party\\.");
    // endregion

    private static final ScoreboardPart PARTY_SCOREBOARD_PART = new PartyScoreboardPart();

    private boolean expectingPartyMessage = false; // Whether the client is expecting a response from "/party list"
    private long lastPartyRequest = 0; // The last time the client requested party data
    private boolean nextKickHandled = false; // Whether the next "/party kick" sent by the client is being handled

    private boolean inParty; // Whether the player is in a party
    private String partyLeader = null; // The name of the party leader
    private List<String> partyMembers = new ArrayList<>(); // A set of Strings representing all party members
    private Set<String> offlineMembers =
            new HashSet<>(); // A set of Strings representing all offline (disconnected) party members

    public PartyModel() {
        super(List.of());

        resetData();
        Handlers.Scoreboard.addPart(PARTY_SCOREBOARD_PART);
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

        StyledText chatMessage = event.getOriginalStyledText();

        if (tryParsePartyMessages(chatMessage)) return;

        if (expectingPartyMessage) {
            if (tryParseNoPartyMessage(chatMessage) || tryParsePartyList(chatMessage)) {
                event.setCanceled(true);
                expectingPartyMessage = false;
                return;
            }
        }
    }

    private boolean tryParsePartyMessages(StyledText chatMessage) {
        if (chatMessage.matches(PARTY_CREATE_SELF)) {
            WynntilsMod.info("Player created a new party.");

            inParty = true;
            partyLeader = McUtils.playerName();
            partyMembers = new ArrayList<>(List.of(partyLeader));
            WynntilsMod.postEvent(new HadesRelationsUpdateEvent.PartyList(
                    Set.copyOf(partyMembers), HadesRelationsUpdateEvent.ChangeType.RELOAD));
            WynntilsMod.postEvent(new PartyEvent.Listed());
            return true;
        }

        if (chatMessage.matches(PARTY_DISBAND_ALL)
                || chatMessage.matches(PARTY_LEAVE_SELF_KICK)
                || chatMessage.matches(PARTY_LEAVE_SELF_ALREADYLEFT)
                || chatMessage.matches(PARTY_DISBAND_SELF)) {
            WynntilsMod.info("Player left the party.");

            resetData(); // (!) resetData() already posts events for both HadesRelationsUpdateEvent and PartyEvent
            return true;
        }

        if (chatMessage.matches(PARTY_JOIN_SELF)) {
            WynntilsMod.info("Player joined a party.");
            requestData();
            return true;
        }

        Matcher matcher = chatMessage.getMatcher(PARTY_JOIN_OTHER);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player's party has a new member: " + player);

            partyMembers.add(player);
            WynntilsMod.postEvent(
                    new HadesRelationsUpdateEvent.PartyList(Set.of(player), HadesRelationsUpdateEvent.ChangeType.ADD));
            WynntilsMod.postEvent(new PartyEvent.OtherJoined(player));
            return true;
        }

        matcher = chatMessage.getMatcher(PARTY_JOIN_OTHER_SWITCH);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player's party has a new member #2: " + player);

            partyMembers.add(player);
            WynntilsMod.postEvent(
                    new HadesRelationsUpdateEvent.PartyList(Set.of(player), HadesRelationsUpdateEvent.ChangeType.ADD));
            WynntilsMod.postEvent(new PartyEvent.OtherJoined(player));
            return true;
        }

        matcher = chatMessage.getMatcher(PARTY_LEAVE_OTHER);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Other player left player's party: " + player);

            partyMembers.remove(player);
            WynntilsMod.postEvent(new HadesRelationsUpdateEvent.PartyList(
                    Set.of(player), HadesRelationsUpdateEvent.ChangeType.REMOVE));
            WynntilsMod.postEvent(new PartyEvent.OtherLeft(player));
            return true;
        }

        matcher = chatMessage.getMatcher(PARTY_PROMOTE_OTHER);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player's party has a new leader: " + player);

            partyLeader = player;
            return true;
        }

        matcher = chatMessage.getMatcher(PARTY_PROMOTE_SELF);
        if (matcher.matches()) {
            WynntilsMod.info("Player has been promoted to party leader.");

            partyLeader = McUtils.playerName();
            return true;
        }

        matcher = chatMessage.getMatcher(PARTY_INVITED);
        if (matcher.matches()) {
            String inviter = matcher.group(1);
            WynntilsMod.info("Player has been invited to party by " + inviter);

            WynntilsMod.postEvent(new PartyEvent.Invited(inviter));
            return true;
        }

        matcher = chatMessage.getMatcher(PARTY_KICK_OTHER);
        if (matcher.matches()) {
            WynntilsMod.info("Other player was kicked from player's party");

            /*
            (!) This message/matcher is ONLY triggered when the player sends "/party kick". Since that message
            does not tell us who we actually kicked, the #processPartyKick() method handles all the logic for this event.
            That method should be invoked whereever we run "/party kick".
            HOWEVER: if the player decides to run "/party kick" manually from the chat for whatever reason,
            this event will be triggered and the #processPartyKick() method will not be invoked.

            So, the nextKickHandled boolean will tell us if the kick was already handled.
            If it was not handled, we have no choice but to re-request the party data.

            But also, we have a delay here because it takes time for the server to update the leaderboard. (Which is partially
            the reason why we don't like to use the request function too much.)
            */

            if (!nextKickHandled) {
                Managers.TickScheduler.scheduleLater(this::requestData, 2);
            } else {
                nextKickHandled = false;
            }

            return true;
        }

        return false;
    }

    private boolean tryParseNoPartyMessage(StyledText coded) {
        if (coded.matches(PARTY_LIST_SELF_FAILED)) {
            resetData();
            WynntilsMod.info("Player is not in a party.");
            return true;
        }

        return false;
    }

    private boolean tryParsePartyList(StyledText coded) {
        Matcher matcher = coded.getMatcher(PARTY_LIST_ALL);
        if (!matcher.matches()) return false;

        String[] partyList = matcher.group(1).split(", ");
        List<String> newPartyMembers = new ArrayList<>();

        for (String member : partyList) {
            Matcher m = PARTY_LIST_LEADER.matcher(member);
            if (m.matches()) {
                partyLeader = m.group(1);
            }

            newPartyMembers.add(StyledText.fromString(member).getStringWithoutFormatting());
        }

        // Sort the party members by the order they appear in the old party list, to preserve the order
        partyMembers = newPartyMembers.stream()
                .sorted(Comparator.comparing(element -> partyMembers.indexOf(element)))
                .collect(Collectors.toList());

        inParty = true;
        WynntilsMod.postEvent(new HadesRelationsUpdateEvent.PartyList(
                Set.copyOf(partyMembers), HadesRelationsUpdateEvent.ChangeType.RELOAD));
        WynntilsMod.postEvent(new PartyEvent.Listed());
        WynntilsMod.info("Successfully updated party list, user has " + partyList.length + " party members.");

        Collection<String> teamNames = McUtils.mc().level.getScoreboard().getTeamNames();
        offlineMembers = partyMembers.stream()
                .filter(member -> !teamNames.contains(member))
                .collect(Collectors.toSet());
        WynntilsMod.info("Successfully updated offline members, user's party has " + offlineMembers.size()
                + " offline members.");

        return true;
    }

    /**
     * Wynncraft doesn't tell us who we kicked when we do "/party kick", and manually requesting the party list
     * is slow and expensive. So this method exists.
     */
    private void processPartyKick(String playerName) {
        partyMembers.remove(playerName);
        offlineMembers.remove(playerName);

        WynntilsMod.postEvent(new HadesRelationsUpdateEvent.PartyList(
                Set.of(playerName), HadesRelationsUpdateEvent.ChangeType.REMOVE));
        WynntilsMod.postEvent(new PartyEvent.OtherLeft(playerName));
    }

    /**
     * Resets all party data to a state where the player is not in a party.
     * Posts events for both PartyEvent and HadesRelationsUpdateEvent.
     */
    private void resetData() {
        partyMembers = new ArrayList<>();
        partyLeader = null;
        inParty = false;
        offlineMembers = new HashSet<>();

        WynntilsMod.postEvent(new HadesRelationsUpdateEvent.PartyList(
                Set.copyOf(partyMembers), HadesRelationsUpdateEvent.ChangeType.RELOAD));
        WynntilsMod.postEvent(new PartyEvent.Listed());
    }

    /**
     * Sends "/party list" to Wynncraft and waits for the response.
     * (!) Skips if the last request was less than 250ms ago.
     * When the response is received, partyMembers and partyLeader will be updated.
     * After that, the offlineMembers list will be updated from scoreboard data.
     */
    public void requestData() {
        if (McUtils.player() == null) return;

        if (System.currentTimeMillis() - lastPartyRequest < 250) {
            WynntilsMod.info("Skipping party list request because it was requested less than 250ms ago.");
            return;
        }

        expectingPartyMessage = true;
        lastPartyRequest = System.currentTimeMillis();
        Handlers.Command.queueCommand("party list");
    }

    public void increasePlayerPriority(String playerName) {
        int index = partyMembers.indexOf(playerName);

        if (index == -1) return;

        partyMembers.add(Math.max(0, index - 1), partyMembers.remove(index));
        WynntilsMod.postEvent(new PartyEvent.PriorityChanged(playerName, index - 1));
    }

    public void decreasePlayerPriority(String playerName) {
        int index = partyMembers.indexOf(playerName);

        if (index == -1) return;

        partyMembers.add(Math.min(partyMembers.size() - 1, index + 1), partyMembers.remove(index));
        WynntilsMod.postEvent(new PartyEvent.PriorityChanged(playerName, index + 1));
    }

    public boolean isInParty() {
        return inParty;
    }

    public boolean isPartyLeader(String userName) {
        return userName.equals(partyLeader);
    }

    public Optional<String> getPartyLeader() {
        return Optional.ofNullable(partyLeader);
    }

    public List<String> getPartyMembers() {
        return partyMembers;
    }

    public Set<String> getOfflineMembers() {
        return offlineMembers;
    }

    @SubscribeEvent
    public void onSetTeam(SetPlayerTeamEvent e) {
        if (!inParty) return;

        if (e.getMethod() == 0) { // ADD, so player joined the server
            offlineMembers.remove(e.getTeamName());
            WynntilsMod.postEvent(new PartyEvent.OtherReconnected(e.getTeamName()));
        } else if (e.getMethod() == 1) { // REMOVE, so player left the server
            if (partyMembers.contains(e.getTeamName())) {
                offlineMembers.add(e.getTeamName());
                WynntilsMod.postEvent(new PartyEvent.OtherDisconnected(e.getTeamName()));
            }
        }
    }

    // region Party Commands
    /**
     * Kicks a player from the party.
     */
    public void partyKick(String player) {
        nextKickHandled = true;
        Handlers.Command.queueCommand("party kick " + player);
        processPartyKick(player);
    }

    /**
     * Promotes a player to party leader.
     */
    public void partyPromote(String player) {
        Handlers.Command.queueCommand("party promote " + player);
    }

    /**
     * Invites a player to the party. Creates a party if the player is not in one.
     */
    public void partyInvite(String player) {
        if (!inParty) partyCreate();
        Handlers.Command.queueCommand("party invite " + player);
    }

    /**
     * Leaves the party.
     */
    public void partyLeave() {
        Handlers.Command.queueCommand("party leave");
    }

    /**
     * Disbands the party.
     */
    public void partyDisband() {
        Handlers.Command.queueCommand("party disband");
    }

    /**
     * Creates a party.
     */
    public void partyCreate() {
        Handlers.Command.queueCommand("party create");
    }

    /**
     * Join another players party
     */
    public void partyJoin(String playerName) {
        Handlers.Command.queueCommand("party join " + playerName);
    }

    /**
     * Kicks everyone in offlineMembers.
     * Consider running {@link #requestData()} before this.
     */
    public void partyKickOffline() {
        Set<String> offlineMembersCopy = new HashSet<>(offlineMembers);
        offlineMembersCopy.forEach(this::partyKick);
    }
    // endregion
}
