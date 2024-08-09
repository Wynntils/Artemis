/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.wynn;

import com.wynntils.utils.mc.McUtils;
import java.util.Optional;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class RaycastUtils {
    private static final float RAYCAST_RANGE = 5f;

    public static Optional<Player> getHoveredPlayer() {
        LocalPlayer player = McUtils.player();

        Vec3 start = player.getEyePosition(1f);
        Vec3 look = player.getLookAngle();
        Vec3 direction = start.add(look.x * RAYCAST_RANGE, look.y * RAYCAST_RANGE, look.z * RAYCAST_RANGE);
        AABB bb = player.getBoundingBox()
                .expandTowards(look.x * RAYCAST_RANGE, look.y * RAYCAST_RANGE, look.z * RAYCAST_RANGE)
                .expandTowards(1, 1, 1);

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                McUtils.mc().level, player, start, direction, bb, (e) -> e instanceof Player);

        if (hitResult == null) return Optional.empty();

        return Optional.of((Player) hitResult.getEntity());
    }
}
