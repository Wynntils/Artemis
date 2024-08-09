/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilities;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.event.TextDisplayChangedEvent;
import com.wynntils.mc.event.AddEntityEvent;
import com.wynntils.mc.event.ChangeCarriedItemEvent;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.models.abilities.event.TotemEvent;
import com.wynntils.models.abilities.type.ShamanTotem;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.models.spells.event.SpellEvent;
import com.wynntils.models.spells.type.SpellType;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;

public class ShamanTotemModel extends Model {
    // Test in ShamanTotemModel_SHAMAN_TOTEM_TIMER_PATTERN
    private static final Pattern SHAMAN_TOTEM_TIMER = Pattern.compile("§c(?<time>\\d+)s(\n\\+(?<regen>\\d+)❤§7/s)?");
    private static final int MAX_TOTEM_COUNT = 4;
    private static final double TOTEM_SEARCH_RADIUS = 1;
    private static final int TOTEM_DATA_DELAY_TICKS = 2;
    private static final int CAST_MAX_DELAY_MS = 240;
    // TODO: CAST_MAX_DELAY could be a config when model configs eventually exist
    // it kind of depends on ping and server lag

    private final ShamanTotem[] totems = new ShamanTotem[MAX_TOTEM_COUNT];
    private final Integer[] pendingTotemVisibleIds = new Integer[MAX_TOTEM_COUNT];
    private int nextTotemSlot = 1;
    private long totemCastTimestamp = 0;

    public ShamanTotemModel() {
        super(List.of());
    }

