/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.seaskipper;

import com.google.common.reflect.TypeToken;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.models.containers.containers.SeaskipperContainer;
import com.wynntils.models.items.items.gui.SeaskipperDestinationItem;
import com.wynntils.models.seaskipper.type.SeaskipperDestination;
import com.wynntils.models.seaskipper.type.SeaskipperDestinationProfile;
import com.wynntils.screens.maps.CustomSeaskipperScreen;
import com.wynntils.services.map.pois.SeaskipperDestinationPoi;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public final class SeaskipperModel extends Model {
    private static final StyledText OAK_BOAT_NAME = StyledText.fromString("§bOak Boat");

    private List<SeaskipperDestination> allDestinations = new ArrayList<>();
    private List<SeaskipperDestination> availableDestinations = new ArrayList<>();

    private int boatSlot = -1;
    private int containerId = -2;

    public SeaskipperModel() {
        super(List.of());

        reloadData();
    }

    @Override
    public void reloadData() {
        loadSeaskipperPois();
    }

    @SubscribeEvent
    public void onScreenInit(ScreenInitEvent e) {
        if (!(Models.Container.getCurrentContainer() instanceof SeaskipperContainer seaskipperContainer)) return;

        containerId = seaskipperContainer.getContainerId();
        availableDestinations = new ArrayList<>();
    }

    @SubscribeEvent
    public void onContainerSetContent(ContainerSetContentEvent.Post event) {
        if (event.getContainerId() != containerId) return;

        for (int i = 0; i < event.getItems().size(); i++) {
            ItemStack item = event.getItems().get(i);

            if (boatSlot == -1 && StyledText.fromComponent(item.getHoverName()).equals(OAK_BOAT_NAME)) {
                boatSlot = i;
                continue;
            }

            Optional<SeaskipperDestinationItem> optionalItem =
                    Models.Item.asWynnItem(item, SeaskipperDestinationItem.class);

            if (optionalItem.isEmpty()) continue;

            SeaskipperDestinationItem destinationItem = optionalItem.get();

            Optional<SeaskipperDestination> destinationOptional = allDestinations.stream()
                    .filter(profile -> profile.profile().destination().equals(destinationItem.getDestination()))
                    .findFirst();

            if (destinationOptional.isEmpty()) {
                WynntilsMod.warn("Could not find profile for destination: " + destinationItem.getDestination());
                continue;
            }

            SeaskipperDestinationProfile profile = destinationOptional.get().profile();

            availableDestinations.add(new SeaskipperDestination(profile, destinationItem, i));
        }

        // Reload the map
        if (McUtils.mc().screen instanceof CustomSeaskipperScreen customSeaskipperScreen) {
            customSeaskipperScreen.reloadDestinationPois();
        }
    }

    public List<SeaskipperDestinationPoi> getPois(boolean includeAll) {
        List<SeaskipperDestinationPoi> pois = new ArrayList<>();

        for (SeaskipperDestination destination : availableDestinations) {
            pois.add(new SeaskipperDestinationPoi(destination));
        }

        // Include the destination we are currently at
        allDestinations.stream()
                .filter(SeaskipperDestination::isPlayerInside)
                .findFirst()
                .ifPresent(profile -> pois.add(new SeaskipperDestinationPoi(profile)));

        if (includeAll) {
            List<SeaskipperDestination> notAvailableProfiles = allDestinations.stream()
                    .filter(profile -> pois.stream()
                            .map(SeaskipperDestinationPoi::getDestination)
                            .noneMatch(destination -> destination.profile().equals(profile.profile())))
                    .toList();

            pois.addAll(notAvailableProfiles.stream()
                    .map(SeaskipperDestinationPoi::new)
                    .toList());
        }

        return pois;
    }

    public void purchaseBoat() {
        ContainerUtils.clickOnSlot(
                boatSlot,
                containerId,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                McUtils.containerMenu().getItems());
    }

    public void purchasePass(SeaskipperDestination destination) {
        if (destination.slot() == -1) return;

        ContainerUtils.clickOnSlot(
                destination.slot(),
                containerId,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                McUtils.containerMenu().getItems());
    }

    public boolean isProfileLoaded() {
        return !allDestinations.isEmpty();
    }

    private void loadSeaskipperPois() {
        Download dl = Managers.Net.download(UrlId.DATA_STATIC_SEASKIPPER_DESTINATIONS);

        dl.handleReader(reader -> {
            Type type = new TypeToken<ArrayList<SeaskipperDestinationProfile>>() {}.getType();
            List<SeaskipperDestinationProfile> profiles = WynntilsMod.GSON.fromJson(reader, type);

            allDestinations = profiles.stream()
                    .map(profile -> new SeaskipperDestination(profile, null, -1))
                    .toList();
        });
    }
}
