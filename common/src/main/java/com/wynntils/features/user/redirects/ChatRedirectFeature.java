/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.redirects;

import com.wynntils.core.chat.MessageType;
import com.wynntils.core.chat.RecipientType;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.notifications.NotificationManager;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.wynn.event.ChatMessageReceivedEvent;
import com.wynntils.wynn.utils.WynnPlayerUtils;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo
public class ChatRedirectFeature extends UserFeature {
    private static final Pattern UNUSED_POINTS_1 =
            Pattern.compile("You have (\\d+) unused Skill Points?! Right-Click while holding your compass to use them");
    private static final Pattern UNUSED_POINTS_2 = Pattern.compile(
            "You have (\\d+) unused Ability Points?! Right-Click while holding your compass to use them");
    private static final Pattern UNUSED_POINTS_3 = Pattern.compile(
            "You have (\\d+) unused Skill Points? and (\\d+) unused Ability Points?! Right-Click while holding your compass to use them");

    private static final Pattern NO_TOOL_DURABILITY_PATTERN = Pattern.compile(
            "^Your tool has 0 durability left! You will not receive any new resources until you repair it at a Blacksmith.$");
    private static final Pattern NO_CRAFTED_DURABILITY_PATTERN = Pattern.compile(
            "^Your items are damaged and have become less effective. Bring them to a Blacksmith to repair them.$");
    private static final Pattern NO_MANA_LEFT_TO_CAST_PATTERN =
            Pattern.compile("^§4You don't have enough mana to cast that spell!$");

    // Wynncraft forgot the period at the end of this message, so be on the lookout for a potential break in the future
    // if they fix it.
    private static final Pattern NO_ACTIVE_TOTEMS_PATTERN = Pattern.compile("§4You have no active totems near you$");

    private static final Pattern HEALED_PATTERN = Pattern.compile("^.+ gave you §r§c\\[\\+(\\d+) ❤\\]$");

    private static final Pattern HEAL_PATTERN = Pattern.compile("^§r§c\\[\\+(\\d+) ❤\\]$");

    private static final Pattern SPEED_PATTERN = Pattern.compile("^\\+3 minutes speed boost.$");

    private static final Pattern BACKGROUND_HEALED_PATTERN = Pattern.compile("^.+ gave you §r§7§o\\[\\+(\\d+) ❤\\]$");

    private static final Pattern NO_ROOM_PATTERN = Pattern.compile("§4There is no room for a horse.");
    private static final Pattern HORSE_DESPAWNED_PATTERN =
            Pattern.compile("§dSince you interacted with your inventory, your horse has despawned.");

    private static final Pattern MAX_POTIONS_ALLOWED_PATTERN =
            Pattern.compile("§4You already are holding the maximum amount of potions allowed.");
    private static final Pattern LESS_POWERFUL_POTION_REMOVED_PATTERN =
            Pattern.compile("§7One less powerful potion was replaced to open space for the added one.");

    @Config
    private FilterType loginAnnouncements = FilterType.REDIRECT;

    @Config
    private FilterType soulPoint = FilterType.REDIRECT;

    @Config
    private FilterType unusedPoints = FilterType.REDIRECT;

    @Config
    private FilterType friendJoin = FilterType.REDIRECT;

    @Config
    private FilterType toolDurability = FilterType.REDIRECT;

    @Config
    private FilterType craftedDurability = FilterType.REDIRECT;

    @Config
    private FilterType notEnoughMana = FilterType.REDIRECT;

    @Config
    private FilterType heal = FilterType.REDIRECT;

    @Config
    private FilterType speed = FilterType.REDIRECT;

    @Config
    private FilterType horse = FilterType.REDIRECT;

    @Config
    private FilterType potion = FilterType.REDIRECT;

    @Config
    private FilterType shaman = FilterType.REDIRECT;

    private final List<Redirector> redirectors =
            List.of(new LoginRedirector(), new FriendJoinRedirector(), new FriendLeaveRedirector(),
                    new SoulPointRedirector(), new SoulPointDiscarder());

