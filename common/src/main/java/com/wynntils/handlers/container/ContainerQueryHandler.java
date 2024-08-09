/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.container;

import com.google.common.collect.ImmutableList;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handler;
import com.wynntils.handlers.container.type.ContainerContent;
import com.wynntils.handlers.container.type.ContainerContentChangeType;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.ContainerSetSlotEvent;
import com.wynntils.mc.event.LocalSoundEvent;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ItemUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

public final class ContainerQueryHandler extends Handler {
    private static final int NO_CONTAINER = -2;
    private static final int OPERATION_TIMEOUT_TICKS = 60; // normal operation is ~10 ticks
    private static final int NEXT_OPERATION_DELAY_TICKS = 5; // normal operation is ~10 ticks
    private static final String MENU_CLICK_SOUND = "minecraft.block.wooden_pressure_plate.click_on";

    private final LinkedList<ContainerQueryStep> queuedQueries = new LinkedList<>();

    private ContainerQueryStep currentStep;
    private String firstStepName;

    private Component currentTitle;
    private MenuType<?> currentMenuType;
    private ContainerContent currentContent;
    private int containerId = NO_CONTAINER;
    private int lastHandledContentId = NO_CONTAINER;
    private List<ItemStack> lastHandledItems = List.of();
    private int ticksRemaining;
    private int ticksUntilNextOperation = -1;

    public void runQuery(ContainerQueryStep firstStep) {
        if (currentStep != null) {
            // Only add if it is not already enqueued
            boolean alreadyRunning = currentStep.getName().equals(firstStep.getName());
            boolean alreadyQueued =
                    queuedQueries.stream().anyMatch(query -> query.getName().equals(firstStep.getName()));

            if (alreadyRunning || alreadyQueued) return;

            queuedQueries.add(firstStep);
            return;
        }

        Screen screen = McUtils.mc().screen;
        if (screen instanceof AbstractContainerScreen) {
            // Another inventory screen is already open, cannot do this
            firstStep.onError("Another container screen is already open");
            return;
        }

        if (McUtils.containerMenu().containerId != 0) {
            // For safety, check this way too
            firstStep.onError("Another container is already open");
            return;
        }

        currentStep = firstStep;
        firstStepName = firstStep.getName();
        resetTimer();
        try {
            if (!firstStep.startStep(null)) {
                endQuery();
            }
        } catch (Throwable t) {
            raiseError("Cannot execute first step: " + t.getMessage());
        }
    }

    public void endAllQueries() {
        // Close current container and cancel current query
        if (containerId != NO_CONTAINER) {
            McUtils.sendPacket(new ServerboundContainerClosePacket(containerId));
            raiseError("Container query interrupted by user");
        }

        // Cancel all queued queries
        for (ContainerQueryStep queuedQuery : queuedQueries) {
            queuedQuery.onError("Container query interrupted by user");
        }
        queuedQueries.clear();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.WORLD) return;

