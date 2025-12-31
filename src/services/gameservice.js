// 5 turns
// 250 character limit
// player 1: count turn
// every prompt will inc count turn
// we save 

import { PrismaClient } from '@prisma/client';

class GameService {
    async createGame(playerOneId, templateId) {
        const template = await prisma.scenarioTemplate.findUnique({
            where: { id: templateId }
        });

        // Generate character and secret
        const character = this.generateCharacter(template);
        const secret = this.generateSecret(template);

        return await prisma.game.create({
            data: {
                playerOneId,
                templateId,
                generatedCharacter: character,
                generatedSecret: secret,
                status: 'WAITING_FOR_PLAYER',
                phase: 'DEFENSE'
            }
        });
    }
    async joinGame(gameId, playerTwoId) {
        return await prisma.game.update({
            where: { id: gameId },
            data: {
                playerTwoId,
                status: 'IN_PROGRESS'
            }
        });
    }

    async submitTurn(gameId, playerId, message) {
        const game = await prisma.game.findUnique({
            where: { id: gameId },
            include: { template: true }
        });

        //once this error is caught, send a message in the frontend to wait for their opponent
        const turnCount = await this.getTurnCount(gameId, playerId, game.phase);
        if (turnCount >= game.maxTurnsPerPhase) {
            throw new Error('Turn limit reached');
        }


        const aiResponse = await aiService.getResponse(
            game,
            playerId,
            message,
            game.phase
        );
        const turn = await prisma.gameTurn.create({
            data: {
                gameId,
                playerId,
                phase: game.phase,
                turnNumber: turnCount + 1,
                playerMessage: message,
                aiResponse
            }
        });

        await this.checkPhaseTransition(gameId);
        await this.checkGameEnd(gameId);
        return turn;
    }
    validateTurn(game, playerId, message) {
        if (game.status !== 'IN_PROGRESS') {
            throw new Error('Game not in progress');
        }

        //need to do something about spaces or white space characters can probably trim
        if (message.length > game.maxCharsPerMessage) {
            throw new Error(`Message exceeds ${game.maxCharsPerMessage} characters`);
        }

        // Verify player is part of this game
        if (playerId !== game.playerOneId && playerId !== game.playerTwoId) {
            throw new Error('Player not in this game');
        }
    }
    async getTurnCount(gameId, playerId, phase) {
        return await prisma.gameTurn.count({
            where: {
                gameId,
                playerId,
                phase
            }
        });
    }
    async checkPhaseTransition(gameId) {
        const game = await prisma.game.findUnique({
            where: { id: gameId }
        });

        if (game.phase !== 'DEFENSE') return;


        //may have to change something here specifically for isdefendingboolean
        const p1Turns = await this.getTurnCount(gameId, game.playerOneId, 'DEFENSE');
        const p2Turns = await this.getTurnCount(gameId, game.playerTwoId, 'DEFENSE');

        if (p1Turns >= game.maxTurnsPerPhase && p2Turns >= game.maxTurnsPerPhase) {
            await this.transitionToAttack(gameId);
        }
    }
    async transitionToAttack(gameId) {
        const game = await prisma.game.findUnique({
            where: { id: gameId },
            include: { template: true }
        });

        //should save costs? only if summary is actually shorter than the entire list of prompt and response
        const p1Summary = await this.generateDefenseSummary(gameId, game.playerOneId);
        const p2Summary = await this.generateDefenseSummary(gameId, game.playerTwoId);
        await prisma.game.update({
            where: { id: gameId },
            data: {
                phase: 'ATTACK',
                playerOneDefenseSummary: p1Summary,
                playerTwoDefenseSummary: p2Summary
            }
        });
    }
    async generateDefenseSummary(gameId, playerId) {
        const turns = await prisma.gameTurn.findMany({
            where: {
                gameId,
                playerId,
                phase: 'DEFENSE'
            },
            orderBy: { turnNumber: 'asc' }
        });
        //summarize defense, prompt and response list should be sent to wrapper "Can you summarize"
        return await aiService.summarizeDefense(turns);

    }
    async checkGameEnd(gameId) {
        const game = await prisma.game.findUnique({
            where: { id: gameId }
        });

        if (game.phase !== 'ATTACK') return;

        const p1AttackTurns = await this.getTurnCount(gameId, game.playerOneId, 'ATTACK');
        const p2AttackTurns = await this.getTurnCount(gameId, game.playerTwoId, 'ATTACK');

        // both players finished their attack turns
        if (p1AttackTurns >= game.maxTurnsPerPhase && p2AttackTurns >= game.maxTurnsPerPhase) {
            await this.determineWinner(gameId);
        }
    }

    async determineWinner(gameId) {
        const game = await prisma.game.findUnique({
            where: { id: gameId }
        });
        //get all the attack turns and then check if the users win
        //must achieve some goal like revealing the secret or convincing ai to do something

        const p1AttackTurns = await prisma.gameTurn.findMany({
            where: {
                gameId,
                playerId: game.playerOneId,
                phase: 'ATTACK'
            },
            orderBy: { turnNumber: 'asc' }
        });

        const p2AttackTurns = await prisma.gameTurn.findMany({
            where: {
                gameId,
                playerId: game.playerTwoId,
                phase: 'ATTACK'
            },
            orderBy: { turnNumber: 'asc' }
        });
        // Check if Player 1 broke Player 2's AI
        const p1Succeeded = await aiService.checkSecretRevealed(
            game,
            game.playerTwoId, // P1 attacks P2's AI
            p1AttackTurns
        );

        // Check if Player 2 broke Player 1's AI
        const p2Succeeded = await aiService.checkSecretRevealed(
            game,
            game.playerOneId, // P2 attacks P1's AI
            p2AttackTurns
        );

        if (p1Succeeded && !p2Succeeded) {
            winnerId = game.playerOneId;
            endReason = 'SECRET_REVEALED';
        } else if (p2Succeeded && !p1Succeeded) {
            winnerId = game.playerTwoId;
            endReason = 'SECRET_REVEALED';
        } else if (p1Succeeded && p2Succeeded) {
            // Both broke it - it's a draw (or you could have tiebreaker logic)
            endReason = 'DRAW';
        } else {
            // Neither broke it - both AIs held strong (draw)
            endReason = 'DRAW';
        }
        await prisma.game.update({
            where: { id: gameId },
            data: {
                status: 'FINISHED',
                winnerId,
                endReason
            }
        });

    }
    //can fully implement later, users can look at history of  games wins and losses
    async getGame(gameId) {
        return await prisma.game.findUnique({
            where: { id: gameId },
            include: {
                playerOne: true,
                playerTwo: true,
                template: true,
                turns: {
                    orderBy: { createdAt: 'asc' }
                }
            }
        });
    }

    //use of templates - madlib style so we can keep some consistency
    generateCharacter(template) {
        // TODO: Use template to generate random character
        return "A suspicious guard";
    }


    generateSecret(template) {
        // TODO: Use template to generate random secret
        return "The password is blue42";
    }
}
export default new GameService();