    @SubscribeEvent
    public void onInfoMessage(ChatMessageReceivedEvent e) {
        if (e.getRecipientType() != RecipientType.INFO) return;

        String msg = e.getOriginalCodedMessage();
        String uncoloredMsg = ComponentUtils.stripFormatting(msg);
        MessageType messageType = e.getMessageType();

        for (Redirector redirector : redirectors) {
            FilterType action = redirector.getAction();
            if (action == FilterType.KEEP) continue;

            Pattern pattern = redirector.getPattern(messageType);
            if (pattern == null) continue;

            Matcher matcher = pattern.matcher(msg);
            pattern.matcher(msg);
            if (matcher.find()) {
                e.setCanceled(true);
                if (redirector.getAction() == FilterType.HIDE) continue;

                String notification = redirector.getNotification(matcher);
                if (notification == null) continue;

                NotificationManager.queueMessage(notification);
            }
        }

        if (messageType == MessageType.NORMAL) {
            if (heal != FilterType.KEEP) {
                Matcher matcher = HEAL_PATTERN.matcher(msg);
                if (matcher.matches()) {
                    e.setCanceled(true);
                    if (heal == FilterType.HIDE) {
                        return;
                    }

                    String amount = matcher.group(1);

                    sendHealMessage(amount);
                    return;
                }

                matcher = HEALED_PATTERN.matcher(msg);
                if (matcher.matches()) {
                    e.setCanceled(true);
                    if (heal == FilterType.HIDE) {
                        return;
                    }

                    String amount = matcher.group(1);

                    sendHealMessage(amount);
                    return;
                }
            }

            if (speed != FilterType.KEEP) {
                Matcher matcher = SPEED_PATTERN.matcher(uncoloredMsg);
                if (matcher.matches()) {
                    e.setCanceled(true);
                    if (speed == FilterType.HIDE) {
                        return;
                    }

                    NotificationManager.queueMessage(new TextComponent("+3 minutes")
                            .withStyle(ChatFormatting.AQUA)
                            .append(new TextComponent(" speed boost").withStyle(ChatFormatting.GRAY)));
                    return;
                }
            }
        } else if (messageType == MessageType.SYSTEM) {
            if (unusedPoints != FilterType.KEEP) {

                Matcher matcher = UNUSED_POINTS_1.matcher(uncoloredMsg);

                int unusedSkillPoints = 0;
                int unusedAbilityPoints = 0;
                if (matcher.matches()) {
                    unusedSkillPoints = Integer.parseInt(matcher.group(1));
                }

                matcher = UNUSED_POINTS_2.matcher(uncoloredMsg);
                if (matcher.matches()) {
                    unusedAbilityPoints = Integer.parseInt(matcher.group(2));
                }

                matcher = UNUSED_POINTS_3.matcher(uncoloredMsg);
                if (matcher.matches()) {
                    unusedSkillPoints = Integer.parseInt(matcher.group(1));
                    unusedAbilityPoints = Integer.parseInt(matcher.group(2));
                }

                if (unusedPoints == FilterType.REDIRECT) {
                    if (unusedSkillPoints != 0) {
                        NotificationManager.queueMessage(new TextComponent("You have ")
                                .withStyle(ChatFormatting.DARK_RED)
                                .append(new TextComponent(String.valueOf(unusedSkillPoints))
                                        .withStyle(ChatFormatting.BOLD)
                                        .withStyle(ChatFormatting.DARK_RED))
                                .append(new TextComponent(" unused skill points").withStyle(ChatFormatting.DARK_RED)));
                    }

                    if (unusedAbilityPoints != 0) {
                        NotificationManager.queueMessage(new TextComponent("You have ")
                                .withStyle(ChatFormatting.DARK_AQUA)
                                .append(new TextComponent(String.valueOf(unusedAbilityPoints))
                                        .withStyle(ChatFormatting.BOLD)
                                        .withStyle(ChatFormatting.DARK_AQUA))
                                .append(new TextComponent(" unused ability points")
                                        .withStyle(ChatFormatting.DARK_AQUA)));
                    }
                }

                if (unusedPoints != FilterType.KEEP && (unusedAbilityPoints != 0 || unusedSkillPoints != 0)) {
                    e.setCanceled(true);
                }
            }

            if (shaman != FilterType.KEEP) {
                Matcher matcher = NO_ACTIVE_TOTEMS_PATTERN.matcher(msg);
                if (matcher.matches()) {
                    e.setCanceled(true);
                    if (shaman == FilterType.HIDE) {
                        return;
                    }

                    NotificationManager.queueMessage(
                            new TextComponent("No totems nearby!").withStyle(ChatFormatting.DARK_RED));
                    return;
                }
            }

            if (horse != FilterType.KEEP) {
                Matcher roomMatcher = NO_ROOM_PATTERN.matcher(msg);
                if (roomMatcher.matches()) {
                    e.setCanceled(true);
                    if (horse == FilterType.HIDE) {
                        return;
                    }
                    NotificationManager.queueMessage(
                            new TextComponent("No room for a horse!").withStyle(ChatFormatting.DARK_RED));
                    return;
                }

                Matcher despawnMatcher = HORSE_DESPAWNED_PATTERN.matcher(msg);
                if (despawnMatcher.matches()) {
                    e.setCanceled(true);
                    if (horse == FilterType.HIDE) {
                        return;
                    }
                    NotificationManager.queueMessage(
                            new TextComponent("Your horse has despawned.").withStyle(ChatFormatting.DARK_PURPLE));
                    return;
                }
            }

            if (toolDurability != FilterType.KEEP) {
                Matcher matcher = NO_TOOL_DURABILITY_PATTERN.matcher(uncoloredMsg);
                if (matcher.find()) {
                    e.setCanceled(true);
                    if (toolDurability == FilterType.HIDE) {
                        return;
                    }

                    NotificationManager.queueMessage(
                            new TextComponent("Your tool has 0 durability!").withStyle(ChatFormatting.DARK_RED));

                    return;
                }
            }

            if (craftedDurability != FilterType.KEEP) {
                Matcher matcher = NO_CRAFTED_DURABILITY_PATTERN.matcher(uncoloredMsg);
                if (matcher.find()) {
                    e.setCanceled(true);
                    if (craftedDurability == FilterType.HIDE) {
                        return;
                    }

                    NotificationManager.queueMessage(
                            new TextComponent("Your items are damaged.").withStyle(ChatFormatting.DARK_RED));

                    return;
                }
            }

            if (notEnoughMana != FilterType.KEEP) {
                Matcher matcher = NO_MANA_LEFT_TO_CAST_PATTERN.matcher(msg);
                if (matcher.matches()) {
                    e.setCanceled(true);
                    if (notEnoughMana == FilterType.HIDE) {
                        return;
                    }

                    NotificationManager.queueMessage(
                            new TextComponent("Not enough mana to do that spell!").withStyle(ChatFormatting.DARK_RED));

                    return;
                }
            }

            if (potion != FilterType.KEEP) {
                Matcher maxPotionsMatcher = MAX_POTIONS_ALLOWED_PATTERN.matcher(msg);
                if (maxPotionsMatcher.matches()) {
                    e.setCanceled(true);
                    if (potion == FilterType.HIDE) {
                        return;
                    }

                    NotificationManager.queueMessage(
                            new TextComponent("At potion charge limit!").withStyle(ChatFormatting.DARK_RED));

                    return;
                }

                Matcher lessPowerfulPotionMatcher = LESS_POWERFUL_POTION_REMOVED_PATTERN.matcher(msg);
                if (lessPowerfulPotionMatcher.matches()) {
                    e.setCanceled(true);
                    if (potion == FilterType.HIDE) {
                        return;
                    }

                    NotificationManager.queueMessage(
                            new TextComponent("Lesser potion replaced.").withStyle(ChatFormatting.GRAY));

                    return;
                }
            }
        } else if (messageType == MessageType.BACKGROUND) {
            if (heal != FilterType.KEEP) {
                Matcher matcher = BACKGROUND_HEALED_PATTERN.matcher(msg);
                if (matcher.matches()) {
                    e.setCanceled(true);
                    if (heal == FilterType.HIDE) {
                        return;
                    }

                    String amount = matcher.group(1);

                    sendHealMessage(amount);
                    return;
                }
            }
        }
    }

