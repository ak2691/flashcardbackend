
import { useState, useEffect } from 'react';

export const SubmitTurn = async (turnData) => {
    const response = await fetch(`http://localhost:8080/api/game/${turnData.gameId}/submit-turn`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(turnData)
    })
    if (!response.ok) {
        throw new Error('Failed to submit turn');

    }
    return response.json();
}