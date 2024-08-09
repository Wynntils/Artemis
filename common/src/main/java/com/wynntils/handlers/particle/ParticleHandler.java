/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.particle;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handler;
import com.wynntils.handlers.particle.event.ParticleVerifiedEvent;
import com.wynntils.handlers.particle.type.Particle;
import com.wynntils.handlers.particle.type.ParticleType;
import com.wynntils.handlers.particle.type.ParticleVerifier;
import com.wynntils.handlers.particle.type.UnverifiedParticle;
import com.wynntils.mc.event.ParticleAddedEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;

public class ParticleHandler extends Handler {
    private final Map<ParticleType, ParticleVerifier> particleVerifiers = new EnumMap<>(ParticleType.class);

    private final Set<UnverifiedParticle> unverifiedParticles = new HashSet<>();

    @SubscribeEvent
    public void onParticleAdded(ParticleAddedEvent event) {
        Position particlePosition = new Vec3(event.getX(), event.getY(), event.getZ());

        // We depend on the fact that particle groups are sent in order:
        // If we have A and B particle animation, then we will receive all A particles, then all B particles
        if (unverifiedParticles.isEmpty()) {
            for (ParticleType particleType : particleVerifiers.keySet()) {
                // Check if the this type is compatible with the particle effect type
                if (!particleType.getCompatibleParticleEffect().equals(event.getParticle())) continue;

                UnverifiedParticle unverifiedParticleOfType = new UnverifiedParticle(particleType, event.getParticle());

                // Check if the particle passes the verifier
                if (unverifiedParticleOfType.addNewParticle(particlePosition, event.getParticle())) {
                    unverifiedParticles.add(unverifiedParticleOfType);
                }
            }
        } else {
            List<UnverifiedParticle> invalidParticles = new ArrayList<>();

            for (UnverifiedParticle unverifiedParticle : unverifiedParticles) {
                // Check if the this type is compatible with the particle effect type
                if (!unverifiedParticle.getParticleEffect().equals(event.getParticle())) continue;

                // Check if the particle passes the verifier
                if (unverifiedParticle.addNewParticle(particlePosition, event.getParticle())) {
                    ParticleVerifier.VerificationResult verificationResult = unverifiedParticle.verifyCompleteness();

                    switch (verificationResult) {
                        case VERIFIED -> {
                            Particle particle = unverifiedParticle.getParticle();
                            WynntilsMod.postEvent(new ParticleVerifiedEvent(particle));

                            // We have verified this particle, so we can reset the unverified particles
                            unverifiedParticles.clear();
                            return;
                        }
                        case UNVERIFIED -> {
                            // Do nothing, we will wait for more particles
                        }
                        case INVALID -> {
                            invalidParticles.add(unverifiedParticle);
                        }
                    }
                } else {
                    // This must mean that either this particle is invalid or this particle type is not what we are
                    // looking for
                    invalidParticles.add(unverifiedParticle);
                }
            }

            invalidParticles.forEach(unverifiedParticles::remove);
        }
    }

    public void registerParticleVerifier(ParticleType particleType, ParticleVerifier particleVerifier) {
        particleVerifiers.put(particleType, particleVerifier);
    }

    public ParticleVerifier getParticleVerifier(ParticleType particleType) {
        return particleVerifiers.get(particleType);
    }
}
