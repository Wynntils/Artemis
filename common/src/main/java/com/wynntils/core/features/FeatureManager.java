/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.features;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.commands.CommandManager;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.overlays.OverlayManager;
import com.wynntils.core.features.properties.StartDisabled;
import com.wynntils.core.keybinds.KeyBindManager;
import com.wynntils.core.mod.CrashReportManager;
import com.wynntils.core.mod.type.CrashType;
import com.wynntils.features.GammabrightFeature;
import com.wynntils.features.LootrunFeature;
import com.wynntils.features.TerritoryDefenseMessageFeature;
import com.wynntils.features.TranslationFeature;
import com.wynntils.features.chat.ChatCoordinatesFeature;
import com.wynntils.features.chat.ChatItemFeature;
import com.wynntils.features.chat.ChatMentionFeature;
import com.wynntils.features.chat.ChatTabsFeature;
import com.wynntils.features.chat.ChatTimestampFeature;
import com.wynntils.features.chat.DeathCoordinatesFeature;
import com.wynntils.features.chat.DialogueOptionOverrideFeature;
import com.wynntils.features.chat.GuildRankReplacementFeature;
import com.wynntils.features.chat.MessageFilterFeature;
import com.wynntils.features.chat.TradeMarketAutoOpenChatFeature;
import com.wynntils.features.chat.TradeMarketPriceConversionFeature;
import com.wynntils.features.combat.AbbreviateMobHealthFeature;
import com.wynntils.features.combat.CombatXpGainMessageFeature;
import com.wynntils.features.combat.FixCastingSpellsFromInventoryFeature;
import com.wynntils.features.combat.HealthPotionBlockerFeature;
import com.wynntils.features.combat.HorseMountFeature;
import com.wynntils.features.combat.LowHealthVignetteFeature;
import com.wynntils.features.combat.MythicBlockerFeature;
import com.wynntils.features.combat.MythicBoxScalerFeature;
import com.wynntils.features.combat.PreventTradesDuelsFeature;
import com.wynntils.features.combat.QuickCastFeature;
import com.wynntils.features.combat.RangeVisualizerFeature;
import com.wynntils.features.commands.AddCommandExpansionFeature;
import com.wynntils.features.commands.CommandAliasesFeature;
import com.wynntils.features.commands.CustomCommandKeybindsFeature;
import com.wynntils.features.commands.FilterAdminCommandsFeature;
import com.wynntils.features.debug.AbilityTreeDataDumpFeature;
import com.wynntils.features.debug.ConnectionProgressFeature;
import com.wynntils.features.debug.ItemDebugTooltipsFeature;
import com.wynntils.features.debug.LogItemInfoFeature;
import com.wynntils.features.debug.PacketDebuggerFeature;
import com.wynntils.features.embellishments.MythicFoundSoundFeature;
import com.wynntils.features.embellishments.WybelSoundFeature;
import com.wynntils.features.inventory.BulkBuyFeature;
import com.wynntils.features.inventory.ContainerSearchFeature;
import com.wynntils.features.inventory.DurabilityArcFeature;
import com.wynntils.features.inventory.EmeraldPouchFillArcFeature;
import com.wynntils.features.inventory.EmeraldPouchHotkeyFeature;
import com.wynntils.features.inventory.ExtendedItemCountFeature;
import com.wynntils.features.inventory.IngredientPouchHotkeyFeature;
import com.wynntils.features.inventory.InventoryEmeraldCountFeature;
import com.wynntils.features.inventory.ItemFavoriteFeature;
import com.wynntils.features.inventory.ItemHighlightFeature;
import com.wynntils.features.inventory.ItemLockFeature;
import com.wynntils.features.inventory.ItemScreenshotFeature;
import com.wynntils.features.inventory.ItemTextOverlayFeature;
import com.wynntils.features.inventory.ReplaceRecipeBookFeature;
import com.wynntils.features.inventory.UnidentifiedItemIconFeature;
import com.wynntils.features.map.BeaconBeamFeature;
import com.wynntils.features.map.GatheringNodeCooldownFeature;
import com.wynntils.features.map.GuildMapFeature;
import com.wynntils.features.map.MapFeature;
import com.wynntils.features.map.MinimapFeature;
import com.wynntils.features.map.WorldWaypointDistanceFeature;
import com.wynntils.features.overlays.ArrowShieldTrackingFeature;
import com.wynntils.features.overlays.AuraTimerOverlayFeature;
import com.wynntils.features.overlays.CombatExperienceOverlayFeature;
import com.wynntils.features.overlays.CustomBarsOverlayFeature;
import com.wynntils.features.overlays.GameBarsOverlayFeature;
import com.wynntils.features.overlays.GameNotificationOverlayFeature;
import com.wynntils.features.overlays.GuildAttackTimerOverlayFeature;
import com.wynntils.features.overlays.HadesPartyOverlayFeature;
import com.wynntils.features.overlays.InfoBoxFeature;
import com.wynntils.features.overlays.MobTotemTrackingFeature;
import com.wynntils.features.overlays.NpcDialogueOverlayFeature;
import com.wynntils.features.overlays.ObjectivesOverlayFeature;
import com.wynntils.features.overlays.PowderSpecialBarOverlayFeature;
import com.wynntils.features.overlays.QuestInfoOverlayFeature;
import com.wynntils.features.overlays.ShamanMasksOverlayFeature;
import com.wynntils.features.overlays.ShamanTotemTrackingFeature;
import com.wynntils.features.overlays.SpellCastRenderFeature;
import com.wynntils.features.overlays.StatusOverlayFeature;
import com.wynntils.features.overlays.StopwatchFeature;
import com.wynntils.features.overlays.TokenTrackerFeature;
import com.wynntils.features.overlays.TradeMarketBulkSellFeature;
import com.wynntils.features.players.AutoJoinPartyFeature;
import com.wynntils.features.players.CustomNametagRendererFeature;
import com.wynntils.features.players.GearViewerFeature;
import com.wynntils.features.players.HadesFeature;
import com.wynntils.features.players.PartyManagementScreenFeature;
import com.wynntils.features.players.PlayerArmorHidingFeature;
import com.wynntils.features.players.PlayerGhostTransparencyFeature;
import com.wynntils.features.players.WynntilsCosmeticsFeature;
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
import com.wynntils.features.ui.AutoApplyResourcePackFeature;
import com.wynntils.features.ui.ContainerScrollFeature;
import com.wynntils.features.ui.CosmeticsPreviewFeature;
import com.wynntils.features.ui.CustomAbilityTreeFeature;
import com.wynntils.features.ui.CustomCharacterSelectionScreenFeature;
import com.wynntils.features.ui.CustomLoadingScreenFeature;
import com.wynntils.features.ui.CustomSeaskipperScreenFeature;
import com.wynntils.features.ui.LobbyUptimeFeature;
import com.wynntils.features.ui.SoulPointTimerFeature;
import com.wynntils.features.ui.WynncraftButtonFeature;
import com.wynntils.features.ui.WynncraftPauseScreenFeature;
import com.wynntils.features.ui.WynntilsQuestBookFeature;
import com.wynntils.features.wynntils.BetaWarningFeature;
import com.wynntils.features.wynntils.ChangelogFeature;
import com.wynntils.features.wynntils.CommandsFeature;
import com.wynntils.features.wynntils.FixPacketBugsFeature;
import com.wynntils.features.wynntils.TelemetryFeature;
import com.wynntils.features.wynntils.UpdatesFeature;
import com.wynntils.mc.event.ClientsideMessageEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.Event;

