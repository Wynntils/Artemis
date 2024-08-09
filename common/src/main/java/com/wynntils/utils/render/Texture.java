/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render;

import net.minecraft.resources.ResourceLocation;

// If a texture is currently in a specific category but you want to use
// it elsewhere, please move it to a more appropriate location
public enum Texture {
    // region Character Selection
    CHANGE_WORLD_BUTTON("character_selection/change_world_button.png", 26, 52),
    CHARACTER_BUTTON("character_selection/character_button.png", 104, 64),
    CHARACTER_INFO("character_selection/character_info.png", 123, 38),
    CHARACTER_LIST_BACKGROUND("character_selection/character_list_background.png", 118, 254),
    DISCONNECT_BUTTON("character_selection/disconnect_button.png", 26, 52),
    PLAY_BUTTON("character_selection/play_button.png", 79, 76),
    XP_BAR("character_selection/xp_bar.png", 100, 12),
    // endregion

    // region Content Book
    CONFIG_BOOK_BACKGROUND("content_book/config_book.png", 400, 220),
    CONFIG_BOOK_SCROLL_AREA("content_book/config_book_scroll_area.png", 168, 175),
    CONFIG_BOOK_SCROLL_BUTTON("content_book/config_book_scroll_button.png", 5, 18),
    CONTENT_BOOK_BACKGROUND("content_book/content_book.png", 339, 220),
    CONTENT_BOOK_SEARCH("content_book/content_book_search.png", 133, 23),
    CONTENT_BOOK_TITLE("content_book/content_book_title.png", 168, 33),
    TAG_BLUE("content_book/tag_blue.png", 44, 22),
    TAG_RED("content_book/tag_red.png", 22, 44),
    TAG_RED_SELECTED("content_book/tag_red_selected.png", 22, 44),
    TAG_SEARCH("content_book/tag_search.png", 140, 30),
    // endregion

    // region Icons
    WYNNCRAFT_ICON("icons/wynncraft_icon.png", 64, 64),

    // Activities
    ACTIVITY_CAN_START("icons/activities/activity_can_start_icon.png", 11, 7),
    ACTIVITY_CANNOT_START("icons/activities/activity_cannot_start_icon.png", 7, 7),
    ACTIVITY_FINISHED("icons/activities/activity_finished_icon.png", 11, 7),
    ACTIVITY_STARTED("icons/activities/activity_started_icon.png", 7, 7),
    CAVE_AVALIABLE_ICON("icons/activities/cave_avaliable_icon.png", 7, 7),

    // Config Categories
    ALL_CONFIG_ICON("icons/config_categories/all_config_icon.png", 16, 16),
    CHAT_CONFIG_ICON("icons/config_categories/chat_config_icon.png", 16, 16),
    COMBAT_CONFIG_ICON("icons/config_categories/combat_config_icon.png", 16, 16),
    COMMANDS_CONFIG_ICON("icons/config_categories/commands_config_icon.png", 16, 16),
    DEBUG_CONFIG_ICON("icons/config_categories/debug_config_icon.png", 16, 16),
    EMBELLISHMENTS_CONFIG_ICON("icons/config_categories/embellishments_config_icon.png", 16, 16),
    INVENTORY_CONFIG_ICON("icons/config_categories/inventory_config_icon.png", 16, 16),
    MAP_CONFIG_ICON("icons/config_categories/map_config_icon.png", 16, 16),
    OVERLAYS_CONFIG_ICON("icons/config_categories/overlays_config_icon.png", 16, 16),
    PLAYERS_CONFIG_ICON("icons/config_categories/players_config_icon.png", 16, 16),
    REDIRECTS_CONFIG_ICON("icons/config_categories/redirects_config_icon.png", 16, 16),
    TOOLTIPS_CONFIG_ICON("icons/config_categories/tooltips_config_icon.png", 16, 16),
    TRADE_MARKET_CONFIG_ICON("icons/config_categories/trade_market_config_icon.png", 16, 16),
    UI_CONFIG_ICON("icons/config_categories/ui_config_icon.png", 16, 16),
    UNCATEGORIZED_CONFIG_ICON("icons/config_categories/uncategorized_config_icon.png", 16, 16),
    UTILITIES_CONFIG_ICON("icons/config_categories/utilities_config_icon.png", 16, 16),
    WYNNTILS_CONFIG_ICON("icons/config_categories/wynntils_config_icon.png", 16, 16),
    APPLY_SETTINGS_ICON("icons/config_categories/apply_settings_icon.png", 16, 16),
    DISCARD_SETTINGS_ICON("icons/config_categories/discard_settings_icon.png", 16, 16),
    EXPORT_SETTINGS_ICON("icons/config_categories/export_settings_icon.png", 16, 16),
    IMPORT_SETTINGS_ICON("icons/config_categories/import_settings_icon.png", 16, 16),

