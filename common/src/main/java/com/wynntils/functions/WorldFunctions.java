/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions;

import com.wynntils.core.components.Models;
import com.wynntils.core.functions.Function;
import com.wynntils.core.functions.arguments.FunctionArguments;
import com.wynntils.models.worlds.profile.ServerProfile;
import java.util.List;

public class WorldFunctions {
    public static class CurrentWorldFunction extends Function<String> {
        private static final String NO_DATA = "<unknown>";
        private static final String NO_WORLD = "<not on world>";

        @Override
        public String getValue(FunctionArguments arguments) {
            if (!Models.WorldState.onWorld()) {
                return NO_WORLD;
            }

            String currentWorldName = Models.WorldState.getCurrentWorldName();
            return currentWorldName.isEmpty() ? NO_DATA : currentWorldName;
        }

        @Override
        public List<String> getAliases() {
            return List.of("world");
        }
    }

    public static class CurrentWorldUptimeFunction extends Function<String> {
        private static final String NO_DATA = "<unknown>";
        private static final String NO_WORLD = "<not on world>";

        @Override
        public String getValue(FunctionArguments arguments) {
            if (!Models.WorldState.onWorld()) {
                return NO_WORLD;
            }

            String currentWorldName = Models.WorldState.getCurrentWorldName();

            ServerProfile server = Models.ServerList.getServer(currentWorldName);

            if (server == null) {
                return NO_DATA;
            }

            return server.getUptime();
        }

        @Override
        public List<String> getAliases() {
            return List.of("world_uptime", "uptime");
        }
    }

    public static class MobTotemCountFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.MobTotem.getMobTotems().size();
        }
    }

    public static class MobTotemOwnerFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return Models.MobTotem.getMobTotem(
                            arguments.getArgument("totemNumber").getIntegerValue() - 1)
                    .getOwner();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("totemNumber", Integer.class, null)));
        }
    }

    public static class MobTotemDistanceToPlayerFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return Models.MobTotem.getMobTotem(
                            arguments.getArgument("totemNumber").getIntegerValue() - 1)
                    .getDistanceToPlayer();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("totemNumber", Integer.class, null)));
        }
    }

    public static class MobTotemTimeLeftFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            return Models.MobTotem.getMobTotem(
                            arguments.getArgument("totemNumber").getIntegerValue() - 1)
                    .getTimerString();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("totemNumber", Integer.class, null)));
        }
    }
}
