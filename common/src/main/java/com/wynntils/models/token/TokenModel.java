/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.token;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.event.EntityLabelEvent;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.models.inventory.InventoryWatcher;
import com.wynntils.models.items.items.game.MiscItem;
import com.wynntils.models.token.event.TokenGatekeeperEvent;
import com.wynntils.models.token.type.TokenGatekeeper;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.mc.PosUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.TimedSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.Position;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

public class TokenModel extends Model {
    private static final Pattern TOA_GATEKEEPER_NAME_PATTERN =
            Pattern.compile("^§2Floormaster \\[Floor (\\d+), Level (\\d+)\\]$");
    private static final Pattern HIVE_GATEKEEPER_NAME_PATTERN = Pattern.compile("^§2(.*) Catalyst Collector (\\d+)$");

    private static final Pattern TOKEN_PATTERN = Pattern.compile("^§a(\\d+)§2/(\\d+)(?:§r)?$");
    private static final Pattern TYPE_PATTERN = Pattern.compile("^§7Get §[e6]\\[(?:(\\d+) )?(.*)\\]$");
    private static final StyledText VERIFICATION_STRING = StyledText.fromString("§7Right-click to add");

    private final Map<Integer, TokenGatekeeper> activeGatekeepers = new HashMap<>();
    private final Map<TokenGatekeeper, TokenInventoryWatcher> inventoryWatchers = new HashMap<>();
    private final TimedSet<BakingTokenGatekeeper> bakingGatekeepers = new TimedSet<>(5, TimeUnit.SECONDS, true);
    private final Map<Integer, TokenGatekeeper> invisibleGatekeepers = new HashMap<>();

    public TokenModel() {
        super(List.of());
    }

    public List<TokenGatekeeper> getGatekeepers() {
        return activeGatekeepers.values().stream().sorted().toList();
    }

    public int inInventory(TokenGatekeeper gatekeeper) {
        TokenInventoryWatcher watcher = inventoryWatchers.get(gatekeeper);
        // The null case should not happen, but just to be safe
        int inventoryCount = watcher != null ? watcher.getTotalCount() : 0;
        return inventoryCount;
    }

    public CappedValue getCollected(TokenGatekeeper gatekeeper) {
        TokenInventoryWatcher watcher = inventoryWatchers.get(gatekeeper);
        // The null case should not happen, but just to be safe
        int inventoryCount = watcher != null ? watcher.getTotalCount() : 0;

        CappedValue deposited = gatekeeper.getDeposited();
        int total = Math.min(deposited.current() + inventoryCount, deposited.max());
        return new CappedValue(total, deposited.max());
    }