    // Content Book
    DIALOGUE_ICON("icons/content_book/dialogue_icon.png", 14, 11),
    DISCOVERIES_ICON("icons/content_book/discoveries_icon.png", 16, 32),
    GUIDES_ICON("icons/content_book/guides_icon.png", 18, 34),
    LOOTRUN_ICON("icons/content_book/lootrun_icon.png", 16, 28),
    MAP_ICON("icons/content_book/map_icon.png", 21, 38),
    OVERLAYS_ICON("icons/content_book/overlays_icon.png", 19, 38),
    QUEST_BOOK_ICON("icons/content_book/quests_icon.png", 24, 34),
    SECRET_DISCOVERIES_ICON("icons/content_book/secret_discoveries_icon.png", 16, 26),
    SETTINGS_ICON("icons/content_book/settings_icon.png", 17, 34),

    // Discoveries
    DISCOVERED_SECRET("icons/discoveries/discovered_secret.png", 19, 36),
    DISCOVERED_SECRET_ICON("icons/discoveries/discovered_secret_icon.png", 5, 7),
    DISCOVERED_TERRITORY("icons/discoveries/discovered_territory.png", 17, 40),
    DISCOVERED_TERRITORY_ICON("icons/discoveries/discovered_territory_icon.png", 8, 7),
    DISCOVERED_WORLD("icons/discoveries/discovered_world.png", 18, 34),
    DISCOVERED_WORLD_ICON("icons/discoveries/discovered_world_icon.png", 5, 7),
    UNDISCOVERED_SECRET("icons/discoveries/undiscovered_secret.png", 17, 32),
    UNDISCOVERED_SECRET_ICON("icons/discoveries/undiscovered_secret_icon.png", 11, 7),
    UNDISCOVERED_TERRITORY("icons/discoveries/undiscovered_territory.png", 21, 38),
    UNDISCOVERED_TERRITORY_ICON("icons/discoveries/undiscovered_territory_icon.png", 6, 7),
    UNDISCOVERED_WORLD("icons/discoveries/undiscovered_world.png", 21, 34),
    UNDISCOVERED_WORLD_ICON("icons/discoveries/undiscovered_world_icon.png", 11, 7),

    // Trade Market
    PRESET("trade_market/preset.png", 16, 16),
    SAVED_PRESET("trade_market/saved_preset.png", 16, 16),
    SORT("trade_market/sort.png", 16, 16),