/** Loads {@link Feature}s */
public final class FeatureManager extends Manager {
    private static final Map<Feature, FeatureState> FEATURES = new LinkedHashMap<>();
    private static final Map<Class<? extends Feature>, Feature> FEATURE_INSTANCES = new LinkedHashMap<>();

    private final FeatureCommands commands = new FeatureCommands();

    public FeatureManager(
            CommandManager command, CrashReportManager crashReport, KeyBindManager keyBind, OverlayManager overlay) {
        super(List.of(command, crashReport, keyBind, overlay));
    }

    public void init() {
        // debug
        registerFeature(new AbilityTreeDataDumpFeature());
        registerFeature(new ConnectionProgressFeature());
        registerFeature(new ItemDebugTooltipsFeature());
        registerFeature(new LogItemInfoFeature());
        registerFeature(new PacketDebuggerFeature());

        // always on
        registerFeature(new LootrunFeature());

        // region chat
        registerFeature(new ChatCoordinatesFeature());
        registerFeature(new ChatItemFeature());
        registerFeature(new ChatMentionFeature());
        registerFeature(new ChatTabsFeature());
        registerFeature(new ChatTimestampFeature());
        registerFeature(new DeathCoordinatesFeature());
        registerFeature(new DialogueOptionOverrideFeature());
        registerFeature(new GuildRankReplacementFeature());
        registerFeature(new MessageFilterFeature());
        registerFeature(new TradeMarketAutoOpenChatFeature());
        registerFeature(new TradeMarketPriceConversionFeature());
        // endregion

        // region combat
        registerFeature(new AbbreviateMobHealthFeature());
        registerFeature(new CombatXpGainMessageFeature());
        registerFeature(new FixCastingSpellsFromInventoryFeature());
        registerFeature(new HealthPotionBlockerFeature());
        registerFeature(new HorseMountFeature());
        registerFeature(new LowHealthVignetteFeature());
        registerFeature(new MythicBlockerFeature());
        registerFeature(new MythicBoxScalerFeature());
        registerFeature(new PreventTradesDuelsFeature());
        registerFeature(new QuickCastFeature());
        registerFeature(new RangeVisualizerFeature());
        // endregion

        // region commands
        registerFeature(new AddCommandExpansionFeature());
        registerFeature(new CommandAliasesFeature());
        registerFeature(new CommandsFeature());
        registerFeature(new ContainerSearchFeature());
        registerFeature(new CosmeticsPreviewFeature());
        registerFeature(new CustomBarsOverlayFeature());
        registerFeature(new CustomCharacterSelectionScreenFeature());
        registerFeature(new CustomCommandKeybindsFeature());
        registerFeature(new FilterAdminCommandsFeature());
        // endregion

        // region embellishments
        registerFeature(new MythicFoundSoundFeature());
        registerFeature(new WybelSoundFeature());
        // endregion

        // region inventory
        registerFeature(new BulkBuyFeature());
        registerFeature(new ContainerSearchFeature());
        registerFeature(new DurabilityArcFeature());
        registerFeature(new EmeraldPouchFillArcFeature());
        registerFeature(new EmeraldPouchHotkeyFeature());
        registerFeature(new ExtendedItemCountFeature());
        registerFeature(new IngredientPouchHotkeyFeature());
        registerFeature(new InventoryEmeraldCountFeature());
        registerFeature(new ItemFavoriteFeature());
        registerFeature(new ItemHighlightFeature());
        registerFeature(new ItemLockFeature());
        registerFeature(new ItemScreenshotFeature());
        registerFeature(new ItemTextOverlayFeature());
        registerFeature(new ReplaceRecipeBookFeature());
        registerFeature(new UnidentifiedItemIconFeature());
        // endregion

        // region map
        registerFeature(new BeaconBeamFeature());
        registerFeature(new GatheringNodeCooldownFeature());
        registerFeature(new GuildMapFeature());
        registerFeature(new MapFeature());
        registerFeature(new MinimapFeature());
        registerFeature(new WorldWaypointDistanceFeature());
        // endregion

        // region overlays
        registerFeature(new ArrowShieldTrackingFeature());
        registerFeature(new AuraTimerOverlayFeature());
        registerFeature(new CombatExperienceOverlayFeature());
        registerFeature(new CustomBarsOverlayFeature());
        registerFeature(new GameBarsOverlayFeature());
        registerFeature(new GameNotificationOverlayFeature());
        registerFeature(new GuildAttackTimerOverlayFeature());
        registerFeature(new HadesPartyOverlayFeature());
        registerFeature(new InfoBoxFeature());
        registerFeature(new MobTotemTrackingFeature());
        registerFeature(new NpcDialogueOverlayFeature());
        registerFeature(new ObjectivesOverlayFeature());
        registerFeature(new PowderSpecialBarOverlayFeature());
        registerFeature(new QuestInfoOverlayFeature());
        registerFeature(new ShamanMasksOverlayFeature());
        registerFeature(new ShamanTotemTrackingFeature());
        registerFeature(new SpellCastRenderFeature());
        registerFeature(new StatusOverlayFeature());
        registerFeature(new StopwatchFeature());
        registerFeature(new TokenTrackerFeature());
        registerFeature(new TradeMarketBulkSellFeature());
        // endregion

        // region players
        registerFeature(new AutoJoinPartyFeature());
        registerFeature(new CustomNametagRendererFeature());
        registerFeature(new GearViewerFeature());
        registerFeature(new HadesFeature());
        registerFeature(new PartyManagementScreenFeature());
        registerFeature(new PlayerArmorHidingFeature());
        registerFeature(new PlayerGhostTransparencyFeature());
        registerFeature(new WynntilsCosmeticsFeature());
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

        // region ui
        registerFeature(new AutoApplyResourcePackFeature());
        registerFeature(new ContainerScrollFeature());
        registerFeature(new CosmeticsPreviewFeature());
        registerFeature(new CustomAbilityTreeFeature());
        registerFeature(new CustomCharacterSelectionScreenFeature());
        registerFeature(new CustomLoadingScreenFeature());
        registerFeature(new CustomSeaskipperScreenFeature());
        registerFeature(new LobbyUptimeFeature());
        registerFeature(new SoulPointTimerFeature());
        registerFeature(new WynncraftButtonFeature());
        registerFeature(new WynncraftPauseScreenFeature());
        registerFeature(new WynntilsQuestBookFeature());
        // endregion

        // region wynntils
        registerFeature(new BetaWarningFeature());
        registerFeature(new ChangelogFeature());
        registerFeature(new CommandsFeature());
        registerFeature(new FixPacketBugsFeature());
        registerFeature(new TelemetryFeature());
        registerFeature(new UpdatesFeature());
        // endregion

        // region uncategorized
        registerFeature(new GammabrightFeature());
        registerFeature(new TerritoryDefenseMessageFeature());
        registerFeature(new TranslationFeature());
        // endregion

        // Reload Minecraft's config files so our own keybinds get loaded
        // This is needed because we are late to register the keybinds,
        // but we cannot move it earlier to the init process because of I18n
        synchronized (McUtils.options()) {
            McUtils.options().load();
        }

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
                    feature.getClass().getName() + ":initialize",
                    feature.getClass().getSimpleName() + " during init",
                    CrashType.FEATURE,
                    exception,
                    false,
                    true);
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
        feature.userEnabled.updateConfig(!startDisabled);

        Managers.Overlay.discoverOverlays(feature);
        Managers.Overlay.discoverOverlayGroups(feature);

        // Assert that the feature name is properly translated
        assert !feature.getTranslatedName().startsWith("feature.wynntils.")
                : "Fix i18n for " + feature.getTranslatedName();

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

    private void crashFeature(Feature feature) {
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
                feature.getClass().getName(), feature.getTranslatedName(), CrashType.FEATURE, t, shouldSendChat, true);

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
