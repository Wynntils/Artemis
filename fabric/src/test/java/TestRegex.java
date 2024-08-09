/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
import com.wynntils.features.chat.GuildRankReplacementFeature;
import com.wynntils.features.chat.MessageFilterFeature;
import com.wynntils.features.chat.RevealNicknamesFeature;
import com.wynntils.features.inventory.PersonalStorageUtilitiesFeature;
import com.wynntils.features.redirects.ChatRedirectFeature;
import com.wynntils.features.trademarket.TradeMarketAutoOpenChatFeature;
import com.wynntils.features.trademarket.TradeMarketPriceMatchFeature;
import com.wynntils.features.ui.BulkBuyFeature;
import com.wynntils.handlers.chat.ChatHandler;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.models.abilities.ShamanTotemModel;
import com.wynntils.models.abilities.bossbars.OphanimBar;
import com.wynntils.models.character.CharacterModel;
import com.wynntils.models.character.CharacterSelectionModel;
import com.wynntils.models.containers.ContainerModel;
import com.wynntils.models.damage.DamageModel;
import com.wynntils.models.damage.label.DamageLabelParser;
import com.wynntils.models.items.annotators.game.GearAnnotator;
import com.wynntils.models.items.annotators.game.IngredientAnnotator;
import com.wynntils.models.items.annotators.game.RuneAnnotator;
import com.wynntils.models.items.annotators.gui.AbilityTreeAnnotator;
import com.wynntils.models.items.annotators.gui.ArchetypeAbilitiesAnnotator;
import com.wynntils.models.items.annotators.gui.CharacterAnnotator;
import com.wynntils.models.items.annotators.gui.SkillPointAnnotator;
import com.wynntils.models.items.annotators.gui.TerritoryUpgradeAnnotator;
import com.wynntils.models.npc.label.NpcLabelParser;
import com.wynntils.models.players.FriendsModel;
import com.wynntils.models.players.GuildModel;
import com.wynntils.models.players.label.GuildSeasonLeaderboardLabelParser;
import com.wynntils.models.statuseffects.StatusEffectModel;
import com.wynntils.models.territories.GuildAttackTimerModel;
import com.wynntils.models.trademarket.TradeMarketModel;
import com.wynntils.models.war.bossbar.WarTowerBar;
import com.wynntils.models.worlds.BombModel;
import com.wynntils.models.worlds.bossbars.InfoBar;
import com.wynntils.models.wynnitem.parsing.WynnItemParser;
import java.lang.reflect.Field;
import java.util.regex.Pattern;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestRegex {
    @BeforeAll
    public static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    public static final class PatternTester {
        private final String regexName;
        private final Pattern pattern;

        private PatternTester(String regexName, Class<?> clazz, Object obj, String fieldName) {
            this.regexName = regexName;
            Pattern pattern = null;

            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                pattern = (Pattern) field.get(obj);
            } catch (NoSuchFieldException e) {
                Assertions.fail("Pattern field " + regexName + " does not exist");
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            this.pattern = pattern;
        }

        public PatternTester(Class<?> clazz, String fieldName) {
            this(clazz.getSimpleName() + "." + fieldName, clazz, null, fieldName);
        }

        public PatternTester(Enum<?> enumType, String fieldName) {
            this(
                    enumType.getDeclaringClass().getSimpleName() + "." + enumType.name() + "." + fieldName,
                    enumType.getDeclaringClass(),
                    enumType,
                    fieldName);
        }

        public void shouldMatch(String s) {
            Assertions.assertTrue(
                    pattern.matcher(s).matches(),
                    "Regex failure: " + regexName + " should have matched " + s + ", but it did not.");
        }

        public void shouldNotMatch(String s) {
            Assertions.assertFalse(
                    pattern.matcher(s).matches(),
                    "Regex failure: " + regexName + " should NOT have matched " + s + ", but it did.");
        }
    }

    @Test
    public void AbilityTreeAnnotator_TREE_ABILITY_POINTS_PATTERN() {
        PatternTester p = new PatternTester(AbilityTreeAnnotator.class, "TREE_ABILITY_POINTS_PATTERN");
        p.shouldMatch("§b✦ Available Points: §f0§7/45");
        p.shouldMatch("§b✦ Available Points: §f15§7/45");
    }

    @Test
    public void ArchetypeAbilitiesAnnotator_ARCHETYPE_NAME() {
        PatternTester p = new PatternTester(ArchetypeAbilitiesAnnotator.class, "ARCHETYPE_NAME");
        p.shouldMatch("§e§lBoltslinger Archetype");
        p.shouldMatch("§d§lSharpshooter Archetype");
        p.shouldMatch("§2§lTrapper Archetype");
        p.shouldMatch("§d§lLight Bender Archetype");
    }

    @Test
    public void ArchetypeAbilitiesAnnotator_ARCHETYPE_PATTERN() {
        PatternTester p = new PatternTester(ArchetypeAbilitiesAnnotator.class, "ARCHETYPE_PATTERN");
        p.shouldMatch("§a✔ §7Unlocked Abilities: §f14§7/15");
        p.shouldMatch("§a✔ §7Unlocked Abilities: §f2§7/16");
        p.shouldMatch("§a✔ §7Unlocked Abilities: §f2§7/16");
        p.shouldMatch("§a✔ §7Unlocked Abilities: §f5§7/16");
        p.shouldMatch("§a✔ §7Unlocked Abilities: §f14§7/15");
        p.shouldMatch("§a✔ §7Unlocked Abilities: §f0§7/15");
    }

    @Test
    public void BombModel_BOMB_THROWN_PATTERN() {
        PatternTester p = new PatternTester(BombModel.class, "BOMB_THROWN_PATTERN");
        p.shouldMatch(
                "§bExampleUser1§3 has thrown a §bProfession XP Bomb§3! The entire server gets §bdouble profession xp §3for §b20 minutes§3!");
        p.shouldMatch(
                "§bExampleUser1§3 has thrown a §bProfession Speed Bomb§3! The entire server gets §bdouble Crafting/Gathering Speed, and Resource respawn time/Crafting Resource requirements are halved §3for §b10 minutes§3!");
    }

    @Test
    public void BulkBuyFeature_PRICE_PATTERN() {
        PatternTester p = new PatternTester(BulkBuyFeature.class, "PRICE_PATTERN");
        p.shouldMatch("§6 - §a✔ §f24§7²");
        p.shouldMatch("§6 - §a✔ §f648§7²");
        p.shouldMatch("§6 - §c✖ §f24§7²");
    }

    @Test
    public void CharacterModel_SILVERBULL_PATTERN() {
        PatternTester p = new PatternTester(CharacterModel.class, "SILVERBULL_PATTERN");
        p.shouldMatch("§7Subscription: §c✖ Inactive");
        p.shouldMatch("§7Subscription: §a✔ Active");
    }

    @Test
    public void CharacterModel_SILVERBULL_DURATION_PATTERN() {
        PatternTester p = new PatternTester(CharacterModel.class, "SILVERBULL_DURATION_PATTERN");
        p.shouldMatch("§7Expiration: §f1 week 5 days");
        p.shouldMatch("§7Expiration: §f5 days");
        p.shouldMatch("§7Expiration: §f1 week");
        p.shouldMatch("§7Expiration: §f2 days 12 hours");
    }

    @Test
    public void CharacterModel_VETERAN_PATTERN() {
        PatternTester p = new PatternTester(CharacterModel.class, "VETERAN_PATTERN");
        // Champion
        p.shouldMatch("§7Rank: §6Vet");
        // Hero
        p.shouldMatch("§7Rank: §dVet");
        // VIP+
        p.shouldMatch("§7Rank: §bVet");
        // VIP
        p.shouldMatch("§7Rank: §aVet");
    }

    @Test
    public void CharacterSelectionModel_CLASS_ITEM_CLASS_PATTERN() {
        PatternTester p = new PatternTester(CharacterSelectionModel.class, "CLASS_ITEM_CLASS_PATTERN");
        // Hunter
        p.shouldMatch("§e- §7Class: §fHunter");
        // Mage
        p.shouldMatch("§e- §7Class: §fMage");
        // Craftsman Dark Wizard
        p.shouldMatch("§e- §7Class: §3\uE026§r §fDark Wizard");
        // Hardcore Assassin
        p.shouldMatch("§e- §7Class: §c\uE027§r §fAssassin");
        // Hunted Ninja
        p.shouldMatch("§e- §7Class: §5\uE028§r §fNinja");
        // Ultimate Ironman Shaman
        p.shouldMatch("§e- §7Class: §b\uE083§r §fShaman");
        // Ultimate HIC Warrior
        p.shouldMatch("§e- §7Class: §c\uE027§b\uE083§3\uE026§5\uE028§r §fWarrior");
        // HIC Skyseer
        p.shouldMatch("§e- §7Class: §c\uE027§6\uE029§3\uE026§5\uE028§r §fSkyseer");
        // Ironman Archer
        p.shouldMatch("§e- §7Class: §6\uE029§r §fArcher");
    }

    @Test
    public void ChatHandler_NPC_CONFIRM_PATTERN() {
        PatternTester p = new PatternTester(ChatHandler.class, "NPC_CONFIRM_PATTERN");
        p.shouldMatch("§7Press §fSHIFT §7to continue");
        p.shouldMatch("§4Press §cSNEAK §4to continue");
    }

    @Test
    public void ChatHandler_NPC_SELECT_PATTERN() {
        PatternTester p = new PatternTester(ChatHandler.class, "NPC_SELECT_PATTERN");
        p.shouldMatch("§7Select §fan option §7to continue");
        p.shouldMatch("§cCLICK §4an option to continue");
    }

    @Test
    public void ChatRedirectFeature_LoginRedirector_FOREGROUND_PATTERN() {
        PatternTester p = new PatternTester(ChatRedirectFeature.LoginRedirector.class, "FOREGROUND_PATTERN");
        // champion
        p.shouldMatch("\uE017 §#ffe60000v8j§6 has just logged in!");
        // hero
        p.shouldMatch("\uE01B §#a344aa00v8j§d has just logged in!");
        // vip+
        p.shouldMatch("\uE024 §#8a99ee00v8j§3 has just logged in!");
        // vip
        p.shouldMatch("\uE023 §#44aa3300v8j§a has just logged in!");
        // champion nickname
        p.shouldMatch("\uE017 §#ffe60000§ocharlie268IsAWizard§6 has just logged in!");
        // moderator nickname, new style
        p.shouldMatch("\uE01F §6§o§<1>Navi§e has just logged in!");
        // music, new style
        p.shouldMatch("\uE020 §3Texilated§b has just logged in!");
    }

    @Test
    public void ContainerModel_ABILITY_TREE_PATTERN() {
        PatternTester p = new PatternTester(ContainerModel.class, "ABILITY_TREE_PATTERN");
        p.shouldMatch("\uDAFF\uDFEA\uE000");
    }

    @Test
    public void DamageLabelParser_DAMAGE_LABEL_PATTERN() {
        PatternTester p = new PatternTester(DamageLabelParser.class, "DAMAGE_LABEL_PATTERN");
        p.shouldMatch("§4-13 ❤ ");
        p.shouldMatch("§4-10 ❤ ");
        p.shouldMatch("§c-8 ✹ ");
        p.shouldMatch("§e-30 ✦ ");
        p.shouldMatch("§2-41 ✤ ");
        p.shouldMatch("§b-21 ❉ ");
        p.shouldMatch("§f-32 ❋ ");
        p.shouldMatch("§c-28 ✹ ");
        p.shouldMatch("§c-116 ✹ §2-17 ✤ ");
    }

    @Test
    public void DamageModel_DAMAGE_BAR_PATTERN() {
        PatternTester p = new PatternTester(DamageModel.class, "DAMAGE_BAR_PATTERN");
        p.shouldMatch("§aTravelling Merchant§r - §c5985§4❤");
        p.shouldMatch("§aGrook§r - §c23§4❤");
        p.shouldMatch("§cZombie§r - §c43§4❤");
        p.shouldMatch("§cFeligember Frog§r - §c1553§4❤§r - §7§e✦Weak §c✹Dam §c✹Def");
        p.shouldMatch("§cLongleg Gripper§r - §c40500§4❤§r - §2✤Dam §e✦§c✹Def");
        p.shouldMatch("§cBlinder§r - §c6566§4❤");
    }

    @Test
    public void FriendsModel_ONLINE_FRIENDS_HEADER() {
        PatternTester p = new PatternTester(FriendsModel.class, "ONLINE_FRIENDS_HEADER");
        p.shouldMatch("§a\uDAFF\uDFFC\uE001\uDB00\uDC06 Online Friends:");
        p.shouldMatch("§a\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE Online Friends:");
    }

    @Test
    public void FriendsModel_ONLINE_FRIEND() {
        PatternTester p = new PatternTester(FriendsModel.class, "ONLINE_FRIEND");
        p.shouldMatch("§2 - §auserName914__§2 [Server: §aWC3§2]");
        p.shouldMatch("§2 - §av8j§2 [Server: §aWC103§2]");
        p.shouldMatch("§2 - §a__asdf__§2 [Server: §aWC91§2]");
    }

    @Test
    public void FriendsModel_JOIN_PATTERN() {
        PatternTester p = new PatternTester(FriendsModel.class, "JOIN_PATTERN");
        p.shouldMatch("§a\uDAFF\uDFFC\uE001\uDB00\uDC06 Mirvun§2 has logged into server §aWC1§2 as §aan Archer");
        p.shouldMatch(
                "§a\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE Mirvun§2 has logged into server §aWC27§2 as §aa Mage");
    }

    @Test
    public void FriendsModel_LEAVE_PATTERN() {
        PatternTester p = new PatternTester(FriendsModel.class, "LEAVE_PATTERN");
        p.shouldMatch("§a\uDAFF\uDFFC\uE001\uDB00\uDC06 Mirvun left the game.");
        p.shouldMatch("§a\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE Mirvun left the game.");
    }

    @Test
    public void GuildAttackTimerModel_WAR_MESSAGE_PATTERN() {
        PatternTester p = new PatternTester(GuildAttackTimerModel.class, "WAR_MESSAGE_PATTERN");
        p.shouldMatch("§c\uE006\uE002 The war for Detlas will start in 1 minute.");
        p.shouldMatch("§c\uE006\uE002 The war for Detlas will start in 2 minutes.");
        p.shouldMatch("§c\uE006\uE002 The war for Detlas will start in 1 minute and 30 seconds.");
        p.shouldMatch("§c\uE006\uE002 The war for Detlas Close Suburbs will start in 30 seconds.");
    }

    @Test
    public void GuildModel_GUILD_NAME_MATCHER() {
        PatternTester p = new PatternTester(GuildModel.class, "GUILD_NAME_MATCHER");
        // Guild menu item
        p.shouldMatch("§3guildName§b [aAaA]");
        p.shouldMatch("§3guild Name§b [aaaa]");
        p.shouldMatch("§3GUILD NAME§b [wynn]");
        // Allied guild item
        p.shouldMatch("§a§lGUILD NAME [wynn]");
    }

    @Test
    public void GuildModel_GUILD_RANK_MATCHER() {
        PatternTester p = new PatternTester(GuildModel.class, "GUILD_RANK_MATCHER");
        p.shouldMatch("§7Rank: §fRecruit");
        p.shouldMatch("§7Rank: §fRecruiter");
        p.shouldMatch("§7Rank: §fCaptain");
        p.shouldMatch("§7Rank: §fStrategist");
        p.shouldMatch("§7Rank: §fChief");
        p.shouldMatch("§7Rank: §fOwner");
    }

    @Test
    public void GuildModel_LEVEL_MATCHER() {
        PatternTester p = new PatternTester(GuildModel.class, "LEVEL_MATCHER");
        p.shouldMatch("§b§lChiefs Of Corkus§3§l [Lv. 87]");
    }

    @Test
    public void GuildModel_LEVEL_PROGRESS_MATCHER() {
        PatternTester p = new PatternTester(GuildModel.class, "LEVEL_PROGRESS_MATCHER");
        p.shouldMatch("§f20,588,573,849§7/25,447,702,087 XP");
    }

    @Test
    public void GuildModel_MSG_LEFT_GUILD() {
        PatternTester p = new PatternTester(GuildModel.class, "MSG_LEFT_GUILD");
        p.shouldMatch("§3You have left §bExample Guild§3!");
    }

    @Test
    public void GuildModel_MSG_JOINED_GUILD() {
        PatternTester p = new PatternTester(GuildModel.class, "MSG_JOINED_GUILD");
        p.shouldMatch("§3You have joined §bExample Guild§3!");
    }

    @Test
    public void GuildModel_MSG_RANK_CHANGED() {
        PatternTester p = new PatternTester(GuildModel.class, "MSG_RANK_CHANGED");
        p.shouldMatch("§3[INFO]§b v8j has set USERNAME's guild rank from Recruit to Chief");
        p.shouldMatch("§3[INFO]§b v8j has set USERNAMES' guild rank from Recruiter to Chief");
    }

    @Test
    public void GuildModel_MSG_OBJECTIVE_COMPLETED() {
        PatternTester p = new PatternTester(GuildModel.class, "MSG_OBJECTIVE_COMPLETED");
        p.shouldMatch("§3[INFO]§b Flyxdre has finished their weekly objective.");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 has finished their weekly objective.");
    }

    @Test
    public void GuildModel_MSG_NEW_OBJECTIVES() {
        PatternTester p = new PatternTester(GuildModel.class, "MSG_NEW_OBJECTIVES");
        p.shouldMatch("§3[INFO]§b New Weekly Guild Objectives are being assigned.");
    }

    @Test
    public void GuildModel_MSG_TRIBUTE_SCEDULED() {
        PatternTester p = new PatternTester(GuildModel.class, "MSG_TRIBUTE_SCEDULED");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled 1 Emerald per hour to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled Ⓙ 1 Crop per hour to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled Ⓚ 1 Fish per hour to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled Ⓒ 1 Wood per hour to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled Ⓑ 1 Ore per hour to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled 2 Emeralds per hour to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled Ⓙ 2 Crops per hour to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled Ⓚ 2 Fish per hour to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled Ⓒ 2 Wood per hour to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 scheduled Ⓑ 2 Ore per hour to Example Guild");
    }

    @Test
    public void GuildModel_MSG_TRIBUTE_STOPPED() {
        PatternTester p = new PatternTester(GuildModel.class, "MSG_TRIBUTE_STOPPED");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 stopped scheduling Emeralds to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 stopped scheduling Fish to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 stopped scheduling Ore to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 stopped scheduling Wood to Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 stopped scheduling Crops to Example Guild");
        p.shouldMatch("§3[INFO]§b Example Guild stopped scheduling Emeralds to Example Guild");
    }

    @Test
    public void GuildModel_MSG_ALLIANCE_FORMED() {
        PatternTester p = new PatternTester(GuildModel.class, "MSG_ALLIANCE_FORMED");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 formed an alliance with Example Guild");
        p.shouldMatch("§3[INFO]§b Example Guild formed an alliance with Example Guild");
    }

    @Test
    public void GuildModel_MSG_ALLIANCE_REVOKED() {
        PatternTester p = new PatternTester(GuildModel.class, "MSG_ALLIANCE_REVOKED");
        p.shouldMatch("§3[INFO]§b Example Guild revoked the alliance with Example Guild");
        p.shouldMatch("§3[INFO]§b ExamplePlayer1 revoked the alliance with Example Guild");
    }

    @Test
    public void GuildModel_OBJECTIVES_COMPLETED_PATTERN() {
        PatternTester p = new PatternTester(GuildModel.class, "OBJECTIVES_COMPLETED_PATTERN");
        p.shouldMatch("§6Current Guild Goal: §f23§7/30");
    }

    @Test
    public void GuildModel_OBJECTIVE_STREAK_PATTERN() {
        PatternTester p = new PatternTester(GuildModel.class, "OBJECTIVE_STREAK_PATTERN");
        p.shouldMatch("§a- §7Streak: §f14");
    }

    @Test
    public void GuildModel_TRIBUTE_PATTERN() {
        PatternTester p = new PatternTester(GuildModel.class, "TRIBUTE_PATTERN");
        p.shouldMatch("§fⒷ -1000 Ore per Hour");
        p.shouldMatch("§6Ⓒ -1000 Wood per Hour");
        p.shouldMatch("§bⓀ -11000 Fish per Hour");
        p.shouldMatch("§eⒿ -1000 Crops per Hour");
        p.shouldMatch("§fⒷ -1 Ore per Hour");
        p.shouldMatch("§6Ⓒ -1 Wood per Hour");
        p.shouldMatch("§bⓀ -1 Fish per Hour");
        p.shouldMatch("§eⒿ -1 Crop per Hour");
        p.shouldMatch("§a-1 Emerald per Hour");
        p.shouldMatch("§a-2 Emeralds per Hour");
        p.shouldMatch("§a+9000 Emeralds per Hour");
    }

    @Test
    public void GuildModel_ALLIED_GUILD_PATTERN() {
        PatternTester p = new PatternTester(GuildModel.class, "ALLIED_GUILD_PATTERN");
        p.shouldMatch("§a- §7GUILD NAME [wynn]");
    }

    @Test
    public void GuildRankReplacementFeature_GUILD_MESSAGE_PATTERN() {
        PatternTester p = new PatternTester(GuildRankReplacementFeature.class, "GUILD_MESSAGE_PATTERN");
        p.shouldMatch("§3[§b★★★★★§3§oDisco reroller§3]§b");
        p.shouldMatch("§3[§b★★★★★§3§oafKing§3]§b");
        p.shouldMatch("§3[§b★★★★§3§obol§3]§b");
    }

    @Test
    public void GuildRankReplacementFeature_RECRUIT_USERNAME_PATTERN() {
        PatternTester p = new PatternTester(GuildRankReplacementFeature.class, "RECRUIT_USERNAME_PATTERN");
        p.shouldMatch("§3[_user0name_");
    }

    @Test
    public void InfoBar_BOMB_INFO_PATTERN() {
        PatternTester p = new PatternTester(InfoBar.class, "BOMB_INFO_PATTERN");
        p.shouldMatch("§3Double Profession Speed from §bCorkian§7 [§f2§7 min]");
    }

    @Test
    public void InfoBar_GUILD_INFO_PATTERN() {
        PatternTester p = new PatternTester(InfoBar.class, "GUILD_INFO_PATTERN");
        p.shouldMatch("§7Lv. 92§f - §bKingdom Foxes§f - §762% XP");
    }

    @Test
    public void InfoBar_TERRITORY_INFO_PATTERN() {
        PatternTester p = new PatternTester(InfoBar.class, "TERRITORY_INFO_PATTERN");
        p.shouldMatch("§aLutho§2 [PROF]");
        p.shouldMatch("§bCorkus City§3 [HOC]");
        p.shouldMatch("§cDetlas§4 [AVO]");
    }

    @Test
    public void IngredientAnnotator_INGREDIENT_PATTERN() {
        PatternTester p = new PatternTester(IngredientAnnotator.class, "INGREDIENT_PATTERN");
        p.shouldMatch("§7Perkish Potato [§8✫✫✫§7]");
        p.shouldMatch("§7Sylphid Tears§6 [§e✫§8✫✫§6]");
        p.shouldMatch("§7Bob's Tear§5 [§d✫✫§8✫§5]");
        p.shouldMatch("§7Contorted Stone§3 [§b✫✫✫§3]");
    }

    @Test
    public void OphanimBar_OPHANIM_PATTERN() {
        PatternTester p = new PatternTester(OphanimBar.class, "OPHANIM_PATTERN");
        p.shouldMatch("§710s Healed: §f66% §3[§b⏺⏺⏺⏺⏺⏺§3]");
        p.shouldMatch("§710s Healed: §f0% §3[§b⏺§b⏺§b⏺§b⏺§b⏺§b⏺§3]");
        p.shouldMatch("§710s Healed: §f0% §4[§c⏺§c⏺§c⏺§c⏺§c⏺§7⏺§4]");
        p.shouldMatch("§710s Healed: §f0% §4[§e⏺§e⏺§e⏺§c⏺§c⏺§e⏺§4]");
        p.shouldMatch("§710s Healed: §f0% §6[§e⏺§e⏺§e⏺§e⏺§e⏺§e⏺§6]");
        p.shouldMatch("§710s Healed: §f22% §3[§b⏺§b⏺§b⏺§b⏺§b⏺§b⏺§3]");
        p.shouldMatch("§710s Healed: §f12% §3[§e⏺§b⏺§e⏺§b⏺⏺⏺§3]");
        p.shouldMatch("§710s Healed: §f0% §4[§c⏺⏺⏺§e⏺⏺⏺§4]");
        p.shouldMatch("§710s Healed: §f0% §8[]");
    }

    @Test
    public void MessageFilterFeature_PARTY_FINDER_FG() {
        PatternTester p = new PatternTester(MessageFilterFeature.class, "PARTY_FINDER_FG");
        p.shouldMatch(
                "§5Party Finder:§d Hey Rafii2198, over here! Join the §bThe Canyon Colossus§d queue and match up with §e2 other players§d!"); // Name 2 players
        p.shouldMatch(
                "§5Party Finder:§d Hey Rafii2198, over here! Join the §bThe Canyon Colossus§d queue and match up with §e1 other player§d!"); // Name 1 player
        p.shouldMatch(
                "§5Party Finder:§d Hey nickname spaces, over here! Join the §bThe Canyon Colossus§d queue and match up with §e1 other player§d!"); // Nickname 1 player
        p.shouldMatch(
                "§5Party Finder:§d Hey nickname spaces 20cr, over here! Join the §bThe Canyon Colossus§d queue and match up with §e11 other players§d!"); // Nickname 11 players
    }

    @Test
    public void PersonalStorageUtilitiesFeature_PAGE_PATTERN() {
        PatternTester p = new PatternTester(PersonalStorageUtilitiesFeature.class, "PAGE_PATTERN");
        p.shouldMatch("§7- §f\uE006§8 Page 1");
        p.shouldMatch("§7- §f\uE007§8 Page 3");
    }

    @Test
    public void RecipientType_NPC_foregroundPattern() {
        PatternTester p = new PatternTester(RecipientType.NPC, "foregroundPattern");
        p.shouldMatch("§7[3/5]§2 Jesp:§a Keep fighting! We're almost halfway to the other side!");
        p.shouldMatch("§7[1/11]§0 §2Scientist Ynnos:§a *Ahem* Welcome, everyone.");
        p.shouldMatch(
                "§7[3/11]§0 §2Scientist Ynnos:§0 §r§aAllow me to explain the situation we’re in. It’s a series of unfortunate events. I study the amazing properties of crystals and other geodes.");
        p.shouldMatch(
                "§7[6/6] §5Aster: §dSo remember, find a balance in elements! Whether you invest in one, or select many to excell in, I trust you may succeed. Thank you for you time.");
    }

    @Test
    public void RecipientType_GLOBAL_foregroundPattern() {
        PatternTester p = new PatternTester(RecipientType.GLOBAL, "foregroundPattern");
        // wc5 106(archer)VAI CHAMPION v8j: test
        p.shouldNotMatch(
                "§f\uE056\uE042\uE065§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE066§8\uE00E§f\uE012\uE012\uE013\uE02C§8\uE00F§f\uE012\uE055§8\uE00F§f\uE012\uE040§8\uE00F§f\uE012\uE048§8\uE011\u2064§r\uE013\uE017\uE013\u2064\u2064§#ffe60000v8j: §ftest");
        // wc5 106(archer)VAI VIP v8j: hello 2
        p.shouldNotMatch(
                "§f\uE056\uE042\uE065§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE066§8\uE00E§f\uE012\uE012\uE013\uE02C§8\uE00F§f\uE012\uE055§8\uE00F§f\uE012\uE040§8\uE00F§f\uE012\uE048§8\uE011\u2064§r\uE013\uE023\uE013\u2064\u2064§#44aa3300v8j: §fhello 2");
        // REMOTE wc6 105(mage) UNVERIFIED Sebastiankungen: 4
        p.shouldMatch(
                "§7\uE056\uE042\uE066§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE065§8\uE00F§f\uE012\uE013\uE013\u2064\u2064\u2064\u2064§8\uE011§f\uE012\u2064\uE030\u2064\u2064\u2064§r\uE013\uE013\u2064\u2064\u2064§7Sebastiankungen: §f4");
        // wc37 104(skyseer)VAI silverbullVIP+ v8j: test
        p.shouldNotMatch(
                "§f\uE056\uE042\uE063\uE067§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE064§8\uE00E§f\uE012\uE012\uE013\uE02F§8\uE00F§f\uE012\uE055§8\uE00F§f\uE012\uE040§8\uE00F§f\uE012\uE048§8\uE011\u2064§r  \u2064\u2064\u2064§f\uE02B\u2064\u2064§r\uE024\uE013\u2064\u2064§#8a99ee00v8j: §ftest");
        // REMOTE wc3 105(archer)BXP VERIFIED moumbear: oblivion
        p.shouldMatch(
                "§7\uE056\uE042\uE063§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE065§8\uE00E§f\uE012\uE012\uE013\uE02C§8\uE00F§f\uE012\uE041§8\uE00F§f\uE012\uE057§8\uE00F§f\uE012\uE04F§8\uE011\u2064§r\uE013\uE013\u2064\u2064\u2064§fmoumbear: oblivion");
        // REMOTE wc1 105(archer)SEQ GuildMilestone2 VERIFIED warpo: i love bolt
        p.shouldMatch(
                "§7\uE056\uE042\uE061§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE065§8\uE00E§f\uE012\uE012\uE013\uE02C§8\uE00F§f\uE012\uE052§8\uE00F§f\uE012\uE044§8\uE00F§f\uE012\uE050§8\uE011\u2064§r §#54fcfc00\uE07C \u2064§r\uE013\u2064\u2064\u2064§fwarpo: i love bolt");
        // REMOTE wc25 106(warrior)FURS GuildMilestone1 silverbullCHAMPION angycathy: 6500
        p.shouldMatch(
                "§7\uE056\uE042\uE062\uE065§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE066§8\uE00E§f\uE012\uE012\uE013\uE030§8\uE00F§f\uE012\uE045§8\uE00F§f\uE012\uE054§8\uE00F§f\uE012\uE051§8\uE00F§f\uE012\uE052§8\uE011\u2064§r §#54fcfc00\uE07B \u2064\u2064\u2064§f\uE02B\u2064\u2064§r\uE017\uE013\u2064\u2064§#ffe60000angycathy: §f6500");
        // REMOTE wc1 105(warrior)MAG GuildMilestone2 HERO Mythicized: gaming
        p.shouldMatch(
                "§7\uE056\uE042\uE061§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE065§8\uE00E§f\uE012\uE012\uE013\uE030§8\uE00F§f\uE012\uE04C§8\uE00F§f\uE012\uE040§8\uE00F§f\uE012\uE046§8\uE011\u2064§r §#54fcfc00\uE07C \u2064§r\uE01B\uE013\u2064\u2064§#a344aa00Mythicized: §fgaming");
    }

    @Test
    public void RecipientType_LOCAL_foregroundPattern() {
        PatternTester p = new PatternTester(RecipientType.LOCAL, "foregroundPattern");
        // wc5 106(archer)VAI CHAMPION v8j: test
        p.shouldMatch(
                "§f\uE056\uE042\uE065§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE066§8\uE00E§f\uE012\uE012\uE013\uE02C§8\uE00F§f\uE012\uE055§8\uE00F§f\uE012\uE040§8\uE00F§f\uE012\uE048§8\uE011\u2064§r\uE013\uE017\uE013\u2064\u2064§#ffe60000v8j: §ftest");
        // wc5 (friend)58(mage)VAI UNVERIFIED Mirvun: test
        p.shouldMatch(
                "§f\uE056\uE042\uE065§r §#00a80000\uE010\u2064\uE00F§f\uE012\uE065§#00a80000\uE00F§f\uE012\uE068§#00a80000\uE00E§f\uE012\uE012\uE013\uE02E§#00a80000\uE00F§f\uE012\uE055§#00a80000\uE00F§f\uE012\uE040§#00a80000\uE00F§f\uE012\uE048§#00a80000\uE011\u2064§r\uE013\uE013\u2064\u2064\u2064§7Mirvun: §f.");
        // wc5 (guild)58(mage)VAI UNVERIFIED Mirvun: bingbing
        p.shouldMatch(
                "§f\uE056\uE042\uE065§r §#4ec7c700\uE010\u2064\uE00F§f\uE012\uE065§#4ec7c700\uE00F§f\uE012\uE068§#4ec7c700\uE00E§f\uE012\uE012\uE013\uE02E§#4ec7c700\uE00F§f\uE012\uE055§#4ec7c700\uE00F§f\uE012\uE040§#4ec7c700\uE00F§f\uE012\uE048§#4ec7c700\uE011\u2064§r\uE013\uE013\u2064\u2064\u2064§7Mirvun: §fbingbing)");
        // wc5 106(archer)VAI VIP v8j: hello 2
        p.shouldMatch(
                "§f\uE056\uE042\uE065§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE066§8\uE00E§f\uE012\uE012\uE013\uE02C§8\uE00F§f\uE012\uE055§8\uE00F§f\uE012\uE040§8\uE00F§f\uE012\uE048§8\uE011\u2064§r\uE013\uE023\uE013\u2064\u2064§#44aa3300v8j: §fhello 2");
        // wc5 105(warrior)VAI HERO v8j: 4
        p.shouldMatch(
                "§f\uE056\uE042\uE065§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE065§8\uE00E§f\uE012\uE012\uE013\uE030§8\uE00F§f\uE012\uE055§8\uE00F§f\uE012\uE040§8\uE00F§f\uE012\uE048§8\uE011\u2064§r\uE013\uE01B\uE013\u2064\u2064§#a344aa00v8j: §f4");
        // wc37 105(assassin)VAI VIP+ v8j: 5test4
        p.shouldMatch(
                "§f\uE056\uE042\uE063\uE067§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE065§8\uE00E§f\uE012\uE012\uE013\uE02D§8\uE00F§f\uE012\uE055§8\uE00F§f\uE012\uE040§8\uE00F§f\uE012\uE048§8\uE011\u2064§r\uE013\uE024\uE013\u2064\u2064§#8a99ee00v8j: §f5test4");
        // REMOTE wc6 105(mage) UNVERIFIED Sebastiankungen: 4
        p.shouldNotMatch(
                "§7\uE056\uE042\uE066§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE065§8\uE00F§f\uE012\uE013\uE013\u2064\u2064\u2064\u2064§8\uE011§f\uE012\u2064\uE030\u2064\u2064\u2064§r\uE013\uE013\u2064\u2064\u2064§7Sebastiankungen: §f4");
        // wc37 104(skyseer)VAI silverbullVIP+ v8j: test
        p.shouldMatch(
                "§f\uE056\uE042\uE063\uE067§r §8\uE010\u2064\uE070§f\uE071\uE061§8\uE00F§f\uE012\uE060§8\uE00F§f\uE012\uE064§8\uE00E§f\uE012\uE012\uE013\uE02F§8\uE00F§f\uE012\uE055§8\uE00F§f\uE012\uE040§8\uE00F§f\uE012\uE048§8\uE011\u2064§r  \u2064\u2064\u2064§f\uE02B\u2064\u2064§r\uE024\uE013\u2064\u2064§#8a99ee00v8j: §ftest");
    }

    @Test
    public void RecipientType_GUILD_foregroundPattern() {
        PatternTester p = new PatternTester(RecipientType.GUILD, "foregroundPattern");
        p.shouldMatch("§3[§b★★★★§3bolyai§3] test 3");
        p.shouldMatch("§3[§b★★★★§3§obol§r§3]§b test ");
        p.shouldMatch("§3[kristof345]§b test");
        p.shouldNotMatch("§3[INFO]§b bolyai has kicked kristof345 from the guild");
    }

    @Test
    public void RuneAnnotator_RUNE_PATTERN() {
        PatternTester p = new PatternTester(RuneAnnotator.class, "RUNE_PATTERN");
        p.shouldMatch("§bAz Rune");
        p.shouldMatch("§4Nii Rune");
        p.shouldMatch("§3Uth Rune");
        p.shouldMatch("§2Tol Rune");
    }

    @Test
    public void ShamanTotemModel_SHAMAN_TOTEM_TIMER_PATTERN() {
        PatternTester p = new PatternTester(ShamanTotemModel.class, "SHAMAN_TOTEM_TIMER");
        p.shouldMatch("§c21s\n+290❤§7/s");
        p.shouldMatch("§c1s\n+36❤§7/s");
        p.shouldMatch("§c35s");
    }

    @Test
    public void SkillPointAnnotator_SKILL_POINT_PATTERN() {
        PatternTester p = new PatternTester(SkillPointAnnotator.class, "SKILL_POINT_PATTERN");
        p.shouldMatch("§dUpgrade your §2✤ Strength§d skill");
        p.shouldMatch("§dUpgrade your §e✦ Dexterity§d skill");
        p.shouldMatch("§dUpgrade your §b❉ Intelligence§d skill");
        p.shouldMatch("§dUpgrade your §c✹ Defence§d skill");
        p.shouldMatch("§dUpgrade your §f❋ Agility§d skill");
    }

    @Test
    public void SkillPointAnnotator_LORE_PATTERN() {
        PatternTester p = new PatternTester(SkillPointAnnotator.class, "LORE_PATTERN");
        p.shouldMatch("\uDB00\uDC03§7116 points§r\uDB00\uDC10       \uDB00\uDC10§6117 points");
        p.shouldMatch("\uDB00\uDC03§7110 points§r\uDB00\uDC10       \uDB00\uDC10§6111 points");
        p.shouldMatch("\uDB00\uDC06§798 points§r\uDB00\uDC13       \uDB00\uDC13§699 points");
        p.shouldMatch("\uDB00\uDC03§7162 points§r\uDB00\uDC10       \uDB00\uDC10§6163 points");
        p.shouldMatch("\uDB00\uDC06§790 points§r\uDB00\uDC13       \uDB00\uDC13§691 points");
        p.shouldMatch("\uDB00\uDC03§7-29 points§r\uDB00\uDC10       \uDB00\uDC10§6-28 points");
    }

    @Test
    public void StatusEffectModel_STATUS_EFFECT_PATTERN() {
        PatternTester p = new PatternTester(StatusEffectModel.class, "STATUS_EFFECT_PATTERN");
        p.shouldMatch("§fⒺ§7 +198 Main Attack Damage §8(00:41)");
        p.shouldMatch("§f§b❤§7 Windy Feet §8(02:57)");
        p.shouldMatch("§a❉ §776.5% Concentration §4(00:13)");
        p.shouldMatch("§b➲ §718% Frenzy §8(**:**)");
        p.shouldMatch("§fⒺ§7 +54% Spell Damage §8(00:41)");
        p.shouldMatch("§8⬤ §7Vanish §a(00:04)");
        p.shouldMatch("§fⒺ§7 +250/3s Life Steal §8(00:41)");
        p.shouldMatch("§8⬤ §7Boiling Blood §a(00:02)");
    }

    @Test
    public void TerritoryUpgradeAnnotator_TERRITORY_UPGRADE_PATTERN() {
        PatternTester p = new PatternTester(TerritoryUpgradeAnnotator.class, "TERRITORY_UPGRADE_PATTERN");
        p.shouldMatch("§6§lDamage §7[Lv. 10]");
        p.shouldMatch("§d§lEmerald Rate §7[Lv. 2]");
        p.shouldMatch("§d§lTower Aura §7[Lv. 3]§8 (Max)");
        p.shouldMatch("§d§lEmerald Seeking §7[Lv. 5]§8 (Max)");
    }

    @Test
    public void TradeMarketModel_PRICE_PATTERN() {
        PatternTester p = new PatternTester(TradeMarketModel.class, "PRICE_PATTERN");
        p.shouldMatch("§7 - §f525§7² §8(8²½ 13²)");
        p.shouldMatch("§7 - §f21,111§7² §8(5¼² 9²½ 55²)");
        p.shouldMatch("§7 - §f8 §7x §f127§7² §8(1²½ 63²)");
        p.shouldMatch("§7 - §f308 §7x §f§m16§7§m²§b ✮ 15§3² §8(15²)");
        p.shouldMatch("§7 - §f308 §7x §f§m16§7§m²§b ✮ 15§3² §8(15²)");
    }

    @Test
    public void TradeMarketPriceMatchFeature_HIGHEST_BUY_PATTERN() {
        PatternTester p = new PatternTester(TradeMarketPriceMatchFeature.class, "HIGHEST_BUY_PATTERN");
        p.shouldMatch("§7Highest Buy Offer: §a100000²§8 (24¼² 26²½ 32²)");
    }

    @Test
    public void TradeMarketPriceMatchFeature_LOWEST_SELL_PATTERN() {
        PatternTester p = new PatternTester(TradeMarketPriceMatchFeature.class, "LOWEST_SELL_PATTERN");
        p.shouldMatch("§7Lowest Sell Offer: §a1050000²§8 (4stx 0.35¼²)");
    }

    @Test
    public void WynnItemParser_ITEM_ATTACK_SPEED_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "ITEM_ATTACK_SPEED_PATTERN");
        p.shouldMatch("§7Normal Attack Speed");
        p.shouldMatch("§7Super Fast Attack Speed");
    }

    @Test
    public void WynnItemParser_ITEM_DAMAGE_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "ITEM_DAMAGE_PATTERN");
        p.shouldMatch("§6✣ Neutral Damage: 55-68");
    }

    @Test
    public void WynnItemParser_ITEM_DEFENCE_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "ITEM_DEFENCE_PATTERN");
        p.shouldMatch("§e✦ Thunder§7 Defence: +56");
    }

    @Test
    public void WynnItemParser_IDENTIFICATION_STAT_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "IDENTIFICATION_STAT_PATTERN");
        p.shouldMatch("§a+10% §7Health Regen");
        p.shouldMatch("§a+5%§2* §7XP Bonus");
        p.shouldMatch("§a+5/5s §7Mana Regen");
        p.shouldMatch("§a+42 §7Water Spell Damage");
        p.shouldMatch("§a+4 §7Intelligence");
        p.shouldMatch("§a+1 tier§2* §7Attack Speed");
        p.shouldMatch("§a+16%§2*** §7XP Bonus");
        p.shouldMatch("§c-28000§4 to §c-52000%§7 Spell Damage");
        p.shouldMatch("§c-28000§4 to §c-52000§7 Spell Damage");
        p.shouldMatch("§a+12§2 to §a52%§7 Main Attack Damage");
        p.shouldMatch("§c-280§4 to §c-520§7 {sp1} Cost");
        p.shouldMatch("§a+291/3s§2** §7Life Steal");
        p.shouldMatch("§c-28% §7Soul Point Regen");
        // Crafted gear 18/18% Main Attack Damage
        p.shouldMatch("§a+18%§8/18% §7Main Attack Damage");
    }

    @Test
    public void WynnItemParser_TIER_AND_REROLL_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "TIER_AND_REROLL_PATTERN");
        p.shouldMatch("§eUnique Item");
        p.shouldMatch("§dRare Item");
        p.shouldMatch("§dRare Item [2]");
        p.shouldMatch("§bLegendary Item");
        p.shouldMatch("§cFabled Item");
        p.shouldMatch("§aSet Item");

        // Crafted gear
        p.shouldMatch("§3Crafted Wand§8 [134/137 Durability]");
        p.shouldMatch("§3Crafted by player_name§8 [177/177 Durability]");
        p.shouldMatch("§3Crafted by v8j§8 [177/177 Durability]");
    }

    @Test
    public void WynnItemParser_POWDER_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "POWDER_PATTERN");
        p.shouldMatch("§7[0/1] Powder Slots");
        p.shouldMatch("§7[1/1] Powder Slots [§r§c✹§r§7]");
        p.shouldMatch("§7[3/3] Powder Slots [§r§f❋ ❋ ❋§r§7]");
        p.shouldMatch("§7[2/2] Powder Slots [§r§e✦ ✦§r§7]");
        p.shouldMatch("§7[2/2] Powder Slots [§r§b❉ ❉§r§7]");
        p.shouldMatch("§7[3/3] Powder Slots [§r§2✤ §r§c✹ §r§b❉§r§7]");
    }

    @Test
    public void WynnItemParser_EFFECT_LINE_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "EFFECT_LINE_PATTERN");
        p.shouldMatch("§6- §7Effect: §f20% XP");
    }

    @Test
    public void WynnItemParser_MIN_LEVEL_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "MIN_LEVEL_PATTERN");
        p.shouldMatch("§a✔§7 Combat Lv. Min: 35");
        p.shouldMatch("§c✖§7 Combat Lv. Min: 65");
    }

    @Test
    public void WynnItemParser_CLASS_REQ_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "CLASS_REQ_PATTERN");
        p.shouldMatch("§c✖§7 Class Req: Shaman/Skyseer§r");
    }

    @Test
    public void WynnItemParser_SKILL_REQ_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "SKILL_REQ_PATTERN");
        p.shouldMatch("§a✔§7 Intelligence Min: 38");
    }

    @Test
    public void WynnItemParser_MISC_REQ_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "MISC_REQ_PATTERN");
        p.shouldMatch("§a✔§7 Quest Req: The Qira Hive");
        p.shouldMatch("§c✖§7 Quest Req: Realm of Light V - The Realm of Light");
    }

    @Test
    public void WynnItemParser_SHINY_STAT_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "SHINY_STAT_PATTERN");
        p.shouldMatch("§f⬡ §7Raids Won: §f0");
        p.shouldMatch("§f⬡ §7Raids Won: §f297");
        p.shouldMatch("§f⬡ §7Mobs Killed: §f0");
        p.shouldNotMatch("§c✖§7 Agility Min: 70");
        p.shouldNotMatch("§f⬡ §7: §f0");
        p.shouldNotMatch("§f⬡ §7Mobs Killed: §f");
        p.shouldMatch("§f⬡ §7Wars Won: §f164");
        p.shouldMatch("§f⬡ §7Raids Won: §f0");
    }

    @Test
    public void WynnItemParser_CRAFTED_ITEM_NAME_PATTERN() {
        PatternTester p = new PatternTester(WynnItemParser.class, "CRAFTED_ITEM_NAME_PATTERN");
        p.shouldMatch("§3§otest item§b§o [24%]À");
        p.shouldMatch("§3§oAbsorbant Skirt of the Skyraider§b§o [100%]À");
    }

    @Test
    public void WarTowerBar_TOWER_BAR_PATTERN() {
        PatternTester p = new PatternTester(WarTowerBar.class, "TOWER_BAR_PATTERN");
        p.shouldMatch("§3[SEPC] §bEfilim South Plains Tower§7 - §4❤ 390000§7 (§610.0%§7) - §c☠ 1300-1949§7 (§b0.5x§7)");
    }

    @Test
    public void GuildSeasonLeaderboardLabelParser_GUILD_SEASON_LEADERBOARD_PATTERN() {
        PatternTester p = new PatternTester(GuildSeasonLeaderboardLabelParser.class, "GUILD_SEASON_LEADERBOARD_LABEL");
        p.shouldMatch("§6§l1§7 - §bIdiot Co§d (11 396 656 SR)");
        p.shouldMatch("§62§7 - §bSequoia§d (11 057 047 SR)");
    }

    @Test
    public void GearAnnotator_GEAR_PATTERN() {
        PatternTester p = new PatternTester(GearAnnotator.class, "GEAR_PATTERN");

        // Unidentified
        p.shouldMatch("§5Unidentified §f⬡ §5Shiny Crusade Sabatons");
        p.shouldMatch("§5Unidentified §f⬡ §5Shiny Gaia");
        p.shouldMatch("§5Unidentified Idol");
        p.shouldMatch("§5Unidentified Nirvana");
        p.shouldMatch("§bUnidentified Follow the Wind");

        // Identified
        p.shouldMatch("§5Apocalypse");
        p.shouldMatch("§cRhythm of the Seasons");
        p.shouldMatch("§f⬡ §5Shiny Stratiformis");
        p.shouldMatch("§f⬡ §5Shiny Aftershock");
        p.shouldMatch("§f⬡ §5Shiny Crusade Sabatons");
    }

    @Test
    public void CharacterAnnotator_CLASS_MENU_CLASS_PATTERN() {
        PatternTester p = new PatternTester(CharacterAnnotator.class, "CLASS_MENU_CLASS_PATTERN");

        p.shouldMatch("§e- §7Class: §6\uE029§5\uE028§r §fAssassin");
        p.shouldMatch("§e- §7Class: §c\uE027§b\uE083§3\uE026§r §fKnight");
        p.shouldMatch("§e- §7Class: §fWarrior");
    }

    @Test
    public void NpcLabelParser_NPC_LABEL_PATTERN() {
        PatternTester p = new PatternTester(NpcLabelParser.class, "NPC_LABEL_PATTERN");

        p.shouldMatch("§dLootrun Master\n§7Start a Lootrun");
        p.shouldMatch("§f\uE003\n§dItem Identifier\n§7NPC");
    }

    @Test
    public void RevealNicknamesFeature_NICKNAME_PATTERN() {
        PatternTester p = new PatternTester(RevealNicknamesFeature.class, "NICKNAME_PATTERN");

        p.shouldMatch("§fbol§7's real username is §fbolyai");
        p.shouldMatch("§fbol's§7 real username is §fbolyai");
    }

    @Test
    public void TradeMarketAutoOpenChatFeature_TYPE_TO_CHAT_PATTERN() {
        PatternTester p = new PatternTester(TradeMarketAutoOpenChatFeature.class, "TYPE_TO_CHAT_PATTERN");

        p.shouldMatch("§5\uE00A\uE002 \n\uE001 Type the item name or type 'cancel' to \n\uE001 cancel:\n\uE001 ");
    }
}
