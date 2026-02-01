package com.backend.promptvprompt.DTO.Game;

import java.util.List;

import com.backend.promptvprompt.models.Game;
import com.backend.promptvprompt.models.GameTurn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameData {
    private int myMessageCount;
    private int opponentMessageCount;
    private List<GameTurn> gameTurns;
    private String phase;
    private boolean isGameComplete;
    private TransitionData transition;
}
