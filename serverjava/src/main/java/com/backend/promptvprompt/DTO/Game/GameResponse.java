package com.backend.promptvprompt.DTO.Game;

import com.backend.promptvprompt.models.Game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameResponse {
    private String status;
    private String errorType;
    private String message;
    private GameData gameData;
}
