/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.arguments.FunctionArguments;
import com.wynntils.models.beacons.type.LootrunBeaconKind;
import com.wynntils.models.containers.type.MythicFind;
import com.wynntils.models.lootrun.type.TaskLocation;
import com.wynntils.utils.EnumUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.CappedValue;
import java.util.Comparator;
import java.util.List;

public class LootrunFunctions {
    public static class DryStreakFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.LootChest.getDryCount();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("dry_s");
        }
    }

    public static class DryBoxesFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.LootChest.getDryBoxes();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("dry_b", "dry_boxes_count");
        }
    }

    public static class DryPullsFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.Lootrun.dryPulls.get();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("dry_p", "dry_pulls_count");
        }
    }

    public static class HighestDryStreakFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.LootChest.getMythicFinds().stream()
                    .max(Comparator.comparing(MythicFind::dryCount))
                    .map(MythicFind::dryCount)
                    .orElse(0);
        }
    }

    public static class LastDryStreakFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            List<MythicFind> mythicFinds = Models.LootChest.getMythicFinds();

            if (mythicFinds.isEmpty()) return 0;

            return mythicFinds.getLast().dryCount();
        }
    }

    public static class LastMythicFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            List<MythicFind> mythicFinds = Models.LootChest.getMythicFinds();

            if (mythicFinds.isEmpty()) return "";

            return mythicFinds.getLast().itemName();
        }
    }

    public static class ChestOpenedFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.LootChest.getOpenedChestCount();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("chest_count");
        }
    }

    public static class LootrunStateFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return Models.Lootrun.getState().toString();
        }
    }

    public static class LootrunBeaconCountFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            String color = arguments.getArgument("color").getStringValue();

            LootrunBeaconKind lootrunBeaconKind = LootrunBeaconKind.fromName(color);
            if (lootrunBeaconKind == null) return -1;

            return Models.Lootrun.getBeaconCount(lootrunBeaconKind);
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("color", String.class, null)));
        }
    }

    public static class LootrunMissionFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            int missionIndex = arguments.getArgument("index").getIntegerValue();
            boolean colored = arguments.getArgument("colored").getBooleanValue();

            return Models.Lootrun.getMissionStatus(missionIndex, colored);
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new FunctionArguments.Argument<>("index", Integer.class, null),
                    new FunctionArguments.Argument<>("colored", Boolean.class, null)));
        }
    }

    public static class LootrunTaskNameFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            String color = arguments.getArgument("color").getStringValue();

            LootrunBeaconKind lootrunBeaconKind = LootrunBeaconKind.fromName(color);
            if (lootrunBeaconKind == null) return "";

            TaskLocation taskLocation = Models.Lootrun.getTaskForColor(lootrunBeaconKind);
            if (taskLocation == null) return "";

            return taskLocation.name();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("color", String.class, null)));
        }
    }

    public static class LootrunTaskLocationFunction extends Function<Location> {
        @Override
        public Location getValue(FunctionArguments arguments) {
            String color = arguments.getArgument("color").getStringValue();

            LootrunBeaconKind lootrunBeaconKind = LootrunBeaconKind.fromName(color);
            if (lootrunBeaconKind == null) return new Location(0, 0, 0);

            TaskLocation taskLocation = Models.Lootrun.getTaskForColor(lootrunBeaconKind);
            if (taskLocation == null) return new Location(0, 0, 0);

            return taskLocation.location();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("color", String.class, null)));
        }
    }

    public static class LootrunTaskTypeFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            String color = arguments.getArgument("color").getStringValue();

            LootrunBeaconKind lootrunBeaconKind = LootrunBeaconKind.fromName(color);
            if (lootrunBeaconKind == null) return "";

            TaskLocation taskLocation = Models.Lootrun.getTaskForColor(lootrunBeaconKind);
            if (taskLocation == null) return "";

            return EnumUtils.toNiceString(taskLocation.taskType());
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("color", String.class, null)));
        }
    }

    public static class LootrunTimeFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.Lootrun.getCurrentTime();
        }
    }

    public static class LootrunChallengesFunction extends Function<CappedValue> {
        @Override
        public CappedValue getValue(FunctionArguments arguments) {
            return Models.Lootrun.getChallenges();
        }
    }

    public static class LootrunLastSelectedBeaconColorFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            LootrunBeaconKind lootrunBeaconKind = Models.Lootrun.getLastTaskBeaconColor();
            if (lootrunBeaconKind == null) return "";

            return EnumUtils.toNiceString(lootrunBeaconKind);
        }
    }

    public static class LootrunRedBeaconChallengeCountFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.Lootrun.getRedBeaconTaskCount();
        }
    }
}
