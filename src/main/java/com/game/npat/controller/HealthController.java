package com.game.npat.controller;

import com.game.npat.model.GameRoom;
import com.game.npat.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final GameService gameService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "NPAT Multiplayer Server");
        response.put("timestamp", System.currentTimeMillis());
        response.put("activeRooms", gameService.getAllRooms().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<GameRoom>> getAllRooms() {
        return ResponseEntity.ok(gameService.getAllRooms());
    }

    @GetMapping("/room/{code}")
    public ResponseEntity<GameRoom> getRoomStatus(@PathVariable String code) {
        GameRoom room = gameService.getRoom(code.toUpperCase());
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(room);
    }
}
