/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wynntils.core.components.CoreComponent;
import com.wynntils.core.components.Handler;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Service;
import com.wynntils.core.components.Services;
import com.wynntils.core.events.EventBusWrapper;
import com.wynntils.core.mod.event.WynntilsCrashEvent;
import com.wynntils.core.mod.type.CrashType;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The common implementation of Wynntils
 */
public final class WynntilsMod {
    public static final String MOD_ID = "wynntils";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final File MOD_STORAGE_ROOT = new File(McUtils.mc().gameDirectory, MOD_ID);

    private static ModLoader modLoader;
    private static String version = "";
    private static boolean developmentBuild = false;
    private static boolean developmentEnvironment;
    private static IEventBus eventBus;
    private static File modJar;
    private static boolean initCompleted = false;
    private static final Map<Class<? extends CoreComponent>, List<CoreComponent>> componentMap = new HashMap<>();

    public static ModLoader getModLoader() {
        return modLoader;
    }

    public static void unregisterEventListener(Object object) {
        eventBus.unregister(object);
    }

    public static void registerEventListener(Object object) {
        eventBus.register(object);
    }

    public static <T extends Event> void registerListener(Consumer<T> eventConsumer) {
        eventBus.addListener(eventConsumer);
    }

    public static <T extends Event> boolean postEvent(T event) {
        try {
            eventBus.post(event);
            return event instanceof ICancellableEvent cancellableEvent && cancellableEvent.isCanceled();
        } catch (Throwable t) {
            handleExceptionInEventListener(t, event);
            return false;
        }
    }

    public static void postEventOnMainThread(Event event) {
        Managers.TickScheduler.scheduleNextTick(() -> postEvent(event));
    }

    public static void reloadAllComponentData() {
        componentMap.get(Manager.class).forEach(c -> ((Manager) c).reloadData());
        componentMap.get(Model.class).forEach(c -> ((Model) c).reloadData());
        componentMap.get(Service.class).forEach(c -> ((Service) c).reloadData());
    }

    private static void handleExceptionInEventListener(Throwable t, Event event) {
        StackTraceElement[] stackTrace = t.getStackTrace();

        String crashingFeatureName = Arrays.stream(stackTrace)
                .filter(line -> line.getClassName().startsWith("com.wynntils.features."))
                .findFirst()
                .map(StackTraceElement::getClassName)
                .orElse(null);

        if (crashingFeatureName == null) {
            WynntilsMod.error("Exception in event listener not belonging to a feature", t);
            return;
        }

        if (Managers.Feature == null) {
            WynntilsMod.warn("Cannot lookup feature name: " + crashingFeatureName, t);
            return;
        }

        Managers.Feature.handleExceptionInEventListener(event, crashingFeatureName, t);
    }

    public static File getModJar() {
        return modJar;
    }

    public static String getVersion() {
        return version;
    }

    public static boolean isBeta() {
        return version.contains("beta");
    }

    public static boolean isDevelopmentBuild() {
        return developmentBuild;
    }

    public static boolean isDevelopmentEnvironment() {
        return developmentEnvironment;
    }

    public static File getModStorageDir(String dirName) {
        return new File(MOD_STORAGE_ROOT, dirName);
    }

    public static InputStream getModResourceAsStream(String resourceName) {
        return WynntilsMod.class.getClassLoader().getResourceAsStream("assets/" + MOD_ID + "/" + resourceName);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void error(String msg) {
        LOGGER.error(msg);
    }

    public static void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }

    public static void warn(String msg) {
        LOGGER.warn(msg);
    }

    public static void warn(String msg, Throwable t) {
        LOGGER.warn(msg, t);
    }

    public static void info(String msg) {
        LOGGER.info(msg);
    }

    // Ran when resources (including I18n) are available
    public static void onResourcesFinishedLoading() {
        if (initCompleted) return;
        initCompleted = true;

        try {
            initFeatures();
        } catch (Throwable t) {
            LOGGER.error("Failed to initialize Wynntils features", t);
        }
    }

    public static void init(ModLoader loader, String modVersion, boolean isDevelopmentEnvironment, File modFile) {
        modJar = modFile;

        // Note that at this point, no resources (including I18n) are available, so we postpone features until then

        // Setup mod core properties
        modLoader = loader;
        developmentEnvironment = isDevelopmentEnvironment;

        parseVersion(modVersion);

        LOGGER.info(
                "Wynntils: Starting version {} (using {} on Minecraft {})",
                version,
                modLoader,
                SharedConstants.getCurrentVersion().getName());

        WynntilsMod.eventBus = EventBusWrapper.createEventBus();

        registerComponents(Managers.class, Manager.class);
        registerComponents(Handlers.class, Handler.class);
        registerComponents(Models.class, Model.class);
        registerComponents(Services.class, Service.class);

        // Init storage for loaded components immediately
        Managers.Storage.initComponents();

        addCrashCallbacks();
    }

    private static void registerComponents(Class<?> registryClass, Class<? extends CoreComponent> componentClass) {
        // Register all handler singletons as event listeners
        List<CoreComponent> components = componentMap.computeIfAbsent(componentClass, k -> new ArrayList<>());

        FieldUtils.getAllFieldsList(registryClass).stream()
                .filter(field -> componentClass.isAssignableFrom(field.getType()))
                .forEach(field -> {
                    try {
                        CoreComponent component = (CoreComponent) field.get(null);
                        WynntilsMod.registerEventListener(component);
                        Managers.Storage.registerStorageable(component);
                        components.add(component);
                    } catch (IllegalAccessException e) {
                        WynntilsMod.error("Internal error in " + registryClass.getSimpleName(), e);
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void parseVersion(String modVersion) {
        if (modVersion.contains("SNAPSHOT")) {
            developmentBuild = true;
        } else {
            developmentBuild = false;
        }
        version = "v" + modVersion;
    }

    private static void initFeatures() {
        // Init all features and functions. Now resources (i.e I18n) are available.
        Managers.Feature.init();
        Managers.Function.init();

        // Init config and data from files
        Managers.Config.init();
        Managers.Storage.initFeatures();

        // Init services that depends on I18n
        Services.Statistics.init();

        LOGGER.info(
                "Wynntils: {} features and {} functions are now loaded and ready",
                Managers.Feature.getFeatures().size(),
                Managers.Function.getFunctions().size());
    }

    private static void addCrashCallbacks() {
        Managers.CrashReport.registerCrashContext("In Development", () -> isDevelopmentEnvironment() ? "Yes" : "No");
    }

    public static void reportCrash(
            CrashType type, String niceName, String fullName, String reason, Throwable throwable) {
        reportCrash(type, niceName, fullName, reason, true, true, throwable);
    }

    public static void reportCrash(
            CrashType type,
            String niceName,
            String fullName,
            String reason,
            boolean shouldSendChat,
            boolean isDisabled,
            Throwable throwable) {
        WynntilsMod.warn(
                "Disabling " + type.toString().toLowerCase(Locale.ROOT) + " " + niceName + " due to " + reason);
        WynntilsMod.error("Exception thrown by " + fullName, throwable);

        if (shouldSendChat) {
            McUtils.sendErrorToClient("Wynntils error: " + type.getName() + " '" + niceName + "' has crashed in "
                    + reason + (isDisabled ? " and has been disabled" : ""));
        }

        postEvent(new WynntilsCrashEvent(fullName, type, throwable));
    }

    public enum ModLoader {
        FORGE,
        FABRIC
    }
}
