/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.character;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.container.scriptedquery.QueryBuilder;
import com.wynntils.handlers.container.scriptedquery.QueryStep;
import com.wynntils.handlers.container.scriptedquery.ScriptedContainerQuery;
import com.wynntils.handlers.container.type.ContainerContent;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.MenuEvent.MenuClosedEvent;
import com.wynntils.mc.event.PlayerTeleportEvent;
import com.wynntils.models.character.event.CharacterDeathEvent;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.containers.ContainerModel;
import com.wynntils.models.items.items.gui.CharacterItem;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.ConfirmedBoolean;
import com.wynntils.utils.wynn.InventoryUtils;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.Position;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class CharacterModel extends Model {
    private static final Pattern CHARACTER_ID_PATTERN = Pattern.compile("^[a-z0-9]{8}$");
    private static final Pattern INFO_MENU_CLASS_PATTERN = Pattern.compile("§7Class: §f(.+)");
    private static final Pattern INFO_MENU_LEVEL_PATTERN = Pattern.compile("§7Combat Lv: §f(\\d+)");
    // Test in CharacterModel_SILVERBULL_PATTERN
    private static final Pattern SILVERBULL_PATTERN = Pattern.compile("§7Subscription: §[ac][✖✔] ((?:Ina|A)ctive)");
    // Test in CharacterModel_SILVERBULL_DURATION_PATTERN
    private static final Pattern SILVERBULL_DURATION_PATTERN = Pattern.compile(
            "§7Expiration: §f(?:(?<weeks>\\d+) weeks?)? ?(?:(?<days>\\d+) days?)? ?(?:(?<hours>\\d+) hours?)? ?(?:(?<minutes>\\d+) minutes?)? ?(?:(?<seconds>\\d+) seconds?)?");
    // Test in CharacterModel_VETERAN_PATTERN
    private static final Pattern VETERAN_PATTERN = Pattern.compile("§7Rank: §[6dba]Vet");
    private static final Pattern SILVERBULL_JOIN_PATTERN =
            Pattern.compile("§3Welcome to the §b✮ Silverbull Trading Company§3!");
    private static final Pattern SILVERBULL_UPDATE_PATTERN = Pattern.compile("§7Your subscription has been extended.");

    private static final int RANK_SUBSCRIPTION_INFO_SLOT = 0;
    public static final int CHARACTER_INFO_SLOT = 7;
    private static final int PROFESSION_INFO_SLOT = 17;
    private static final int COSMETICS_SLOT = 25;
    private static final int COSMETICS_BACK_SLOT = 9;
    public static final int GUILD_MENU_SLOT = 26;

    // we need a .* in front because the message may have a custom timestamp prefix (or some other mod could do
    // something weird)
    private static final Pattern WYNN_DEATH_MESSAGE = Pattern.compile(".*§4§lYou have died\\.\\.\\.");
    private Position lastPositionBeforeTeleport;
    private Location lastDeathLocation;

    private boolean inCharacterSelection;
    private boolean hasCharacter;

    private ClassType classType;
    private boolean reskinned;
    private int level;

    @Persisted
    private final Storage<Boolean> isVeteran = new Storage<>(false);

    @Persisted
    private final Storage<Long> silverbullExpiresAt = new Storage<>(0L);

    @Persisted
    private final Storage<ConfirmedBoolean> silverbullSubscriber = new Storage<>(ConfirmedBoolean.UNCONFIRMED);

    // A hopefully unique string for each character ("class"). This is part of the
    // full character uuid, as presented by Wynncraft in the tooltip.
    private String id = "-";

    public CharacterModel() {
        super(List.of());
    }

    public boolean isSilverbullSubscriber() {
        return silverbullSubscriber.get() == ConfirmedBoolean.TRUE;
    }

    public ClassType getClassType() {
        if (!hasCharacter) return ClassType.NONE;

        return classType;
    }

    public boolean isReskinned() {
        if (!hasCharacter) return false;

        return reskinned;
    }

    /** Returns the current class name, wrt reskinned or not.
     */
    public String getActualName() {
        return getClassType().getActualName(isReskinned());
    }

    public boolean hasCharacter() {
        return hasCharacter;
    }

    public String getId() {
        // We can't return an empty string, otherwise we risk making our config file messed up (empty string map key for
        // ItemLockFeature)
        if (!hasCharacter) return "-";

        return id;
    }

    public boolean isVeteran() {
        return isVeteran.get();
    }

    // FIXME: Remove if this is not needed, or fix it for 2.1
    public boolean isHuntedMode() {
        return false;
    }

    @SubscribeEvent
    public void onMenuClosed(MenuClosedEvent e) {
        inCharacterSelection = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldStateChanged(WorldStateEvent e) {
        // Whenever we're leaving a world, clear the current character
        if (e.getOldState() == WorldState.WORLD) {
            hasCharacter = false;
            // This should not be needed, but have it as a safeguard
            inCharacterSelection = false;
        }

        if (e.getNewState() == WorldState.CHARACTER_SELECTION) {
            inCharacterSelection = true;
        }

        if (e.getNewState() == WorldState.WORLD) {
            // We need to parse the current character id from our inventory
            updateCharacterId();

            // We need to scan character info and profession info as well.
            scanCharacterInfo(e.isFirstJoinWorld());
        }
    }

    @SubscribeEvent
    public void onChatReceived(ChatMessageReceivedEvent e) {
        StyledText message = e.getOriginalStyledText();

        if (message.matches(WYNN_DEATH_MESSAGE)) {
            lastDeathLocation = Location.containing(lastPositionBeforeTeleport);
            CharacterDeathEvent deathEvent = new CharacterDeathEvent(lastDeathLocation);
            WynntilsMod.postEvent(deathEvent);

            if (deathEvent.isCanceled()) {
                e.setCanceled(true);
            }
            return;
        }

        StyledText trimmedMessage = message.trim();
        if (trimmedMessage.matches(SILVERBULL_JOIN_PATTERN)) {
            silverbullSubscriber.store(ConfirmedBoolean.TRUE);
            return;
        }

        if (trimmedMessage.matches(SILVERBULL_UPDATE_PATTERN)) {
            silverbullSubscriber.store(ConfirmedBoolean.TRUE);
            return;
        }
    }

    @SubscribeEvent
    public void beforePlayerTeleport(PlayerTeleportEvent e) {
        if (McUtils.player() == null) return;
        lastPositionBeforeTeleport = McUtils.player().position();
    }

    @SubscribeEvent
    public void onContainerClick(ContainerClickEvent e) {
        if (inCharacterSelection) {
            if (!parseCharacter(e.getItemStack())) return;
            hasCharacter = true;
            WynntilsMod.postEvent(new CharacterUpdateEvent());
            WynntilsMod.info("Selected character " + getCharacterString());
        }
    }

    public void scanCharacterInfo(boolean forceParseEverything) {
        WynntilsMod.info("Scheduling character info query");
        QueryBuilder queryBuilder = ScriptedContainerQuery.builder("Character Info Query");
        queryBuilder.onError(msg -> WynntilsMod.warn("Error querying Character Info: " + msg));

        // Open compass/character menu
        queryBuilder.then(QueryStep.useItemInHotbar(InventoryUtils.COMPASS_SLOT_NUM)
                .expectContainerTitle(ContainerModel.CHARACTER_INFO_NAME)
                .processIncomingContainer(this::parseCharacterContainer));

        if (forceParseEverything
                || silverbullSubscriber.get() == ConfirmedBoolean.UNCONFIRMED
                || (silverbullSubscriber.get() != ConfirmedBoolean.FALSE
                        && System.currentTimeMillis() > silverbullExpiresAt.get())) {
            // Open Cosmetics Menu
            queryBuilder
                    .then(QueryStep.clickOnSlot(COSMETICS_SLOT)
                            .expectContainerTitle(ContainerModel.COSMETICS_MENU_NAME)
                            .processIncomingContainer(this::parseCratesBombsCosmeticsContainer))
                    .then(QueryStep.clickOnSlot(COSMETICS_BACK_SLOT)
                            .expectContainerTitle(ContainerModel.CHARACTER_INFO_NAME));
        } else {
            WynntilsMod.info("Skipping silverbull subscription query ("
                    + (silverbullExpiresAt.get() - System.currentTimeMillis()) + " ms left)");
        }

        // Scan guild container, if the player is in a guild
        Models.Guild.addGuildContainerQuerySteps(queryBuilder);

        queryBuilder.build().executeQuery();
    }

    private void parseCharacterContainer(ContainerContent container) {
        ItemStack characterInfoItem = container.items().get(CHARACTER_INFO_SLOT);
        ItemStack professionInfoItem = container.items().get(PROFESSION_INFO_SLOT);
        ItemStack guildInfoItem = container.items().get(GUILD_MENU_SLOT);

        Models.Profession.resetValueFromItem(professionInfoItem);
        Models.Guild.parseGuildInfoFromGuildMenu(guildInfoItem);

        parseCharacterFromCharacterMenu(characterInfoItem);
        hasCharacter = true;
        WynntilsMod.postEvent(new CharacterUpdateEvent());
        WynntilsMod.info("Deducing character " + getCharacterString());
    }

    private void parseCratesBombsCosmeticsContainer(ContainerContent container) {
        ItemStack rankSubscriptionItem = container.items().get(RANK_SUBSCRIPTION_INFO_SLOT);

        Matcher veteran = LoreUtils.matchLoreLine(rankSubscriptionItem, 0, VETERAN_PATTERN);

        isVeteran.store(veteran.matches());

        Matcher status = LoreUtils.matchLoreLine(rankSubscriptionItem, 0, SILVERBULL_PATTERN);
        if (!status.matches()) {
            WynntilsMod.warn("Could not parse Silverbull subscription status from item: "
                    + LoreUtils.getLore(rankSubscriptionItem));
            silverbullSubscriber.store(ConfirmedBoolean.FALSE);
            return;
        }

        silverbullSubscriber.store(status.group(1).equals("Active") ? ConfirmedBoolean.TRUE : ConfirmedBoolean.FALSE);
        if (silverbullSubscriber.get() != ConfirmedBoolean.TRUE) return;

        Matcher expiry = LoreUtils.matchLoreLine(rankSubscriptionItem, 1, SILVERBULL_DURATION_PATTERN);
        if (!expiry.matches()) {
            WynntilsMod.warn("Could not parse Silverbull subscription expiry from item: "
                    + LoreUtils.getLore(rankSubscriptionItem));
            silverbullExpiresAt.store(0L);
            return;
        }
        int weeks = expiry.group("weeks") == null ? 0 : Integer.parseInt(expiry.group("weeks"));
        int days = expiry.group("days") == null ? 0 : Integer.parseInt(expiry.group("days"));
        int hours = expiry.group("hours") == null ? 0 : Integer.parseInt(expiry.group("hours"));
        int minutes = expiry.group("minutes") == null ? 0 : Integer.parseInt(expiry.group("minutes"));
        int seconds = expiry.group("seconds") == null ? 0 : Integer.parseInt(expiry.group("seconds"));

        long expiryTime = System.currentTimeMillis()
                + TimeUnit.DAYS.toMillis(weeks * 7L + days)
                + TimeUnit.HOURS.toMillis(hours)
                + TimeUnit.MINUTES.toMillis(minutes)
                + TimeUnit.SECONDS.toMillis(seconds);
        silverbullExpiresAt.store(expiryTime);

        WynntilsMod.info(
                "Parsed Silverbull subscription status: " + silverbullSubscriber.get() + ", expires at: " + expiryTime);
    }

    private void updateCharacterId() {
        ItemStack compassItem = McUtils.inventory().items.get(CHARACTER_INFO_SLOT);
        List<StyledText> compassLore = LoreUtils.getLore(compassItem);
        StyledText idLine = compassLore.getFirst();

        if (idLine == null || !idLine.matches(CHARACTER_ID_PATTERN)) {
            WynntilsMod.warn("Compass item had unexpected character ID line: " + idLine);
            return;
        }

        id = idLine.getString();
        WynntilsMod.info("Selected character: " + id);
    }

    private String getCharacterString() {
        return "CharacterInfo{" + "classType="
                + classType + ", reskinned="
                + reskinned + ", level="
                + level + ", id="
                + id + ", silverbullSubscriber="
                + silverbullSubscriber.get() + '}';
    }

    private void parseCharacterFromCharacterMenu(ItemStack characterInfoItem) {
        List<StyledText> lore = LoreUtils.getLore(characterInfoItem);

        int level = 0;
        String className = "";

        for (StyledText line : lore) {
            Matcher levelMatcher = line.getMatcher(INFO_MENU_LEVEL_PATTERN);
            if (levelMatcher.matches()) {
                level = Integer.parseInt(levelMatcher.group(1));
                continue;
            }

            Matcher classMatcher = line.getMatcher(INFO_MENU_CLASS_PATTERN);

            if (classMatcher.matches()) {
                className = classMatcher.group(1);
            }
        }
        ClassType classType = ClassType.fromName(className);

        updateCharacterInfo(classType, classType != null && ClassType.isReskinned(className), level);
    }

    private boolean parseCharacter(ItemStack itemStack) {
        Optional<CharacterItem> characterItemOpt = Models.Item.asWynnItem(itemStack, CharacterItem.class);
        if (characterItemOpt.isEmpty()) return false;

        CharacterItem characterItem = characterItemOpt.get();

        updateCharacterInfo(characterItem.getClassType(), characterItem.isReskinned(), characterItem.getLevel());
        return true;
    }

    private void updateCharacterInfo(ClassType classType, boolean reskinned, int level) {
        this.classType = classType;
        this.reskinned = reskinned;
        this.level = level;
    }
}
