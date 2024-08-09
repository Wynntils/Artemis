/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.wynntils.services.resourcepack.WynntilsResourceProvider;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @Mutable
    @Shadow
    @Final
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // WynntilsResourceProvider is directly injected, as this called really early in the game initialization
        // See WynntilsResourceProvider and ResourcePackService for more information
        Set<RepositorySource> repositorySources = new LinkedHashSet<>(this.sources);
        repositorySources.add(new WynntilsResourceProvider());
        this.sources = repositorySources;
    }
}
