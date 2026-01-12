import express from 'express';
const router = express.Router();
import GameService from '../services/gameService.js';
router.post('/game/:gameId/submit-turn', async (req, res) => {
    const { gameId } = req.params;
    const { userId, message } = req.body;
    //need a check to see if userId is part of game
    try {
        // Your game logic here
        const result = await GameService.submitTurn(gameId, userId, message);
        const isGameComplete = await GameService.checkGameEnd(gameId);

        // Access io from app
        const io = req.app.get('io');
        io.to(`game-${gameId}`).emit('turnSubmitted', {
            userId: userId,
            messageCount: result.messageCount
        });

        if (isGameComplete) {
            const finalGame = await GameService.getGame(gameId);
            io.to(`game-${gameId}`).emit('gameComplete', {
                winnerId: finalGame.winnerId,
                endReason: finalGame.endReason
            });
        }

        res.json({ turn, isGameComplete });
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

router.get('/game/:gameId', async (req, res) => {

    const { gameId } = req.params;
    const { userId } = req.query;

    try {
        const game = await GameService.getGame(gameId);

        if (!game) {
            return res.status(404).json({ error: 'Game not found' });
        }

        const myMessageCount = await GameService.getTurnCount(gameId, userId, 'ATTACK');

        const opponentId = game.playerOneId === userId ? game.playerTwoId : game.playerOneId;
        const opponentMessageCount = await GameService.getTurnCount(gameId, opponentId, 'ATTACK');

        res.json({
            myMessageCount,
            opponentMessageCount,
            game
        });
    } catch (error) {
        res.status(400).json({ error: error.message });
    }

});

export default router;