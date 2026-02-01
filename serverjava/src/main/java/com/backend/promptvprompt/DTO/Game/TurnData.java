package com.backend.promptvprompt.DTO.Game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnData {
    private String gameId;
    private String userId;
    private String message;
}