    private void sendHealMessage(String amount) {
        NotificationManager.queueMessage(
                new TextComponent("[+%s ❤]".formatted(amount)).withStyle(ChatFormatting.DARK_RED));
    }

    public enum FilterType {
        KEEP,
        HIDE,
        REDIRECT
    }

    private abstract static class Redirector {
        Pattern getPattern(MessageType messageType) {
            return switch (messageType) {
                case NORMAL -> getNormalPattern();
                case BACKGROUND -> getBackgroundPattern();
                case SYSTEM -> getSystemPattern();
            };
        }

        Pattern getSystemPattern() {
            return null;
        }

        Pattern getNormalPattern() {
            return null;
        }

        Pattern getBackgroundPattern() {
            return null;
        }

        abstract FilterType getAction();

        abstract String getNotification(Matcher matcher);
    }

    private class LoginRedirector extends Redirector {
        private static final Pattern LOGIN_ANNOUNCEMENT =
                Pattern.compile("^§.\\[§r§.([A-Z+]+)§r§.\\] §r§.(.*)§r§. has just logged in!$");
        private static final Pattern BACKGROUND_LOGIN_ANNOUNCEMENT =
                Pattern.compile("^(?:§r§8)?\\[§r§7([A-Z+]+)§r§8\\] §r§7(.*)§r§8 has just logged in!$");

