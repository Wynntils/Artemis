/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.lootrun.particle;

import com.wynntils.handlers.particle.type.Particle;
import com.wynntils.handlers.particle.type.ParticleType;
import com.wynntils.handlers.particle.type.ParticleVerifier;
import java.util.List;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;

public class LootrunTaskParticleVerifier implements ParticleVerifier {
    // The particles are a circle with a radius of 5 blocks
    private static final int RADIUS = 5;

    // The particles are not perfectly aligned, so we add a small error margin, 1 block should be enough
    private static final double CIRCLE_RADIUS_WITH_ERROR = RADIUS + 1d;

    @Override
    public boolean verifyNewPosition(List<Position> positions, Position addedPosition) {
        // We have no reference point, assume it's valid
        if (positions.isEmpty()) {
            // Left, Top, Right, Bottom particles are always on .5 or .0 on x and z axis
            return isParticlePrecise(addedPosition);
        }

        // Lootrun task particles are a circle, we get the packets in this order:
        // 1. Right particle
        // 2. Right to Top particles (4)
        // 3. Top particle
        // 4. Top to Left particles (4)
        // 5. Left particle
        // 6. Left to Bottom particles (4)
        // 7. Bottom particle
        // 8. Bottom to Right particles (4)

        // We verify particles by checking if they are in the radius of the circle
        Position rightParticle = positions.getFirst();

        Vec3 circleCenter = new Vec3(rightParticle.x() - RADIUS, rightParticle.y(), rightParticle.z());

        boolean isPartOfCircle = circleCenter.closerThan(addedPosition, CIRCLE_RADIUS_WITH_ERROR);
        if (!isPartOfCircle) return false;

        // Left, Top, Right, Bottom particles are always on .5 or .0 on x and z axis
        if (positions.size() % 5 == 0) {
            return isParticlePrecise(addedPosition);
        }

        return true;
    }

    @Override
    public VerificationResult verifyCompleteness(List<Position> positions) {
        // We only get two particles if the player trips the particle limit
        if (positions.size() == 2) {
            // The two particles are the right and left particles
            Position rightParticle = positions.get(0);
            Position leftParticle = positions.get(1);

            // Verify both particles are on .5 on x and z axis
            if (isParticlePrecise(rightParticle) && isParticlePrecise(leftParticle)) {
                // Verify the distance between the two particles is 10 blocks
                Vec3 rightParticleVec = new Vec3(rightParticle.x(), rightParticle.y(), rightParticle.z());
                Vec3 leftParticleVec = new Vec3(leftParticle.x(), leftParticle.y(), leftParticle.z());

                return rightParticleVec.distanceToSqr(leftParticleVec) == 100d
                        ? VerificationResult.VERIFIED
                        : VerificationResult.INVALID;
            }

            // We might still get a full circle
            return VerificationResult.UNVERIFIED;
        }

        // verifyNewPosition is quite strict, so we can just check the size
        if (positions.size() == 20) {
            return VerificationResult.VERIFIED;
        } else if (positions.size() > 20) {
            return VerificationResult.INVALID;
        }

        return VerificationResult.UNVERIFIED;
    }

    @Override
    public Particle getParticle(List<Position> positions) {
        Position rightParticle = positions.getFirst();

        Position verifiedParticlePosition = new Vec3(rightParticle.x() - RADIUS, rightParticle.y(), rightParticle.z());
        return new Particle(verifiedParticlePosition, ParticleType.LOOTRUN_TASK);
    }

    private static boolean isParticlePrecise(Position addedPosition) {
        return Math.abs(addedPosition.x() % 0.5d) == 0 && Math.abs(addedPosition.z() % 0.5d) == 0;
    }
}
