/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.model.scoreboard;

import java.util.ArrayList;
import java.util.List;

public final class Segment {
    private final ScoreboardModel.SegmentType type;
    private final String header;
    private String end;
    private List<String> content = null;
    private final int startIndex;
    private int endIndex = -1;

    private boolean changed;

    public Segment(ScoreboardModel.SegmentType type, String header, int startIndex) {
        this.type = type;
        this.header = header;
        this.startIndex = startIndex;
        this.changed = false;
    }

    @Override
    public String toString() {
        return "Segment[" + "type="
                + type + ", " + "header="
                + header + ", " + "content="
                + content + ", " + "startIndex="
                + startIndex + ", " + "endIndex="
                + endIndex + ']';
    }

    public List<String> getContent() {
        return content;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public ScoreboardModel.SegmentType getType() {
        return type;
    }

    public String getHeader() {
        return header;
    }

    public String getEnd() {
        return end;
    }

    public List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<>(this.content);
        lines.add(this.header);
        if (this.end != null) {
            lines.add(this.end);
        }

        return lines;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
