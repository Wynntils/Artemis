/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render;

import net.minecraft.resources.ResourceLocation;

public enum Texture {
    BUBBLE_BAR("bars_bubbles.png", 256, 256),
    CHANGELOG_BACKGROUND("changelog.png", 294, 198),
    COSMETIC_VIEWER_BACKGROUND("cosmetics_viewer_background.png", 72, 92),
    EMERALD_COUNT_BACKGROUND("emerald_count_background.png", 24, 24),
    EXPERIENCE_BAR("bars_exp.png", 256, 256),
    FAVORITE("favorite.png", 18, 18),
    GEAR_ICONS("gear_icons.png", 64, 128),
    GEAR_VIEWER_BACKGROUND("gear_viewer_background.png", 103, 92),
    HEALTH_BAR("bars_health.png", 256, 256),
    HEALTH_BAR_OVERFLOW("bars_health_overflow.png", 256, 256),
    HIGHLIGHT("highlight.png", 256, 256),
    ITEM_LOCK("item_lock.png", 16, 16),
    LOOTRUN_LINE("path_arrow.png", 16, 16),
    MANA_BAR("bars_mana.png", 256, 256),
    MANA_BAR_OVERFLOW("bars_mana_overflow.png", 256, 256),
    OVERLAY_SELECTION_GUI("overlay_selection_gui.png", 195, 256),
    UNIVERSAL_BAR("universal_bar.png", 81, 16),
    VIGNETTE("vignette.png", 512, 512),
    WYNNCRAFT_ICON("wynncraft_icon.png", 64, 64),

    // Discovery Screen
    DISCOVERED_SECRET("discovered_secret.png", 19, 36),
    DISCOVERED_SECRET_ICON("discovered_secret_icon.png", 5, 7),
    DISCOVERED_TERRITORY("discovered_territory.png", 17, 40),
    DISCOVERED_TERRITORY_ICON("discovered_territory_icon.png", 8, 7),
    DISCOVERED_WORLD("discovered_world.png", 18, 34),
    DISCOVERED_WORLD_ICON("discovered_world_icon.png", 5, 7),
    UNDISCOVERED_SECRET("undiscovered_secret.png", 17, 32),
    UNDISCOVERED_SECRET_ICON("undiscovered_secret_icon.png", 11, 7),
    UNDISCOVERED_TERRITORY("undiscovered_territory.png", 21, 38),
    UNDISCOVERED_TERRITORY_ICON("undiscovered_territory_icon.png", 6, 7),
    UNDISCOVERED_WORLD("undiscovered_world.png", 21, 34),
    UNDISCOVERED_WORLD_ICON("undiscovered_world_icon.png", 11, 7),

    // Settings Screen
    SETTING_BACKGROUND("book.png", 400, 220),
    SETTING_SCROLL_AREA("book_scrollarea.png", 168, 175),
    SETTING_SCROLL_BUTTON("book_scroll_button.png", 5, 18),

    // Quest book related
    BACK_ARROW("back_arrow.png", 32, 9),
    BACKWARD_ARROW("backward_arrow.png", 36, 10),
    DIALOGUE_BUTTON("dialogue_button.png", 14, 11),
    FORWARD_ARROW("forward_arrow.png", 36, 10),
    QUEST_BOOK_BACKGROUND("quest_book.png", 339, 220),
    QUESTS_BUTTON("quests_button.png", 16, 16),
    QUEST_BOOK_SEARCH("quest_book_search.png", 133, 23),
    QUEST_BOOK_TITLE("quest_book_title.png", 168, 33),
    RELOAD_BUTTON("reload_button.png", 40, 20),
    SORT_ALPHABETICALLY("sort_alphabetical.png", 23, 46),
    SORT_DISTANCE("sort_distance.png", 23, 46),
    SORT_LEVEL("sort_level.png", 23, 46),

    // Quest State Icons
    QUEST_CANNOT_START("quest_cannot_start_icon.png", 7, 7),
    QUEST_CAN_START("quest_can_start_icon.png", 11, 7),
    QUEST_FINISHED("quest_finished_icon.png", 11, 7),
    QUEST_STARTED("quest_started_icon.png", 7, 7),

