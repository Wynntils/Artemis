/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.raid;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Model;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.TitleSetTextEvent;
import com.wynntils.models.raid.event.RaidChallengeEvent;
import com.wynntils.models.raid.event.RaidEndedEvent;
import com.wynntils.models.raid.event.RaidNewBestTimeEvent;
import com.wynntils.models.raid.scoreboard.RaidScoreboardPart;
import com.wynntils.models.raid.type.RaidKind;
import com.wynntils.models.raid.type.RaidRoomType;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.CappedValue;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;

public class RaidModel extends Model {
    public static final int MAX_CHALLENGES = 3;
    public static final int ROOM_TIMERS_COUNT = 5;
    private static final Pattern RAID_COMPLETED_PATTERN = Pattern.compile("§f§lR§#4d4d4dff§laid Completed!");

    private static final RaidScoreboardPart RAID_SCOREBOARD_PART = new RaidScoreboardPart();

    @Persisted
    private final Storage<Map<String, Long>> bestTimes = new Storage<>(new TreeMap<>());

    private final Map<RaidRoomType, Long> roomTimers = new EnumMap<>(RaidRoomType.class);

    private boolean completedCurrentChallenge = false;
    private CappedValue challenges = CappedValue.EMPTY;
    private int timeLeft = 0;
    private long raidStartTime;
    private long roomStartTime;
    private RaidKind currentRaid = null;
    private RaidRoomType currentRoom = null;

    public RaidModel() {
        super(List.of());

        Handlers.Scoreboard.addPart(RAID_SCOREBOARD_PART);
    }

