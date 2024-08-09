/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.components;

import com.wynntils.services.accountswitchdetection.AccountSwitchDetectionService;
import com.wynntils.services.athena.UpdateService;
import com.wynntils.services.athena.WynntilsAccountService;
import com.wynntils.services.chat.ChatTabService;
import com.wynntils.services.cosmetics.CosmeticsService;
import com.wynntils.services.destination.DestinationService;
import com.wynntils.services.discord.DiscordService;
import com.wynntils.services.favorites.FavoritesService;
import com.wynntils.services.hades.HadesService;
import com.wynntils.services.itemfilter.ItemFilterService;
import com.wynntils.services.itemrecord.ItemRecordService;
import com.wynntils.services.leaderboard.LeaderboardService;
import com.wynntils.services.lootrunpaths.LootrunPathsService;
import com.wynntils.services.map.MapService;
import com.wynntils.services.map.PoiService;
import com.wynntils.services.mapdata.MapDataService;
import com.wynntils.services.ping.PingService;
import com.wynntils.services.resourcepack.ResourcePackService;
import com.wynntils.services.splashes.SplashService;
import com.wynntils.services.statistics.StatisticsService;
import com.wynntils.services.stopwatch.StopwatchService;
import com.wynntils.services.translation.TranslationService;

public final class Services {
    public static final AccountSwitchDetectionService AccountSwitchDetection = new AccountSwitchDetectionService();
    public static final ChatTabService ChatTab = new ChatTabService();
    public static final CosmeticsService Cosmetics = new CosmeticsService();
    public static final DestinationService Destination = new DestinationService();
    public static final DiscordService Discord = new DiscordService();
    public static final FavoritesService Favorites = new FavoritesService();
    public static final HadesService Hades = new HadesService();
    public static final ItemFilterService ItemFilter = new ItemFilterService();
    public static final ItemRecordService ItemRecord = new ItemRecordService();
    public static final LeaderboardService Leaderboard = new LeaderboardService();
    public static final LootrunPathsService LootrunPaths = new LootrunPathsService();
    public static final MapDataService MapData = new MapDataService();
    public static final MapService Map = new MapService();
    public static final PingService Ping = new PingService();
    public static final PoiService Poi = new PoiService();
    public static final ResourcePackService ResourcePack = new ResourcePackService();
    public static final SplashService Splash = new SplashService();
    public static final StatisticsService Statistics = new StatisticsService();
    public static final StopwatchService Stopwatch = new StopwatchService();
    public static final TranslationService Translation = new TranslationService();
    public static final UpdateService Update = new UpdateService();
    public static final WynntilsAccountService WynntilsAccount = new WynntilsAccountService();
}
