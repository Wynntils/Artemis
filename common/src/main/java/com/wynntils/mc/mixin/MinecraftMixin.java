/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.wynntils.core.components.Services;
import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.DisplayResizeEvent;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.mc.event.ServerResourcePackEvent;
import com.wynntils.mc.event.TickAlwaysEvent;
import com.wynntils.mc.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("RETURN"))
    private void setScreenPost(Screen screen, CallbackInfo ci, @Share("oldScreen") LocalRef<Screen> oldScreen) {
        if (screen == null) {
            MixinHelper.post(new ScreenClosedEvent(oldScreen.get()));
        } else {
            MixinHelper.post(new ScreenOpenedEvent.Post(screen));
        }
    }

    @Inject(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"), cancellable = true)
    private void setScreenPre(Screen screen, CallbackInfo ci, @Share("oldScreen") LocalRef<Screen> oldScreen) {
        oldScreen.set(((Minecraft) (Object) this).screen);

        if (screen == null) return;

        ScreenOpenedEvent.Pre event = new ScreenOpenedEvent.Pre(screen);
        MixinHelper.postAlways(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void tickPost(CallbackInfo ci) {
        // TickAlwaysEvent is posted before TickEvent to ensure
        // that the tasks in TickSchedulerManager are run before
        // any other tick event listeners could schedule new tasks
        // making it run in the same tick
        MixinHelper.postAlways(new TickAlwaysEvent());
        MixinHelper.post(new TickEvent());
    }

    @Inject(method = "resizeDisplay()V", at = @At("RETURN"))
    private void resizeDisplayPost(CallbackInfo ci) {
        MixinHelper.postAlways(new DisplayResizeEvent());
    }

    @Inject(
            method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/Options;loadSelectedResourcePacks(Lnet/minecraft/server/packs/repository/PackRepository;)V",
                            shift = At.Shift.AFTER))
    private void onInitialResourcePackLoad(CallbackInfo ci) {
        // Too early to post events here, but Service components are initialized (and their storages loaded)
        // We add the resource pack to the selected list here
        Services.ResourcePack.preloadResourcePack();
        // Explicitly do not trigger a reload here, as it's too early, and the game will do it later
    }

    @Inject(
            method = "clearDownloadedResourcePacks()V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/resources/server/DownloadedPackSource;cleanupAfterDisconnect()V",
                            shift = At.Shift.AFTER))
    private void handleResourcePackPopPre(CallbackInfo ci) {
        ServerResourcePackEvent.Clear event = new ServerResourcePackEvent.Clear();
        MixinHelper.postAlways(event);
    }
}
