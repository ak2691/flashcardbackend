package com.backend.promptvprompt.DTO.Game;

import com.backend.promptvprompt.models.GamePhase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransitionData {
    private boolean isTransitioning;
    private int countdown;
    private String newPhase;
}
