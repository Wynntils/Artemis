/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.redirects;

import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.MessageType;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.models.players.type.PlayerRank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.REDIRECTS)
public class ChatRedirectFeature extends Feature {
    @Persisted
    public final Config<RedirectAction> craftedDurability = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> emptyManaBank = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> friendJoin = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> heal = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> horse = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> housingTeleport = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> ingredientPouch = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> loginAnnouncements = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> mageTeleport = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> notEnoughMana = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> potion = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> scrollTeleport = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> shaman = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> speed = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> toolDurability = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> unusedPoints = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> guildBank = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> guildRewards = new Config<>(RedirectAction.REDIRECT);

    @Persisted
    public final Config<RedirectAction> merchant = new Config<>(RedirectAction.REDIRECT);

    private final List<Redirector> redirectors = new ArrayList<>();

    public ChatRedirectFeature() {
        register(new CraftedDurabilityRedirector());
        register(new EmptyManaBankRedirector());
        register(new FriendJoinRedirector());
        register(new FriendLeaveRedirector());
        register(new GuildBankRedirector());
        register(new GuildRewardRedirector());
        register(new HealRedirector());
        register(new HealedByOtherRedirector());
        register(new HorseDespawnedRedirector());
        register(new HorseScaredRedirector());
        register(new HorseSpawnFailRedirector());
        register(new HousingTeleportArrivalCooldownRedirector());
        register(new HousingTeleportArrivalRedirector());
        register(new HousingTeleportDepartureCooldownRedirector());
        register(new HousingTeleportDepartureRedirector());
        register(new IngredientPouchSellRedirector());
        register(new LoginRedirector());
        register(new MageTeleportationFailRedirector());
        register(new ManaDeficitRedirector());
        register(new MerchantRedirector());
        register(new NoTotemRedirector());
        register(new PotionAlreadyActiveRedirector());
        register(new PotionsMaxRedirector());
        register(new PotionsMovedRedirector());
        register(new PotionsReplacedRedirector());
        register(new ScrollTeleportationHousingFailRedirector());
        register(new ScrollTeleportationMobFailRedirector());
        register(new SpeedBoostRedirector());
        register(new ToolDurabilityRedirector());
        register(new UnusedAbilityPointsRedirector());
        register(new UnusedSkillAndAbilityPointsRedirector());
        register(new UnusedSkillPointsRedirector());
    }

    private void register(Redirector redirector) {
        redirectors.add(redirector);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChatMessage(ChatMessageReceivedEvent e) {
        if (e.getRecipientType() != RecipientType.INFO) return;

        StyledText message = e.getOriginalStyledText();
        MessageType messageType = e.getMessageType();

        for (Redirector redirector : redirectors) {
            RedirectAction action = redirector.getAction();
            if (action == RedirectAction.KEEP) continue;

            Pattern pattern = redirector.getPattern(messageType);
            if (pattern == null) continue;

            Matcher matcher = message.getMatcher(pattern);

            if (matcher.find()) {
                e.setCanceled(true);
                if (redirector.getAction() == RedirectAction.HIDE) continue;

                for (StyledText notification : redirector.getNotifications(matcher)) {
                    Managers.Notification.queueMessage(notification);
                }
            }
        }
    }

    public enum RedirectAction {
        KEEP,
        HIDE,
        REDIRECT
    }

    protected interface Redirector {
        Pattern getPattern(MessageType messageType);

        ChatRedirectFeature.RedirectAction getAction();

        List<StyledText> getNotifications(Matcher matcher);
    }

    public abstract static class SimpleRedirector implements Redirector {
        @Override
        public Pattern getPattern(MessageType messageType) {
            return switch (messageType) {
                case FOREGROUND -> getForegroundPattern();
                case BACKGROUND -> getBackgroundPattern();
            };
        }

        protected Pattern getForegroundPattern() {
            return null;
        }

        protected Pattern getBackgroundPattern() {
            return null;
        }

        @Override
        public List<StyledText> getNotifications(Matcher matcher) {
            return List.of(getNotification(matcher));
        }

        protected abstract StyledText getNotification(Matcher matcher);
    }

    private class CraftedDurabilityRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§cYour items are damaged and have become less effective. Bring them to a Blacksmith to repair them.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return craftedDurability.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.craftedDurability.notification")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class EmptyManaBankRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("§4Your mana bank is empty!");
        private static final Pattern BACKGROUND_PATTERN = Pattern.compile("§7Your mana bank is empty!");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return emptyManaBank.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.emptyManaBank.notification")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class FriendJoinRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§a(§o)?(?<name>.+)§2 has logged into server §a(?<server>.+)§2 as §aan? (?<class>.+)");
        private static final Pattern BACKGROUND_PATTERN = Pattern.compile(
                "§7(§o)?(?<name>.+)§8(§o)? has logged into server §7(§o)?(?<server>.+)§8(§o)? as §7(§o)?an? (?<class>.+)");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return friendJoin.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String playerName = matcher.group("name");
            String server = matcher.group("server");
            String playerClass = matcher.group("class");