    // Generic
    ADD_ICON("icons/generic/add_icon.png", 14, 14),
    ARROW_LEFT_ICON("icons/generic/arrow_left_icon.png", 16, 16),
    BOAT_ICON("icons/generic/boat_icon.png", 15, 14),
    CHALLENGES_ICON("icons/generic/challenges_icon.png", 9, 14),
    CHECKMARK_GRAY("icons/generic/check_gray.png", 16, 16),
    CHECKMARK_GREEN("icons/generic/check_green.png", 16, 16),
    CHECKMARK_YELLOW("icons/generic/check_yellow.png", 16, 16),
    CLOSE("icons/generic/close.png", 16, 16),
    DEFENSE_FILTER_ICON("icons/generic/defense_filter_icon.png", 16, 16),
    EDIT_ICON("icons/generic/edit_icon.png", 6, 16),
    EDIT_NAME_ICON("icons/generic/edit_name_icon.png", 16, 16),
    FAVORITE_ICON("icons/generic/favorite_icon.png", 18, 18),
    HELP_ICON("icons/generic/help_icon.png", 10, 16),
    INFO("icons/generic/info.png", 25, 25),
    ITEM_LOCK("icons/generic/item_lock_icon.png", 16, 16),
    NEXT("icons/generic/next.png", 16, 16),
    OVERLAY_EXTRA_ICON("icons/generic/overlay_extra_icon.png", 16, 16),
    PREVIOUS("icons/generic/previous.png", 16, 16),
    QUESTION_MARK("icons/generic/question_mark.png", 4, 7),
    QUESTS_SCROLL_ICON("icons/generic/quests_scroll_icon.png", 16, 16),
    SAVE("icons/generic/save.png", 16, 16),
    SHARE_ICON("icons/generic/share_icon.png", 16, 14),
    SIGN_ICON("icons/generic/sign_icon.png", 17, 18),
    SMALL_ADD_ICON("icons/generic/small_add_icon.png", 16, 16),
    WAYPOINT_FOCUS_ICON("icons/generic/waypoint_focus_icon.png", 12, 16),
    WAYPOINT_MANAGER_ICON("icons/generic/waypoint_manager_icon.png", 12, 16),

    // Generic Offset
    ADD_ICON_OFFSET("icons/generic_offset/add_icon_offset.png", 14, 28),
    BACK_ARROW_OFFSET("icons/generic_offset/back_arrow_icon_offset.png", 32, 9),
    BACKWARD_ARROW_OFFSET("icons/generic_offset/backward_arrow_icon_offset.png", 36, 10),
    FORWARD_ARROW_OFFSET("icons/generic_offset/forward_arrow_icon_offset.png", 36, 10),
    RELOAD_ICON_OFFSET("icons/generic_offset/reload_icon_offset.png", 40, 20),
    REMOVE_ICON_OFFSET("icons/generic_offset/remove_icon_offset.png", 14, 28),
    SORT_ALPHABETICALLY_OFFSET("icons/generic_offset/sort_alphabetical_icon_offset.png", 23, 46),
    SORT_DISTANCE_OFFSET("icons/generic_offset/sort_distance_icon_offset.png", 23, 46),
    SORT_LEVEL_OFFSET("icons/generic_offset/sort_level_icon_offset.png", 23, 46),

    // Map
    ALCHEMIST_STATION("icons/map/alchemist_station.png", 16, 19),
    ARMOR_MERCHANT("icons/map/armor_merchant.png", 17, 17),
    ARMORING_STATION("icons/map/armoring_station.png", 17, 17),
    BLACKSMITH("icons/map/blacksmith.png", 18, 18),
    BOOTH_SHOP("icons/map/booth_shop.png", 19, 17),
    BOSS_ALTAR("icons/map/boss_altar.png", 18, 13),
    CAVE("icons/map/cave.png", 16, 18),
    CHEST_T1("icons/map/chest_t1.png", 18, 16),
    CHEST_T2("icons/map/chest_t2.png", 18, 16),
    CHEST_T3("icons/map/chest_t3.png", 18, 16),
    CHEST_T4("icons/map/chest_t4.png", 18, 16),
    COOKING_STATION("icons/map/cooking_station.png", 18, 16),
    DIAMOND("icons/map/diamond.png", 18, 15),
    DUNGEON_ENTRANCE("icons/map/dungeon_entrance.png", 12, 12),
    DUNGEON_SCROLL_MERCHANT("icons/map/dungeon_scroll_merchant.png", 18, 17),
    EMERALD_MERCHANT("icons/map/emerald_merchant.png", 17, 18),
    FARMING("icons/map/farming.png", 15, 19),
    FAST_TRAVEL("icons/map/fast_travel.png", 18, 18),
    FIREBALL("icons/map/fireball.png", 14, 14),
    FISHING("icons/map/fishing.png", 16, 14),
    FLAG("icons/map/flag.png", 16, 16),
    GRIND_SPOT("icons/map/grind_spot.png", 12, 12),
    GUILD_HEADQUARTERS("icons/map/guild_headquarters.png", 16, 13),
    HOUSING_BALLOON("icons/map/housing_balloon.png", 13, 23),
    ITEM_IDENTIFIER("icons/map/item_identifier.png", 18, 17),
    JEWELING_STATION("icons/map/jeweling_station.png", 17, 16),
    LIQUID_MERCHANT("icons/map/liquid_merchant.png", 15, 17),
    MINING("icons/map/mining.png", 18, 14),
    PARTY_FINDER("icons/map/party_finder.png", 18, 18),
    POINTER("icons/map/pointer.png", 10, 8),
    POTION_MERCHANT("icons/map/potion_merchant.png", 16, 18),
    POWDER_MASTER("icons/map/powder_master.png", 17, 17),
    RAID_ENTRANCE("icons/map/raid_entrance.png", 12, 12),
    SCRIBING_STATION("icons/map/scribing_station.png", 18, 17),
    SCROLL_MERCHANT("icons/map/scroll_merchant.png", 18, 17),
    SEASKIPPER("icons/map/seaskipper.png", 18, 18),
    SHRINE("icons/map/shrine.png", 18, 18),
    SIGN("icons/map/sign.png", 17, 18),
    STAR("icons/map/star.png", 18, 18),
    TAILORING_STATION("icons/map/tailoring_station.png", 18, 15),
    TOOL_MERCHANT("icons/map/tool_merchant.png", 17, 15),
    TRADE_MARKET("icons/map/trade_market.png", 18, 18),
    WALL("icons/map/wall.png", 12, 16),
    WAYPOINT("icons/map/waypoint.png", 14, 18),
    WEAPON_MERCHANT("icons/map/weapon_merchant.png", 18, 15),
    WEAPONSMITHING_STATION("icons/map/weaponsmithing_station.png", 18, 15),
    WOODCUTTING("icons/map/woodcutting.png", 16, 17),
    WOODWORKING_STATION("icons/map/woodworking_station.png", 17, 15),
    // endregion

