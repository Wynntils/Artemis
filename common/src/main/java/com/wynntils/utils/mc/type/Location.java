/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.mc.type;

import com.wynntils.utils.MathUtils;
import com.wynntils.utils.mc.PosUtils;
import java.util.Comparator;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;

public class Location implements Comparable<Location> {
    // Compare first x, then z, and finally y
    private static final Comparator<Location> LOCATION_COMPARATOR = Comparator.comparing(
                    Location::x, Integer::compareTo)
            .thenComparing(Location::z, Integer::compareTo)
            .thenComparing(Location::y, Integer::compareTo);

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

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
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

    public Position toPosition() {
        return new Vec3(x, y, z);
    }

    public boolean equalsIgnoringY(Location other) {
        return this.x() == other.x() && this.z() == other.z();
    }

    public double distanceToSqr(Position position) {
        double x_diff = position.x() - this.x;
        double y_diff = position.y() - this.y;
        double z_diff = position.z() - this.z;

        return x_diff * x_diff + y_diff * y_diff + z_diff * z_diff;
    }

    public String asChatCoordinates() {
        return x + " " + y + " " + z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return x == location.x && y == location.y && z == location.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.y + ", " + this.z + "]";
    }

    @Override
    public int compareTo(Location that) {
        return LOCATION_COMPARATOR.compare(this, that);
    }
}
