/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.tooltip;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.tooltip.type.TooltipIdentificationDecorator;
import com.wynntils.handlers.tooltip.type.TooltipStyle;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.stats.type.StatListOrdering;
import com.wynntils.models.wynnitem.parsing.WynnItemParser;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import net.minecraft.network.chat.Component;

public abstract class TooltipBuilder {
    protected static final TooltipStyle DEFAULT_TOOLTIP_STYLE =
            new TooltipStyle(StatListOrdering.WYNNCRAFT, false, false, true, true);
    protected final List<Component> header;
    protected final List<Component> footer;

    // The identificationsCache is only valid if the cached dependencies matchs
    protected ClassType cachedCurrentClass;
    protected TooltipStyle cachedStyle;
    protected TooltipIdentificationDecorator cachedDecorator;
    protected List<Component> identificationsCache;

    protected TooltipBuilder(List<Component> header, List<Component> footer) {
        this.header = header;
        this.footer = footer;
    }

    public List<Component> getTooltipLines(ClassType currentClass) {
        return getTooltipLines(currentClass, DEFAULT_TOOLTIP_STYLE, null);
    }

    public List<Component> getTooltipLines(
            ClassType currentClass, TooltipStyle style, TooltipIdentificationDecorator decorator) {
        List<Component> tooltip = new ArrayList<>();

        // Header and footer are always constant
        tooltip.addAll(header);

        List<Component> identifications;

        // Identification lines are rendered differently depending on current class, requested
        // style and provided decorator. If all match, use cache.
        if (currentClass != cachedCurrentClass || cachedStyle != style || cachedDecorator != decorator) {
            identifications = getIdentificationLines(currentClass, style, decorator);
            identificationsCache = identifications;
            cachedCurrentClass = currentClass;
            cachedStyle = style;
            cachedDecorator = decorator;
        } else {
            identifications = identificationsCache;
        }

        tooltip.addAll(identifications);

        tooltip.addAll(footer);

        return tooltip;
    }

    protected abstract List<Component> getIdentificationLines(
            ClassType currentClass, TooltipStyle style, TooltipIdentificationDecorator decorator);

    protected static Pair<List<Component>, List<Component>> extractHeaderAndFooter(List<Component> lore) {
        List<Component> header = new ArrayList<>();
        List<Component> footer = new ArrayList<>();

        boolean headerEnded = false;
        boolean footerStarted = false;
        boolean skillPointsStarted = false;

        boolean foundSkills = false;
        boolean foundIdentifications = false;
        for (Component loreLine : lore) {
            StyledText codedLine = StyledText.fromComponent(loreLine).getNormalized();

            if (!footerStarted) {
                if (codedLine.matches(WynnItemParser.SET_BONUS_PATTERN)) {
                    headerEnded = true;
                    footerStarted = true;
                } else {
                    Matcher matcher = codedLine.getMatcher(WynnItemParser.IDENTIFICATION_STAT_PATTERN);
                    if (matcher.matches()) {
                        // Some orders do not have a blank line after a skill point line,
                        // so reset the flag here
                        skillPointsStarted = false;

                        String statName = matcher.group(6);

                        if (Skill.isSkill(statName)) {
                            skillPointsStarted = true;
                            foundSkills = true;
                            // Skill points are in a separate section to the rest of the identifications,
                            // but we still don't want to keep them
                        } else {
                            foundIdentifications = true;
                            // Don't keep identifications lines at all
                        }

                        headerEnded = true;
                        continue;
                    } else if (skillPointsStarted) {
                        // If there were skill points, there might be a blank line after them
                        skillPointsStarted = false;
                        continue;
                    }
                }
            }

            // We want to keep this line, so figure out where to put it
            if (!headerEnded) {
                header.add(loreLine);
            } else {
                // From now on, we can skip looking for identification lines
                footerStarted = true;
                footer.add(loreLine);
            }
        }

        if (foundSkills && !foundIdentifications) {
            // If there were skills but no identifications,
            // then the footer is missing a blank line
            footer.addFirst(Component.literal(""));
        }

        return Pair.of(header, footer);
    }
}
