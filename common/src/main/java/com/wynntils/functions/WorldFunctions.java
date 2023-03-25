/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions;

import com.wynntils.core.components.Models;
import com.wynntils.core.functions.Function;
import com.wynntils.core.functions.arguments.FunctionArguments;
import com.wynntils.models.token.type.TokenGatekeeper;
import com.wynntils.models.worlds.profile.ServerProfile;
import com.wynntils.utils.type.CappedValue;
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

    public static class TokenGatekeeperCountFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return Models.Token.getGatekeepers().size();
        }

        @Override
        public List<String> getAliases() {
            return List.of("token_count");
        }
    }

    public static class TokenGatekeeperDepositedFunction extends Function<CappedValue> {
        @Override
        public CappedValue getValue(FunctionArguments arguments) {
            int index = arguments.getArgument("gatekeeperNumber").getIntegerValue() - 1;
            List<TokenGatekeeper> gatekeeperList = Models.Token.getGatekeepers();
            if (index >= gatekeeperList.size()) return CappedValue.EMPTY;

            return gatekeeperList.get(index).getDeposited();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.OptionalArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("gatekeeperNumber", Integer.class, 0)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("token_dep");
        }
    }

    public static class TokenGatekeeperFunction extends Function<CappedValue> {
        @Override
        public CappedValue getValue(FunctionArguments arguments) {
            int index = arguments.getArgument("gatekeeperNumber").getIntegerValue() - 1;
            List<TokenGatekeeper> gatekeeperList = Models.Token.getGatekeepers();
            if (index >= gatekeeperList.size()) return CappedValue.EMPTY;

            return Models.Token.getCollected(gatekeeperList.get(index));
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.OptionalArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("gatekeeperNumber", Integer.class, 0)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("token");
        }
    }

    public static class TokenGatekeeperTypeFunction extends Function<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            int index = arguments.getArgument("gatekeeperNumber").getIntegerValue() - 1;
            List<TokenGatekeeper> gatekeeperList = Models.Token.getGatekeepers();
            if (index >= gatekeeperList.size()) return "";

            return gatekeeperList.get(index).getGatekeeperTokenName();
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.OptionalArgumentBuilder(
                    List.of(new FunctionArguments.Argument<>("gatekeeperNumber", Integer.class, 0)));
        }

        @Override
        public List<String> getAliases() {
            return List.of("token_type");
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

    public static class MobTotemDistanceFunction extends Function<Double> {
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
