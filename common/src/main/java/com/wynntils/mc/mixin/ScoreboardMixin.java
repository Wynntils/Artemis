/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.google.common.collect.Maps;
import com.wynntils.mc.EventFactory;
import java.util.Map;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public abstract class ScoreboardMixin {

    @Shadow
    public Map<String, Map<Objective, Score>> playerScores;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onCtor() {
        this.playerScores = Maps.newConcurrentMap();
    }

    @Inject(
            method = "removePlayerFromTeam(Ljava/lang/String;Lnet/minecraft/world/scores/PlayerTeam;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void removePlayerFromTeamPre(String username, PlayerTeam playerTeam, CallbackInfo ci) {
        if (EventFactory.onRemovePlayerFromTeam(username, playerTeam).isCanceled()) {
            ci.cancel();
        }
    }
}
