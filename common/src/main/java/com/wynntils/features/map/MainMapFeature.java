/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.map;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.config.HiddenConfig;
import com.wynntils.mc.event.PlayerInteractEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.models.containers.containers.reward.LootChestContainer;
import com.wynntils.models.containers.type.LootChestType;
import com.wynntils.screens.maps.MainMapScreen;
import com.wynntils.screens.maps.PoiCreationScreen;
import com.wynntils.services.map.pois.CustomPoi;
import com.wynntils.services.mapdata.providers.builtin.WaypointsProvider;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.type.HealthTexture;
import com.wynntils.utils.render.type.PointerType;
import com.wynntils.utils.render.type.TextShadow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.MAP)
public class MainMapFeature extends Feature {
    @Persisted
    public final HiddenConfig<List<CustomPoi>> customPois = new HiddenConfig<>(new ArrayList<>());

    @Persisted
    public final Config<Float> poiFadeAdjustment = new Config<>(0.4f);

    @Persisted
    public final Config<Float> combatPoiMinZoom = new Config<>(0.166f);

    @Persisted
    public final Config<Float> cavePoiMinZoom = new Config<>(0.28f);

    @Persisted
    public final Config<Float> servicePoiMinZoom = new Config<>(0.8f);

    @Persisted
    public final Config<Float> fastTravelPoiMinZoom = new Config<>(0.166f);

    @Persisted
    public final Config<Float> customPoiMinZoom = new Config<>(0.28f);

    @Persisted
    public final Config<Float> lootChestTier1PoiMinZoom = new Config<>(0.8f);

    @Persisted
    public final Config<Float> lootChestTier2PoiMinZoom = new Config<>(0.8f);

    @Persisted
    public final Config<Float> lootChestTier3PoiMinZoom = new Config<>(0.28f);

    @Persisted
    public final Config<Float> lootChestTier4PoiMinZoom = new Config<>(0.28f);

    @Persisted
    public final Config<PointerType> pointerType = new Config<>(PointerType.ARROW);

    @Persisted
    public final Config<CustomColor> pointerColor = new Config<>(new CustomColor(1f, 1f, 1f, 1f));

    @Persisted
    public final Config<Float> playerPointerScale = new Config<>(1.5f);

    @Persisted
    public final Config<Float> poiScale = new Config<>(1f);

    @Persisted
    public final Config<Boolean> centerWhenUnmapped = new Config<>(true);

    @Persisted
    public final Config<Boolean> autoWaypointChests = new Config<>(true);

    @Persisted
    public final Config<LootChestType> minTierForAutoWaypoint = new Config<>(LootChestType.TIER_3);

    @Persisted
    public final Config<Boolean> renderRemoteFriendPlayers = new Config<>(true);

    @Persisted
    public final Config<Boolean> renderRemotePartyPlayers = new Config<>(true);

    @Persisted
    public final Config<HealthTexture> remotePlayerHealthTexture = new Config<>(HealthTexture.A);

    @Persisted
    public final Config<TextShadow> remotePlayerNameShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> holdGuildMapOpen = new Config<>(true);

    private BlockPos lastChestPos;

    @RegisterKeyBind
    public final KeyBind openMapKeybind = new KeyBind("Open Main Map", GLFW.GLFW_KEY_M, false, this::openMainMap);

    @RegisterKeyBind
    public final KeyBind newWaypointKeybind =
            new KeyBind("New Waypoint", GLFW.GLFW_KEY_B, true, this::openWaypointSetup);

    private void openMainMap() {
        // If the current screen is already the map, and we get this event, this means we are holding the keybind
        // and should signal that we should close when the key is not held anymore.
        if (McUtils.mc().screen instanceof MainMapScreen mainMapScreen) {
            mainMapScreen.setHoldingMapKey(true);
            return;
        }

        McUtils.mc().setScreen(MainMapScreen.create());
    }

    private void openWaypointSetup() {
        PoiLocation location = new PoiLocation(
                McUtils.player().getBlockX(),
                McUtils.player().getBlockY(),
                McUtils.player().getBlockZ());

        McUtils.mc().setScreen(PoiCreationScreen.create(null, location));
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.InteractAt event) {
        if (!autoWaypointChests.get()) return;

        Entity entity = event.getEntityHitResult().getEntity();
        if (entity != null && entity.getType() == EntityType.SLIME) {
            // We don't actually know if this is a chest, but it's a good enough guess.
            lastChestPos = entity.blockPosition();
        }
    }

    @SubscribeEvent
    public void onScreenOpened(ScreenOpenedEvent.Post event) {
        if (!autoWaypointChests.get()) return;
        if (lastChestPos == null) return;
        if (!(event.getScreen() instanceof ContainerScreen)) return;

        if (!(Models.Container.getCurrentContainer() instanceof LootChestContainer)) {
            lastChestPos = null;
            return;
        }

        LootChestType chestType = Models.LootChest.getChestType(event.getScreen());
        if (chestType == null) return;

        if (chestType.ordinal() < minTierForAutoWaypoint.get().ordinal()) {
            lastChestPos = null;
            return;
        }

        PoiLocation location = new PoiLocation(lastChestPos.getX(), lastChestPos.getY(), lastChestPos.getZ());
        CustomPoi newPoi = new CustomPoi(
                location,
                chestType.getWaypointName(),
                CommonColors.WHITE,
                chestType.getWaypointTexture(),
                CustomPoi.Visibility.DEFAULT);

        if (customPois.get().stream().noneMatch(customPoi -> customPoi.equals(newPoi))) {
            customPois.get().add(newPoi);

            // TODO: Replace this notification with a popup
            Managers.Notification.queueMessage(
                    Component.literal("Added new waypoint for " + chestType.getWaypointName())
                            .withStyle(ChatFormatting.AQUA));

            customPois.touched();
            updateWaypoints();
        }
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        if (config == customPois) {
            updateWaypoints();
        }
    }

    public void updateWaypoints() {
        WaypointsProvider.resetFeatures();
        customPois.get().forEach(WaypointsProvider::registerFeature);
    }
}