    // region Item Storage
    ITEM_RECORD("item_storage/record.png", 171, 167),
    ITEM_RECORD_ADD("item_storage/record_add.png", 9, 18),
    ITEM_RECORD_BUTTON_LEFT("item_storage/record_button_left.png", 11, 20),
    ITEM_RECORD_BUTTON_RIGHT("item_storage/record_button_right.png", 11, 20),
    ITEM_RECORD_CONFIRM("item_storage/record_confirm.png", 9, 18),
    ITEM_RECORD_DELETE("item_storage/record_delete.png", 9, 18),
    ITEM_RECORD_HELP("item_storage/record_help.png", 9, 18),
    ITEM_RECORD_SCROLL("item_storage/record_scroll_button.png", 7, 8),
    // endregion

    // region Lootrun
    LOOTRUN_LINE("lootrun/path_arrow.png", 16, 16),

    // Challenge Icons
    DEFEND("lootrun/challenge_icons/defend.png", 20, 23),
    DESTROY("lootrun/challenge_icons/destroy.png", 12, 12),
    SLAY("lootrun/challenge_icons/slay.png", 20, 20),
    SPELUNK("lootrun/challenge_icons/spelunk.png", 18, 16),
    // endregion

    // region Map
    // Map Borders
    FULLSCREEN_MAP_BORDER("map/map_borders/full_map_border.png", 510, 254),
    GILDED_MAP_TEXTURES("map/map_borders/gilded_map_textures.png", 262, 524),
    PAPER_MAP_TEXTURES("map/map_borders/paper_map_textures.png", 256, 512),
    WYNN_MAP_TEXTURES("map/map_borders/wynn_map_textures.png", 126, 256),

    // Map Components
    CIRCLE_MASK("map/map_components/circle_mask.png", 256, 256),
    MAP_BUTTONS_BACKGROUND("map/map_components/map_buttons_background.png", 144, 26),
    MAP_INFO_NAME_BOX("map/map_components/map_info_name_box.png", 200, 20),
    MAP_INFO_TOOLTIP_CENTER("map/map_components/map_info_tooltip_center.png", 200, 5),
    MAP_INFO_TOOLTIP_TOP("map/map_components/map_info_tooltip_top.png", 200, 10),
    MAP_POINTERS("map/map_components/map_pointers.png", 256, 256),
    // endregion

