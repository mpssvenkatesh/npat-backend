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
public class ScoringAssignment {
    private String targetPlayerId;
    private String targetPlayerName;
    private Map<String, String> targetAnswers;
    private Map<String, String> scorerAnswers;
}