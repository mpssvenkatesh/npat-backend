package com.game.npat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NpatMultiplayerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NpatMultiplayerApplication.class, args);
        System.out.println("\n===========================================");
        System.out.println("🎮 NPAT Multiplayer Server Started!");
        System.out.println("===========================================");
        System.out.println("Server running on: http://localhost:8080");
        System.out.println("WebSocket endpoint: ws://localhost:8080/api/game-websocket");
        System.out.println("===========================================\n");
    }
}
