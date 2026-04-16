package com.game.npat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRoom {
    private String code;
    private String hostId;
    private List<Player> players;
    private GameState gameState;
    private String letter;
    private LocalDateTime startTime;
    private LocalDateTime createdAt;
    private int timerDuration;
    
    public enum GameState {
        LOBBY,
        PLAYING,
        RESULTS,
        FINISHED
    }
    
    public void addPlayer(Player player) {
        if (this.players == null) {
            this.players = new ArrayList<>();
        }
        this.players.add(player);
    }
    
    public void removePlayer(String playerId) {
        if (this.players != null) {
            this.players.removeIf(p -> p.getId().equals(playerId));
        }
    }
    
    public Player getPlayer(String playerId) {
        if (this.players == null) return null;
        return this.players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
    
    public boolean allPlayersSubmitted() {
        if (this.players == null || this.players.isEmpty()) return false;
        return this.players.stream().allMatch(Player::isSubmitted);
    }
    
    public void resetPlayersForNewRound() {
        if (this.players != null) {
            this.players.forEach(Player::resetForNewRound);
        }
    }
}
