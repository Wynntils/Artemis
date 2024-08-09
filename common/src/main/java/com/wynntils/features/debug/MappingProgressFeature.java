/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterCommand;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.mc.event.ChunkReceivedEvent;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.DEBUG)
@StartDisabled
public class MappingProgressFeature extends Feature {
    @RegisterCommand
    private final LiteralCommandNode<CommandSourceStack> resetCommand = Commands.literal("mapping")
            .then(Commands.literal("reset"))
            .executes(this::resetMappedChunks)
            .build();

    @Persisted
    private final Storage<Set<Long>> mappedChunks = new Storage<>(new TreeSet<>());

    @SubscribeEvent
    public void onChunkLoaded(ChunkReceivedEvent event) {
        mappedChunks.get().add(ChunkPos.asLong(event.getChunkX(), event.getChunkZ()));
        mappedChunks.touched();
    }

    public Set<Long> getMappedChunks() {
        return Collections.unmodifiableSet(mappedChunks.get());
    }

    private int resetMappedChunks(CommandContext<CommandSourceStack> context) {
        mappedChunks.get().clear();
        mappedChunks.touched();
        return 1;
    }
}
