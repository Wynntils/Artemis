/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.consumers.features;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.mod.type.CrashType;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.features.DiscordRichPresenceFeature;
import com.wynntils.features.ExtendedSeasonLeaderboardFeature;
import com.wynntils.features.LootrunFeature;
import com.wynntils.features.MythicFoundFeature;
import com.wynntils.features.TerritoryDefenseMessageFeature;
import com.wynntils.features.chat.BombBellRelayFeature;
import com.wynntils.features.chat.ChatCoordinatesFeature;
import com.wynntils.features.chat.ChatItemFeature;
import com.wynntils.features.chat.ChatMentionFeature;
import com.wynntils.features.chat.ChatTabsFeature;
import com.wynntils.features.chat.ChatTimestampFeature;
import com.wynntils.features.chat.DeathCoordinatesFeature;
import com.wynntils.features.chat.DialogueOptionOverrideFeature;
import com.wynntils.features.chat.GuildRankReplacementFeature;
import com.wynntils.features.chat.InputTranscriptionFeature;
import com.wynntils.features.chat.MessageFilterFeature;
import com.wynntils.features.chat.RevealNicknamesFeature;
import com.wynntils.features.combat.AbbreviateMobHealthFeature;
import com.wynntils.features.combat.ContentTrackerFeature;
import com.wynntils.features.combat.CustomLootrunBeaconsFeature;
import com.wynntils.features.combat.FixCastingSpellsFromInventoryFeature;
import com.wynntils.features.combat.HealthPotionBlockerFeature;
import com.wynntils.features.combat.HorseMountFeature;
import com.wynntils.features.combat.LowHealthVignetteFeature;
import com.wynntils.features.combat.MythicBlockerFeature;
import com.wynntils.features.combat.MythicBoxScalerFeature;
import com.wynntils.features.combat.PreventTradesDuelsFeature;
import com.wynntils.features.combat.QuickCastFeature;
import com.wynntils.features.combat.RangeVisualizerFeature;
import com.wynntils.features.combat.ShamanTotemTrackingFeature;
import com.wynntils.features.combat.SpellCastVignetteFeature;
import com.wynntils.features.combat.TokenTrackerBellFeature;
import com.wynntils.features.commands.AddCommandExpansionFeature;
import com.wynntils.features.commands.CommandAliasesFeature;
import com.wynntils.features.commands.CustomCommandKeybindsFeature;
import com.wynntils.features.commands.FilterAdminCommandsFeature;
import com.wynntils.features.debug.AbilityTreeDataDumpFeature;
import com.wynntils.features.debug.ConnectionProgressFeature;
import com.wynntils.features.debug.ContentBookDumpFeature;
import com.wynntils.features.debug.FunctionDumpFeature;
import com.wynntils.features.debug.ItemDebugTooltipsFeature;
import com.wynntils.features.debug.LogItemInfoFeature;
import com.wynntils.features.debug.MappingProgressFeature;
import com.wynntils.features.debug.PacketDebuggerFeature;
import com.wynntils.features.embellishments.WarHornFeature;
import com.wynntils.features.embellishments.WybelSoundFeature;
import com.wynntils.features.embellishments.WynntilsCosmeticsFeature;
import com.wynntils.features.inventory.ContainerSearchFeature;
import com.wynntils.features.inventory.DisableRecipeBookFeature;
import com.wynntils.features.inventory.DurabilityArcFeature;
import com.wynntils.features.inventory.EmeraldPouchFillArcFeature;
import com.wynntils.features.inventory.EmeraldPouchHotkeyFeature;
import com.wynntils.features.inventory.ExtendedItemCountFeature;
import com.wynntils.features.inventory.GuildBankHotkeyFeature;
import com.wynntils.features.inventory.HightlightDuplicateCosmeticsFeature;
import com.wynntils.features.inventory.IngredientPouchHotkeyFeature;
import com.wynntils.features.inventory.InventoryEmeraldCountFeature;
import com.wynntils.features.inventory.ItemFavoriteFeature;
import com.wynntils.features.inventory.ItemHighlightFeature;
import com.wynntils.features.inventory.ItemLockFeature;
import com.wynntils.features.inventory.ItemScreenshotFeature;
import com.wynntils.features.inventory.ItemTextOverlayFeature;
import com.wynntils.features.inventory.LootchestTextFeature;
import com.wynntils.features.inventory.PersonalStorageUtilitiesFeature;
import com.wynntils.features.inventory.UnidentifiedItemIconFeature;
import com.wynntils.features.map.BeaconBeamFeature;
import com.wynntils.features.map.GuildMapFeature;
import com.wynntils.features.map.MainMapFeature;
import com.wynntils.features.map.MinimapFeature;
import com.wynntils.features.map.WorldWaypointDistanceFeature;
import com.wynntils.features.overlays.ArrowShieldTrackerOverlayFeature;
import com.wynntils.features.overlays.BombBellOverlayFeature;
import com.wynntils.features.overlays.CombatExperienceOverlayFeature;
import com.wynntils.features.overlays.ContentTrackerOverlayFeature;
import com.wynntils.features.overlays.CustomBarsOverlayFeature;
import com.wynntils.features.overlays.CustomPlayerListOverlayFeature;
import com.wynntils.features.overlays.GameBarsOverlayFeature;
import com.wynntils.features.overlays.GameNotificationOverlayFeature;
import com.wynntils.features.overlays.InfoBoxFeature;
import com.wynntils.features.overlays.LootrunOverlaysFeature;
import com.wynntils.features.overlays.MobTotemTimerOverlayFeature;
import com.wynntils.features.overlays.NpcDialogueFeature;
import com.wynntils.features.overlays.ObjectivesOverlayFeature;
import com.wynntils.features.overlays.PartyMembersOverlayFeature;
import com.wynntils.features.overlays.PowderSpecialBarOverlayFeature;
import com.wynntils.features.overlays.RaidProgressFeature;
import com.wynntils.features.overlays.ServerUptimeInfoOverlayFeature;
import com.wynntils.features.overlays.ShamanMaskOverlayFeature;
import com.wynntils.features.overlays.ShamanTotemTimerOverlayFeature;
import com.wynntils.features.overlays.SpellCastMessageOverlayFeature;
import com.wynntils.features.overlays.StatusEffectsOverlayFeature;
import com.wynntils.features.overlays.StopwatchFeature;
import com.wynntils.features.overlays.TerritoryAttackTimerOverlayFeature;
import com.wynntils.features.overlays.TokenBarsOverlayFeature;
import com.wynntils.features.overlays.TowerEffectOverlayFeature;
import com.wynntils.features.overlays.TowerStatsFeature;
import com.wynntils.features.players.AutoJoinPartyFeature;
import com.wynntils.features.players.CustomNametagRendererFeature;
import com.wynntils.features.players.GearViewerFeature;
import com.wynntils.features.players.HadesFeature;
import com.wynntils.features.players.PartyManagementScreenFeature;
import com.wynntils.features.players.PlayerArmorHidingFeature;
import com.wynntils.features.players.PlayerGhostTransparencyFeature;
import com.wynntils.features.redirects.AbilityRefreshRedirectFeature;
import com.wynntils.features.redirects.BlacksmithRedirectFeature;
import com.wynntils.features.redirects.ChatRedirectFeature;
import com.wynntils.features.redirects.InventoryRedirectFeature;
import com.wynntils.features.redirects.TerritoryMessageRedirectFeature;
import com.wynntils.features.tooltips.ItemCompareFeature;
import com.wynntils.features.tooltips.ItemGuessFeature;
import com.wynntils.features.tooltips.ItemStatInfoFeature;
import com.wynntils.features.tooltips.TooltipFittingFeature;
import com.wynntils.features.tooltips.TooltipVanillaHideFeature;
import com.wynntils.features.trademarket.TradeMarketAutoOpenChatFeature;
import com.wynntils.features.trademarket.TradeMarketBulkSellFeature;
import com.wynntils.features.trademarket.TradeMarketPriceConversionFeature;
import com.wynntils.features.trademarket.TradeMarketPriceMatchFeature;
import com.wynntils.features.ui.BulkBuyFeature;
import com.wynntils.features.ui.ContainerScrollFeature;
import com.wynntils.features.ui.CosmeticsPreviewFeature;
import com.wynntils.features.ui.CraftingProfessionLevelProgressBarFeature;
import com.wynntils.features.ui.CustomCharacterSelectionScreenFeature;
import com.wynntils.features.ui.CustomLoadingScreenFeature;
import com.wynntils.features.ui.CustomSeaskipperScreenFeature;
import com.wynntils.features.ui.CustomTerritoryManagementScreenFeature;
import com.wynntils.features.ui.CustomTradeMarketResultScreenFeature;
import com.wynntils.features.ui.LobbyUptimeFeature;
import com.wynntils.features.ui.ProfessionHighlightFeature;
import com.wynntils.features.ui.WynncraftButtonFeature;
import com.wynntils.features.ui.WynncraftPauseScreenFeature;
import com.wynntils.features.ui.WynntilsContentBookFeature;
import com.wynntils.features.utilities.AutoApplyResourcePackFeature;
import com.wynntils.features.utilities.GammabrightFeature;
import com.wynntils.features.utilities.PerCharacterGuildContributionFeature;
import com.wynntils.features.utilities.SilencerFeature;
import com.wynntils.features.utilities.SkillPointLoadoutsFeature;
import com.wynntils.features.utilities.TranscribeMessagesFeature;
import com.wynntils.features.utilities.TranslationFeature;
import com.wynntils.features.utilities.XpGainMessageFeature;
import com.wynntils.features.wynntils.BetaWarningFeature;
import com.wynntils.features.wynntils.ChangelogFeature;
import com.wynntils.features.wynntils.CommandsFeature;
import com.wynntils.features.wynntils.DataCrowdSourcingFeature;
import com.wynntils.features.wynntils.FixPacketBugsFeature;
import com.wynntils.features.wynntils.TelemetryFeature;
import com.wynntils.features.wynntils.UpdatesFeature;
import com.wynntils.features.wynntils.WeeklyConfigBackupFeature;
import com.wynntils.mc.event.ClientsideMessageEvent;
import com.wynntils.mc.event.CommandsAddedEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;

