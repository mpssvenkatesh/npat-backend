package com.game.npat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String id;
    private String name;
    private boolean isHost;
    private boolean ready;
    private Map<String, String> answers;
    private int score;
    private boolean submitted;
    private String sessionId; // WebSocket session ID

    // NEW: For scoring phase
    private Map<String, Integer> givenScores; // Scores this player gave to someone else
    private int finalScore; // Final score after peer scoring
    private boolean scoringCompleted; // Track if player completed scoring

    public void resetForNewRound() {
        this.submitted = false;
        this.answers = null;
        this.score = 0;
        this.ready = false;
        this.givenScores = null;
        this.finalScore = 0;
        this.scoringCompleted = false;
    }
}