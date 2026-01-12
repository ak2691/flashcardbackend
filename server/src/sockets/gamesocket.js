
import matchmakingService from "../services/matchmakingService.js";
import GameService from "../services/gameService.js";
export default function setupGameSocket(io) {
    io.on('connection', (socket) => {
        socket.on('joinQueue', async ({ userId }) => {
            const match = matchmakingService.addPlayer(userId, socket.id);

            if (match) {

                const game = await GameService.createGameFromMatch(
                    match.playerOne.playerId,
                    match.playerTwo.playerId,
                );

                io.sockets.sockets.get(match.playerOne.socketId)?.join(`game-${game.id}`);
                io.sockets.sockets.get(match.playerTwo.socketId)?.join(`game-${game.id}`);
                io.to(match.playerOne.socketId).emit('gameFound', game);
                io.to(match.playerTwo.socketId).emit('gameFound', game);

                //manual testing
                //io.to(match.playerOne.socketId).emit('gameFound', { gameId: 1, userId: match.playerOne.playerId });
                //io.to(match.playerTwo.socketId).emit('gameFound', { gameId: 1, userId: match.playerTwo.playerId });
            } else {
                socket.emit('queueJoined', { position: matchmakingService.queue.length });
            }
        });
    })
}

