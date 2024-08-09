/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.actionbar;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handler;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.actionbar.event.ActionBarRenderEvent;
import com.wynntils.handlers.actionbar.event.ActionBarUpdatedEvent;
import com.wynntils.mc.event.ChatPacketReceivedEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.type.IterationDecision;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;

public final class ActionBarHandler extends Handler {
    private static final ResourceLocation ACTION_BAR_FONT = ResourceLocation.withDefaultNamespace("hud/default/center");
    private static final ResourceLocation COORDINATES_FONT =
            ResourceLocation.withDefaultNamespace("hud/default/top_right");

    private static final FallBackSegmentMatcher FALLBACK_SEGMENT_MATCHER = new FallBackSegmentMatcher();

    private final List<ActionBarSegmentMatcher> segmentMatchers = new ArrayList<>();

    private StyledText lastParsedActionBarText = StyledText.EMPTY;
    private List<ActionBarSegment> lastMatchedSegments = new ArrayList<>();

    public void registerSegment(ActionBarSegmentMatcher segmentMatcher) {
        segmentMatchers.add(segmentMatcher);
    }

    @SubscribeEvent
    public void onActionBarUpdate(ChatPacketReceivedEvent.GameInfo event) {
        // FIXME: Reverse dependency!
        if (!Models.WorldState.onWorld()) return;

        StyledText packetText = StyledText.fromComponent(event.getMessage());

        // Separate the action bar text from the coordinates
        StyledText actionBarText = packetText.iterate((part, changes) -> {
            if (!ACTION_BAR_FONT.equals(part.getPartStyle().getFont())) {
                changes.remove(part);
            }

            return IterationDecision.CONTINUE;
        });

        StyledText coordinatesText = packetText.iterate((part, changes) -> {
            if (!COORDINATES_FONT.equals(part.getPartStyle().getFont())) {
                changes.remove(part);
            }

            return IterationDecision.CONTINUE;
        });

        if (actionBarText.isEmpty()) {
            WynntilsMod.warn("Failed to find action bar text in packet: " + packetText.getString());
            return;
        }

        List<ActionBarSegment> matchedSegments;

        // Skip parsing if the action bar text is the same as the last parsed one
        if (lastParsedActionBarText.equals(packetText)) {
            matchedSegments = lastMatchedSegments;
        } else {
            matchedSegments = parseActionBarSegments(actionBarText);

            lastParsedActionBarText = packetText;
            lastMatchedSegments = matchedSegments;

            if (WynntilsMod.isDevelopmentBuild() || WynntilsMod.isDevelopmentEnvironment()) {
                debugChecks(matchedSegments, actionBarText);
            }

            WynntilsMod.postEvent(new ActionBarUpdatedEvent(matchedSegments));
        }

        ActionBarRenderEvent actionBarRenderEvent = new ActionBarRenderEvent(matchedSegments);
        WynntilsMod.postEvent(actionBarRenderEvent);

        // Remove disabled segments from the action bar text
        for (ActionBarSegment disabledSegment : actionBarRenderEvent.getDisabledSegments()) {
            actionBarText = actionBarText.replaceFirst(disabledSegment.getSegmentText(), "");
        }

        StyledText renderedText = actionBarText;

        // Append coordinates if needed
        if (actionBarRenderEvent.shouldRenderCoordinates()) {
            renderedText = actionBarText.append(coordinatesText);
        }

        event.setMessage(renderedText.getComponent());
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        lastParsedActionBarText = StyledText.EMPTY;
        lastMatchedSegments = new ArrayList<>();
    }

    public List<ActionBarSegment> parseActionBarSegments(StyledText actionBarText) {
        List<ActionBarSegment> matchedSegments = new ArrayList<>();

        for (ActionBarSegmentMatcher segmentMatcher : segmentMatchers) {
            ActionBarSegment parsedSegment =
                    segmentMatcher.parse(actionBarText.getString().replaceAll("%", ""));
            if (parsedSegment == null) continue;

            matchedSegments.add(parsedSegment);
            actionBarText = actionBarText.replaceFirst(parsedSegment.getSegmentText(), "%");
        }

        // Check if there is any leftover text, add them as separate fallback segments
        // (as we could be missing a segment matcher in separate, not continuous parts of the action bar text)
        Arrays.stream(actionBarText.split("%"))
                .filter(text -> !text.isEmpty())
                .forEach(part -> matchedSegments.add(FALLBACK_SEGMENT_MATCHER.parse(part.getString())));

        return matchedSegments;
    }

    private static void debugChecks(List<ActionBarSegment> matchedSegments, StyledText actionBarText) {
        List<ActionBarSegment> fallbackSegments = matchedSegments.stream()
                .filter(segment -> segment instanceof FallbackSegment)
                .toList();

        fallbackSegments.forEach(segment ->
                WynntilsMod.warn("Failed to match a portion of the action bar text, using a fallback: " + segment));

        if (!fallbackSegments.isEmpty()) {
            WynntilsMod.warn("Action bar text: " + actionBarText.getString());
        }
    }

    /**
     * A fallback matcher that matches any action bar text that doesn't match any other segment.
     * This is used to prevent the action bar text from being lost if it doesn't match any other segment.
     */
    private static final class FallBackSegmentMatcher implements ActionBarSegmentMatcher {
        @Override
        public ActionBarSegment parse(String actionBar) {
            return new FallbackSegment(actionBar);
        }
    }

    private static final class FallbackSegment extends ActionBarSegment {
        private FallbackSegment(String segmentText) {
            super(segmentText);
        }

        @Override
        public String toString() {
            return "FallbackSegment{" + "segmentText='" + segmentText + '\'' + '}';
        }
    }
}
