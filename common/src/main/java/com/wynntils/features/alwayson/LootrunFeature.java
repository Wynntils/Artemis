/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.alwayson;

import com.google.common.collect.ImmutableList;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.features.AlwaysOnFeature;
import com.wynntils.core.features.properties.StartDisabled;
import com.wynntils.core.managers.Manager;
import com.wynntils.mc.event.ClientTickEvent;
import com.wynntils.mc.event.PlayerInteractEvent;
import com.wynntils.mc.event.RenderLevelLastEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.utils.FileUtils;
import com.wynntils.utils.objects.CommonColors;
import com.wynntils.utils.objects.CustomColor;
import com.wynntils.wc.utils.ContainerUtils;
import com.wynntils.wc.utils.lootrun.LootrunUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@StartDisabled
public class LootrunFeature extends AlwaysOnFeature {
    public static LootrunFeature INSTANCE;

    // TODO: Add textured path type
    //    @Config
    //    public PathType pathType = PathType.TEXTURED;

    @Config
    public CustomColor activePathColour = CommonColors.LIGHT_BLUE;

    @Config
    public CustomColor recordingPathColour = CommonColors.RED;

    @Config
    public boolean rainbowLootRun = false;

    @Config
    public int cycleDistance = 20; // TODO limit this later

    @Config
    public boolean showNotes = true;

    @Override
    protected void onInit(
            ImmutableList.Builder<Condition> conditions, ImmutableList.Builder<Class<? extends Manager>> dependencies) {
        FileUtils.mkdir(LootrunUtils.LOOTRUNS);
    }

    @SubscribeEvent
    public void recordMovement(ClientTickEvent event) {
        if (event.getTickPhase() == ClientTickEvent.Phase.START) {
            LootrunUtils.recordMovementIfRecording();
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockState block = event.getWorld().getBlockState(event.getPos());
        if (block.is(Blocks.CHEST)) {
            LootrunUtils.setLastChestIfRecording(event.getPos());
        }
    }

    @SubscribeEvent
    public void onOpen(ScreenOpenedEvent event) {
        if (ContainerUtils.isLootChest(event.getScreen())) {
            LootrunUtils.addChestIfRecording();
        }
    }

    @SubscribeEvent
    public void onRenderLastLevel(RenderLevelLastEvent event) {
        LootrunUtils.render(event.getPoseStack());
    }

    @Override
    protected void onConfigUpdate(ConfigHolder configHolder) {
        LootrunUtils.recompileLootrun(false);
    }

    public enum PathType {
        TEXTURED,
        LINE
    }
}
