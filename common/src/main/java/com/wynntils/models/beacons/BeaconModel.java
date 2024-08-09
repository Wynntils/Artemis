/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.beacons;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.mc.event.SetEntityDataEvent;
import com.wynntils.mc.event.TeleportEntityEvent;
import com.wynntils.models.beacons.event.BeaconEvent;
import com.wynntils.models.beacons.type.Beacon;
import com.wynntils.models.beacons.type.LootrunBeaconKind;
import com.wynntils.utils.mc.McUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class BeaconModel extends Model {
    // Maps base entity id to corresponding beacon
    private final Map<Integer, Beacon> beacons = new Int2ObjectArrayMap<>();

    public BeaconModel() {
        super(List.of());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntitySetData(SetEntityDataEvent event) {
        Entity entity = McUtils.mc().level.getEntity(event.getId());
        if (!(entity instanceof Display.ItemDisplay)) return;

        SynchedEntityData.DataValue<?> dataValue = event.getPackedItems().stream()
                .filter(data -> data.id() == Display.ItemDisplay.DATA_ITEM_STACK_ID.id())
                .findFirst()
                .orElse(null);
        if (dataValue == null) return;

        ItemStack itemStack = (ItemStack) dataValue.value();

        // Try to identify the beacon kind, when the display item's data is set
        LootrunBeaconKind lootrunBeaconKind = LootrunBeaconKind.fromItemStack(itemStack);

        if (lootrunBeaconKind == null) return;

        Beacon beacon = new Beacon(entity.position(), lootrunBeaconKind);
        beacons.put(event.getId(), beacon);
        WynntilsMod.postEvent(new BeaconEvent.Added(beacon, entity));
    }

    @SubscribeEvent
    public void onEntityTeleport(TeleportEntityEvent event) {
        Beacon movedBeacon = beacons.get(event.getEntity().getId());
        if (movedBeacon == null) return;

        Beacon newBeacon = new Beacon(event.getNewPosition(), movedBeacon.color());
        // Replace the old map entry
        beacons.put(event.getEntity().getId(), newBeacon);
        WynntilsMod.postEvent(new BeaconEvent.Moved(movedBeacon, newBeacon));
    }

    @SubscribeEvent
    public void onEntityRemoved(RemoveEntitiesEvent event) {
        event.getEntityIds().stream().filter(beacons::containsKey).forEach(entityId -> {
            Beacon removedBeacon = beacons.get(entityId);
            beacons.remove(entityId);
            WynntilsMod.postEvent(new BeaconEvent.Removed(removedBeacon));
        });
    }
}
