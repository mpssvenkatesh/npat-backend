package com.game.npat.service;

import com.game.npat.model.GameRoom;
import com.game.npat.model.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameService {

    @Value("${game.timer.duration.seconds}")
    private int timerDuration;

    @Value("${game.min.players}")
    private int minPlayers;

    @Value("${game.max.players}")
    private int maxPlayers;

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final List<String> availableLetters = Arrays.asList(
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "R", "S", "T", "U", "V", "W", "Y"
    );

    public GameRoom createRoom(String playerName, String sessionId) {
        String roomCode = generateRoomCode();
        String playerId = generatePlayerId();

        Player host = Player.builder()
                .id(playerId)
                .name(playerName)
                .isHost(true)
                .ready(false)
                .submitted(false)
                .score(0)
                .sessionId(sessionId)
                .build();

        GameRoom room = GameRoom.builder()
                .code(roomCode)
                .hostId(playerId)
                .players(new ArrayList<>(Collections.singletonList(host)))
                .gameState(GameRoom.GameState.LOBBY)
                .createdAt(LocalDateTime.now())
                .timerDuration(timerDuration)
                .build();

        rooms.put(roomCode, room);
        log.info("Room created: {} by player: {}", roomCode, playerName);
        
        return room;
    }

    public GameRoom joinRoom(String roomCode, String playerName, String sessionId) throws Exception {
        GameRoom room = rooms.get(roomCode.toUpperCase());
        
        if (room == null) {
            throw new Exception("Room not found");
        }

        if (room.getGameState() != GameRoom.GameState.LOBBY) {
            throw new Exception("Game already in progress");
        }

        if (room.getPlayers().size() >= maxPlayers) {
            throw new Exception("Room is full");
        }

        String playerId = generatePlayerId();
        Player player = Player.builder()
                .id(playerId)
                .name(playerName)
                .isHost(false)
                .ready(false)
                .submitted(false)
                .score(0)
                .sessionId(sessionId)
                .build();

        room.addPlayer(player);
        log.info("Player {} joined room: {}", playerName, roomCode);
        
        return room;
    }

    public GameRoom leaveRoom(String roomCode, String playerId) throws Exception {
        GameRoom room = rooms.get(roomCode);
        
        if (room == null) {
            throw new Exception("Room not found");
        }

        room.removePlayer(playerId);
        log.info("Player {} left room: {}", playerId, roomCode);

        // If room is empty, delete it
        if (room.getPlayers().isEmpty()) {
            rooms.remove(roomCode);
            log.info("Room {} deleted (empty)", roomCode);
            return null;
        }

        // If host left, assign new host
        if (playerId.equals(room.getHostId())) {
            Player newHost = room.getPlayers().get(0);
            newHost.setHost(true);
            room.setHostId(newHost.getId());
            log.info("New host assigned in room {}: {}", roomCode, newHost.getName());
        }

        return room;
    }

    public GameRoom startGame(String roomCode, String playerId) throws Exception {
        GameRoom room = rooms.get(roomCode);
        
        if (room == null) {
            throw new Exception("Room not found");
        }

        if (!playerId.equals(room.getHostId())) {
            throw new Exception("Only host can start the game");
        }

        if (room.getPlayers().size() < minPlayers) {
            throw new Exception("Need at least " + minPlayers + " players to start");
        }

        room.setGameState(GameRoom.GameState.PLAYING);
        room.setLetter(getRandomLetter());
        room.setStartTime(LocalDateTime.now());
        room.resetPlayersForNewRound();

        log.info("Game started in room: {} with letter: {}", roomCode, room.getLetter());
        
        return room;
    }

    public GameRoom submitAnswers(String roomCode, String playerId, Map<String, String> answers) throws Exception {
        GameRoom room = rooms.get(roomCode);
        
        if (room == null) {
            throw new Exception("Room not found");
        }

        Player player = room.getPlayer(playerId);
        if (player == null) {
            throw new Exception("Player not found");
        }

        player.setAnswers(answers);
        player.setSubmitted(true);
        player.setScore(calculateScore(answers, room.getLetter()));

        log.info("Player {} submitted answers in room: {}, score: {}", 
                 player.getName(), roomCode, player.getScore());

        // Check if all players submitted
        if (room.allPlayersSubmitted()) {
            room.setGameState(GameRoom.GameState.RESULTS);
            log.info("All players submitted in room: {}", roomCode);
        }

        return room;
    }

    public GameRoom getRoom(String roomCode) {
        return rooms.get(roomCode);
    }

    public List<GameRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public void removeRoomBySessionId(String sessionId) {
        rooms.values().forEach(room -> {
            List<Player> playersToRemove = room.getPlayers().stream()
                    .filter(p -> sessionId.equals(p.getSessionId()))
                    .collect(Collectors.toList());

            playersToRemove.forEach(player -> {
                try {
                    leaveRoom(room.getCode(), player.getId());
                } catch (Exception e) {
                    log.error("Error removing player on disconnect", e);
                }
            });
        });
    }

    private int calculateScore(Map<String, String> answers, String letter) {
        if (answers == null || letter == null) return 0;

        int score = 0;
        for (String answer : answers.values()) {
            if (answer != null && !answer.trim().isEmpty()) {
                String firstChar = answer.trim().substring(0, 1).toUpperCase();
                if (firstChar.equals(letter)) {
                    score += 10;
                }
            }
        }
        return score;
    }

    private String generateRoomCode() {
        Random random = new Random();
        String code;
        do {
            code = random.ints(4, 0, 26)
                    .mapToObj(i -> String.valueOf((char) ('A' + i)))
                    .collect(Collectors.joining());
        } while (rooms.containsKey(code));
        return code;
    }

    private String generatePlayerId() {
        return "player_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String getRandomLetter() {
        Random random = new Random();
        return availableLetters.get(random.nextInt(availableLetters.size()));
    }

    public void cleanupOldRooms(int hours) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);
        rooms.entrySet().removeIf(entry -> {
            boolean shouldRemove = entry.getValue().getCreatedAt().isBefore(threshold);
            if (shouldRemove) {
                log.info("Cleaning up old room: {}", entry.getKey());
            }
            return shouldRemove;
        });
    }
}
