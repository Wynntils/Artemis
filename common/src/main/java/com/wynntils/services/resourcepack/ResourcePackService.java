/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.resourcepack;

import com.google.common.hash.Hashing;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Service;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.mc.event.ResourcePackClearEvent;
import com.wynntils.mc.event.ResourcePackEvent;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ResourcePackService extends Service {
    @Persisted
    private final Storage<String> requestedPreloadHash = new Storage<>("");

    public ResourcePackService() {
        super(List.of());
    }

    @SuppressWarnings("deprecation")
    public String calculateHash(String url) {
        return Hashing.sha1().hashString(url, StandardCharsets.UTF_8).toString();
    }

    public String getRequestedPreloadHash() {
        return requestedPreloadHash.get();
    }

    public void setRequestedPreloadHash(String hash) {
        requestedPreloadHash.store(hash);
    }

    @Override
    public void onStorageLoad(Storage<?> storage) {
        preloadResourcePack();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onResourcePackLoad(ResourcePackEvent event) {
        Optional<String> currentHash = getCurrentPreloadedHash();
        if (currentHash.isEmpty()) return;

        // If it's already loaded, skip re-loading it
        String fileHashName = calculateHash(event.getUrl());
        if (currentHash.get().equals(fileHashName)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onResourcePackClear(ResourcePackClearEvent event) {
        Optional<String> currentHash = getCurrentPreloadedHash();
        if (currentHash.isEmpty()) return;

        // If we have what we want, keep it
        if (currentHash.get().equals(requestedPreloadHash.get())) {
            event.setCanceled(true);
        }
    }

    private Optional<String> getCurrentPreloadedHash() {
        Pack pack = McUtils.mc().getDownloadedPackSource().serverPack;
        if (!(pack instanceof PreloadedPack preloadedPack)) return Optional.empty();

        String currentHash = preloadedPack.getHash();
        return Optional.ofNullable(currentHash);
    }

    private void preloadResourcePack() {
        if (requestedPreloadHash.get().isEmpty()) return;

        Optional<String> current = getCurrentPreloadedHash();
        if (current.isPresent() && current.get().equals(requestedPreloadHash.get())) return;

        Pack pack = getPackForHash(requestedPreloadHash.get());
        if (pack == null) {
            // File is missing, forget about it
            requestedPreloadHash.store("");
            return;
        }

        try {
            // If the pack is corrupted, this will throw an exception
            pack.open();
        } catch (Throwable t) {
            WynntilsMod.warn("Failed to open preloaded resource pack", t);

            // File is corrupted, don't try to load it again
            requestedPreloadHash.store("");
            return;
        }

        McUtils.mc().getDownloadedPackSource().serverPack = pack;
    }

    private Pack getPackForHash(String hash) {
        File serverPackDir = new File(McUtils.mc().gameDirectory, "server-resource-packs");
        File file = new File(serverPackDir, hash);

        if (!file.exists()) return null;

        Pack.ResourcesSupplier resourcesSupplier = new FilePackResources.FileResourcesSupplier(file, false);
        Pack.Info info = Pack.readPackInfo(
                "server",
                resourcesSupplier,
                SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        return new PreloadedPack(
                "server",
                true,
                resourcesSupplier,
                Component.literal("Wynntils Resource Pack"),
                info,
                Pack.Position.TOP,
                true,
                PackSource.DEFAULT,
                hash);
    }

    public boolean hasCustomResourcePack() {
        return getCurrentPreloadedHash().isPresent();
    }

    private static final class PreloadedPack extends Pack {
        private final String hash;

        private PreloadedPack(
                String id,
                boolean required,
                Pack.ResourcesSupplier resourcesSupplier,
                Component component,
                Pack.Info info,
                Pack.Position defaultPosition,
                boolean fixedPosition,
                PackSource packSource,
                String hash) {
            super(id, required, resourcesSupplier, component, info, defaultPosition, fixedPosition, packSource);
            this.hash = hash;
        }

        private String getHash() {
            return hash;
        }
    }
}