    // Icons
    DISCOVERIES_ICON("discoveries_icon.png", 16, 32),
    GUIDES_ICON("guides_icon.png", 18, 34),
    MAP_ICON("map_icon.png", 21, 38),
    LOOTRUN_ICON("lootrun_icon.png", 16, 28),
    OVERLAYS_ICON("overlays_icon.png", 19, 38),
    QUEST_BOOK_ICON("quests_icon.png", 24, 34),
    SECRET_DISCOVERIES_ICON("secret_discoveries.png", 16, 26),
    SETTINGS_ICON("settings_icon.png", 17, 34),

    // Map related
    CIRCLE_MASK("map/circle_mask.png", 256, 256),
    FULLSCREEN_MAP_BORDER("map/map_border/full_map_border.png", 510, 254),
    GILDED_MAP_TEXTURES("map/map_border/gilded_map_textures.png", 262, 524),
    MAP_ADD_BUTTON("map/map_add_button.png", 14, 14),
    MAP_BOAT_BUTTON("map/map_boat_button.png", 15, 14),
    MAP_BUTTONS_BACKGROUND("map/map_buttons_background.png", 144, 26),
    MAP_DEFENSE_FILTER_BUTTON("map/map_defense_filter_button.png", 16, 16),
    MAP_HELP_BUTTON("map/map_help_button.png", 10, 16),
    MAP_MANAGER_BUTTON("map/map_manager_button.png", 12, 16),
    MAP_OVERLAY_BUTTON("map/map_overlay_button.png", 16, 16),
    MAP_POINTERS("map/map_pointers.png", 256, 256),
    MAP_SHARE_BUTTON("map/map_share_button.png", 16, 14),
    MAP_WAYPOINT_FOCUS_BUTTON("map/map_waypoint_focus_button.png", 12, 16),
    PAPER_MAP_TEXTURES("map/map_border/paper_map_textures.png", 256, 512),
    WYNN_MAP_TEXTURES("map/map_border/wynn_map_textures.png", 126, 256),

    // Map Icons
    ALCHEMIST_STATION("map/map_icons/alchemist_station.png", 16, 19),
    ARMOR_MERCHANT("map/map_icons/armor_merchant.png", 17, 17),
    ARMORING_STATION("map/map_icons/armoring_station.png", 17, 17),
    BLACKSMITH("map/map_icons/blacksmith.png", 18, 18),
    BOOTH_SHOP("map/map_icons/booth_shop.png", 19, 17),
    BOSS_ALTAR("map/map_icons/boss_altar.png", 18, 13),
    CAVE("map/map_icons/cave.png", 16, 18),
    CHEST_T1("map/map_icons/chest_t1.png", 18, 16),
    CHEST_T2("map/map_icons/chest_t2.png", 18, 16),
    CHEST_T3("map/map_icons/chest_t3.png", 18, 16),
    CHEST_T4("map/map_icons/chest_t4.png", 18, 16),
    COOKING_STATION("map/map_icons/cooking_station.png", 18, 16),
    DIAMOND("map/map_icons/diamond.png", 18, 15),
    DUNGEON_ENTRANCE("map/map_icons/dungeon_entrance.png", 12, 12),
    DUNGEON_SCROLL_MERCHANT("map/map_icons/dungeon_scroll_merchant.png", 18, 17),
    EMERALD_MERCHANT("map/map_icons/emerald_merchant.png", 17, 18),
    FARMING("map/map_icons/farming.png", 15, 19),
    FAST_TRAVEL("map/map_icons/fast_travel.png", 18, 18),
    FIREBALL("map/map_icons/fireball.png", 14, 14),
    FISHING("map/map_icons/fishing.png", 16, 14),
    FLAG("map/map_icons/flag.png", 16, 16),
    GRIND_SPOT("map/map_icons/grind_spot.png", 12, 12),
    HOUSING_BALLOON("map/map_icons/housing_balloon.png", 13, 23),
    ITEM_IDENTIFIER("map/map_icons/item_identifier.png", 18, 17),
    JEWELING_STATION("map/map_icons/jeweling_station.png", 17, 16),
    LIQUID_MERCHANT("map/map_icons/liquid_merchant.png", 15, 17),
    MINING("map/map_icons/mining.png", 18, 14),
    PARTY_FINDER("map/map_icons/party_finder.png", 18, 18),
    POINTER("map/map_icons/pointer.png", 10, 8),
    POTION_MERCHANT("map/map_icons/potion_merchant.png", 16, 18),
    POWDER_MASTER("map/map_icons/powder_master.png", 17, 17),
    RAID_ENTRANCE("map/map_icons/raid_entrance.png", 12, 12),
    SCRIBING_STATION("map/map_icons/scribing_station.png", 18, 17),
    SCROLL_MERCHANT("map/map_icons/scroll_merchant.png", 18, 17),
    SEASKIPPER("map/map_icons/seaskipper.png", 18, 18),
    SHRINE("map/map_icons/shrine.png", 18, 18),
    SIGN("map/map_icons/sign.png", 17, 18),
    STAR("map/map_icons/star.png", 18, 18),
    TAILORING_STATION("map/map_icons/tailoring_station.png", 18, 15),
    TOOL_MERCHANT("map/map_icons/tool_merchant.png", 17, 15),
    TRADE_MARKET("map/map_icons/trade_market.png", 18, 18),
    WALL("map/map_icons/wall.png", 12, 16),
    WAYPOINT("map/map_icons/waypoint.png", 14, 18),
    WEAPON_MERCHANT("map/map_icons/weapon_merchant.png", 18, 15),
    WEAPONSMITHING_STATION("map/map_icons/weaponsmithing_station.png", 18, 15),
    WOODCUTTING("map/map_icons/woodcutting.png", 16, 17),
    WOODWORKING_STATION("map/map_icons/woodworking_station.png", 17, 15),