        // Cancel all queued queries when world state changes
        for (ContainerQueryStep queuedQuery : queuedQueries) {
            queuedQuery.onError("Container query interrupted by world state change");
        }
        queuedQueries.clear();
    }

    @SubscribeEvent
    public void onSound(LocalSoundEvent.Client e) {
        // Silence the menu click sound when we are processing query
        if (currentStep == null) return;
        if (e.getSource() != SoundSource.BLOCKS) return;
        if (!e.getSound().getLocation().toLanguageKey().equals(MENU_CLICK_SOUND)) return;

        e.setCanceled(true);
        return;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (currentStep == null) return;

        if (ticksUntilNextOperation >= 0) {
            ticksUntilNextOperation--;

            if (ticksUntilNextOperation == 0) {
                try {
                    // Reset the timer for the next operation
                    ticksUntilNextOperation = -1;
                    // Return true iff taking the next step succeeded
                    if (currentStep.startStep(currentContent)) return;
                } catch (Throwable t) {
                    McUtils.sendPacket(new ServerboundContainerClosePacket(containerId));
                    raiseError("Error while processing content for " + firstStepName + ": " + t.getMessage());
                    return;
                }

                // We're done
                endQuery();
                McUtils.sendPacket(new ServerboundContainerClosePacket(containerId));
                // Start next query in queue, if any
                if (!queuedQueries.isEmpty()) {
                    runQuery(queuedQueries.pop());
                }
            }

            return;
        }

        ticksRemaining--;

        if (ticksRemaining <= 0) {
            McUtils.sendPacket(new ServerboundContainerClosePacket(containerId));
            raiseError("Container reply timed out");
        }
    }

    @SubscribeEvent
    public void onMenuOpened(MenuEvent.MenuOpenedEvent.Pre e) {
        // Are we processing a query?
        if (currentStep == null) return;

        if (currentStep.verifyContainer(e.getTitle(), e.getMenuType())) {
            containerId = e.getContainerId();
            currentTitle = e.getTitle();
            currentMenuType = e.getMenuType();
            resetTimer();
            e.setCanceled(true);
        } else {
            McUtils.sendPacket(new ServerboundContainerClosePacket(containerId));
            raiseError("Unexpected container opened: '" + e.getTitle().getString() + "'");
        }
    }

    @SubscribeEvent
    public void onMenuForcefullyClosed(MenuEvent.MenuClosedEvent e) {
        if (currentStep == null) return;

        // Server closed our container window. This should not happen
        // but if it do, report failure
        if (e.getContainerId() == containerId) {
            raiseError("Server closed container");
        } else {
            WynntilsMod.warn("Server closed container " + e.getContainerId() + " but we are querying " + containerId);
        }
    }

    @SubscribeEvent
    public void onContainerSetContent(ContainerSetContentEvent.Pre e) {
        if (currentStep == null) return;
        // We got an inventory update, can happen all the time
        if (e.getContainerId() == 0) return;

        if (containerId == NO_CONTAINER) {
            // We have not registered a MenuOpenedEvent. Assume this means that this is the
            // content of another container, so just pass it on
            return;
        }

        int id = e.getContainerId();
        if (id != containerId) {
            raiseError("Another container opened");
            return;
        }

        // We already processed the current step and are waiting to execute it
        if (ticksUntilNextOperation >= 0) return;

        if (containerId == lastHandledContentId && ItemUtils.isItemListsEqual(e.getItems(), lastHandledItems)) {
            // After opening a new container, Wynncraft sometimes sends contents twice. Ignore this.
            e.setCanceled(true);
            resetTimer();
            return;
        }

        lastHandledContentId = containerId;
        lastHandledItems = e.getItems();
        currentContent =
                new ContainerContent(ImmutableList.copyOf(e.getItems()), currentTitle, currentMenuType, containerId);
        resetTimer();

        try {
            // Now actually process this container

            // Create a map of the changes
            Int2ObjectArrayMap<ItemStack> changeMap = new Int2ObjectArrayMap<>();
            e.getItems().forEach(itemStack -> changeMap.put(changeMap.size(), itemStack));

            // Process container iff verifying succeeded
            if (currentStep.verifyContentChange(currentContent, changeMap, ContainerContentChangeType.SET_CONTENT)) {
                processContainer(currentContent);
            }
        } catch (Throwable t) {
            McUtils.sendPacket(new ServerboundContainerClosePacket(containerId));
            raiseError("Error while processing content for " + firstStepName + ": " + t.getMessage());
        } finally {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onContainerSetSlot(ContainerSetSlotEvent.Pre e) {
        if (currentStep == null) return;
        // We got an inventory update, can happen all the time
        if (e.getContainerId() == 0) return;
        // Ignore weird set slot packets from Wynn
        if (e.getContainerId() == -1) return;

        if (containerId == NO_CONTAINER) {
            // We have not registered a MenuOpenedEvent. Assume this means that this is the
            // content of another container, so just pass it on
            return;
        }

        int id = e.getContainerId();
        if (id != containerId) {
            raiseError("Another container opened #2");
            return;
        }

        // We already processed the current step and are waiting to execute it
        if (ticksUntilNextOperation >= 0) return;

        lastHandledContentId = containerId;

        List<ItemStack> items = new ArrayList<>(currentContent.items());
        items.set(e.getSlot(), e.getItemStack());
        currentContent = new ContainerContent(ImmutableList.copyOf(items), currentTitle, currentMenuType, containerId);

        resetTimer();

        try {
            // Now actually process this container

            // Create a map of the changes
            Int2ObjectArrayMap<ItemStack> changeMap = new Int2ObjectArrayMap<>();
            changeMap.put(e.getSlot(), e.getItemStack());

            // Process container iff verifying succeeded
            if (currentStep.verifyContentChange(currentContent, changeMap, ContainerContentChangeType.SET_SLOT)) {
                processContainer(currentContent);
            }
        } catch (Throwable t) {
            McUtils.sendPacket(new ServerboundContainerClosePacket(containerId));
            raiseError("Error while processing set slot for " + firstStepName + ": " + t.getMessage());
        } finally {
            e.setCanceled(true);
        }
    }

    private void processContainer(ContainerContent currentContainer) throws ContainerQueryException {
        currentStep.handleContent(currentContainer);

        ContainerQueryStep nextStep = currentStep.getNextStep(currentContainer);

        if (nextStep != null) {
            // Go on and query another container
            currentStep = nextStep;
            ticksUntilNextOperation = NEXT_OPERATION_DELAY_TICKS;
        } else {
            // We're done
            endQuery();
            McUtils.sendPacket(new ServerboundContainerClosePacket(containerId));
            // Start next query in queue, if any
            if (!queuedQueries.isEmpty()) {
                runQuery(queuedQueries.pop());
            }
        }
    }

    private void raiseError(String errorMsg) {
        if (currentStep == null) {
            WynntilsMod.error("Internal error in ContainerQueryManager: handleError called with no currentStep");
            return;
        }
        currentStep.onError(errorMsg);
        endQuery();

        // Try to start next query in queue, if any
        // This may very well fail, but we can't do much about it, we trust error handling in the queries
        if (!queuedQueries.isEmpty()) {
            runQuery(queuedQueries.pop());
        }
    }

    private void endQuery() {
        containerId = NO_CONTAINER;
        lastHandledContentId = NO_CONTAINER;
        lastHandledItems = List.of();
        currentStep = null;
        currentContent = null;
        ticksUntilNextOperation = -1;
    }

    private void resetTimer() {
        ticksRemaining = OPERATION_TIMEOUT_TICKS;
    }
}