/** Loads {@link Feature}s */
public final class FeatureManager extends Manager {
    private static final Map<Feature, FeatureState> FEATURES = new LinkedHashMap<>();
    private static final Map<Class<? extends Feature>, Feature> FEATURE_INSTANCES = new LinkedHashMap<>();

    private final FeatureCommands commands = new FeatureCommands();

    public FeatureManager() {
        super(List.of());
    }

    @SubscribeEvent
    public void onCommandsAdded(CommandsAddedEvent event) {
        // Register feature commands
        Managers.Command.addNode(event.getRoot(), commands.getCommandNode());
    }

    public void init() {
        // debug
        registerFeature(new AbilityTreeDataDumpFeature());
        registerFeature(new ConnectionProgressFeature());
        registerFeature(new ContentBookDumpFeature());
        registerFeature(new FunctionDumpFeature());
        registerFeature(new ItemDebugTooltipsFeature());
        registerFeature(new LogItemInfoFeature());
        registerFeature(new MappingProgressFeature());
        registerFeature(new PacketDebuggerFeature());

        // always on
        registerFeature(new LootrunFeature());

        // region chat
        registerFeature(new BombBellRelayFeature());
        registerFeature(new ChatCoordinatesFeature());
        registerFeature(new ChatItemFeature());
        registerFeature(new ChatMentionFeature());
        registerFeature(new ChatTabsFeature());
        registerFeature(new ChatTimestampFeature());
        registerFeature(new DeathCoordinatesFeature());
        registerFeature(new DialogueOptionOverrideFeature());
        registerFeature(new GuildRankReplacementFeature());
        registerFeature(new InputTranscriptionFeature());
        registerFeature(new MessageFilterFeature());
        registerFeature(new RevealNicknamesFeature());
        // endregion

        // region combat
        registerFeature(new AbbreviateMobHealthFeature());
        registerFeature(new ContentTrackerFeature());
        registerFeature(new CustomLootrunBeaconsFeature());
        registerFeature(new FixCastingSpellsFromInventoryFeature());
        registerFeature(new HealthPotionBlockerFeature());
        registerFeature(new HorseMountFeature());
        registerFeature(new LowHealthVignetteFeature());
        registerFeature(new MythicBlockerFeature());
        registerFeature(new MythicBoxScalerFeature());
        registerFeature(new PreventTradesDuelsFeature());
        registerFeature(new QuickCastFeature());
        registerFeature(new RangeVisualizerFeature());
        registerFeature(new ShamanTotemTrackingFeature());
        registerFeature(new SpellCastVignetteFeature());
        registerFeature(new TokenTrackerBellFeature());
        // endregion

        // region commands
        registerFeature(new AddCommandExpansionFeature());
        registerFeature(new CommandAliasesFeature());
        registerFeature(new CustomCommandKeybindsFeature());
        registerFeature(new FilterAdminCommandsFeature());
        // endregion

        // region embellishments
        registerFeature(new WarHornFeature());
        registerFeature(new WybelSoundFeature());
        registerFeature(new WynntilsCosmeticsFeature());
        // endregion

        // region inventory
        registerFeature(new ContainerSearchFeature());
        registerFeature(new DisableRecipeBookFeature());
        registerFeature(new DurabilityArcFeature());
        registerFeature(new EmeraldPouchFillArcFeature());
        registerFeature(new EmeraldPouchHotkeyFeature());
        registerFeature(new ExtendedItemCountFeature());
        registerFeature(new GuildBankHotkeyFeature());
        registerFeature(new HightlightDuplicateCosmeticsFeature());
        registerFeature(new IngredientPouchHotkeyFeature());
        registerFeature(new InventoryEmeraldCountFeature());
        registerFeature(new ItemFavoriteFeature());
        registerFeature(new ItemHighlightFeature());
        registerFeature(new ItemLockFeature());
        registerFeature(new ItemScreenshotFeature());
        registerFeature(new ItemTextOverlayFeature());
        registerFeature(new LootchestTextFeature());
        registerFeature(new PersonalStorageUtilitiesFeature());
        registerFeature(new UnidentifiedItemIconFeature());
        // endregion

        // region map
        registerFeature(new BeaconBeamFeature());
        registerFeature(new GuildMapFeature());
        registerFeature(new MainMapFeature());
        registerFeature(new MinimapFeature());
        registerFeature(new WorldWaypointDistanceFeature());
        // endregion

        // region overlays
        registerFeature(new ArrowShieldTrackerOverlayFeature());
        registerFeature(new BombBellOverlayFeature());
        registerFeature(new CombatExperienceOverlayFeature());
        registerFeature(new ContentTrackerOverlayFeature());
        registerFeature(new CustomBarsOverlayFeature());
        registerFeature(new CustomPlayerListOverlayFeature());
        registerFeature(new GameBarsOverlayFeature());
        registerFeature(new GameNotificationOverlayFeature());
        registerFeature(new InfoBoxFeature());
        registerFeature(new LootrunOverlaysFeature());
        registerFeature(new MobTotemTimerOverlayFeature());
        registerFeature(new NpcDialogueFeature());
        registerFeature(new ObjectivesOverlayFeature());
        registerFeature(new PartyMembersOverlayFeature());
        registerFeature(new PowderSpecialBarOverlayFeature());
        registerFeature(new RaidProgressFeature());
        registerFeature(new ServerUptimeInfoOverlayFeature());
        registerFeature(new ShamanMaskOverlayFeature());
        registerFeature(new ShamanTotemTimerOverlayFeature());
        registerFeature(new SpellCastMessageOverlayFeature());
        registerFeature(new StatusEffectsOverlayFeature());
        registerFeature(new StopwatchFeature());
        registerFeature(new TerritoryAttackTimerOverlayFeature());
        registerFeature(new TokenBarsOverlayFeature());
        registerFeature(new TowerEffectOverlayFeature());
        registerFeature(new TowerStatsFeature());
        // endregion

        // region players
        registerFeature(new AutoJoinPartyFeature());
        registerFeature(new CustomNametagRendererFeature());
        registerFeature(new GearViewerFeature());
        registerFeature(new HadesFeature());
        registerFeature(new PartyManagementScreenFeature());
        registerFeature(new PlayerArmorHidingFeature());
        registerFeature(new PlayerGhostTransparencyFeature());
        // endregion

        // region redirects
        registerFeature(new AbilityRefreshRedirectFeature());
        registerFeature(new BlacksmithRedirectFeature());
        registerFeature(new ChatRedirectFeature());
        registerFeature(new InventoryRedirectFeature());
        registerFeature(new TerritoryMessageRedirectFeature());
        // endregion

        // region tooltips
        registerFeature(new ItemCompareFeature());
        registerFeature(new ItemGuessFeature());
        registerFeature(new ItemStatInfoFeature());
        registerFeature(new TooltipFittingFeature());
        registerFeature(new TooltipVanillaHideFeature());
        // endregion

        // region trademarket
        registerFeature(new TradeMarketAutoOpenChatFeature());
        registerFeature(new TradeMarketBulkSellFeature());
        registerFeature(new TradeMarketPriceConversionFeature());
        registerFeature(new TradeMarketPriceMatchFeature());
        // endregion

        // region ui
        registerFeature(new BulkBuyFeature());
        registerFeature(new ContainerScrollFeature());
        registerFeature(new CosmeticsPreviewFeature());
        registerFeature(new CraftingProfessionLevelProgressBarFeature());
        registerFeature(new CustomCharacterSelectionScreenFeature());
        registerFeature(new CustomLoadingScreenFeature());
        registerFeature(new CustomSeaskipperScreenFeature());
        registerFeature(new CustomTerritoryManagementScreenFeature());
        registerFeature(new CustomTradeMarketResultScreenFeature());
        registerFeature(new LobbyUptimeFeature());
        registerFeature(new ProfessionHighlightFeature());
        registerFeature(new WynncraftButtonFeature());
        registerFeature(new WynncraftPauseScreenFeature());
        registerFeature(new WynntilsContentBookFeature());
        // endregion

        // region utilities
        registerFeature(new AutoApplyResourcePackFeature());
        registerFeature(new GammabrightFeature());
        registerFeature(new PerCharacterGuildContributionFeature());
        registerFeature(new SilencerFeature());
        registerFeature(new SkillPointLoadoutsFeature());
        registerFeature(new TranscribeMessagesFeature());
        registerFeature(new TranslationFeature());
        registerFeature(new XpGainMessageFeature());
        // endregion

        // region wynntils
        registerFeature(new BetaWarningFeature());
        registerFeature(new ChangelogFeature());
        registerFeature(new CommandsFeature());
        registerFeature(new DataCrowdSourcingFeature());
        registerFeature(new FixPacketBugsFeature());
        registerFeature(new TelemetryFeature());
        registerFeature(new UpdatesFeature());
        registerFeature(new WeeklyConfigBackupFeature());
        // endregion

        // region uncategorized
        registerFeature(new DiscordRichPresenceFeature());
        registerFeature(new ExtendedSeasonLeaderboardFeature());
        registerFeature(new MythicFoundFeature());
        registerFeature(new TerritoryDefenseMessageFeature());
        // endregion

        // Reload Minecraft's config files so our own keybinds get loaded
        // This is needed because we are late to register the keybinds,
        // but we cannot move it earlier to the init process because of I18n
        synchronized (McUtils.options()) {
            McUtils.options().load();
        }

        commands.init();

        addCrashCallbacks();
    }

