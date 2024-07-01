/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.google.common.hash.Hashing;
import com.wynntils.core.components.Managers;
import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.ConnectionEvent;
import com.wynntils.mc.event.ServerResourcePackEvent;
import com.wynntils.utils.mc.McUtils;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Inject(
            method = "handleResourcePack(Lnet/minecraft/network/protocol/common/ClientboundResourcePackPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handleResourcePackPre(ClientboundResourcePackPacket packet, CallbackInfo ci) {
        // Hash is SHA-1 of the URL in 1.20.2
        // Hash is packet#hash() in 1.21
        ServerResourcePackEvent.Load event = new ServerResourcePackEvent.Load(
                packet.getUrl(),
                Hashing.sha1()
                        .hashString(packet.getUrl().toString(), StandardCharsets.UTF_8)
                        .toString(),
                packet.isRequired());
        MixinHelper.postAlways(event);
        if (event.isCanceled()) {
            McUtils.sendPacket(
                    new ServerboundResourcePackPacket(ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
            ci.cancel();
        }
    }

    @Inject(
            method = "handleDisconnect(Lnet/minecraft/network/protocol/common/ClientboundDisconnectPacket;)V",
            at = @At("HEAD"))
    private void handleDisconnectPre(ClientboundDisconnectPacket packet, CallbackInfo ci) {
        // Unexpected disconnect.
        // NOTE: This will happen on a Netty thread instead of the main thread, but all other
        // ConnectionEvents are sent on the main thread, so let's do so with this one too.
        if (Managers.TickScheduler == null) return;

        Managers.TickScheduler.scheduleNextTick(
                () -> MixinHelper.postAlways(new ConnectionEvent.UnexpectedDisconnectedEvent()));
    }
}
