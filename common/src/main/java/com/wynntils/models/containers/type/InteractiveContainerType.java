/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.type;

import com.wynntils.core.text.StyledText;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public enum InteractiveContainerType {
    ABILITY_TREE(
            Pattern.compile("(?:Warrior|Shaman|Mage|Assassin|Archer) Abilities"),
            Pattern.compile("§7Next Page"),
            Pattern.compile("§7Previous Page"),
            59,
            57),
    ACCOUNT_BANK(
            Pattern.compile("§0\\[Pg. (\\d+)\\] §8(.*)'s?§0 Account Bank"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<"),
            8,
            17,
            new ContainerBounds(0, 0, 5, 6),
            true),
    BLOCK_BANK(
            Pattern.compile("§0\\[Pg. (\\d+)\\] §8(.*)'s?§0 Block Bank"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<"),
            8,
            17,
            new ContainerBounds(0, 0, 5, 6),
            true),
    BOOKSHELF(
            Pattern.compile("§0\\[Pg. (\\d+)\\] §8(.*)'s?§0 Bookshelf"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<"),
            8,
            17,
            new ContainerBounds(0, 0, 5, 6),
            true),
    CHARACTER_BANK(
            Pattern.compile("§0\\[Pg. (\\d+)\\] §8(.*)'s?§0 Character Bank"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<"),
            8,
            17,
            new ContainerBounds(0, 0, 5, 6),
            true),
    CONTENT_BOOK(
            Pattern.compile("§f\uE000\uE072"),
            Pattern.compile("§7Scroll Down"),
            Pattern.compile("§7Scroll Up"),
            69,
            65,
            new ContainerBounds(0, 0, 5, 8)),
    GUILD_BANK( // Guild Bank not classed as a bank as it does not act like account/character bank, misc bucket etc
            Pattern.compile(".+: Bank \\(.+\\)"),
            Pattern.compile("§a§lNext Page"),
            Pattern.compile("§a§lPrevious Page"),
            27,
            9,
            new ContainerBounds(0, 2, 4, 8)),
    GUILD_MEMBER_LIST(
            Pattern.compile(".+: Members"),
            Pattern.compile("§a§lNext Page"),
            Pattern.compile("§a§lPrevious Page"),
            28,
            10,
            new ContainerBounds(0, 2, 4, 8)),
    GUILD_TERRITORIES( // Needs verification for next/previous name and slot
            Pattern.compile(".+: Territories"),
            Pattern.compile("§a§lNext Page"),
            Pattern.compile("§a§lPrevious Page"),
            27,
            9,
            new ContainerBounds(0, 2, 4, 8)),
    HOUSING_JUKEBOX(
            Pattern.compile("Select Songs"),
            Pattern.compile("§7Next Page"),
            Pattern.compile("§7Previous Page"),
            8,
            17,
            new ContainerBounds(0, 0, 5, 6)),
    HOUSING_LIST(
            Pattern.compile("Available Islands"),
            Pattern.compile("§7Next Page"),
            Pattern.compile("§7Previous Page"),
            28,
            10,
            new ContainerBounds(0, 2, 4, 8)),
    JUKEBOX(
            Pattern.compile("Player's Jukebox"),
            Pattern.compile("§7Next Page"),
            Pattern.compile("§7Previous Page"),
            8,
            17,
            new ContainerBounds(0, 0, 5, 6)),
    LOBBY(
            Pattern.compile("Wynncraft Servers"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<"),
            44,
            36),
    MISC_BUCKET(
            Pattern.compile("§0\\[Pg. (\\d+)\\] §8(.*)'s?§0 Misc. Bucket"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<"),
            8,
            17,
            new ContainerBounds(0, 0, 5, 6),
            true),
    PET_MENU(
            Pattern.compile("Pet Menu"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<"),
            8,
            0,
            new ContainerBounds(1, 0, 5, 8)),
    SCRAP_MENU(
            Pattern.compile("Scrap Rewards"),
            Pattern.compile("§7Next Page"),
            Pattern.compile("§7Previous Page"),
            8,
            0,
            new ContainerBounds(1, 0, 5, 8)),
    TRADE_MARKET_FILTERS(
            Pattern.compile("\\[Pg\\. \\d] Filter Items"),
            Pattern.compile("§7Forward to §fPage \\d+"),
            Pattern.compile("§7Back to §fPage \\d+"),
            35,
            26),
    TRADE_MARKET_PRIMARY(
            Pattern.compile("Trade Market"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<"),
            26,
            17),
    TRADE_MARKET_SECONDARY(
            Pattern.compile("Search Results"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<"),
            35,
            26);

    private final Predicate<Screen> screenPredicate;
    private final Pattern nextItemPattern;
    private final Pattern previousItemPattern;
    private final int nextItemSlot;
    private final int previousItemSlot;
    private final ContainerBounds bounds;
    private final boolean isBank;

    // Searchable, not a bank
    InteractiveContainerType(
            Pattern titlePattern,
            Pattern nextItemPattern,
            Pattern previousItemPattern,
            int nextItemSlot,
            int previousItemSlot,
            ContainerBounds bounds,
            boolean isBank) {
        this.screenPredicate =
                screen -> titlePattern.matcher(screen.getTitle().getString()).matches();
        this.nextItemPattern = nextItemPattern;
        this.previousItemPattern = previousItemPattern;
        this.nextItemSlot = nextItemSlot;
        this.previousItemSlot = previousItemSlot;
        this.bounds = bounds;
        this.isBank = isBank;
    }

    // Searchable, bank
    InteractiveContainerType(
            Pattern titlePattern,
            Pattern nextItemPattern,
            Pattern previousItemPattern,
            int nextItemSlot,
            int previousItemSlot,
            ContainerBounds bounds) {
        this.screenPredicate =
                screen -> titlePattern.matcher(screen.getTitle().getString()).matches();
        this.nextItemPattern = nextItemPattern;
        this.previousItemPattern = previousItemPattern;
        this.nextItemSlot = nextItemSlot;
        this.previousItemSlot = previousItemSlot;
        this.bounds = bounds;
        this.isBank = false;
    }

    // Not searchable, not bank
    InteractiveContainerType(
            Pattern titlePattern,
            Pattern nextItemPattern,
            Pattern previousItemPattern,
            int nextItemSlot,
            int previousItemSlot) {
        this.screenPredicate =
                screen -> titlePattern.matcher(screen.getTitle().getString()).matches();
        this.nextItemPattern = nextItemPattern;
        this.previousItemPattern = previousItemPattern;
        this.nextItemSlot = nextItemSlot;
        this.previousItemSlot = previousItemSlot;
        this.bounds = null;
        this.isBank = false;
    }

    public int getNextItemSlot() {
        return nextItemSlot;
    }

    public int getPreviousItemSlot() {
        return previousItemSlot;
    }

    public Pattern getNextItemPattern() {
        return nextItemPattern;
    }

    public Pattern getPreviousItemPattern() {
        return previousItemPattern;
    }

    public ContainerBounds getBounds() {
        return bounds;
    }

    public boolean isSearchable() {
        return bounds != null;
    }

    public boolean isBank() {
        return isBank;
    }

    public boolean isScreen(Screen screen) {
        return screenPredicate.test(screen);
    }

    public static Optional<Integer> getScrollButton(AbstractContainerScreen<?> screen, boolean previousPage) {
        for (InteractiveContainerType type : InteractiveContainerType.values()) {
            if (type.isScreen(screen)) {
                StyledText buttonText = StyledText.fromComponent(screen.getMenu()
                        .slots
                        .get(previousPage ? type.getPreviousItemSlot() : type.getNextItemSlot())
                        .getItem()
                        .getHoverName());

                if (buttonText
                        .getMatcher(previousPage ? type.getPreviousItemPattern() : type.getNextItemPattern())
                        .matches()) {
                    return Optional.of(previousPage ? type.getPreviousItemSlot() : type.getNextItemSlot());
                }
            }
        }

        return Optional.empty();
    }
}
