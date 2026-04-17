package com.game.npat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {
    private MessageType type;
    private String roomCode;
    private String playerId;
    private String playerName;
    private Map<String, String> answers;
    private Map<String, Integer> categoryScores;  // NEW: For scoring phase
    private Object data;

    public enum MessageType {
        // Client -> Server
        CREATE_ROOM,
        JOIN_ROOM,
        LEAVE_ROOM,
        START_GAME,
        SUBMIT_ANSWERS,
        SUBMIT_SCORES,      // NEW
        PLAY_AGAIN,         // NEW
        READY,

        // Server -> Client
        ROOM_CREATED,
        ROOM_JOINED,
        PLAYER_JOINED,
        PLAYER_LEFT,
        GAME_STARTED,
        GAME_UPDATE,
        SCORING_PHASE,      // NEW
        SCORING_UPDATE,     // NEW
        RESULTS_READY,
        ERROR
    }
}