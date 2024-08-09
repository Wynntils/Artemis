/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.wynntils;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.mod.event.WynntilsCrashEvent;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.JsonUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.ConfirmedBoolean;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;

@ConfigCategory(Category.WYNNTILS)
public class TelemetryFeature extends Feature {
    @Persisted
    public final Config<ConfirmedBoolean> crashReports = new Config<>(ConfirmedBoolean.UNCONFIRMED);

    @SubscribeEvent
    public void onCrash(WynntilsCrashEvent event) {
        if (crashReports.get() != ConfirmedBoolean.TRUE) return;
        // Only send telemetry for released versions
        if (WynntilsMod.isDevelopmentEnvironment()) return;
        if (WynntilsMod.getVersion().contains("SNAPSHOT")) return;

        String title = "Crashed " + event.getType().toString().toLowerCase(Locale.ROOT) + ": " + event.getName() + "\n";
        String trace = ExceptionUtils.getStackTrace(event.getThrowable());

        ApiResponse apiResponse =
                Managers.Net.callApi(UrlId.API_ATHENA_TELEMETRY_CRASH, Map.of("trace", title + trace));

        apiResponse.handleJsonObject(json -> {
            String response = JsonUtils.getNullableJsonString(json, "message");
            String hash = JsonUtils.getNullableJsonString(json, "hash");
            if (response != null && response.equals("Crash report logged successfully.")) {
                WynntilsMod.info("Crash reported to Athena as " + hash);
            } else {
                WynntilsMod.warn("Failed to report crash reported to Athena: " + response);
            }
        });
    }

    @SubscribeEvent
    public void onWorldChange(WorldStateEvent event) {
        if (event.getNewState() != WorldState.WORLD) return;
        if (crashReports.get() != ConfirmedBoolean.UNCONFIRMED) return;

        MutableComponent component = Component.literal("Wynntils Telemetry\n").withStyle(ChatFormatting.AQUA);
        component.append(Component.literal(
                        """
                        Wynntils can send telemetry data when a component fails.
                        This data does not contain any personal information,
                        but is helpful for developers for fixing bugs in Wynntils.
                        """)
                .withStyle(ChatFormatting.GRAY));

        component.append(Component.literal("Click here")
                .withStyle(ChatFormatting.GREEN)
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/wynntils config set Telemetry crashReports true"))));
        component.append(
                Component.literal(" to accept crash report telemetry\n").withStyle(ChatFormatting.GREEN));

        component.append(Component.literal("Click here")
                .withStyle(ChatFormatting.RED)
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/wynntils config set Telemetry crashReports false"))));
        component.append(
                Component.literal(" to opt out of crash report telemetry\n").withStyle(ChatFormatting.RED));

        McUtils.sendMessageToClient(component);
    }
}
