package com.backend.promptvprompt.services;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Service;

import com.backend.promptvprompt.DTO.Matchmaking.Match;
import com.backend.promptvprompt.DTO.Matchmaking.PlayerQueue;

@Service
public class MatchmakingService {
    private final ConcurrentLinkedQueue<PlayerQueue> queue = new ConcurrentLinkedQueue<>();

    public Match addPlayer(String playerId, String socketId) {
        queue.add(new PlayerQueue(playerId, socketId, Instant.now().toEpochMilli()));
        return tryMatch();
    }

    public Match tryMatch() {
        // Just need any 2 players, no elo, no region just yet
        if (queue.size() >= 2) {
            PlayerQueue playerOne = queue.poll();
            PlayerQueue playerTwo = queue.poll();

            if (playerOne != null && playerTwo != null) {
                return new Match(playerOne, playerTwo);
            }
        }
        return null;
    }

    public void removePlayer(String playerId) {
        queue.removeIf(p -> p.getPlayerId().equals(playerId));
    }

    public int getQueueSize() {
        return queue.size();
    }
}
