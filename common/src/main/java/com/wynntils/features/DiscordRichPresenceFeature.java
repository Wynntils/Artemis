/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ConnectionEvent;
import com.wynntils.mc.event.SetXpEvent;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.models.worlds.event.StreamModeEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.CappedValue;
import java.util.Locale;
import net.minecraft.core.Position;
import net.neoforged.bus.api.SubscribeEvent;

public class DiscordRichPresenceFeature extends Feature {
    @Persisted
    public final Config<Boolean> displayLocation = new Config<>(true);

    @Persisted
    public final Config<Boolean> displayCharacterInfo = new Config<>(true);

    @Persisted
    public final Config<Boolean> displayWorld = new Config<>(true);

    @Persisted
    public final Config<Boolean> disableInStream = new Config<>(true);

    private static final int TERRITORY_TICKS_DELAY = 10;

    private boolean territoryChecking = false;
    private TerritoryProfile lastTerritoryProfile = null;

    @SubscribeEvent
    public void onCharacterUpdate(CharacterUpdateEvent event) {
        if (!Services.Discord.isReady()) return;
        if (!Models.WorldState.onWorld()) return;

        if (displayCharacterInfo.get()) {
            displayCharacterDetails();
        }
    }

    @SubscribeEvent
    public void onXpChange(SetXpEvent event) {
        if (!Services.Discord.isReady()) return;
        if (!Models.WorldState.onWorld()) return;

        if (displayCharacterInfo.get()) {
            displayCharacterDetails();
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldStateEvent event) {
        if (!Services.Discord.isReady()) return;

        if (displayWorld.get()) {
            switch (event.getNewState()) {
                case WORLD -> {
                    Services.Discord.setState(Models.WorldState.getCurrentWorldName());
                    startTerritoryCheck();
                }
                case HUB, CONNECTING -> {
                    Services.Discord.setDetails("");
                    Services.Discord.setState("In Hub");
                    Services.Discord.setWynncraftLogo();
                    stopTerritoryCheck();
                }
                case CHARACTER_SELECTION -> {
                    if (displayLocation.get()) {
                        Services.Discord.setDetails("Selecting a character");
                    }
                    Services.Discord.setState("");
                    Services.Discord.setWynncraftLogo();
                    stopTerritoryCheck();
                }
            }
        } else {
            Services.Discord.setState("");
            stopTerritoryCheck();
        }
    }

    @SubscribeEvent
    public void onStreamToggle(StreamModeEvent e) {
        if (disableInStream.get() && e.isEnabled()) {
            disableRichPresence();
        } else if (!e.isEnabled()) {
            enableRichPresence();
        }
    }

    @SubscribeEvent
    public void onDisconnect(ConnectionEvent.DisconnectedEvent e) {
        disableRichPresence();
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        if (config == disableInStream && Models.WorldState.isInStream()) {
            if (disableInStream.get()) {
                disableRichPresence();
            } else {
                enableRichPresence();
            }
        } else {
            tryUpdateDisplayedInfo();
        }
    }

    @Override
    public void onEnable() {
        enableRichPresence();
    }

    @Override
    public void onDisable() {
        disableRichPresence();
    }

    private void enableRichPresence() {
        // This isReady() check is required for Linux to not crash on config change.
        if (!Services.Discord.isReady()) {
            // Load the Discord SDK
            if (!Services.Discord.load()) {
                // happens when wrong version of GLIBC is installed and Discord SDK fails to load
                Managers.Feature.crashFeature(this);
                return;
            }
        }

        tryUpdateDisplayedInfo();
    }

    private void disableRichPresence() {
        Services.Discord.unload();
        stopTerritoryCheck();
    }

    private void tryUpdateDisplayedInfo() {
        if (!Services.Discord.isReady()) return;
        if (!Models.WorldState.onWorld()) return;

        if (displayLocation.get()) {
            startTerritoryCheck();
        } else {
            // Most likely it was already stopped, but just in case
            stopTerritoryCheck();
            Services.Discord.setDetails("");
        }

        if (displayCharacterInfo.get()) {
            displayCharacterDetails();
        } else {
            Services.Discord.setWynncraftLogo();
        }

        if (displayWorld.get()) {
            Services.Discord.setState(Models.WorldState.getCurrentWorldName());
        } else {
            Services.Discord.setState("");
        }
    }

    private void displayCharacterDetails() {
        if (McUtils.player() == null) return;

        CappedValue combatLevel = Models.CombatXp.getCombatLevel();
        int level = combatLevel.current();
        ClassType classType = Models.Character.getClassType();

        if (classType == null) return;
        String name = StyledText.fromComponent(McUtils.player().getName()).getString(PartStyle.StyleType.NONE);
        Services.Discord.setImageText(name + " - Level " + level + " " + classType.getName());
        Services.Discord.setImage(classType.getActualName(false).toLowerCase(Locale.ROOT));
    }

    private void startTerritoryCheck() {
        if (territoryChecking) {
            // Already checking, no need to start again
            return;
        }

        territoryChecking = true;
        checkTerritory();
    }

    private void stopTerritoryCheck() {
        lastTerritoryProfile = null;
        territoryChecking = false;
    }

    private void checkTerritory() {
        if (!territoryChecking) return;

        // Player is not on world, or the feature is disabled, skip territory check, and stop scheduling
        if (!Models.WorldState.onWorld()) {
            stopTerritoryCheck();
            return;
        }
        if (McUtils.player() == null) {
            stopTerritoryCheck();
            return;
        }

        Position position = McUtils.player().position();
        if (position != null) {
            TerritoryProfile territoryProfile = Models.Territory.getTerritoryProfileForPosition(
                    McUtils.player().position());
            if (territoryProfile != null && territoryProfile != lastTerritoryProfile) {
                lastTerritoryProfile = territoryProfile;
                String location = territoryProfile.getName();
                Services.Discord.setDetails(location);
            }
        }

        // Schedule next check
        Managers.TickScheduler.scheduleLater(this::checkTerritory, TERRITORY_TICKS_DELAY);
    }
}