    private void registerFeature(Feature feature) {
        FEATURES.put(feature, FeatureState.DISABLED);
        FEATURE_INSTANCES.put(feature.getClass(), feature);

        try {
            initializeFeature(feature);
        } catch (AssertionError ae) {
            WynntilsMod.error("Fix i18n for " + feature.getClass().getSimpleName(), ae);
            if (WynntilsMod.isDevelopmentEnvironment()) {
                System.exit(1);
            }
        } catch (Throwable exception) {
            // Log and handle gracefully, just disable this feature
            crashFeature(feature);
            WynntilsMod.reportCrash(
                    CrashType.FEATURE,
                    feature.getClass().getSimpleName(),
                    feature.getClass().getName(),
                    "init",
                    false,
                    true,
                    exception);
        }
    }

    private void initializeFeature(Feature feature) {
        Class<? extends Feature> featureClass = feature.getClass();

        // Set feature category
        ConfigCategory configCategory = feature.getClass().getAnnotation(ConfigCategory.class);
        Category category = configCategory != null ? configCategory.value() : Category.UNCATEGORIZED;
        feature.setCategory(category);

        // Register commands and key binds
        commands.discoverCommands(feature);
        Managers.KeyBind.discoverKeyBinds(feature);

        // Determine if feature should be enabled & set default enabled value for user features
        boolean startDisabled = featureClass.isAnnotationPresent(StartDisabled.class);
        feature.userEnabled.store(!startDisabled);

        Managers.Overlay.discoverOverlays(feature);
        Managers.Overlay.discoverOverlayGroups(feature);

        // Assert that the feature name is properly translated
        assert !feature.getTranslatedName().startsWith("feature.wynntils.")
                : "Fix i18n for " + feature.getTranslatedName();

        // Assert that the feature description is properly translated
        assert !feature.getTranslatedDescription().startsWith("feature.wynntils.")
                : "Fix i18n for " + feature.getTranslatedDescription();

        if (!feature.userEnabled.get()) return; // not enabled by user

        enableFeature(feature);
    }