    @SubscribeEvent
    public void onLabelChange(EntityLabelEvent.Changed event) {
        if (!(event.getEntity() instanceof ArmorStand)) return;

        StyledText name = event.getName();

        Matcher typeMatcher = name.getMatcher(TYPE_PATTERN);
        if (typeMatcher.matches()) {
            String countString = typeMatcher.group(1);
            int max = countString != null ? Integer.parseInt(countString) : 1;
            StyledText type = StyledText.fromString(typeMatcher.group(2));

            BakingTokenGatekeeper baking = getBaking(event.getEntity().position());
            baking.type = type;
            // If the gatekeeper requires only a single item, the token line might be
            // missing. If so, use the type line as entity instead
            baking.typeMax = max;
            if (baking.valueEntityId == 0) {
                baking.valueEntityId = event.getEntity().getId();
            }
            checkAndPromoteBaking();
            return;
        }

        Matcher tokensMatcher = name.getMatcher(TOKEN_PATTERN);
        if (tokensMatcher.matches()) {
            CappedValue tokens =
                    new CappedValue(Integer.parseInt(tokensMatcher.group(1)), Integer.parseInt(tokensMatcher.group(2)));

            int id = event.getEntity().getId();
            TokenGatekeeper gatekeeper = activeGatekeepers.get(id);
            if (gatekeeper != null) {
                // Update active gatekeeper
                gatekeeper.setDeposited(tokens);
                WynntilsMod.postEvent(new TokenGatekeeperEvent.Deposited(gatekeeper));
                return;
            }

            TokenGatekeeper invisibleGatekeeper = invisibleGatekeepers.get(id);
            if (invisibleGatekeeper != null) {
                // We need to keep invisible gatekeepers up to date if they suddenly become visible
                invisibleGatekeeper.setDeposited(tokens);
                return;
            }

            // Create new baking gatekeeper
            BakingTokenGatekeeper baking = getBaking(event.getEntity().position());
            baking.value = tokens;
            baking.valueEntityId = event.getEntity().getId();
            checkAndPromoteBaking();
            return;
        }

        // This does not provide any new information for the TokenGatekeeper, but it is a good
        // extra check that we really match only on real gatekeepers
        if (name.equals(VERIFICATION_STRING)) {
            BakingTokenGatekeeper baking = getBaking(event.getEntity().position());
            baking.confirmed = true;
            checkAndPromoteBaking();
            return;
        }

        Matcher toaMatcher = name.getMatcher(TOA_GATEKEEPER_NAME_PATTERN);
        if (toaMatcher.matches()) {
            int floor = Integer.parseInt(toaMatcher.group(1));
            int level = Integer.parseInt(toaMatcher.group(2));
            int maxTokens = level == 10 ? 1 : 5;
            Location location =
                    Location.containing(event.getEntity().position()).offset(0, 3, 0);

            StyledText gatekeeperTokenName = StyledText.fromString("Shard [Floor " + floor + " - Level " + level + "]");
            StyledText itemName = StyledText.fromString("§d[Floor " + floor + " - Lv. " + level + "]");
            addGatekeeper(
                    event.getEntity().getId(),
                    new TokenGatekeeper(gatekeeperTokenName, itemName, location, new CappedValue(0, maxTokens)));
        }

        Matcher hiveMatcher = name.getMatcher(HIVE_GATEKEEPER_NAME_PATTERN);
        if (hiveMatcher.matches()) {
            String division = hiveMatcher.group(1);
            int level = Integer.parseInt(hiveMatcher.group(2));
            int maxTokens = level == 10 ? 1 : 5;
            Location location =
                    Location.containing(event.getEntity().position()).offset(0, 3, 0);

            StyledText tokenName = StyledText.fromString(division + " Catalyst " + MathUtils.toRoman(level));
            addGatekeeper(
                    event.getEntity().getId(), new TokenGatekeeper(tokenName, location, new CappedValue(0, maxTokens)));
        }
    }

    @SubscribeEvent
    public void onLabelVisibility(EntityLabelEvent.Visibility event) {
        if (!event.getVisibility()) {
            // This is the normal way in which gatekeepers are "removed" when done
            int id = event.getEntity().getId();
            TokenGatekeeper gatekeeper = activeGatekeepers.get(id);
            if (gatekeeper == null) return;

            removeGatekeeper(id, gatekeeper);
            invisibleGatekeepers.put(id, gatekeeper);
        } else {
            // It became visible again; restore it
            int id = event.getEntity().getId();
            TokenGatekeeper gatekeeper = invisibleGatekeepers.get(id);
            if (gatekeeper == null) return;

            invisibleGatekeepers.remove(id);
            addGatekeeper(id, gatekeeper);
        }
    }

