/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.notifications.MessageContainer;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.container.ContainerQueryException;
import com.wynntils.handlers.container.scriptedquery.QueryStep;
import com.wynntils.handlers.container.scriptedquery.ScriptedContainerQuery;
import com.wynntils.handlers.container.type.ContainerContent;
import com.wynntils.handlers.container.type.ContainerContentChangeType;
import com.wynntils.handlers.container.type.ContainerContentVerification;
import com.wynntils.models.activities.type.ActivityInfo;
import com.wynntils.models.activities.type.ActivityType;
import com.wynntils.models.items.items.gui.ActivityItem;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import com.wynntils.utils.wynn.InventoryUtils;
import com.wynntils.utils.wynn.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class ContentBookQueries {
    // A config in the future, turned off for performance for now
    private static final boolean RESET_FILTERS = false;

    private static final int CHANGE_VIEW_SLOT = 66;
    private static final int PROGRESS_SLOT = 68;
    private static final int NEXT_PAGE_SLOT = 69;

    private static final StyledText SCROLL_DOWN_TEXT = StyledText.fromString("Scroll Down");
    private static final String FILTER_ITEM_TITLE = "Filter";
    private static final Pattern ACTIVE_FILTER = Pattern.compile("^§f- §7(.*)$");
    private static final Pattern INACTIVE_FILTER = Pattern.compile("^§7- §8(.*)$");
    private static final int MAX_FILTERS = 12;

    private String selectedFilter;
    private String activeFilter;
    private int filterChangeDirection;
    private int filterLoopCount;

    private MessageContainer stateMessageContainer;

    /**
     * Trigger a rescan of the content book. When the rescan is done, Models.Content.updateFromContentBookQuery
     * will be called.
     */
    protected void queryContentBook(
            ActivityType activityType,
            BiConsumer<List<ActivityInfo>, List<StyledText>> processResult,
            boolean showUpdates,
            boolean firstPageOnly) {
        List<ActivityInfo> newActivity = new ArrayList<>();
        List<StyledText> progress = new ArrayList<>();

        ScriptedContainerQuery query = ScriptedContainerQuery.builder(
                        "Content Book Query for " + activityType.getDisplayName())
                .onError(msg -> {
                    WynntilsMod.warn("Problem querying Content Book: " + msg);
                    if (showUpdates && stateMessageContainer != null) {
                        Managers.Notification.editMessage(
                                stateMessageContainer,
                                StyledText.fromComponent(Component.literal(
                                                "Error loading " + activityType.getGroupName() + " from content book")
                                        .withStyle(ChatFormatting.RED)));
                    }
                })
                .execute(() -> {
                    if (showUpdates) {
                        stateMessageContainer = Managers.Notification.queueMessage(
                                Component.literal("Loading " + activityType.getGroupName() + " from content book...")
                                        .withStyle(ChatFormatting.YELLOW));
                    }
                })

                // Open content book
                .then(QueryStep.useItemInHotbar(InventoryUtils.CONTENT_BOOK_SLOT_NUM)
                        .expectContainerTitle(Models.Activity.CONTENT_BOOK_TITLE))

                // Save filter state, and set it correctly
                .execute(() -> {
                    filterLoopCount = 0;
                    selectedFilter = null;
                })
                .reprocess(c -> {
                    // Determine the best direction to change the filter
                    filterChangeDirection =
                            getFilterChangeDirection(c.items().get(CHANGE_VIEW_SLOT), activityType.getDisplayName());
                })
                .repeat(
                        c -> {
                            filterLoopCount++;
                            if (filterLoopCount > MAX_FILTERS) {
                                throw new ContainerQueryException("Filter setting has exceeded max loops");
                            }

                            activeFilter = getActiveFilter(c.items().get(CHANGE_VIEW_SLOT));
                            if (activeFilter == null) {
                                throw new ContainerQueryException("Cannot determine active filter");
                            }

                            if (selectedFilter == null) {
                                selectedFilter = activeFilter;
                            }

                            // Continue looping until filter matches
                            return !activeFilter.equals(activityType.getDisplayName());
                        },
                        QueryStep.clickOnSlot(CHANGE_VIEW_SLOT, () -> filterChangeDirection)
                                .verifyContentChange(getContentBookFilterChangeVerification()))

                // Process first page
                .reprocess(c -> {
                    processContentBookPage(c, newActivity);
                    ItemStack itemStack = c.items().get(PROGRESS_SLOT);
                    progress.add(ItemUtils.getItemName(itemStack));
                    progress.addAll(LoreUtils.getLore(itemStack));
                })

                // Repeatedly click next page, if available, and process the following page
                .repeat(
                        c -> {
                            if (firstPageOnly) {
                                return false;
                            }
                            return ScriptedContainerQuery.containerHasSlot(
                                    c, NEXT_PAGE_SLOT, Items.POTION, SCROLL_DOWN_TEXT);
                        },
                        QueryStep.clickOnSlot(NEXT_PAGE_SLOT)
                                .processIncomingContainer(c -> processContentBookPage(c, newActivity)))

                // Restore filter to original value
                .execute(() -> filterLoopCount = 0)
                .execute(() -> {
                    // Inverse the filter change direction
                    filterChangeDirection = filterChangeDirection == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                            ? GLFW.GLFW_MOUSE_BUTTON_LEFT
                            : GLFW.GLFW_MOUSE_BUTTON_RIGHT;
                })
                .repeat(
                        c -> {
                            if (!RESET_FILTERS) {
                                return false;
                            }

                            filterLoopCount++;
                            if (filterLoopCount > MAX_FILTERS) {
                                throw new ContainerQueryException("Filter setting has exceeded max loops");
                            }

                            String activeFilter = getActiveFilter(c.items().get(CHANGE_VIEW_SLOT));
                            if (activeFilter == null) {
                                throw new ContainerQueryException("Cannot determine active filter");
                            }

                            // Continue looping until filter matches original value
                            return !activeFilter.equals(selectedFilter);
                        },
                        QueryStep.clickOnSlot(CHANGE_VIEW_SLOT, () -> filterChangeDirection)
                                .verifyContentChange(getContentBookFilterChangeVerification()))

                // Finally signal we're done
                .execute(() -> processResult.accept(newActivity, progress))
                .execute(() -> {
                    if (showUpdates) {
                        Managers.Notification.editMessage(
                                stateMessageContainer,
                                StyledText.fromComponent(Component.literal(
                                                "Loaded " + activityType.getGroupName() + " from content book")
                                        .withStyle(ChatFormatting.GREEN)));
                    }
                })
                .build();

        query.executeQuery();
    }

    private String getActiveFilter(ItemStack itemStack) {
        StyledText itemName = ItemUtils.getItemName(itemStack);
        if (!itemName.equals(StyledText.fromString(FILTER_ITEM_TITLE))) return null;

        List<StyledText> lore = LoreUtils.getLore(itemStack);
        for (StyledText line : lore) {
            Matcher m = line.getMatcher(ACTIVE_FILTER);
            if (m.matches()) {
                return m.group(1);
            }
        }

        return null;
    }

    private int getFilterChangeDirection(ItemStack itemStack, String targetFilter) {
        StyledText itemName = ItemUtils.getItemName(itemStack);
        if (!itemName.equals(StyledText.fromString(FILTER_ITEM_TITLE))) return GLFW.GLFW_MOUSE_BUTTON_LEFT;

        int activeFilterIndex = -1;
        int targetFilterIndex = -1;
        int filterCount = 0;
        List<StyledText> lore = LoreUtils.getLore(itemStack);
        for (int i = 0; i < lore.size(); i++) {
            StyledText line = lore.get(i);

            Matcher m = line.getMatcher(ACTIVE_FILTER);
            if (m.matches()) {
                activeFilterIndex = i;
                filterCount++;
                continue;
            }

            m = line.getMatcher(INACTIVE_FILTER);
            if (m.matches()) {
                filterCount++;

                if (m.group(1).equals(targetFilter)) {
                    targetFilterIndex = i;
                }
            }
        }

        if (activeFilterIndex == -1 || targetFilterIndex == -1) {
            return GLFW.GLFW_MOUSE_BUTTON_LEFT;
        }

        // Calculate the direction for the shortest path, handle wrap-around
        // Left is forward, right is backward
        int forward = targetFilterIndex - activeFilterIndex;
        int backward = activeFilterIndex - targetFilterIndex;
        if (forward < 0) forward += filterCount;
        if (backward < 0) backward += filterCount;

        return forward < backward ? GLFW.GLFW_MOUSE_BUTTON_LEFT : GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private ContainerContentVerification getContentBookFilterChangeVerification() {
        return (container, changes, changeType) -> {
            // Only set slot changes can be valid
            if (changeType == ContainerContentChangeType.SET_CONTENT) return false;

            // Check if the progress item changed, this is the last item to change
            if (!changes.containsKey(PROGRESS_SLOT)) return false;

            // Check if the filter item changed, and if so, if the active filter changed
            String itemFilter = getActiveFilter(container.items().get(CHANGE_VIEW_SLOT));
            return !Objects.equals(itemFilter, activeFilter);
        };
    }

    private void processContentBookPage(ContainerContent container, List<ActivityInfo> newActivities) {
        for (int slot = 0; slot < 54; slot++) {
            ItemStack itemStack = container.items().get(slot);
            Optional<ActivityItem> activityItemOpt = Models.Item.asWynnItem(itemStack, ActivityItem.class);
            if (activityItemOpt.isEmpty()) continue;

            ActivityInfo activityInfo = activityItemOpt.get().getActivityInfo();

            newActivities.add(activityInfo);
        }
    }

    protected void toggleTracking(String name, ActivityType activityType) {
        // We do not want to change filtering when tracking, since we get
        // no chance to reset it
        ScriptedContainerQuery query = ScriptedContainerQuery.builder("Toggle Activity Tracking Query: " + name)
                .onError(msg -> {
                    WynntilsMod.warn("Problem querying Content Book for tracking: " + msg);
                    Managers.Notification.queueMessage(
                            StyledText.fromComponent(Component.literal("Setting tracking in Content Book failed")
                                    .withStyle(ChatFormatting.RED)));
                })

                // Open compass/character menu
                .then(QueryStep.useItemInHotbar(InventoryUtils.CONTENT_BOOK_SLOT_NUM)
                        .expectContainerTitle(Models.Activity.CONTENT_BOOK_TITLE))

                // Save filter state, and set it correctly
                .execute(() -> {
                    filterLoopCount = 0;
                    selectedFilter = null;
                })
                .reprocess(c -> {
                    // Determine the best direction to change the filter
                    filterChangeDirection =
                            getFilterChangeDirection(c.items().get(CHANGE_VIEW_SLOT), activityType.getDisplayName());
                })
                .repeat(
                        c -> {
                            filterLoopCount++;
                            if (filterLoopCount > MAX_FILTERS) {
                                throw new ContainerQueryException("Filter setting has exceeded max loops");
                            }

                            activeFilter = getActiveFilter(c.items().get(CHANGE_VIEW_SLOT));
                            if (activeFilter == null) {
                                throw new ContainerQueryException("Cannot determine active filter");
                            }

                            if (selectedFilter == null) {
                                selectedFilter = activeFilter;
                            }

                            // Continue looping until filter matches
                            return !activeFilter.equals(activityType.getDisplayName());
                        },
                        QueryStep.clickOnSlot(CHANGE_VIEW_SLOT, () -> filterChangeDirection)
                                .verifyContentChange(getContentBookFilterChangeVerification()))

                // Repeatedly check if the requested task is on this page,
                // if so, click it, otherwise click on next slot (if available)
                .repeat(
                        c -> {
                            int slot = findTrackedActivity(c, name, activityType);
                            // Not found, try to go to next page
                            if (slot == -1) return true;

                            // Found it, now click it
                            ContainerUtils.clickOnSlot(slot, c.containerId(), GLFW.GLFW_MOUSE_BUTTON_LEFT, c.items());
                            return false;
                        },
                        QueryStep.clickOnMatchingSlot(NEXT_PAGE_SLOT, Items.GOLDEN_SHOVEL, SCROLL_DOWN_TEXT))

                // Restore filter to original value
                .execute(() -> filterLoopCount = 0)
                .execute(() -> {
                    // Inverse the filter change direction
                    filterChangeDirection = filterChangeDirection == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                            ? GLFW.GLFW_MOUSE_BUTTON_LEFT
                            : GLFW.GLFW_MOUSE_BUTTON_RIGHT;
                })
                .repeat(
                        c -> {
                            if (!RESET_FILTERS) {
                                return false;
                            }

                            filterLoopCount++;
                            if (filterLoopCount > MAX_FILTERS) {
                                throw new ContainerQueryException("Filter setting has exceeded max loops");
                            }

                            String activeFilter = getActiveFilter(c.items().get(CHANGE_VIEW_SLOT));
                            if (activeFilter == null) {
                                throw new ContainerQueryException("Cannot determine active filter");
                            }

                            // Continue looping until filter matches original value
                            return !activeFilter.equals(selectedFilter);
                        },
                        QueryStep.clickOnSlot(CHANGE_VIEW_SLOT, () -> filterChangeDirection)
                                .verifyContentChange(getContentBookFilterChangeVerification()))
                .build();

        query.executeQuery();
    }

    private int findTrackedActivity(ContainerContent container, String name, ActivityType activityType) {
        for (int slot = 0; slot < 54; slot++) {
            ItemStack itemStack = container.items().get(slot);
            Optional<ActivityItem> activityItemOpt = Models.Item.asWynnItem(itemStack, ActivityItem.class);
            if (activityItemOpt.isEmpty()) continue;

            ActivityInfo activityInfo = activityItemOpt.get().getActivityInfo();
            if (activityInfo.type().matchesTracking(activityType)
                    && activityInfo.name().equals(name)) {
                // Found it!
                return slot;
            }
        }

        return -1;
    }
}
