/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net;

import java.util.Optional;

/*
URL ids are build up like <TYPE>_<PROVIDER>_<NAME>, where <TYPE> is one of:

1) API -- this is a REST API call entry point, which will need to be done dynamically
2) DATA -- this will download a json file which can be cached locally
3) LINK -- this is a link that will be opened in the user's browser

<PROVIDER> is a moniker for the provider, and <NAME> is a descriptive but short name
of the kind of service, data or link this URL is dealing with.
 */
public enum UrlId {
    API_ATHENA_AUTH_PUBLIC_KEY("apiAthenaAuthPublicKey"),
    API_ATHENA_AUTH_RESPONSE("apiAthenaAuthResponse"),
    API_ATHENA_UPDATE_CHECK("apiAthenaUpdateCheck"),
    API_ATHENA_UPDATE_CHANGELOG("apiAthenaUpdateChangelog"),
    API_ATHENA_USER_INFO("apiAthenaUserInfo"),
    API_GOOGLE_TRANSLATION("apiGoogleTranslation"),
    API_WIKI_DISCOVERY_QUERY("apiWikiDiscoveryQuery"),
    API_WIKI_QUEST_PAGE_QUERY("apiWikiQuestPageQuery"),
    DATA_ATHENA_SERVER_LIST("dataAthenaServerList"),
    DATA_ATHENA_TERRITORY_LIST("dataAthenaTerritoryList"),
    DATA_STATIC_COMBAT_LOCATIONS("dataStaticCombatLocations"),
    DATA_STATIC_DISCOVERIES("dataStaticDiscoveries"),
    DATA_STATIC_GEAR("dataStaticGear"),
    DATA_STATIC_INGREDIENTS("dataStaticIngredients"),
    DATA_STATIC_INGREDIENT_SKINS("dataStaticIngredientSkins"),
    DATA_STATIC_MAJOR_IDS("dataStaticMajorIds"),
    DATA_STATIC_MAPS("dataStaticMaps"),
    DATA_STATIC_PLACES("dataStaticPlaces"),
    DATA_STATIC_SERVICES("dataStaticServices"),
    DATA_STATIC_SPLASHES("dataStaticSplashes"),
    DATA_STATIC_URLS("dataStaticUrls"),
    LINK_WIKI_LOOKUP("linkWikiLookup"),
    LINK_WYNNCRAFT_PLAYER_STATS("linkWynncraftPlayerStats"),
    LINK_WYNNDATA_ITEM_LOOKUP("linkWynndataItemLookup"),
    LINK_WYNNTILS_DISCORD_INVITE("linkWynntilsDiscordInvite"),
    LINK_WYNNTILS_PATREON("linkWynntilsPatreon"),
    LINK_WYNNTILS_REGISTER_ACCOUNT("linkWynntilsRegisterAccount"),
    LINK_WYNNTILS_STATUS("linkWynntilsStatus");

    private final String id;

    UrlId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static Optional<UrlId> from(String str) {
        for (UrlId urlId : values()) {
            if (urlId.getId().equals(str)) {
                return Optional.of(urlId);
            }
        }

        // We have not found an enum for this key.
        // Don't consider this fatal, the user may have an outdated, or rather, a "too new" version of urls.json for
        // this version of the mod

        return Optional.empty();
    }
}
