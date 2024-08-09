/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.discord;

import com.wynntils.antiope.core.DiscordGameSDKCore;
import com.wynntils.antiope.core.type.CreateParams;
import com.wynntils.antiope.core.type.GameSDKException;
import com.wynntils.antiope.core.type.Result;
import com.wynntils.antiope.manager.activity.type.Activity;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Service;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.utils.mc.McUtils;
import java.time.Instant;
import java.util.List;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.bus.api.SubscribeEvent;

public class DiscordService extends Service {
    private static final long DISCORD_APPLICATION_ID = 387266678607577088L;
    private static final int TICKS_PER_UPDATE = 5;

    private CreateParams params;
    private DiscordGameSDKCore core;
    private Activity activity;

    private int ticksUntilUpdate = 0;

    public DiscordService() {
        super(List.of());
    }

    public boolean load() {
        try {
            DiscordGameSDKCore.loadLibrary();
        } catch (UnsatisfiedLinkError e) {
            McUtils.sendErrorToClient(I18n.get("service.wynntils.discord.failedToLoadSDK"));
            WynntilsMod.error("Failed to load Discord Rich Presence library", e);
            return false;
        }
        if (!isReady()) {
            createCore();
        }
        return true;
    }

    public boolean isReady() {
        return core != null && core.isOpen() && activity != null;
    }

    public void unload() {
        if (!isReady()) return;
        try {
            activity.close();
            activity = null;
            core.close();
            core = null;
            params.close();
            params = null;
        } catch (GameSDKException e) {
            if (e.getResult() == Result.TRANSACTION_ABORTED) {
                // This occurs when player closes game and JVM exits before we can close the core
                return;
            }
            WynntilsMod.error("Could not unload Discord Game SDK", e);
        }
    }

    private void createCore() {
        params = new CreateParams();
        try {
            params.setClientID(DISCORD_APPLICATION_ID);
            params.setFlags(CreateParams.getNoRequireDiscordFlags());
            core = new DiscordGameSDKCore(params);
            activity = new Activity();
            activity.timestamps().setStart(Instant.now());
            activity.assets().setSmallImage("wynntils");
            activity.assets()
                    .setSmallText("Wynntils " + WynntilsMod.getVersion() + " ("
                            + McUtils.mc().getLaunchedVersion() + ")");
        } catch (Throwable e) {
            if (e instanceof GameSDKException gameSDKException
                    && gameSDKException.getResult() == Result.INTERNAL_ERROR) {
                // This occurs when player closes game and JVM exits before we can close the core
                return;
            }
            WynntilsMod.error("Could not initialize Discord Game SDK", e);
        }
    }

    public void setDetails(String details) {
        if (!isReady()) return;
        activity.setDetails(details);
        core.activityManager().updateActivity(activity);
    }

    public void setImage(String imageId) {
        if (!isReady()) return;
        activity.assets().setLargeImage(imageId);
        core.activityManager().updateActivity(activity);
    }

    public void setImageText(String text) {
        if (!isReady()) return;
        activity.assets().setLargeText(text);
        core.activityManager().updateActivity(activity);
    }

    public void setWynncraftLogo() {
        if (!isReady()) return;
        activity.assets().setLargeImage("wynn");
        activity.assets().setLargeText("play.wynncraft.com");
        core.activityManager().updateActivity(activity);
    }

    public void setState(String state) {
        if (!isReady()) return;
        activity.setState(state);
        core.activityManager().updateActivity(activity);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (ticksUntilUpdate > 0) {
            ticksUntilUpdate--;
            return;
        }
        ticksUntilUpdate = TICKS_PER_UPDATE;
        if (!isReady()) return;

        core.runCallbacks();
    }
}
