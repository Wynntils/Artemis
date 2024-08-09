/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;

public class PlayerNametagRenderEvent extends EntityNameTagRenderEvent implements ICancellableEvent {
    public PlayerNametagRenderEvent(
            AbstractClientPlayer entity,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            EntityRenderDispatcher entityRenderDispatcher,
            Font font) {
        super(entity, displayName, poseStack, buffer, packedLight, entityRenderDispatcher, font, 0f);
    }

    @Override
    public Player getEntity() {
        return (Player) super.getEntity();
    }

    @Override
    public void setBackgroundOpacity(float backgroundOpacity) {
        // Cannot set background opacity for PlayerNametagRenderEvent. Use EntityNameTagRenderEvent instead.
        // For custom rendered player nametags, CustomNameTagRendererFeature takes care of the background.
    }
}
