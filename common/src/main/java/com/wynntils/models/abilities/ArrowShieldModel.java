/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilities;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.mc.event.AddEntityEvent;
import com.wynntils.mc.event.ChangeCarriedItemEvent;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.models.abilities.event.ArrowShieldEvent;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.models.spells.event.SpellEvent;
import com.wynntils.models.spells.type.SpellType;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;

public class ArrowShieldModel extends Model {
    private List<Integer> collectedArrowIds;
    private List<Integer> spawnedArrowIds;

    private static final double ARROW_SEARCH_RADIUS = 4.5;

    public ArrowShieldModel() {
        super(List.of());
    }

    @SubscribeEvent
    public void onArrowShieldSpellCast(SpellEvent.Completed e) {
        if (e.getSpell() != SpellType.ARROW_SHIELD) return;

        collectedArrowIds = new ArrayList<>();
        spawnedArrowIds = null;
        // Give the server (incl lag)  8 ticks (400 ms) to spawn all arrows
        Managers.TickScheduler.scheduleLater(this::registerShield, 8);
    }

    @SubscribeEvent
    public void onArrowSpawn(AddEntityEvent event) {
        // If we're not collecting arrows, do nothing.
        if (collectedArrowIds == null) return;

        Entity entity = McUtils.mc().level.getEntity(event.getId());
        if (entity == null) return;
        if (!(entity instanceof ArmorStand arrowAS)) return;

        Vec3 playerPos = McUtils.player().position();
        Managers.TickScheduler.scheduleLater(
                () -> {
                    // Verify that this is an armor stand holding an arrow. This must be ran with
                    // a delay, as inventory contents are set a couple ticks after the entity spawns.
                    ItemStack heldItem = arrowAS.getMainHandItem();
                    if (!heldItem.getItem().equals(Items.ARROW)) return;

                    // If the player is standing still, the armor stands spawn about 2.1 blocks away
                    // from the player. But if the player moves, it can be up to ~ 4 blocks depending
                    // on walk speed.
                    if (arrowAS.position().distanceTo(playerPos) > ARROW_SEARCH_RADIUS) return;

                    // Save field in local variable to avoid surprises where it is overwritten by null
                    List<Integer> collector = collectedArrowIds;
                    // If we're not collecting arrows, do nothing.
                    if (collector == null) return;

                    collector.add(arrowAS.getId());
                },
                3);
    }

    @SubscribeEvent
    public void onArrowDestroy(RemoveEntitiesEvent event) {
        if (spawnedArrowIds == null) return;

        for (Integer destroyedEntityId : event.getEntityIds()) {
            spawnedArrowIds.stream()
                    .filter(id -> Objects.equals(id, destroyedEntityId))
                    .findFirst()
                    .ifPresent(id -> {
                        spawnedArrowIds.remove(id);
                        WynntilsMod.postEvent(new ArrowShieldEvent.Degraded(spawnedArrowIds.size()));
                    });
        }

        if (spawnedArrowIds.isEmpty()) {
            removeShield();
        }
    }

    @SubscribeEvent
    public void onClassChange(CharacterUpdateEvent e) {
        removeShield();
    }

    @SubscribeEvent
    public void onHeldItemChange(ChangeCarriedItemEvent e) {
        removeShield();
    }

    @SubscribeEvent
    public void onWorldStateChanged(WorldStateEvent e) {
        removeShield();
    }

    public int getArrowShieldCharge() {
        return spawnedArrowIds == null ? 0 : spawnedArrowIds.size();
    }

    private void registerShield() {
        if (collectedArrowIds != null) {
            spawnedArrowIds = collectedArrowIds;
            collectedArrowIds = null;
            WynntilsMod.postEvent(new ArrowShieldEvent.Created(spawnedArrowIds.size()));
        }
    }

    private void removeShield() {
        spawnedArrowIds = null;
        WynntilsMod.postEvent(new ArrowShieldEvent.Removed());
    }
}
