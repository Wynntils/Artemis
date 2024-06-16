/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.features;

import com.wynntils.services.mapdata.providers.json.JsonMapAttributes;
import com.wynntils.services.mapdata.providers.json.JsonMapLocation;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.Texture;
import java.util.Arrays;

public final class ServiceLocation extends JsonMapLocation {
    public ServiceLocation(String featureId, String categoryId, JsonMapAttributes attributes, Location location) {
        super(featureId, categoryId, attributes, location);
    }

    public enum ServiceKind {
        ALCHEMISM_STATION("Alchemism Station", Texture.ALCHEMIST_STATION, "profession:alchemism"),
        ARMOUR_MERCHANT("Armour Merchant", Texture.ARMOR_MERCHANT, "merchant:armor"),
        ARMOURING_STATION("Armouring Station", Texture.ARMORING_STATION, "profession:armoring"),
        BLACKSMITH("Blacksmith", Texture.BLACKSMITH, "blacksmith"),
        BOOTH_SHOP("Booth Shop", Texture.BOOTH_SHOP, "booth-shop"),
        COOKING_STATION("Cooking Station", Texture.COOKING_STATION, "profession:cooking"),
        DUNGEON_SCROLL_MERCHANT("Dungeon Scroll Merchant", Texture.DUNGEON_SCROLL_MERCHANT, "merchant:dungeon-scroll"),
        EMERALD_MERCHANT("Emerald Merchant", Texture.EMERALD_MERCHANT, "merchant:emerald"),
        FAST_TRAVEL("Fast Travel", Texture.FAST_TRAVEL, "fast-travel"),
        HOUSING_BALLOON("Housing Balloon", Texture.HOUSING_BALLOON, "housing-balloon"),
        ITEM_IDENTIFIER("Item Identifier", Texture.ITEM_IDENTIFIER, "identifier"),
        JEWELING_STATION("Jeweling Station", Texture.JEWELING_STATION, "profession:jeweling"),
        LIQUID_MERCHANT("Liquid Merchant", Texture.LIQUID_MERCHANT, "merchant:liquid-emerald"),
        PARTY_FINDER("Party Finder", Texture.PARTY_FINDER, "party-finder"),
        POTION_MERCHANT("Potion Merchant", Texture.POTION_MERCHANT, "merchant:potion"),
        POWDER_MASTER("Powder Master", Texture.POWDER_MASTER, "powder-master"),
        SCRIBING_STATION("Scribing Station", Texture.SCRIBING_STATION, "profession:scribing"),
        SCROLL_MERCHANT("Scroll Merchant", Texture.SCROLL_MERCHANT, "merchant:scroll"),
        SEASKIPPER("Seaskipper", Texture.SEASKIPPER, "seaskipper"),
        TAILORING_STATION("Tailoring Station", Texture.TAILORING_STATION, "profession:tailoring"),
        TOOL_MERCHANT("Tool Merchant", Texture.TOOL_MERCHANT, "merchant:tool"),
        TRADE_MARKET("Trade Market", Texture.TRADE_MARKET, "trade-market"),
        WEAPON_MERCHANT("Weapon Merchant", Texture.WEAPON_MERCHANT, "merchant:weapon"),
        WEAPONSMITHING_STATION("Weaponsmithing Station", Texture.WEAPONSMITHING_STATION, "profession:weaponsmithing"),
        WOODWORKING_STATION("Woodworking Station", Texture.WOODWORKING_STATION, "profession:woodworking");

        private final String name;
        private final Texture texture;
        private final String mapDataId;

        ServiceKind(String name, Texture texture, String mapDataId) {
            this.name = name;
            this.texture = texture;
            this.mapDataId = mapDataId;
        }

        public String getName() {
            return name;
        }

        public Texture getIcon() {
            return texture;
        }

        public String getMapDataId() {
            return mapDataId;
        }

        public static ServiceKind fromString(String str) {
            return Arrays.stream(values())
                    .filter(kind -> kind.getName().equals(str))
                    .findFirst()
                    .orElse(null);
        }
    }
}