    // This happens if we log out, switch class, etc
    @SubscribeEvent
    public void onEntitiesRemoved(RemoveEntitiesEvent event) {
        for (int id : event.getEntityIds()) {
            if (activeGatekeepers.containsKey(id)) {
                removeGatekeeper(id, activeGatekeepers.get(id));
            }
            invisibleGatekeepers.remove(id);
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldStateEvent event) {
        inventoryWatchers.values().forEach(Models.Inventory::unregisterWatcher);

        Set<TokenGatekeeper> oldGatekeepers = new HashSet<>(activeGatekeepers.values());

        activeGatekeepers.clear();
        invisibleGatekeepers.clear();
        bakingGatekeepers.clear();
        inventoryWatchers.clear();

        // Event is fired last, to make sure the gatekeepers are cleared
        oldGatekeepers.forEach((gatekeeper) -> WynntilsMod.postEvent(new TokenGatekeeperEvent.Removed(gatekeeper)));
    }

    private void addGatekeeper(int entityId, TokenGatekeeper gatekeeper) {
        TokenInventoryWatcher watcher = new TokenInventoryWatcher(gatekeeper);
        inventoryWatchers.put(gatekeeper, watcher);
        Models.Inventory.registerWatcher(watcher);
        activeGatekeepers.put(entityId, gatekeeper);

        WynntilsMod.postEvent(new TokenGatekeeperEvent.Added(gatekeeper));
    }

    private void removeGatekeeper(int entityId, TokenGatekeeper gatekeeper) {
        activeGatekeepers.remove(entityId);
        InventoryWatcher watcher = inventoryWatchers.get(gatekeeper);
        Models.Inventory.unregisterWatcher(watcher);
        inventoryWatchers.remove(gatekeeper);

        WynntilsMod.postEvent(new TokenGatekeeperEvent.Removed(gatekeeper));
    }

    private BakingTokenGatekeeper getBaking(Position position) {
        for (BakingTokenGatekeeper baking : bakingGatekeepers) {
            if (PosUtils.isSame(position, baking.position)) {
                return baking;
            }
        }
        BakingTokenGatekeeper baking = new BakingTokenGatekeeper(position);
        bakingGatekeepers.put(baking);
        return baking;
    }

    private void checkAndPromoteBaking() {
        Iterator<BakingTokenGatekeeper> iter = bakingGatekeepers.iterator();
        while (iter.hasNext()) {
            BakingTokenGatekeeper baking = iter.next();

            if (baking.valueEntityId == 0 || baking.type == null) continue;

            // If just a single item is requested, we might not have the
            // "right click to add" nor the token count line
            if ((baking.confirmed && baking.value != null) || (baking.typeMax == 1)) {
                iter.remove();

                addGatekeeper(baking.valueEntityId, baking.toGatekeeper());
            }
        }
    }

    private static final class BakingTokenGatekeeper {
        private final Position position;
        private StyledText type;
        private int typeMax;
        private CappedValue value;
        private int valueEntityId;
        private boolean confirmed;

        private BakingTokenGatekeeper(Position position) {
            this.position = position;
        }

        private TokenGatekeeper toGatekeeper() {
            Location location = Location.containing(position).offset(0, 3, 0);
            if (typeMax == 1) {
                // If only a single item is requested, the normal value is not present
                return new TokenGatekeeper(type, location, new CappedValue(0, 1));
            } else {
                return new TokenGatekeeper(type, location, value);
            }
        }
    }

    private static final class TokenInventoryWatcher extends InventoryWatcher {
        private final TokenGatekeeper gatekeeper;

        private TokenInventoryWatcher(TokenGatekeeper gatekeeper, StyledText tokenItemName) {
            super(itemStack -> isToken(tokenItemName, itemStack));
            this.gatekeeper = gatekeeper;
        }

        private TokenInventoryWatcher(TokenGatekeeper gatekeeper) {
            this(gatekeeper, gatekeeper.getItemTokenName());
        }

        private static boolean isToken(StyledText tokenItemName, ItemStack itemStack) {
            Optional<MiscItem> miscItemOpt = Models.Item.asWynnItem(itemStack, MiscItem.class);
            if (miscItemOpt.isEmpty()) return false;

            MiscItem miscItem = miscItemOpt.get();
            if (!miscItem.isUntradable()) return false;

            return miscItem.getName().contains(tokenItemName);
        }

        @Override
        protected void onUpdate(int oldSlots, int oldTotalCount) {
            WynntilsMod.postEvent(
                    new TokenGatekeeperEvent.InventoryUpdated(gatekeeper, this.getTotalCount(), oldTotalCount));
        }
    }
}