    @SubscribeEvent
    public void onTitle(TitleSetTextEvent event) {
        Component component = event.getComponent();
        StyledText styledText = StyledText.fromComponent(component);

        if (currentRaid == null) {
            currentRaid = RaidKind.fromTitle(styledText);

            if (currentRaid != null) {
                // In a raid, set to intro room and start timer
                currentRoom = RaidRoomType.INTRO;
                raidStartTime = System.currentTimeMillis();
                completedCurrentChallenge = false;
            }
        } else {
            if (styledText.matches(RAID_COMPLETED_PATTERN)) {
                completeRaid();
            }
        }
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        // Only want to send the message once the user has returned to an actual world
        if (currentRaid != null && event.getNewState() == WorldState.WORLD) {
            currentRaid = null;
            currentRoom = null;
            completedCurrentChallenge = false;
            timeLeft = 0;
            challenges = CappedValue.EMPTY;
            roomTimers.clear();

            McUtils.sendMessageToClient(Component.literal(
                            "Raid tracking has been interrupted, you will not be able to see progress for the current raid")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    // This is called when the "Go to the exit" scoreboard line is displayed.
    // If we are in the final buff room when this is shown, then we are now in the boss intermission.
    // Otherwise, if we are in the intro or another buff room then we are now in an instructions room.
    public void tryEnterChallengeIntermission() {
        if (currentRoom == RaidRoomType.BUFF_3) {
            currentRoom = RaidRoomType.BOSS_INTERMISSION;
        } else if (inBuffRoom() || currentRoom == RaidRoomType.INTRO) {
            currentRoom = RaidRoomType.values()[currentRoom.ordinal() + 1];
        }
    }

    // Since all challenges use a different instructions message the easiest way to check for a challenge
    // beginning is from the scoreboard not showing any of the static messages and being in an instructions room.
    // At the time of making this there is no consistent way to check for entering a boss fight either, so if we
    // are in the boss intermission and this is called, then we have entered the boss fight.
    // So this method will be called when no other patterns matched the first line of the raid scoreboard segment.
    public void tryStartChallenge() {
        if (inInstructionsRoom()) {
            currentRoom = RaidRoomType.values()[currentRoom.ordinal() + 1];
            WynntilsMod.postEvent(new RaidChallengeEvent.Started(currentRaid, currentRoom));
            roomStartTime = System.currentTimeMillis();
        }
    }

    // This will only end the timer for the current room, but we are still in the room so currentRoom isn't updated.
    // It will be called multiple times after completing a challenge so we use completedCurrentChallenge to only
    // post the event and save timers once.
    public void completeChallenge() {
        if (!completedCurrentChallenge) {
            long roomTime = System.currentTimeMillis() - roomStartTime;
            roomTimers.put(currentRoom, roomTime);
            roomStartTime = System.currentTimeMillis();

            WynntilsMod.postEvent(new RaidChallengeEvent.Completed(currentRaid, currentRoom));

            completedCurrentChallenge = true;
        }
    }

    // Only check for entry to a buff room once after the challenge has been completed.
    public void enterBuffRoom() {
        if (completedCurrentChallenge) {
            currentRoom = RaidRoomType.values()[currentRoom.ordinal() + 1];

            completedCurrentChallenge = false;
        }
    }

    public void startBossFight() {
        if (currentRoom == RaidRoomType.BOSS_INTERMISSION) {
            currentRoom = RaidRoomType.BOSS_FIGHT;
            WynntilsMod.postEvent(new RaidChallengeEvent.Started(currentRaid, currentRoom));
            roomStartTime = System.currentTimeMillis();
        }
    }

    public void completeRaid() {
        if (currentRaid == null) return;

        // Add the boss time to room timers
        long bossTime = System.currentTimeMillis() - roomStartTime;
        roomTimers.put(RaidRoomType.BOSS_FIGHT, bossTime);

        WynntilsMod.postEvent(new RaidEndedEvent.Completed(currentRaid, getAllRoomTimes(), currentRaidTime()));

        checkForNewPersonalBest(currentRaid, currentRaidTime());

        currentRaid = null;
        currentRoom = null;
        completedCurrentChallenge = false;
        timeLeft = 0;
        challenges = CappedValue.EMPTY;
        roomTimers.clear();
    }

    public void failedRaid() {
        if (currentRaid == null) return;

        WynntilsMod.postEvent(new RaidEndedEvent.Failed(currentRaid, getAllRoomTimes(), currentRaidTime()));

        currentRaid = null;
        currentRoom = null;
        completedCurrentChallenge = false;
        timeLeft = 0;
        challenges = CappedValue.EMPTY;
        roomTimers.clear();
    }

    public void setTimeLeft(int seconds) {
        timeLeft = seconds;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setChallenges(CappedValue challenges) {
        this.challenges = challenges;
    }

    public CappedValue getChallenges() {
        return challenges;
    }

    public long getRaidBestTime(RaidKind raidKind) {
        return bestTimes.get().getOrDefault(raidKind.getName(), -1L);
    }

    public RaidKind getCurrentRaid() {
        return currentRaid;
    }

    public RaidRoomType getCurrentRoom() {
        return currentRoom;
    }

    public long getRoomTime(RaidRoomType roomType) {
        if (roomType == currentRoom && !completedCurrentChallenge) {
            return currentRoomTime();
        }

        return roomTimers.getOrDefault(roomType, -1L);
    }

    public long currentRaidTime() {
        return System.currentTimeMillis() - raidStartTime;
    }

    public long currentRoomTime() {
        return System.currentTimeMillis() - roomStartTime;
    }

    public long getIntermissionTime() {
        long currentTime = System.currentTimeMillis();
        long timeSpentInChallenges = 0;

        for (long time : roomTimers.values()) {
            timeSpentInChallenges += time;
        }

        long intermissionTime = currentTime - raidStartTime - timeSpentInChallenges;

        if ((inChallengeRoom() && !completedCurrentChallenge) || currentRoom == RaidRoomType.BOSS_FIGHT) {
            intermissionTime -= (currentTime - roomStartTime);
        }

        return intermissionTime;
    }

    private boolean inInstructionsRoom() {
        return currentRoom == RaidRoomType.INSTRUCTIONS_1
                || currentRoom == RaidRoomType.INSTRUCTIONS_2
                || currentRoom == RaidRoomType.INSTRUCTIONS_3;
    }

    private boolean inChallengeRoom() {
        return currentRoom == RaidRoomType.CHALLENGE_1
                || currentRoom == RaidRoomType.CHALLENGE_2
                || currentRoom == RaidRoomType.CHALLENGE_3;
    }

    private boolean inBuffRoom() {
        return currentRoom == RaidRoomType.BUFF_1
                || currentRoom == RaidRoomType.BUFF_2
                || currentRoom == RaidRoomType.BUFF_3;
    }

    private boolean inBossFight() {
        return currentRoom == RaidRoomType.BOSS_FIGHT;
    }

    private List<Long> getAllRoomTimes() {
        List<Long> allRoomTimes = new ArrayList<>();

        // Order is challenge 1, 2, 3, boss, intermission
        allRoomTimes.add(getRoomTime(RaidRoomType.CHALLENGE_1));
        allRoomTimes.add(getRoomTime(RaidRoomType.CHALLENGE_2));
        allRoomTimes.add(getRoomTime(RaidRoomType.CHALLENGE_3));
        allRoomTimes.add(getRoomTime(RaidRoomType.BOSS_FIGHT));
        // Need to add boss time to get correct intermission time since
        // currentRoom will still be boss room when getIntermissionTime() is called
        allRoomTimes.add(getIntermissionTime() + getRoomTime(RaidRoomType.BOSS_FIGHT));

        return allRoomTimes;
    }

    private void checkForNewPersonalBest(RaidKind raidKind, long time) {
        if (bestTimes.get().get(raidKind.getName()) == null) {
            bestTimes.get().put(raidKind.getName(), time);
            bestTimes.touched();
        } else {
            long currentBestTime = bestTimes.get().get(raidKind.getName());

            // New time is faster
            if (currentBestTime > time) {
                bestTimes.get().put(raidKind.getName(), time);
                bestTimes.touched();

                WynntilsMod.postEvent(new RaidNewBestTimeEvent(raidKind, time));
            }
        }
    }
}