        @Override
        Pattern getNormalPattern() {
            return LOGIN_ANNOUNCEMENT;
        }

        @Override
        Pattern getBackgroundPattern() {
            return BACKGROUND_LOGIN_ANNOUNCEMENT;
        }

        @Override
        FilterType getAction() {
            return loginAnnouncements;
        }

        @Override
        String getNotification(Matcher matcher) {
            String rank = matcher.group(1);
            String playerName = matcher.group(2);

            return ChatFormatting.GREEN + "→ " + WynnPlayerUtils.getFormattedRank(rank) + playerName;
        }
    }

    private class FriendJoinRedirector extends Redirector {
        private static final Pattern FRIEND_JOIN_PATTERN = Pattern.compile(
                "§a(§o)?(?<name>.+)§r§2 has logged into server §r§a(?<server>.+)§r§2 as §r§aan? (?<class>.+)");
        private static final Pattern BACKGROUND_FRIEND_JOIN_PATTERN = Pattern.compile(
                "§r§7(§o)?(?<name>.+)§r§8(§o)? has logged into server §r§7(§o)?(?<server>.+)§r§8(§o)? as §r§7(§o)?an? (?<class>.+)");

        @Override
        Pattern getNormalPattern() {
            return FRIEND_JOIN_PATTERN;
        }

        @Override
        Pattern getBackgroundPattern() {
            return BACKGROUND_FRIEND_JOIN_PATTERN;
        }

        @Override
        FilterType getAction() {
            return friendJoin;
        }

        @Override
        String getNotification(Matcher matcher) {
            String playerName = matcher.group("name");
            String server = matcher.group("server");
            String playerClass = matcher.group("class");

            return ChatFormatting.GREEN + "→ " + ChatFormatting.DARK_GREEN
                    + playerName + " [" + ChatFormatting.GREEN
                    + server + "/" + playerClass + ChatFormatting.DARK_GREEN
                    + "]";
        }
    }

    private class FriendLeaveRedirector extends Redirector {
        private static final Pattern FRIEND_LEAVE_PATTERN = Pattern.compile("§a(?<name>.+) left the game.");
        private static final Pattern BACKGROUND_FRIEND_LEAVE_PATTERN =
                Pattern.compile("§r§7(?<name>.+) left the game.");

        @Override
        Pattern getSystemPattern() {
            return FRIEND_LEAVE_PATTERN;
        }

        @Override
        Pattern getBackgroundPattern() {
            return BACKGROUND_FRIEND_LEAVE_PATTERN;
        }

        @Override
        FilterType getAction() {
            return friendJoin;
        }

        @Override
        String getNotification(Matcher matcher) {
            String playerName = matcher.group("name");

            return ChatFormatting.RED + "← " + ChatFormatting.DARK_GREEN + playerName;
        }
    }

    private class SoulPointRedirector extends Redirector {
        private static final Pattern BACKGROUND_SOUL_POINT_2 = Pattern.compile("^§r§7\\[(\\+\\d+ Soul Points?)\\]$");
        private static final Pattern SOUL_POINT_2 = Pattern.compile("^§d\\[(\\+\\d+ Soul Points?)\\]$");

        @Override
        Pattern getSystemPattern() {
            return SOUL_POINT_2;
        }

        @Override
        Pattern getBackgroundPattern() {
            return BACKGROUND_SOUL_POINT_2;
        }

        @Override
        FilterType getAction() {
            return soulPoint;
        }

        @Override
        String getNotification(Matcher matcher) {
            // Send the matching part, which could be +1 Soul Point or +2 Soul Points, etc.
            return ChatFormatting.LIGHT_PURPLE + matcher.group(1);
        }
    }

    private class SoulPointDiscarder extends Redirector {
        private static final Pattern SOUL_POINT_1 = Pattern.compile("^§5As the sun rises, you feel a little bit safer...$");
        private static final Pattern BACKGROUND_SOUL_POINT_1 =
                Pattern.compile("^(§r§8)?As the sun rises, you feel a little bit safer...$");

        @Override
        Pattern getSystemPattern() {
            return SOUL_POINT_1;
        }

        @Override
        Pattern getBackgroundPattern() {
            return BACKGROUND_SOUL_POINT_1;
        }

        @Override
        FilterType getAction() {
            return soulPoint;
        }

        @Override
        String getNotification(Matcher matcher) {
            // Soul point messages comes in two lines. We just throw away the chatty one
            // if we have hide or redirect as action.
            return null;
        }
    }
}