    public void enableFeature(Feature feature) {
        if (!FEATURES.containsKey(feature)) {
            throw new IllegalArgumentException("Tried to enable an unregistered feature: " + feature);
        }

        FeatureState state = FEATURES.get(feature);

        if (state != FeatureState.DISABLED && state != FeatureState.CRASHED) return;

        feature.onEnable();

        FEATURES.put(feature, FeatureState.ENABLED);

        WynntilsMod.registerEventListener(feature);

        Managers.Overlay.enableOverlays(feature);

        Managers.KeyBind.enableFeatureKeyBinds(feature);
    }

    public void disableFeature(Feature feature) {
        if (!FEATURES.containsKey(feature)) {
            throw new IllegalArgumentException("Tried to disable an unregistered feature: " + feature);
        }

        FeatureState state = FEATURES.get(feature);

        if (state != FeatureState.ENABLED) return;

        feature.onDisable();

        FEATURES.put(feature, FeatureState.DISABLED);

        WynntilsMod.unregisterEventListener(feature);

        Managers.Overlay.disableOverlays(feature);

        Managers.KeyBind.disableFeatureKeyBinds(feature);
    }

    public void crashFeature(Feature feature) {
        if (!FEATURES.containsKey(feature)) {
            throw new IllegalArgumentException("Tried to crash an unregistered feature: " + feature);
        }

        disableFeature(feature);

        FEATURES.put(feature, FeatureState.CRASHED);
    }