    // Guild Map
    GUILD_HEADQUARTERS_ICON("map/guild_map/guild_headquarters_icon.png", 16, 13),
    TERRITORY_NAME_BOX("map/guild_map/territory_name_box.png", 200, 20),
    TERRITORY_TOOLTIP_CENTER("map/guild_map/territory_tooltip_center.png", 200, 5),
    TERRITORY_TOOLTIP_TOP("map/guild_map/territory_tooltip_top.png", 200, 10),

    // Ability Tree
    ABILITY_TREE_BACKGROUND("ability_tree/ability_tree_background.png", 317, 161),
    ABILITY_TREE_DOWN_ARROW("ability_tree/down_button.png", 14, 14),
    ABILITY_TREE_UP_ARROW("ability_tree/up_button.png", 14, 14),

    // Character Selection UI
    ADD_BUTTON("character_selection_ui/add_button.png", 14, 28),
    BACKGROUND_SPLASH("character_selection_ui/background_splash.png", 1920, 1027),
    CHALLENGES_ICON("character_selection_ui/challenges.png", 9, 14),
    CHARACTER_BUTTON("character_selection_ui/character_button.png", 104, 64),
    CHARACTER_INFO("character_selection_ui/character_info.png", 123, 38),
    EDIT_BUTTON("character_selection_ui/edit_button.png", 6, 16),
    LIST_BACKGROUND("character_selection_ui/list_background.png", 118, 254),
    PLAY_BUTTON("character_selection_ui/play_button.png", 79, 76),
    QUESTS_ICON("character_selection_ui/quests.png", 16, 16),
    REMOVE_BUTTON("character_selection_ui/remove_button.png", 14, 28),
    CHARACTER_SELECTION_SCROLL_BUTTON("character_selection_ui/scroll_button.png", 7, 17),
    SOUL_POINT_ICON("character_selection_ui/soul_point.png", 10, 16),
    XP_BAR("character_selection_ui/xp_bar.png", 100, 12),

    // Seaskipper UI
    BOAT_BUTTON("boat_button.png", 39, 76),
    DESTINATION_BUTTON("destination_button.png", 66, 40),
    DESTINATION_LIST("destination_list.png", 143, 205),
    TRAVEL_BUTTON("travel_button.png", 100, 76);

    private final ResourceLocation resource;
    private final int width;
    private final int height;

    Texture(String name, int width, int height) {
        this.resource = new ResourceLocation("wynntils", "textures/" + name);
        this.width = width;
        this.height = height;
    }

    public ResourceLocation resource() {
        return resource;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
