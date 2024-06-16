/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.builtin;

import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.utils.render.Texture;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;

public class MapIconsProvider extends BuiltInProvider {
    public static final String FALLBACK_ICON_ID = "wynntils:icon:symbols:waypoint";

    private static final List<MapIcon> PROVIDED_ICONS = List.of(
            new BuiltInIcon("wynntils:icon:content:boss-altar", Texture.BOSS_ALTAR),
            new BuiltInIcon("wynntils:icon:content:cave", Texture.CAVE),
            new BuiltInIcon("wynntils:icon:content:dungeon", Texture.DUNGEON_ENTRANCE),
            new BuiltInIcon("wynntils:icon:content:grind-spot", Texture.GRIND_SPOT),
            new BuiltInIcon("wynntils:icon:content:lootrun-camp", Texture.LOOTRUN_CAMP),
            new BuiltInIcon("wynntils:icon:content:raid", Texture.RAID_ENTRANCE),
            new BuiltInIcon("wynntils:icon:content:shrine", Texture.SHRINE),
            new BuiltInIcon("wynntils:icon:gathering:farming", Texture.FARMING),
            new BuiltInIcon("wynntils:icon:gathering:fishing", Texture.FISHING),
            new BuiltInIcon("wynntils:icon:gathering:mining", Texture.MINING),
            new BuiltInIcon("wynntils:icon:gathering:woodcutting", Texture.WOODCUTTING),
            new BuiltInIcon("wynntils:icon:lootchest:tier-1", Texture.CHEST_T1),
            new BuiltInIcon("wynntils:icon:lootchest:tier-2", Texture.CHEST_T2),
            new BuiltInIcon("wynntils:icon:lootchest:tier-3", Texture.CHEST_T3),
            new BuiltInIcon("wynntils:icon:lootchest:tier-4", Texture.CHEST_T4),
            new BuiltInIcon("wynntils:icon:player:head", Texture.PLAYER_HEAD),
            new BuiltInIcon("wynntils:icon:service:blacksmith", Texture.BLACKSMITH),
            new BuiltInIcon("wynntils:icon:service:booth-shop", Texture.BOOTH_SHOP),
            new BuiltInIcon("wynntils:icon:service:fast-travel", Texture.FAST_TRAVEL),
            new BuiltInIcon("wynntils:icon:service:housing-balloon", Texture.HOUSING_BALLOON),
            new BuiltInIcon("wynntils:icon:service:identifier", Texture.ITEM_IDENTIFIER),
            new BuiltInIcon("wynntils:icon:service:merchant:armor", Texture.ARMOR_MERCHANT),
            new BuiltInIcon("wynntils:icon:service:merchant:dungeon-scroll", Texture.DUNGEON_SCROLL_MERCHANT),
            new BuiltInIcon("wynntils:icon:service:merchant:emerald", Texture.EMERALD_MERCHANT),
            new BuiltInIcon("wynntils:icon:service:merchant:liquid-emerald", Texture.LIQUID_MERCHANT),
            new BuiltInIcon("wynntils:icon:service:merchant:potion", Texture.POTION_MERCHANT),
            new BuiltInIcon("wynntils:icon:service:merchant:scroll", Texture.SCROLL_MERCHANT),
            new BuiltInIcon("wynntils:icon:service:merchant:tool", Texture.TOOL_MERCHANT),
            new BuiltInIcon("wynntils:icon:service:merchant:weapon", Texture.WEAPON_MERCHANT),
            new BuiltInIcon("wynntils:icon:service:party-finder", Texture.PARTY_FINDER),
            new BuiltInIcon("wynntils:icon:service:powder-master", Texture.POWDER_MASTER),
            new BuiltInIcon("wynntils:icon:service:profession:alchemism", Texture.ALCHEMIST_STATION),
            new BuiltInIcon("wynntils:icon:service:profession:armoring", Texture.ARMORING_STATION),
            new BuiltInIcon("wynntils:icon:service:profession:cooking", Texture.COOKING_STATION),
            new BuiltInIcon("wynntils:icon:service:profession:jeweling", Texture.JEWELING_STATION),
            new BuiltInIcon("wynntils:icon:service:profession:scribing", Texture.SCRIBING_STATION),
            new BuiltInIcon("wynntils:icon:service:profession:tailoring", Texture.TAILORING_STATION),
            new BuiltInIcon("wynntils:icon:service:profession:weaponsmithing", Texture.WEAPONSMITHING_STATION),
            new BuiltInIcon("wynntils:icon:service:profession:woodworking", Texture.WOODWORKING_STATION),
            new BuiltInIcon("wynntils:icon:service:seaskipper", Texture.SEASKIPPER),
            new BuiltInIcon("wynntils:icon:service:trade-market", Texture.TRADE_MARKET),
            new BuiltInIcon("wynntils:icon:symbols:diamond", Texture.DIAMOND),
            new BuiltInIcon("wynntils:icon:symbols:fireball", Texture.FIREBALL),
            new BuiltInIcon("wynntils:icon:symbols:flag", Texture.FLAG),
            new BuiltInIcon("wynntils:icon:symbols:pointer", Texture.POINTER),
            new BuiltInIcon("wynntils:icon:symbols:sign", Texture.SIGN),
            new BuiltInIcon("wynntils:icon:symbols:star", Texture.STAR),
            new BuiltInIcon("wynntils:icon:symbols:wall", Texture.WALL),
            new BuiltInIcon("wynntils:icon:symbols:waypoint", Texture.WAYPOINT));

    @Override
    public String getProviderId() {
        return "icons";
    }

    @Override
    public Stream<MapIcon> getIcons() {
        return PROVIDED_ICONS.stream();
    }

    @Override
    public void reloadData() {}

    public static String getIconIdFromTexture(Texture texture) {
        for (MapIcon icon : PROVIDED_ICONS) {
            if (icon.getResourceLocation().equals(texture.resource())) {
                return icon.getIconId();
            }
        }

        return FALLBACK_ICON_ID;
    }

    private static final class BuiltInIcon implements MapIcon {
        private final String id;
        private final Texture texture;

        private BuiltInIcon(String id, Texture texture) {
            this.id = id;
            this.texture = texture;
        }

        @Override
        public String getIconId() {
            return id;
        }

        @Override
        public ResourceLocation getResourceLocation() {
            return texture.resource();
        }

        @Override
        public int getWidth() {
            return texture.width();
        }

        @Override
        public int getHeight() {
            return texture.height();
        }
    }
}
