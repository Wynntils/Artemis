/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.container;

import com.wynntils.handlers.container.type.ContainerContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;

public interface ContainerQueryStep {
    /**
     * Take the action needed to start this step. If this is the very first step, container
     * is null. Otherwise, it will be the currently open container that the next step
     * will be taken on.
     */
    boolean startStep(ContainerContent container);

    /**
     * Verify that the container that has just opened has the expected type and
     * title. To ensure robustness, make this test as tight as possible.
     */
    boolean verifyContainer(Component title, MenuType<?> menuType);

    /**
     * Process the actual content of the container that this step has opened up.
     */
    void handleContent(ContainerContent container);

    /**
     * Return a chained ContainerQueryStep, if another step is needed for the
     * currently open container. If the query session is finished, return null.
     */
    ContainerQueryStep getNextStep(ContainerContent container);

    /**
     * This will be called by ContainerQueryManager if an error occurs. If that happens,
     * no furhter methods will be called on this step.
     */
    void onError(String errorMsg);

    /**
     * This will be called after query competition, after the inventory was closed.
     */
    void onComplete();

    /** A way to identify this query. It is used to help avoid queueing the same query twice. */
    String getName();

    // FIXME: This might not be the best way to handle this
    /** Some gui's do not reopen the menu, they just refresh the content (pressing dialogue history in quest book).
     * Use this to indicate that a content refresh is enough for us, and we do not need to wait for opening a menu
     */
    default boolean shouldWaitForMenuReopen() {
        return true;
    }
}
