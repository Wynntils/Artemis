/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.mc.type;

import com.wynntils.models.map.PoiLocation;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.mc.PosUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class Location {
    public final int x;
    public final int y;
    public final int z;

    public Location(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public Location(PoiLocation location) {
        this(location.getX(), location.getY().orElse(0), location.getZ());
    }

    public static Location containing(Position position) {
        return new Location(
                MathUtils.floor(position.x()), MathUtils.floor(position.y()), MathUtils.floor(position.z()));
    }

    public static Location containing(double x, double y, double z) {
        return new Location(MathUtils.floor(x), MathUtils.floor(y), MathUtils.floor(z));
    }

    public Location offset(int dx, int dy, int dz) {
        return new Location(this.x() + dx, this.y() + dy, this.z() + dz);
    }

    public BlockPos toBlockPos() {
        return PosUtils.newBlockPos(x, y, z);
    }

    public Vec3 toVec3() {
        return new Vec3(x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj instanceof Vector3d) {
            return super.equals(obj);
        }
        return false;
    }

    public String toString() {
        return "[" + (int) Math.round(this.x) + ", " + (int) Math.round(this.y) + ", " + (int) Math.round(this.z) + "]";
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }
}
