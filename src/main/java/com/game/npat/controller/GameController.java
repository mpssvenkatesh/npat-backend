package com.game.npat.controller;

import com.game.npat.dto.GameMessage;
import com.game.npat.model.GameRoom;
import com.game.npat.model.Player;
import com.game.npat.model.ScoringAssignment;
import com.game.npat.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/create-room")
    public void createRoom(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            GameRoom room = gameService.createRoom(message.getPlayerName(), sessionId);

            Player host = room.getPlayers().get(0);

            // Send room created message to creator
            GameMessage response = GameMessage.builder()
                    .type(GameMessage.MessageType.ROOM_CREATED)
                    .roomCode(room.getCode())
                    .playerId(host.getId())
                    .data(createRoomData(room))
                    .build();

            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/messages",
                    response
            );

            log.info("Room created response sent to session: {}", sessionId);

        } catch (Exception e) {
            log.error("Error creating room", e);
            sendError(headerAccessor.getSessionId(), e.getMessage());
        }
    }

    @MessageMapping("/join-room")
    public void joinRoom(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            GameRoom room = gameService.joinRoom(message.getRoomCode(), message.getPlayerName(), sessionId);

            Player joinedPlayer = room.getPlayers().stream()
                    .filter(p -> p.getSessionId().equals(sessionId))
                    .findFirst()
                    .orElseThrow();

            // Send room joined message to the player who joined
            GameMessage joinResponse = GameMessage.builder()
                    .type(GameMessage.MessageType.ROOM_JOINED)
                    .roomCode(room.getCode())
                    .playerId(joinedPlayer.getId())
                    .data(createRoomData(room))
                    .build();

            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/messages",
                    joinResponse
            );

            // Notify all players in the room about new player
            GameMessage playerJoinedMsg = GameMessage.builder()
                    .type(GameMessage.MessageType.PLAYER_JOINED)
                    .roomCode(room.getCode())
                    .data(createRoomData(room))
                    .build();

            broadcastToRoom(room.getCode(), playerJoinedMsg);

            log.info("Player joined room: {} - {}", room.getCode(), message.getPlayerName());

        } catch (Exception e) {
            log.error("Error joining room", e);
            sendError(headerAccessor.getSessionId(), e.getMessage());
        }
    }

    @MessageMapping("/leave-room")
    public void leaveRoom(@Payload GameMessage message) {
        try {
            GameRoom room = gameService.leaveRoom(message.getRoomCode(), message.getPlayerId());

            if (room != null) {
                // Notify remaining players
                GameMessage playerLeftMsg = GameMessage.builder()
                        .type(GameMessage.MessageType.PLAYER_LEFT)
                        .roomCode(room.getCode())
                        .data(createRoomData(room))
                        .build();

                broadcastToRoom(room.getCode(), playerLeftMsg);
            }

            log.info("Player left room: {} - {}", message.getRoomCode(), message.getPlayerId());

        } catch (Exception e) {
            log.error("Error leaving room", e);
        }
    }

    @MessageMapping("/start-game")
    public void startGame(@Payload GameMessage message) {
        try {
            GameRoom room = gameService.startGame(message.getRoomCode(), message.getPlayerId());

            // Notify all players that game started
            GameMessage gameStartedMsg = GameMessage.builder()
                    .type(GameMessage.MessageType.GAME_STARTED)
                    .roomCode(room.getCode())
                    .data(createGameStartData(room))
                    .build();

            broadcastToRoom(room.getCode(), gameStartedMsg);

            log.info("Game started in room: {}", room.getCode());

        } catch (Exception e) {
            log.error("Error starting game", e);
            sendErrorToPlayer(message.getRoomCode(), message.getPlayerId(), e.getMessage());
        }
    }

    @MessageMapping("/submit-answers")
    public void submitAnswers(@Payload GameMessage message) {
        try {
            GameRoom room = gameService.submitAnswers(
                    message.getRoomCode(),
                    message.getPlayerId(),
                    message.getAnswers()
            );

            // Notify all players about the update
            GameMessage updateMsg = GameMessage.builder()
                    .type(GameMessage.MessageType.GAME_UPDATE)
                    .roomCode(room.getCode())
                    .data(createGameUpdateData(room))
                    .build();

            broadcastToRoom(room.getCode(), updateMsg);

            // If all players submitted, start scoring phase
            if (room.allPlayersSubmitted()) {
                // Detect duplicates and assign scoring
                Map<String, Map<String, List<String>>> duplicates = gameService.findDuplicates(room);
                Map<String, ScoringAssignment> assignments = gameService.assignScoring(room);

                room.setGameState(GameRoom.GameState.SCORING);

                // Send scoring phase to each player individually
                for (Player player : room.getPlayers()) {
                    ScoringAssignment assignment = assignments.get(player.getId());

                    Map<String, Object> scoringData = new HashMap<>();
                    scoringData.put("letter", room.getLetter());
                    scoringData.put("categories", Arrays.asList("Name", "Place", "Animal", "Thing"));
                    scoringData.put("duplicates", duplicates);

                    Map<String, ScoringAssignment> playerAssignment = new HashMap<>();
                    playerAssignment.put(player.getId(), assignment);
                    scoringData.put("scoringAssignments", playerAssignment);

                    GameMessage scoringMsg = GameMessage.builder()
                            .type(GameMessage.MessageType.SCORING_PHASE)
                            .roomCode(room.getCode())
                            .data(scoringData)
                            .build();

                    messagingTemplate.convertAndSendToUser(
                            player.getSessionId(),
                            "/queue/messages",
                            scoringMsg
                    );
                }

                log.info("Scoring phase started in room: {}", room.getCode());
            }

            log.info("Answers submitted in room: {}", room.getCode());

        } catch (Exception e) {
            log.error("Error submitting answers", e);
            sendErrorToPlayer(message.getRoomCode(), message.getPlayerId(), e.getMessage());
        }
    }

    // NEW: Handle score submissions
    @MessageMapping("/submit-scores")
    public void submitScores(@Payload GameMessage message) {
        try {
            GameRoom room = gameService.submitScores(
                    message.getRoomCode(),
                    message.getPlayerId(),
                    message.getCategoryScores()
            );

            // Notify all players about scoring progress
            long completedCount = room.getPlayers().stream()
                    .filter(Player::isScoringCompleted)
                    .count();

            GameMessage scoringUpdateMsg = GameMessage.builder()
                    .type(GameMessage.MessageType.SCORING_UPDATE)
                    .roomCode(room.getCode())
                    .data(Map.of(
                            "playersScored", completedCount,
                            "totalPlayers", room.getPlayers().size()
                    ))
                    .build();

            broadcastToRoom(room.getCode(), scoringUpdateMsg);

            // If all players completed scoring, send results
            if (room.allPlayersCompletedScoring()) {
                GameMessage resultsMsg = GameMessage.builder()
                        .type(GameMessage.MessageType.RESULTS_READY)
                        .roomCode(room.getCode())
                        .data(createResultsData(room))
                        .build();

                broadcastToRoom(room.getCode(), resultsMsg);

                log.info("All players completed scoring in room: {}, results sent", room.getCode());
            }

            log.info("Scores submitted in room: {}", room.getCode());

        } catch (Exception e) {
            log.error("Error submitting scores", e);
            sendErrorToPlayer(message.getRoomCode(), message.getPlayerId(), e.getMessage());
        }
    }

    // NEW: Handle play again
    @MessageMapping("/play-again")
    public void playAgain(@Payload GameMessage message) {
        try {
            GameRoom room = gameService.resetGame(message.getRoomCode(), message.getPlayerId());

            // Send game started message
            GameMessage gameStartedMsg = GameMessage.builder()
                    .type(GameMessage.MessageType.GAME_STARTED)
                    .roomCode(room.getCode())
                    .data(createGameStartData(room))
                    .build();

            broadcastToRoom(room.getCode(), gameStartedMsg);

            log.info("Play again initiated in room: {}", room.getCode());

        } catch (Exception e) {
            log.error("Error in play again", e);
            sendErrorToPlayer(message.getRoomCode(), message.getPlayerId(), e.getMessage());
        }
    }

    private void broadcastToRoom(String roomCode, GameMessage message) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode, message);
    }

    private void sendError(String sessionId, String errorMessage) {
        GameMessage errorMsg = GameMessage.builder()
                .type(GameMessage.MessageType.ERROR)
                .data(errorMessage)
                .build();

        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/messages",
                errorMsg
        );
    }

    private void sendErrorToPlayer(String roomCode, String playerId, String errorMessage) {
        GameRoom room = gameService.getRoom(roomCode);
        if (room == null) {
            log.warn("Unable to send player-targeted error. Room not found: {}, player: {}", roomCode, playerId);
            return;
        }

        Player player = room.getPlayer(playerId);
        if (player == null || player.getSessionId() == null) {
            log.warn("Unable to send player-targeted error. Player/session not found in room: {}, player: {}", roomCode, playerId);
            return;
        }

        sendError(player.getSessionId(), errorMessage);
    }

    private Map<String, Object> createRoomData(GameRoom room) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", room.getCode());
        data.put("hostId", room.getHostId());
        data.put("players", room.getPlayers());
        data.put("gameState", room.getGameState());
        data.put("timerDuration", room.getTimerDuration());
        return data;
    }

    private Map<String, Object> createGameStartData(GameRoom room) {
        Map<String, Object> data = createRoomData(room);
        data.put("letter", room.getLetter());
        data.put("startTime", room.getStartTime());
        data.put("categories", Arrays.asList("Name", "Place", "Animal", "Thing"));
        return data;
    }

    private Map<String, Object> createGameUpdateData(GameRoom room) {
        Map<String, Object> data = new HashMap<>();
        data.put("playersSubmitted", room.getPlayers().stream().filter(Player::isSubmitted).count());
        data.put("totalPlayers", room.getPlayers().size());
        data.put("allSubmitted", room.allPlayersSubmitted());
        return data;
    }

    private Map<String, Object> createResultsData(GameRoom room) {
        Map<String, Object> data = new HashMap<>();
        data.put("letter", room.getLetter());
        data.put("players", room.getPlayers());

        // Find winner based on final scores
        Player winner = room.getPlayers().stream()
                .max((p1, p2) -> Integer.compare(p1.getScore(), p2.getScore()))
                .orElse(null);

        data.put("winner", winner);
        return data;
    }
}
