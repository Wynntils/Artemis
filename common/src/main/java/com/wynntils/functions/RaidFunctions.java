/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.arguments.FunctionArguments;
import com.wynntils.models.raid.type.RaidKind;
import com.wynntils.models.raid.type.RaidRoomType;
import com.wynntils.utils.type.CappedValue;
import java.util.List;

public class RaidFunctions {
    public static class CurrentRaidFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            RaidKind currentRaid = Models.Raid.getCurrentRaid();

            return currentRaid == null ? "" : currentRaid.getName();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("raid");
        }
    }

    public static class CurrentRaidRoomFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            if (Models.Raid.getCurrentRaid() == null) return "";
            // Room should never be null if the raid is not but just in case the tracking fails
            if (Models.Raid.getCurrentRoom() == null) return "";

            return Models.Raid.getCurrentRoom().name();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("raid_room");
        }
    }

    public static class CurrentRaidTimeFunction extends Function<Long> {
        @Override
        public Long getValue(FunctionArguments arguments) {
            if (Models.Raid.getCurrentRaid() == null) return -1L;

            return Models.Raid.currentRaidTime();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("raid_time");
        }
    }

    public static class CurrentRaidRoomTimeFunction extends Function<Long> {
        @Override
        public Long getValue(FunctionArguments arguments) {
            if (Models.Raid.getCurrentRaid() == null) return -1L;
            // Room should never be null if the raid is not but just in case the tracking fails
            if (Models.Raid.getCurrentRoom() == null) return -1L;

            return Models.Raid.currentRoomTime();
        }
    }

    public static class RaidChallengesFunction extends Function<CappedValue> {
        @Override
        public CappedValue getValue(FunctionArguments arguments) {
            return Models.Raid.getChallenges();
        }
    }

    public static class RaidIntermissionTimeFunction extends Function<Long> {
        @Override
        public Long getValue(FunctionArguments arguments) {
            if (Models.Raid.getCurrentRaid() == null) return -1L;

            return Models.Raid.getIntermissionTime();
        }
    }

    public static class RaidRoomTimeFunction extends Function<Long> {
        @Override
        public Long getValue(FunctionArguments arguments) {
            if (Models.Raid.getCurrentRaid() == null) return -1L;

            RaidRoomType roomType =
                    RaidRoomType.fromName(arguments.getArgument("roomName").getStringValue());

            if (roomType == null) return -1L;

            return Models.Raid.getRoomTime(roomType);
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("roomName", String.class, null)));
        }
    }

    public static class RaidPersonalBestTimeFunction extends Function<Long> {
        @Override
        public Long getValue(FunctionArguments arguments) {
            RaidKind raidKind =
                    RaidKind.fromName(arguments.getArgument("raidName").getStringValue());

            if (raidKind == null) return -1L;

            return Models.Raid.getRaidBestTime(raidKind);
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("raidName", String.class, null)));
        }

        @Override
        protected List<String> getAliases() {
            return List.of("raid_pb");
        }
    }

    public static class RaidTimeRemainingFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.Raid.getTimeLeft();
        }
    }
}
