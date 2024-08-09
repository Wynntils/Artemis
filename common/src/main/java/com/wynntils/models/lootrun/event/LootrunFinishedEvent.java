/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.lootrun.event;

import net.neoforged.bus.api.Event;

public abstract class LootrunFinishedEvent extends Event {
    protected final int challengesCompleted;
    protected final int timeElapsed;

    protected LootrunFinishedEvent(int challengesCompleted, int timeElapsed) {
        this.challengesCompleted = challengesCompleted;
        this.timeElapsed = timeElapsed;
    }

    public int getChallengesCompleted() {
        return challengesCompleted;
    }

    public int getTimeElapsed() {
        return timeElapsed;
    }

    public static class Completed extends LootrunFinishedEvent {
        protected final int rewardPulls;
        protected final int rewardRerolls;
        protected final int rewardSacrifices;
        private final int experienceGained;
        private final int mobsKilled;
        private final int chestsOpened;

        public Completed(
                int challengesCompleted,
                int timeElapsed,
                int rewardPulls,
                int rewardRerolls,
                int rewardSacrifices,
                int experienceGained,
                int mobsKilled,
                int chestsOpened) {
            super(challengesCompleted, timeElapsed);
            this.rewardPulls = rewardPulls;
            this.rewardRerolls = rewardRerolls;
            this.rewardSacrifices = rewardSacrifices;
            this.experienceGained = experienceGained;
            this.mobsKilled = mobsKilled;
            this.chestsOpened = chestsOpened;
        }

        public int getRewardPulls() {
            return rewardPulls;
        }

        public int getRewardRerolls() {
            return rewardRerolls;
        }

        public int getRewardSacrifices() {
            return rewardSacrifices;
        }

        public int getExperienceGained() {
            return experienceGained;
        }

        public int getMobsKilled() {
            return mobsKilled;
        }

        public int getChestsOpened() {
            return chestsOpened;
        }
    }

    public static class Failed extends LootrunFinishedEvent {
        public Failed(int challengesCompleted, int timeElapsed) {
            super(challengesCompleted, timeElapsed);
        }
    }
}
