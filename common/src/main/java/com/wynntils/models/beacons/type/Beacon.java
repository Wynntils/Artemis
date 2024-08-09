/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.beacons.type;

import net.minecraft.world.phys.Vec3;

public record Beacon(Vec3 position, LootrunBeaconKind color) {}
