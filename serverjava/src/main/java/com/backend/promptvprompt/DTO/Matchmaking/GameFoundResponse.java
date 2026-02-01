package com.backend.promptvprompt.DTO.Matchmaking;

import com.backend.promptvprompt.models.Game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameFoundResponse {
    private String id;
    private String playerOneId;
    private String playerTwoId;
    private String status;
    private String phase;

    public GameFoundResponse(Game game) {
        this.id = game.getId();
        this.playerOneId = game.getPlayerOne().getId();
        this.playerTwoId = game.getPlayerTwo().getId();
        this.status = game.getStatus().name();
        this.phase = game.getPhase().name();
    }

}