    private FeatureState getFeatureState(Feature feature) {
        if (!FEATURES.containsKey(feature)) {
            throw new IllegalArgumentException(
                    "Feature " + feature + " is not registered, but was was queried for its state");
        }

        return FEATURES.get(feature);
    }

    public boolean isEnabled(Feature feature) {
        return getFeatureState(feature) == FeatureState.ENABLED;
    }

    public List<Feature> getFeatures() {
        return FEATURES.keySet().stream().toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Feature> T getFeatureInstance(Class<T> featureClass) {
        return (T) FEATURE_INSTANCES.get(featureClass);
    }

    public Optional<Feature> getFeatureFromString(String featureName) {
        return getFeatures().stream()
                .filter(feature -> feature.getShortName().equals(featureName))
                .findFirst();
    }

    public void handleExceptionInEventListener(Event event, String featureClassName, Throwable t) {
        String featureName = featureClassName.substring(featureClassName.lastIndexOf('.') + 1);

        Optional<Feature> featureOptional = getFeatureFromString(featureName);
        if (featureOptional.isEmpty()) {
            WynntilsMod.error("Exception in event listener in feature that cannot be located: " + featureClassName, t);
            return;
        }

        Feature feature = featureOptional.get();

        crashFeature(feature);

        // If a crash happens in a client-side message event, and we send a new message about disabling X feature,
        // we will cause a new exception and an endless recursion.
        boolean shouldSendChat = !(event instanceof ClientsideMessageEvent);

        WynntilsMod.reportCrash(
                CrashType.FEATURE,
                feature.getTranslatedName(),
                feature.getClass().getName(),
                "event listener",
                shouldSendChat,
                true,
                t);

        if (shouldSendChat) {
            MutableComponent enableMessage = Component.literal("Click here to enable it again.")
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(ChatFormatting.RED)
                    .withStyle(style -> style.withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND, "/feature enable " + feature.getShortName())));

            McUtils.sendMessageToClient(enableMessage);
        }
    }

    private void addCrashCallbacks() {
        Managers.CrashReport.registerCrashContext("Loaded Features", () -> {
            StringBuilder result = new StringBuilder();

            for (Feature feature : FEATURES.keySet()) {
                if (feature.isEnabled()) {
                    result.append("\n\t\t").append(feature.getTranslatedName());
                }
            }

            return result.toString();
        });
    }
}