            return StyledText.fromString(ChatFormatting.GREEN + "→ " + ChatFormatting.DARK_GREEN
                    + playerName + " [" + ChatFormatting.GREEN
                    + server + "/" + playerClass + ChatFormatting.DARK_GREEN
                    + "]");
        }
    }

    private class FriendLeaveRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("§a(?<name>.+) left the game.");
        private static final Pattern BACKGROUND_PATTERN = Pattern.compile("§7(?<name>.+) left the game.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return friendJoin.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String playerName = matcher.group("name");

            return StyledText.fromString(ChatFormatting.RED + "← " + ChatFormatting.DARK_GREEN + playerName);
        }
    }

    private class HealRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^§c\\[\\+(\\d+) ❤\\]$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return heal.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String amount = matcher.group(1);

            return StyledText.fromString(ChatFormatting.DARK_RED + "[+" + amount + " ❤]");
        }
    }

    private class HealedByOtherRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^.+ gave you §c\\[\\+(\\d+) ❤\\]$");
        private static final Pattern BACKGROUND_PATTERN = Pattern.compile("^.+ gave you §7§o\\[\\+(\\d+) ❤\\]$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return heal.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String amount = matcher.group(1);

            return StyledText.fromString(ChatFormatting.DARK_RED + "[+" + amount + " ❤]");
        }
    }

    private class HorseDespawnedRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§dSince you interacted with your inventory, your horse has despawned.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return horse.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.horse.notificationDespawned")
                            .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    private class HorseScaredRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§dYour horse is scared to come out right now, too many mobs are nearby\\.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return horse.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.horse.notificationScared")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class HorseSpawnFailRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("§4There is no room for a horse\\.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return horse.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.horse.notificationNoRoom")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class HousingTeleportArrivalRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^§aYou have flown to your housing island.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return housingTeleport.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.housingTeleport.notificationJoined")
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    private class HousingTeleportArrivalCooldownRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§cYou need to wait a bit before joining another house.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return housingTeleport.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.housingTeleport.notificationCooldown")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class HousingTeleportDepartureRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§aYou have flown to your original position.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return housingTeleport.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.housingTeleport.notificationLeft")
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    private class HousingTeleportDepartureCooldownRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§cYou need to wait a bit before leaving a house.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return housingTeleport.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.housingTeleport.notificationCooldown")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class IngredientPouchSellRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§dYou have sold §7(.+)§d ingredients for a total of §a(.+)§d\\.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return ingredientPouch.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            int ingredientCount = Integer.parseInt(matcher.group(1));
            Component formattedPlural = Component.literal(ingredientCount + " ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable("feature.wynntils.chatRedirect.ingredientPouch.ingredient"
                                    + (ingredientCount == 1 ? "Singular" : "Plural"))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));

            StyledText emeraldString =
                    StyledText.fromString(ChatFormatting.GREEN + matcher.group(2) + ChatFormatting.LIGHT_PURPLE);

            return StyledText.fromComponent(Component.translatable(
                            "feature.wynntils.chatRedirect.ingredientPouch.notification",
                            formattedPlural.getString(),
                            emeraldString.getString())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public class LoginRedirector extends SimpleRedirector {
        private static final String RANK_STRING =
                Arrays.stream(PlayerRank.values()).map(PlayerRank::getTag).collect(Collectors.joining());
        // Test in ChatRedirectFeature_LoginRedirector_FOREGROUND_PATTERN
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^(?<rank>[" + RANK_STRING
                + "]) §(?:#[0-9a-f]{6,8}|.)(?:§o)?(?:§<\\d>)?(?<name>[\\w ]{1,20})§. has just logged in!$");
        private static final Pattern BACKGROUND_PATTERN =
                Pattern.compile("^(?:§8)?\\[(§.)+\\|?(§.)*(?<rank>[" + RANK_STRING
                        + "]) §(?:#[0-9a-f]{6,8}|.)(?:§o)?(?:§<\\d>)?(?<name>[\\w ]{1,20})§. has just logged in!$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return loginAnnouncements.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String rankString = matcher.group("rank");
            String playerName = matcher.group("name");
            PlayerRank rank = PlayerRank.fromString(rankString);

            return StyledText.fromString(ChatFormatting.GREEN + "→ " + ChatFormatting.RESET + rank.getTag() + " "
                    + rank.getTextColor() + playerName);
        }
    }

    private class MageTeleportationFailRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§cSorry, you can't teleport\\.\\.\\. Try moving away from blocks\\.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return mageTeleport.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.mageTeleport.notification")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class ManaDeficitRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§4You don't have enough mana to cast that spell!$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return notEnoughMana.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.notEnoughMana.notification")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class NoTotemRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("§4You have no active totems near you$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return shaman.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(Component.translatable("feature.wynntils.chatRedirect.shaman.notification")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class PotionAlreadyActiveRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§cYou already have that potion active\\.\\.\\.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return potion.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.potion.notificationAlreadyActive")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class PotionsMaxRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§4You already are holding the maximum amount of potions allowed\\.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return potion.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.potion.notificationChargeLimit")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class PotionsMovedRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§7You already are holding the maximum amount of potions allowed so your crafting result was moved to your bank\\.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return potion.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.potion.notificationMoved")
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    private class PotionsReplacedRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§7One less powerful potion was replaced to open space for the added one\\.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return potion.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.potion.notificationReplaced")
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    private class ScrollTeleportationHousingFailRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§cYou can not teleport while inside a house\\.\\.\\.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return scrollTeleport.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.scrollTeleport.notificationHousing")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class ScrollTeleportationMobFailRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§cThere are aggressive mobs nearby\\.\\.\\.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return scrollTeleport.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.scrollTeleport.notificationMobs")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class SpeedBoostRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^§b\\+([23]) minutes§7 speed boost\\.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return speed.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String minutes = matcher.group(1);
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.speed.notificationFirst", minutes)
                            .withStyle(ChatFormatting.AQUA)
                            .append(Component.translatable("feature.wynntils.chatRedirect.speed.notificationSecond")
                                    .withStyle(ChatFormatting.GRAY)));
        }
    }

    private class ToolDurabilityRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§4Your tool has 0 durability left! You will not receive any new resources until you repair it at a Blacksmith\\.$");

        @Override
        public Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return toolDurability.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.toolDurability.notification")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private class UnusedAbilityPointsRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§4You have §b§l(\\d+) unused Ability Points?! §4Right-Click while holding your compass to use them$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return unusedPoints.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String unusedAbilityPoints = matcher.group(1);

            return getUnusedAbilityPointsMessage(unusedAbilityPoints);
        }

        protected static StyledText getUnusedAbilityPointsMessage(String unusedAbilityPoints) {
            String pointsString =
                    ChatFormatting.BOLD + unusedAbilityPoints + ChatFormatting.RESET + ChatFormatting.DARK_AQUA;
            return StyledText.fromComponent(Component.translatable(
                            "feature.wynntils.chatRedirect.unusedPoints.notificationAbility", pointsString)
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private class UnusedSkillAndAbilityPointsRedirector implements Redirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§4You have §c§l(\\d+) unused Skill Points?§4 and §b§l(\\d+) unused Ability Points?! §4Right-Click while holding your compass to use them$");

        @Override
        public Pattern getPattern(MessageType messageType) {
            if (messageType == MessageType.FOREGROUND) {
                return FOREGROUND_PATTERN;
            } else {
                return null;
            }
        }

        @Override
        public RedirectAction getAction() {
            return unusedPoints.get();
        }

        @Override
        public List<StyledText> getNotifications(Matcher matcher) {
            String unusedSkillPoints = matcher.group(1);
            String unusedAbilityPoints = matcher.group(2);

            return List.of(
                    UnusedSkillPointsRedirector.getUnusedSkillPointsMessage(unusedSkillPoints),
                    UnusedAbilityPointsRedirector.getUnusedAbilityPointsMessage(unusedAbilityPoints));
        }
    }

    private class UnusedSkillPointsRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§4You have §c§l(\\d+) unused Skill Points?! §4Right-Click while holding your compass to use them$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return unusedPoints.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String unusedSkillPoints = matcher.group(1);

            return getUnusedSkillPointsMessage(unusedSkillPoints);
        }

        protected static StyledText getUnusedSkillPointsMessage(String unusedSkillPoints) {
            String pointsString =
                    ChatFormatting.BOLD + unusedSkillPoints + ChatFormatting.RESET + ChatFormatting.DARK_RED;
            return StyledText.fromComponent(
                    Component.translatable("feature.wynntils.chatRedirect.unusedPoints.notificationSkill", pointsString)
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private final class GuildBankRedirector extends SimpleRedirector {
        private static final String DEPOSIT_SYMBOL = "←";
        private static final String WITHDRAW_SYMBOL = "→";
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§3\\[INFO\\]§b (?<player>.+) (?<transactiontype>withdrew|deposited) (?<count>\\d+)x (?<item>.+) (?:from|to) the Guild Bank \\((?<banktype>Everyone|High Ranked)\\)$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return guildBank.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String player = matcher.group("player");
            String transactionType = matcher.group("transactiontype");
            String count = matcher.group("count");
            String item = matcher.group("item");
            String bankType = matcher.group("banktype");

            return StyledText.fromString(ChatFormatting.AQUA
                    + (transactionType.equals("withdrew") ? WITHDRAW_SYMBOL : DEPOSIT_SYMBOL) + ChatFormatting.DARK_AQUA
                    + " " + player + " " + count + "x " + item + " (" + bankType + ") ");
        }
    }

    private final class GuildRewardRedirector extends SimpleRedirector {
        private static final String REWARD_SYMBOL = "→";

        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§3\\[INFO\\]§b (?<sender>.+) rewarded §3(?<reward>.+)§b to (?<recipient>.+)\\.\\n§3Rewards can be claimed in the Member Menu\\.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return guildRewards.get();
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            String sender = matcher.group("sender");
            String reward = matcher.group("reward");
            String recipient = matcher.group("recipient");

            return StyledText.fromString(ChatFormatting.AQUA + sender + ChatFormatting.DARK_AQUA + " " + REWARD_SYMBOL
                    + " " + ChatFormatting.AQUA + recipient + ChatFormatting.DARK_AQUA + ": " + reward);
        }
    }

    private final class MerchantRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§5(?<merchant>.*): §dThank you for your business\\. Come again!$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected StyledText getNotification(Matcher matcher) {
            return StyledText.fromComponent(Component.translatable(
                            "feature.wynntils.chatRedirect.merchant.notification", matcher.group("merchant"))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        @Override
        public RedirectAction getAction() {
            return merchant.get();
        }
    }
}
