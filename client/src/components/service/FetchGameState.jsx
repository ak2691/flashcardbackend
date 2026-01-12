export default async function fetchGameState(gameId, userId) {
    const response = await fetch(`http://localhost:3000/api/game/${gameId}?userId=${userId}`);

    if (!response.ok) {
        throw new Error('Failed to fetch game state');
    }

    return response.json();
}