    // region Overlays
    PLAYER_LIST_OVERLAY("overlays/player_list_overlay.png", 385, 229),

    // Bars
    BUBBLE_BAR("overlays/bars/bars_bubbles.png", 182, 60),
    EXPERIENCE_BAR("overlays/bars/bars_exp.png", 182, 60),
    HEALTH_BAR("overlays/bars/bars_health.png", 81, 164),
    HEALTH_BAR_OVERFLOW("overlays/bars/bars_health_overflow.png", 81, 164),
    MANA_BAR("overlays/bars/bars_mana.png", 81, 180),
    MANA_BAR_OVERFLOW("overlays/bars/bars_mana_overflow.png", 81, 180),
    UNIVERSAL_BAR("overlays/bars/universal_bar.png", 81, 16),
    // endregion

    // region Players
    LEADERBOARD_BADGES("players/leaderboard_badges.png", 256, 256),
    // endregion

    // region Seaskipper
    DESTINATION_BUTTON("seaskipper/destination_button.png", 135, 40),
    DESTINATION_LIST("seaskipper/destination_list_background.png", 151, 226),
    TRAVEL_BUTTON("seaskipper/travel_button.png", 151, 60),
    // endregion

    // region Territory Magagement Background
    TERRITORY_MANAGEMENT_BACKGROUND("territory_management/territory_management_background.png", 256, 136),
    TERRITORY_ITEM("territory_management/territory.png", 16, 16),
    TERRITORY_ITEM_HQ("territory_management/territory_hq.png", 16, 16),
    TERRITORY_ITEM_ALERT("territory_management/territory_alert.png", 16, 16),
    TERRITORY_SIDEBAR("territory_management/territory_sidebar.png", 20, 136),
    TERRITORY_LOADOUT("territory_management/territory_loadout.png", 16, 16),

    // region UI Components
    BACKGROUND_SPLASH("ui_components/background_splash.png", 1920, 1027),
    BANK_PANEL("ui_components/bank_panel.png", 100, 117),
    COSMETIC_VIEWER_BACKGROUND("ui_components/cosmetics_viewer_background.png", 72, 92),
    CONTAINER_SIDEBAR("ui_components/container_sidebar.png", 25, 136),
    EMERALD_COUNT_BACKGROUND("ui_components/emerald_count_background.png", 24, 24),
    GEAR_ICONS("ui_components/gear_icons.png", 64, 128),
    GEAR_VIEWER_BACKGROUND("ui_components/gear_viewer_background.png", 103, 92),
    HIGHLIGHT("ui_components/highlight.png", 256, 256),
    ITEM_FILTER_BACKGROUND("ui_components/item_filter_background.png", 358, 206),
    ITEM_SHARING_BACKGROUND("ui_components/item_sharing_background.png", 250, 100),
    OVERLAY_SELECTION_GUI("ui_components/overlay_selection_gui.png", 358, 206),
    PAPER_BUTTON_BOTTOM("ui_components/paper_button_bottom.png", 64, 60),
    PAPER_BUTTON_LEFT("ui_components/paper_button_left.png", 48, 56),
    PAPER_BUTTON_RIGHT("ui_components/paper_button_right.png", 48, 56),
    PAPER_BUTTON_TOP("ui_components/paper_button_top.png", 128, 60),
    QUICK_JUMP_BUTTON("ui_components/quick_jump_button.png", 16, 32),
    SCROLL_BACKGROUND("ui_components/scroll_background.png", 294, 198),
    SCROLL_BUTTON("ui_components/scroll_button.png", 7, 17),
    WAYPOINT_MANAGER_BACKGROUND("ui_components/waypoint_manager_background.png", 400, 256),
    SCROLLBAR_BACKGROUND("ui_components/generic_container_scrollbar.png", 25, 136),
    SCROLLBAR_BUTTON("ui_components/scroll_button_container.png", 12, 15),
    VIGNETTE("ui_components/vignette.png", 512, 512);
    // endregion

    private final ResourceLocation resource;
    private final int width;
    private final int height;

    Texture(String name, int width, int height) {
        this.resource = ResourceLocation.fromNamespaceAndPath("wynntils", "textures/" + name);
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
