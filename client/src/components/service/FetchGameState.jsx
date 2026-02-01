export default async function fetchGameState(gameId, userId) {
    const response = await fetch(`http://localhost:8080/api/game/${gameId}?userId=${userId}`);

    if (!response.ok) {
        throw new Error('Failed to fetch game state');
    }
    const data = await response.json();


    return data;
}