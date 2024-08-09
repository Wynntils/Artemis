/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.resourcepack;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Service;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.mc.event.ConnectionEvent;
import com.wynntils.mc.event.ServerResourcePackEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class ResourcePackService extends Service {
    private static final String PRELOADED_PACK_PREFIX = "wynntils_preloaded/";

    @Persisted
    private final Storage<Pair<UUID, String>> resourcePackIdHash = new Storage<>(Pair.of(null, ""));

    private boolean serverHasResourcePack = false;

    public ResourcePackService() {
        super(List.of());
    }

    public void setRequestedPreloadHash(UUID id, String hash) {
        resourcePackIdHash.store(Pair.of(id, hash));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerResourcePackLoad(ServerResourcePackEvent.Load event) {
        Pack preloadedPack = getPreloadedPack();

        // 1. We have no preloaded pack
        if (preloadedPack == null) {
            // Let Minecraft download and handle the resource pack,
            // and we can preload it next disconnect
            WynntilsMod.info("No preloaded pack, letting Minecraft handle the resource pack.");
            serverHasResourcePack = true;
            return;
        }

        // 2. If we already have the correct resource pack preloaded, cancel the event
        if (preloadedPack.getId().equals(PRELOADED_PACK_PREFIX + event.getHash())) {
            WynntilsMod.info("Preloaded pack is up-to-date, cancelling server pack loading.");
            event.setCanceled(true);
            serverHasResourcePack = false;
            return;
        }

        // 3. Otherwise, we have an old/wrong resource pack preloaded, clear it
        WynntilsMod.info("Preloaded pack is outdated or wrong, clearing server pack.");
        WynntilsMod.info("Preloaded pack: " + preloadedPack.getId() + ", expected: " + getExpectedPackId());

        PackRepository resourcePackRepository = McUtils.mc().getResourcePackRepository();
        List<String> selectedIds = new ArrayList<>(resourcePackRepository.getSelectedIds());
        selectedIds.remove(preloadedPack.getId());
        resourcePackRepository.setSelected(selectedIds);
        // Explicitly do not reload the resource pack repository here,
        // as it will be reloaded when the new pack is loaded
        serverHasResourcePack = true;

        // We can preload the new resource pack next disconnect, like in case #1
        // (or, we joined a non-wynncraft server, so we preload the same pack again)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerResourcePackClear(ServerResourcePackEvent.Clear event) {
        PackRepository resourcePackRepository = McUtils.mc().getResourcePackRepository();

        Pack preloadedPack = getPreloadedPack();

        if (isResourcePackInfoMissing()) {
            WynntilsMod.info(
                    "No preloaded pack, or it was disabled, clearing server pack, checking if a reload should trigger.");
        } else if (preloadedPack == null) {
            WynntilsMod.info("No preloaded pack, clearing server pack, trying to preload a new pack.");
        } else if (!preloadedPack.getId().equals(getExpectedPackId())) {
            WynntilsMod.info("Preloaded pack is outdated, clearing server pack, trying to preload a new pack.");
        } else {
            WynntilsMod.info("Preloaded pack is up-to-date, no-op.");
            return;
        }

        // We try to preload the new pack, as the server pack is cleared
        // If we have no new pack, we just clear the server pack, and don't preload anything
        resourcePackRepository.reload();
        boolean changeMade = preloadResourcePack();

        if (!changeMade) {
            WynntilsMod.info("No changes to preloaded packs, clearing server pack, no reload needed.");
            return;
        }

        if (serverHasResourcePack) {
            WynntilsMod.info("Preloaded pack updated, reloading resource packs is done by Minecraft.");
            return;
        }

        // We reload the resource pack repository, to preload the new pack
        WynntilsMod.info(
                "Triggering a resource pack reload, as the server had no resource packs, and a change was made.");
        McUtils.mc().reloadResourcePacks();
    }

    @SubscribeEvent
    public void onConnect(ConnectionEvent.ConnectedEvent event) {
        // Reset the flag, as we are connecting to a new server
        serverHasResourcePack = false;

        // If we are on a Wynncraft server, we don't need to remove the preloaded pack
        if (Managers.Connection.onServer()) return;

        // We are not on a Wynncraft server, clear the preloaded pack
        WynntilsMod.info("Joined a non-Wynncraft server, clearing preloaded pack.");

        PackRepository resourcePackRepository = McUtils.mc().getResourcePackRepository();
        List<String> selectedIds = new ArrayList<>(resourcePackRepository.getSelectedIds());
        boolean anyRemoved = selectedIds.removeIf(id -> id.startsWith(PRELOADED_PACK_PREFIX));
        resourcePackRepository.setSelected(selectedIds);

        if (anyRemoved) {
            // If the server had resource packs, we trigger the reload twice,
            // but it's a compromise we have to make (applies to non-Wynncraft servers)
            WynntilsMod.info("Preloaded pack removed, reloading resource packs.");
            McUtils.mc().reloadResourcePacks();
        }
    }

    public boolean preloadResourcePack() {
        PackRepository resourcePackRepository = McUtils.mc().getResourcePackRepository();

        // Remove all preloaded packs from the selected list
        List<String> selectedIds = new ArrayList<>(resourcePackRepository.getSelectedIds());
        boolean anyRemoved = selectedIds.removeIf(id -> id.startsWith(PRELOADED_PACK_PREFIX));

        // If we have no packs to preload, make sure some old pack is not loaded
        if (isResourcePackInfoMissing()) {
            resourcePackRepository.setSelected(selectedIds);
            return anyRemoved;
        }

        // We want to position the preloaded pack above the last required pack
        Pack lastRequiredPack = selectedIds.stream()
                .map(resourcePackRepository::getPack)
                .filter(Objects::nonNull)
                .filter(Pack::isRequired)
                .reduce((first, second) -> second)
                .orElse(null);

        int positionToInject = lastRequiredPack == null ? 0 : selectedIds.indexOf(lastRequiredPack.getId()) + 1;

        // We have a pack to preload, make sure it's selected
        for (Pack pack : resourcePackRepository.getAvailablePacks()) {
            if (!pack.getId().equals(getExpectedPackId())) continue;

            // If the resource pack is not already selected, select it, and load it
            if (!selectedIds.contains(pack.getId())) {
                selectedIds.add(positionToInject, pack.getId());
                resourcePackRepository.setSelected(selectedIds);
                return true;
            }

            // We found the pack, no need to continue
            break;
        }

        WynntilsMod.warn("Could not find the preload target pack to select it.");
        resourcePackRepository.setSelected(selectedIds);
        return anyRemoved;
    }

    // Note: This method checks if the preloaded pack is selected,
    //       which is not the same as checking if the pack is loaded
    public boolean isPreloadedPackSelected() {
        return McUtils.mc().getResourcePackRepository().getSelectedIds().contains(getExpectedPackId());
    }

    private Pack getPreloadedPack() {
        if (isResourcePackInfoMissing()) return null;

        PackRepository resourcePackRepository = McUtils.mc().getResourcePackRepository();
        for (Pack pack : resourcePackRepository.getSelectedPacks()) {
            if (pack.getId().equals(getExpectedPackId())) {
                return pack;
            }
        }

        return null;
    }

    private boolean isResourcePackInfoMissing() {
        Pair<UUID, String> resourceInfo = resourcePackIdHash.get();
        return resourceInfo.key() == null
                || resourceInfo.value() == null
                || resourceInfo.value().isEmpty();
    }

    private String getExpectedPackId() {
        return PRELOADED_PACK_PREFIX + resourcePackIdHash.get().value();
    }
}
