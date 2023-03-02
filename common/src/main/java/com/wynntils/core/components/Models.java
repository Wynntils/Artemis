/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.components;

import com.wynntils.models.abilities.ArrowShieldModel;
import com.wynntils.models.abilities.BossBarModel;
import com.wynntils.models.abilities.ShamanMaskModel;
import com.wynntils.models.abilities.ShamanTotemModel;
import com.wynntils.models.character.CharacterModel;
import com.wynntils.models.character.CharacterSelectionModel;
import com.wynntils.models.character.PlayerInventoryModel;
import com.wynntils.models.containers.ContainerModel;
import com.wynntils.models.containers.LootChestModel;
import com.wynntils.models.damage.DamageModel;
import com.wynntils.models.discoveries.DiscoveryModel;
import com.wynntils.models.elements.ElementModel;
import com.wynntils.models.emeralds.EmeraldModel;
import com.wynntils.models.experience.CombatXpModel;
import com.wynntils.models.favorites.FavoritesModel;
import com.wynntils.models.gear.GearModel;
import com.wynntils.models.gear.GearTooltipModel;
import com.wynntils.models.horse.HorseModel;
import com.wynntils.models.ingredients.IngredientModel;
import com.wynntils.models.items.ItemModel;
import com.wynntils.models.lootruns.LootrunModel;
import com.wynntils.models.map.CompassModel;
import com.wynntils.models.map.MapModel;
import com.wynntils.models.mobtotem.MobTotemModel;
import com.wynntils.models.objectives.ObjectivesModel;
import com.wynntils.models.players.FriendsModel;
import com.wynntils.models.players.PartyModel;
import com.wynntils.models.players.PlayerModel;
import com.wynntils.models.players.hades.HadesModel;
import com.wynntils.models.profession.ProfessionModel;
import com.wynntils.models.quests.QuestModel;
import com.wynntils.models.rewards.RewardsModel;
import com.wynntils.models.spells.SpellModel;
import com.wynntils.models.stats.StatModel;
import com.wynntils.models.territories.GuildAttackTimerModel;
import com.wynntils.models.territories.TerritoryModel;
import com.wynntils.models.worlds.BombBellModel;
import com.wynntils.models.worlds.ServerListModel;
import com.wynntils.models.worlds.WorldStateModel;
import com.wynntils.models.wynnitem.WynnItemModel;

public final class Models {
    public static final ArrowShieldModel ArrowShield = new ArrowShieldModel();
    public static final BombBellModel BombBell = new BombBellModel();
    public static final BossBarModel BossBar = new BossBarModel();
    public static final CharacterSelectionModel CharacterSelection = new CharacterSelectionModel();
    public static final CompassModel Compass = new CompassModel();
    public static final ContainerModel Container = new ContainerModel();
    public static final DamageModel Damage = new DamageModel();
    public static final ElementModel Element = new ElementModel();
    public static final FavoritesModel Favorites = new FavoritesModel();
    public static final GuildAttackTimerModel GuildAttackTimer = new GuildAttackTimerModel();
    public static final ObjectivesModel Objectives = new ObjectivesModel();
    public static final PlayerInventoryModel PlayerInventory = new PlayerInventoryModel();
    public static final PlayerModel Player = new PlayerModel();
    public static final RewardsModel Rewards = new RewardsModel();
    public static final ServerListModel ServerList = new ServerListModel();
    public static final StatModel Stat = new StatModel();
    public static final TerritoryModel Territory = new TerritoryModel();
    public static final WorldStateModel WorldState = new WorldStateModel();
    public static final WynnItemModel WynnItem = new WynnItemModel();

    // Models with dependencies, ordered alphabetically as far as possible
    public static final CombatXpModel CombatXp = new CombatXpModel(WorldState);
    public static final CharacterModel Character = new CharacterModel(CombatXp);
    public static final FriendsModel Friends = new FriendsModel(WorldState);
    public static final GearModel Gear = new GearModel(Element, Stat, WynnItem);
    public static final HadesModel Hades = new HadesModel(Character, WorldState);
    public static final IngredientModel Ingredient = new IngredientModel(Stat);
    public static final ItemModel Item = new ItemModel(Element, Gear, Rewards, Ingredient);
    public static final LootChestModel LootChest = new LootChestModel(Container);
    public static final LootrunModel Lootrun = new LootrunModel(Container);
    public static final MapModel Map = new MapModel(GuildAttackTimer);
    public static final MobTotemModel MobTotem = new MobTotemModel(WorldState);
    public static final PartyModel Party = new PartyModel(WorldState);
    public static final ProfessionModel Profession = new ProfessionModel(Character);
    public static final QuestModel Quest = new QuestModel(CombatXp);
    public static final ShamanMaskModel ShamanMask = new ShamanMaskModel(WorldState);
    public static final ShamanTotemModel ShamanTotem = new ShamanTotemModel(WorldState);
    public static final SpellModel Spell = new SpellModel(Character);
    public static final DiscoveryModel Discovery = new DiscoveryModel(CombatXp, Compass, Quest, Territory);
    public static final EmeraldModel Emerald = new EmeraldModel(Item);
    public static final GearTooltipModel GearTooltip = new GearTooltipModel(Character, Quest);
    public static final HorseModel Horse = new HorseModel(Item);
}
