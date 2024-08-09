/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.command;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handler;
import com.wynntils.core.components.Models;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.LinkedList;
import java.util.Queue;
import net.neoforged.bus.api.SubscribeEvent;

public class CommandHandler extends Handler {
    private static final int TICKS_PER_EXECUTE = 7;
    private static final int NPC_DIALOGUE_WAIT_TICKS = 40;

    private final Queue<String> commandQueue = new LinkedList<>();
    private int commandQueueTicks = 0;

    @SubscribeEvent
    public void onTick(TickEvent e) {
        if (!Models.WorldState.onWorld()) return;

        if (Models.NpcDialogue.isInDialogue()) {
            // Reset the command queue ticks when we are in dialogue,
            // so that we don't execute commands mid-dialogue,
            // and we also have a wait time after dialogue ends
            // (because dialogues tend to clear for a tick or two).
            commandQueueTicks = NPC_DIALOGUE_WAIT_TICKS;
            return;
        }

        commandQueueTicks--;

        if (commandQueueTicks <= 0 && !commandQueue.isEmpty()) {
            String command = commandQueue.poll();
            WynntilsMod.info("Executing queued command: " + command);
            McUtils.mc().getConnection().sendCommand(command);
            commandQueueTicks = TICKS_PER_EXECUTE;
        }
    }

    /**
     * Sends a command to the Wynncraft server. Commands will be sent as soon as possible, respecting the
     * server ratelimit. Queued commands may take up to {@link TICKS_PER_EXECUTE} each to execute.
     * Use this when the mod requires a command to be sent, but it is not directly requested by the user.
     * If a queue is required, executions happen in the order they are queued.
     * All commands automatically log when they are executed.
     * @param command The command to queue. The leading '/' should not be included.
     */
    public void queueCommand(String command) {
        if (commandQueueTicks <= 0 && !Models.NpcDialogue.isInDialogue()) {
            WynntilsMod.info("Executing queued command immediately: " + command);
            McUtils.mc().getConnection().sendCommand(command);
            commandQueueTicks = TICKS_PER_EXECUTE;
        } else {
            commandQueue.add(command);
        }
    }

    /**
     * Sends a command to the Wynncraft server immediately, bypassing the queue.
     * Use this method for user initiated commands, or commands that need to be executed immediately.
     * All commands automatically log when they are executed.
     * @param command The command to execute. The leading '/' should not be included.
     */
    public void sendCommandImmediately(String command) {
        WynntilsMod.info("Executing immediate command: " + command);
        McUtils.mc().getConnection().sendCommand(command);
    }
}
