package com.game.npat.scheduler;

import com.game.npat.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCleanupScheduler {

    private final GameService gameService;

    @Value("${game.room.expiry.hours}")
    private int expiryHours;

    // Run every hour
    @Scheduled(fixedRate = 3600000)
    public void cleanupOldRooms() {
        log.info("Running scheduled room cleanup task");
        gameService.cleanupOldRooms(expiryHours);
    }
}
