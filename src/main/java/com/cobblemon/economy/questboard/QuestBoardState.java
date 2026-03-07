package com.cobblemon.economy.questboard;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class QuestBoardState {
    public String boardId;
    public String boardName;
    public int activeCount;
    public int maxActive;
    public int visibleQuests;
    public long rotationRemainingMs;
    public List<QuestCard> quests = new ArrayList<>();

    public static class QuestCard {
        public String questId;
        public String questName;
        public String status;
        public String progressSummary;
        public String previewKind;
        public String previewSpecies;
        public boolean previewShiny;
        public String previewItem;
        public String requiredBall;
        public long timeRemainingMs;
        public long cooldownRemainingMs;
        public Integer timeLimitMinutes;
        public BigDecimal rewardPokedollars = BigDecimal.ZERO;
        public BigDecimal rewardPco = BigDecimal.ZERO;
        public List<String> objectives = new ArrayList<>();
    }
}
