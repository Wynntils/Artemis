/*
 * Copyright © Wynntils 2021-2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core;

import com.wynntils.core.commands.ClientCommandManager;
import com.wynntils.core.config.ConfigManager;
import com.wynntils.core.features.FeatureRegistry;
import com.wynntils.core.features.overlays.OverlayManager;
import com.wynntils.core.keybinds.KeyManager;
import com.wynntils.core.webapi.WebManager;
import com.wynntils.mc.utils.CompassManager;
import com.wynntils.mc.utils.CrashReportManager;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wc.ModelLoader;
import com.wynntils.wc.utils.ActionBarManager;
import com.wynntils.wc.utils.SpellManager;
import com.wynntils.wc.utils.scoreboard.ScoreboardManager;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The common implementation of Wynntils */
public class WynntilsMod {
    public static final String MOD_ID = "wynntils";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final File MOD_STORAGE_ROOT = new File(McUtils.mc().gameDirectory, MOD_ID);
    private static final IEventBus EVENT_BUS = BusBuilder.builder().build();

    private static String version = "";
    private static int buildNumber = -1;
    private static boolean developmentEnvironment;

    public static IEventBus getEventBus() {
        return EVENT_BUS;
    }

    public static String getVersion() {
        return version;
    }

    public static int getBuildNumber() {
        return buildNumber;
    }

    public static boolean isDevelopmentEnvironment() {
        return developmentEnvironment;
    }

    public static File getModStorageDir(String dirName) {
        return new File(MOD_STORAGE_ROOT, dirName);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void error(String msg) {
        LOGGER.error(msg);
    }

    public static void warn(String msg) {
        LOGGER.warn(msg);
    }

    public static void info(String msg) {
        LOGGER.info(msg);
    }

    public static void init(String modVersion, boolean isDevelopmentEnvironment) {
        // Setup mod core properties
        developmentEnvironment = isDevelopmentEnvironment;
        parseVersion(modVersion);
        addCrashCallbacks();
        // MC will sometimes think it's running headless and refuse to set clipboard contents
        // making sure this is set to false will fix that
        System.setProperty("java.awt.headless", "false");

        // Init all managers
        WebManager.init();
        ConfigManager.init();
        ClientCommandManager.init();
        KeyManager.init();
        CompassManager.init();
        ScoreboardManager.init();
        ModelLoader.init();
        OverlayManager.init();
        ActionBarManager.init();
        SpellManager.init();
        FeatureRegistry.init();
    }

    private static void parseVersion(String versionString) {
        if (isDevelopmentEnvironment()) {
            LOGGER.info("Wynntils running on version " + versionString);
        }

        Matcher result = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)\\+(DEV|\\d+).+").matcher(versionString);

        if (!result.find()) {
            LOGGER.warn("Unable to parse mod version");
        }

        version = result.group(1);

        try {
            buildNumber = Integer.parseInt(result.group(2));
        } catch (NumberFormatException ignored) {
        }
    }

    private static void addCrashCallbacks() {
        CrashReportManager.registerCrashContext(new CrashReportManager.ICrashContext() {
            @Override
            public String name() {
                return "In Development";
            }

            @Override
            public Object generate() {
                return isDevelopmentEnvironment() ? "Yes" : "No";
            }
        });
    }
}
