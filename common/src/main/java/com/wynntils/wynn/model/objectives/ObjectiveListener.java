/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.objectives;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.managers.Managers;
import com.wynntils.handlers.scoreboard.ScoreboardListener;
import com.wynntils.handlers.scoreboard.Segment;
import com.wynntils.wynn.model.scoreboard.ScoreboardModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjectiveListener implements ScoreboardListener {
    // §b is guild objective, §a is normal objective and §c is daily objective
    private static final Pattern OBJECTIVE_PATTERN_ONE_LINE =
            Pattern.compile("^§([abc])[- ]\\s§7(.*): *§f(\\d+)§7/(\\d+)$");
    private static final Pattern OBJECTIVE_PATTERN_MULTILINE_START = Pattern.compile("^§([abc])[- ]\\s§7(.*)$");
    private static final Pattern OBJECTIVE_PATTERN_MULTILINE_END = Pattern.compile(".*§f(\\d+)§7/(\\d+)$");
    private static final Pattern SEGMENT_HEADER = Pattern.compile("^§.§l[A-Za-z ]+:.*$");

    @Override
    public void onSegmentChange(Segment newValue, ScoreboardModel.SegmentType segmentType) {
        List<WynnObjective> objectives = parseObjectives(newValue).stream()
                .filter(wynnObjective -> wynnObjective.getScore() < wynnObjective.getMaxScore())
                .toList();

        if (segmentType == ScoreboardModel.SegmentType.GuildObjective) {
            for (WynnObjective objective : objectives) {
                if (objective.isGuildObjective()) {
                    Managers.Objectives.updateGuildObjective(objective);
                }
            }
        } else {
            for (WynnObjective objective : objectives) {
                if (!objective.isGuildObjective()) {
                    Managers.Objectives.updatePersonalObjective(objective);
                }
            }

            // filter out deleted objectives
            Managers.Objectives.purgePersonalObjectives(objectives);
        }
    }

    private List<WynnObjective> parseObjectives(Segment segment) {
        List<WynnObjective> parsedObjectives = new ArrayList<>();

        List<String> actualContent = new ArrayList<>();
        StringBuilder multiLine = new StringBuilder();

        for (String line : segment.getContent()) {
            if (OBJECTIVE_PATTERN_ONE_LINE.matcher(line).matches()) {
                actualContent.add(line);
                continue;
            }

            if (OBJECTIVE_PATTERN_MULTILINE_START.matcher(line).matches()) {
                if (!multiLine.isEmpty()) {
                    WynntilsMod.error("ObjectiveManager: Multi-line objective start repeatedly:");
                    WynntilsMod.error("Already got: " + multiLine);
                    WynntilsMod.error("Next line: " + line);
                }

                multiLine = new StringBuilder(line);
                continue;
            }

            // If we have started collecting a multiline, keep building it
            if (!multiLine.isEmpty()) {
                multiLine.append(line);
            }

            if (OBJECTIVE_PATTERN_MULTILINE_END.matcher(line).matches()) {
                actualContent.add(multiLine.toString().trim().replaceAll(" +", " "));
                multiLine = new StringBuilder();
            }
        }

        if (!multiLine.isEmpty() && !SEGMENT_HEADER.matcher(multiLine).matches()) {
            WynntilsMod.error("ObjectiveManager: Got a not finished multi-line objective: " + multiLine);
        }

        for (String line : actualContent) {
            Matcher objectiveMatcher = OBJECTIVE_PATTERN_ONE_LINE.matcher(line);
            if (!objectiveMatcher.matches()) {
                WynntilsMod.error("ObjectiveManager: Broken objective stored: " + line);
                continue;
            }

            // Determine objective type with the formatting code
            boolean isGuildObjective = Objects.equals(objectiveMatcher.group(1), "b");
            WynnObjective parsed = WynnObjective.parseObjectiveLine(line, isGuildObjective);

            parsedObjectives.add(parsed);
        }
        return parsedObjectives;
    }

    @Override
    public void onSegmentRemove(Segment segment, ScoreboardModel.SegmentType segmentType) {
        List<WynnObjective> objectives = parseObjectives(segment);

        for (WynnObjective objective : objectives) {
            if (objective.getGoal() != null) {
                Managers.Objectives.removeObjective(objective);
            }
        }
    }

    @Override
    public void resetHandler() {
        Managers.Objectives.resetObjectives();
    }
}