    @SubscribeEvent
    public void onTotemSpellCast(SpellEvent.Cast e) {
        if (e.getSpellType() != SpellType.TOTEM) return;
        totemCastTimestamp = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onTotemSpawn(AddEntityEvent e) {
        Entity entity = getBufferedEntity(e.getId());
        if (!(entity instanceof ArmorStand totemAS)) return;

        if (!isClose(totemAS.position(), McUtils.mc().player.position())) return;

        Managers.TickScheduler.scheduleLater(
                () -> {
                    // didn't come from a cast within the delay, probably not casted by the player
                    // this check needs to be ran with a delay, the cast/spawn order is not guaranteed
                    if (System.currentTimeMillis() - totemCastTimestamp > CAST_MAX_DELAY_MS) return;

                    // Checks to verify this is a totem
                    // These must be ran with a delay,
                    // inventory contents are set a couple ticks after the totem actually spawns
                    List<ItemStack> inv = new ArrayList<>();
                    totemAS.getArmorSlots().forEach(inv::add);

                    if (inv.size() < 4) return;

                    ItemStack data = inv.get(3);
                    if (data.getItem() != Items.STONE_SHOVEL) return;

                    // This relies on the fact that damage values 28 (Shaman) and 29 (Skyseer) on the stone shovel set
                    // the totem's texture
                    if (data.getDamageValue() != 28 && data.getDamageValue() != 29) return;

                    // Checks complete, this is a valid totem
                    int totemNumber = getNextTotemSlot();

                    WynntilsMod.postEvent(new TotemEvent.Summoned(totemNumber, totemAS));

                    ShamanTotem newTotem = new ShamanTotem(
                            totemNumber, -1, totemAS.getId(), -1, ShamanTotem.TotemState.SUMMONED, totemAS.position());

                    totems[totemNumber - 1] = newTotem;
                    pendingTotemVisibleIds[totemNumber - 1] = totemAS.getId();
                },
                TOTEM_DATA_DELAY_TICKS);
    }

    @SubscribeEvent
    public void onTimerSpawn(AddEntityEvent e) {
        // We aren't looking for a new timer, skip
        if (Arrays.stream(pendingTotemVisibleIds).allMatch(Objects::isNull)) return;
        int entityId = e.getId();

        // This timer is already bound to a totem but got respawned? skip
        if (getBoundTotem(entityId) != null) return;

        Entity possibleTimer = getBufferedEntity(entityId);
        if (!(possibleTimer instanceof Display.TextDisplay)) return;

        // Given timerId is not a totem, make a new totem
        List<ArmorStand> possibleTotems = McUtils.mc()
                .level
                .getEntitiesOfClass(
                        ArmorStand.class,
                        new AABB(
                                possibleTimer.position().x - TOTEM_SEARCH_RADIUS,
                                possibleTimer.position().y
                                        - TOTEM_SEARCH_RADIUS, // Don't modify this unless you are certain it is causing
                                // issues
                                possibleTimer.position().z - TOTEM_SEARCH_RADIUS,
                                possibleTimer.position().x + TOTEM_SEARCH_RADIUS,
                                // (a LOT) more vertical radius required for totems casted off high places
                                // This increased radius requirement increases as you cast from higher places and as the
                                // server gets laggier
                                possibleTimer.position().y + TOTEM_SEARCH_RADIUS * 5,
                                possibleTimer.position().z + TOTEM_SEARCH_RADIUS));

        for (ArmorStand possibleTotem : possibleTotems) {
            // Recreate position for each ArmorStand checked for most accurate coordinates
            Position position = possibleTotem.position();

            for (int i = 0; i < pendingTotemVisibleIds.length; i++) {
                if (pendingTotemVisibleIds[i] != null && possibleTotem.getId() == pendingTotemVisibleIds[i]) {
                    // we found the totem that this timer belongs to, bind it
                    ShamanTotem totem = totems[i];

                    totem.setTimerEntityId(entityId);
                    totem.setPosition(position);
                    totem.setState(ShamanTotem.TotemState.ACTIVE);

                    WynntilsMod.postEvent(new TotemEvent.Activated(totem.getTotemNumber(), position));

                    pendingTotemVisibleIds[i] = null;

                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onTotemRename(TextDisplayChangedEvent.Text event) {
        if (!Models.WorldState.onWorld()) return;

        Display.TextDisplay textDisplay = event.getTextDisplay();

        StyledText name = event.getText();
        if (name.isEmpty()) return;
        Matcher m = name.getMatcher(SHAMAN_TOTEM_TIMER);
        if (!m.find()) return;

        int parsedTime = Integer.parseInt(m.group("time"));
        Position position = textDisplay.position();

        int entityId = textDisplay.getId();
        if (getBoundTotem(entityId) == null) return;

        ShamanTotem boundTotem = getBoundTotem(entityId);

        if (boundTotem == null) return;

        for (ShamanTotem totem : totems) {
            if (boundTotem == totem) {
                totem.setTime(parsedTime);
                totem.setPosition(position);

                WynntilsMod.postEvent(new TotemEvent.Updated(totem.getTotemNumber(), parsedTime, position));

                break;
            }
        }
    }

    @SubscribeEvent
    public void onTotemDestroy(RemoveEntitiesEvent e) {
        if (!Models.WorldState.onWorld()) return;

        List<Integer> destroyedEntities = e.getEntityIds();

        for (ShamanTotem totem : totems) {
            if (totem != null
                    && (destroyedEntities.contains(totem.getTimerEntityId())
                            || destroyedEntities.contains(totem.getVisibleEntityId()))) {
                removeTotem(totem.getTotemNumber());
            }
        }
    }

    @SubscribeEvent
    public void onClassChange(CharacterUpdateEvent e) {
        removeAllTotems();
    }

    @SubscribeEvent
    public void onHeldItemChange(ChangeCarriedItemEvent e) {
        removeAllTotems();
    }

    private Entity getBufferedEntity(int entityId) {
        Entity entity = McUtils.mc().level.getEntity(entityId);
        if (entity != null) return entity;

        if (entityId == -1) {
            return new ArmorStand(McUtils.mc().level, 0, 0, 0);
        }

        return null;
    }

    /**
     * Removes the given totem from the list of totems.
     * @param totem The totem to remove. Must be 1, 2, 3 or 4
     */
    private void removeTotem(int totem) {
        WynntilsMod.postEvent(new TotemEvent.Removed(totem, totems[totem - 1]));
        totems[totem - 1] = null;
        pendingTotemVisibleIds[totem - 1] = null;
        nextTotemSlot = totem;
    }

    /**
     * Resets all three totem variables.
     */
    private void removeAllTotems() {
        for (int i = 1; i <= MAX_TOTEM_COUNT; i++) {
            removeTotem(i);
        }

        nextTotemSlot = 1;
    }

    private int getNextTotemSlot() {
        int toReturn = nextTotemSlot;

        if (nextTotemSlot == MAX_TOTEM_COUNT) {
            nextTotemSlot = 1;
        } else {
            nextTotemSlot += 1;
        }

        return toReturn;
    }

    /**
     * Gets the totem bound to the given timerId.
     * @param timerId The timerId that is checked against the totems
     * @return The totem bound to the given timerId, or null if no totem is bound
     */
    private ShamanTotem getBoundTotem(int timerId) {
        for (ShamanTotem totem : totems) {
            if (totem != null && totem.getTimerEntityId() == timerId) {
                return totem;
            }
        }

        return null;
    }

    private boolean isClose(Position pos1, Position pos2) {
        LocalPlayer player = McUtils.player();
        double dX = player.getX() - player.xOld;
        double dZ = player.getZ() - player.zOld;
        double dY = player.getY() - player.yOld;
        double speedMultiplier = Math.sqrt((dX * dX) + (dZ * dZ) + (dY * dY)) * 20;
        // wynn never casts perfectly aligned totems
        speedMultiplier = Math.max(speedMultiplier, 1);

        return Math.abs(pos1.x() - pos2.x()) < speedMultiplier
                && Math.abs(pos1.y() - pos2.y()) < speedMultiplier
                && Math.abs(pos1.z() - pos2.z()) < speedMultiplier;
    }

    public List<ShamanTotem> getActiveTotems() {
        return Arrays.stream(totems).filter(Objects::nonNull).toList();
    }

    public ShamanTotem getTotem(int totemNumber) {
        if (totemNumber < 1 || totemNumber > MAX_TOTEM_COUNT) {
            return null;
        }

        return totems[totemNumber - 1];
    }
}
