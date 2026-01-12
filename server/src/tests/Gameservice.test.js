import { jest } from '@jest/globals';
import { mockDeep, mockReset } from 'jest-mock-extended';


// Mock Prisma Client - note to self, must mock and must use unstable_mockModuel due to type module
//do it before imports as imports will access real prisma first before mock was even instantiated


const prismaMock = mockDeep();

const aiServiceMock = mockDeep();

jest.unstable_mockModule('../services/prismaClient.js', () => ({
    default: prismaMock
}));
jest.unstable_mockModule('../services/aiService.js', () => ({
    default: aiServiceMock
}));

let GameService;

describe('GameService', () => {


    beforeAll(async () => {
        GameService = (await import('../services/gameService.js')).default;
    });

    beforeEach(() => {

        mockReset(prismaMock);
        jest.clearAllMocks();
    });

    describe('createGame', () => {
        it('should create a new game with correct initial state', async () => {
            const mockTemplate = {
                id: 'template-1',
                name: 'Teenager Gossip',
                scenario: 'A teenager with a secret',
                maxTurnsPerPhase: 5,
                maxCharsPerMessage: 250,
            };

            const mockGame = {
                id: 'game-1',
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
                generatedCharacter: 'A suspicious guard',
                generatedSecret: 'The password is blue42',
                status: 'IN_PROGRESS',
                phase: 'DEFENSE',
                maxTurnsPerPhase: 5,
                maxCharsPerMessage: 250,
            };


            prismaMock.game.create.mockResolvedValue(mockGame);
            const templateGenerated = jest.spyOn(GameService, 'generateTemplate').mockResolvedValue([{ id: '123' }]);
            const characterGenerated = jest.spyOn(GameService, 'generateCharacter').mockResolvedValue("guard");
            const secretGenerated = jest.spyOn(GameService, 'generateSecret').mockResolvedValue("password");
            const result = await GameService.createGameFromMatch('player-1', 'player-2');

            expect(templateGenerated).toHaveBeenCalled();
            expect(characterGenerated).toHaveBeenCalledWith([{ id: '123' }]);
            expect(secretGenerated).toHaveBeenCalledWith([{ id: '123' }]);
            expect(result.status).toBe('IN_PROGRESS');
            expect(result.phase).toBe('DEFENSE');
            expect(result.playerOneId).toBe('player-1');
            expect(result.playerTwoId).toBe('player-2');
        });
    });



    describe('validateTurn', () => {
        it('should throw error if game is not in progress', () => {
            const game = {
                status: 'FINISHED',
                maxCharsPerMessage: 250,
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
            };

            expect(() => {
                GameService.validateTurn(game, 'player-1', 'Test message');
            }).toThrow('Game not in progress');
        });

        it('should throw error if message exceeds character limit', () => {
            const game = {
                status: 'IN_PROGRESS',
                maxCharsPerMessage: 250,
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
            };

            const longMessage = 'a'.repeat(251);

            expect(() => {
                GameService.validateTurn(game, 'player-1', longMessage);
            }).toThrow('Message exceeds 250 characters');
        });

        it('should throw error if player is not part of the game', () => {
            const game = {
                status: 'IN_PROGRESS',
                maxCharsPerMessage: 250,
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
            };

            expect(() => {
                GameService.validateTurn(game, 'player-3', 'Test');
            }).toThrow('Player not in this game');
        });

        it('should not throw error for valid turn', () => {
            const game = {
                status: 'IN_PROGRESS',
                maxCharsPerMessage: 250,
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
            };

            expect(() => {
                GameService.validateTurn(game, 'player-1', 'Valid message');
            }).not.toThrow();
        });
    });

    describe('getTurnCount', () => {
        it('should return correct turn count for player in specific phase', async () => {
            prismaMock.gameTurn.count.mockResolvedValue(3);

            const count = await GameService.getTurnCount('game-1', 'player-1', 'DEFENSE');

            expect(prismaMock.gameTurn.count).toHaveBeenCalledWith({
                where: {
                    gameId: 'game-1',
                    playerId: 'player-1',
                    phase: 'DEFENSE',
                },
            });
            expect(count).toBe(3);
        });

        it('should return 0 when player has no turns in phase', async () => {
            prismaMock.gameTurn.count.mockResolvedValue(0);

            const count = await GameService.getTurnCount('game-1', 'player-1', 'DEFENSE');

            expect(count).toBe(0);
        });
    });

    describe('checkPhaseTransition', () => {
        it('should not transition if both players have not completed defense turns', async () => {
            const mockGame = {
                id: 'game-1',
                phase: 'DEFENSE',
                maxTurnsPerPhase: 5,
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
            };

            prismaMock.game.findUnique.mockResolvedValue(mockGame);
            prismaMock.gameTurn.count
                .mockResolvedValueOnce(4) // Player 1: 4 turns
                .mockResolvedValueOnce(3); // Player 2: 3 turns

            const transitionSpy = jest.spyOn(GameService, 'transitionToAttack');

            await GameService.checkPhaseTransition('game-1');

            expect(transitionSpy).not.toHaveBeenCalled();
        });

        it('should transition to ATTACK when both players complete defense turns', async () => {
            const mockGame = {
                id: 'game-1',
                phase: 'DEFENSE',
                maxTurnsPerPhase: 5,
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
            };

            prismaMock.game.findUnique.mockResolvedValue(mockGame);
            prismaMock.gameTurn.count
                .mockResolvedValueOnce(5) // Player 1: 5 turns
                .mockResolvedValueOnce(5); // Player 2: 5 turns

            // Mock the transition
            const mockGameWithTemplate = { ...mockGame, template: {} };
            prismaMock.game.findUnique.mockResolvedValue(mockGameWithTemplate);
            prismaMock.gameTurn.findMany.mockResolvedValue([]);
            aiServiceMock.summarizeDefense.mockResolvedValue('Summary');
            prismaMock.game.update.mockResolvedValue({});

            await GameService.checkPhaseTransition('game-1');

            expect(prismaMock.game.update).toHaveBeenCalledWith({
                where: { id: 'game-1' },
                data: {
                    phase: 'ATTACK',
                    playerOneDefenseSummary: 'Summary',
                    playerTwoDefenseSummary: 'Summary',
                },
            });
        });

        it('should not transition if already in ATTACK phase', async () => {
            const mockGame = {
                id: 'game-1',
                phase: 'ATTACK',
                maxTurnsPerPhase: 5,
            };

            prismaMock.game.findUnique.mockResolvedValue(mockGame);

            await GameService.checkPhaseTransition('game-1');

            // Should return early, not count turns
            expect(prismaMock.gameTurn.count).not.toHaveBeenCalled();
        });
    });

    describe('checkGameEnd', () => {
        it('should not check game end if not in ATTACK phase', async () => {
            const mockGame = {
                id: 'game-1',
                phase: 'DEFENSE',
            };

            prismaMock.game.findUnique.mockResolvedValue(mockGame);

            await GameService.checkGameEnd('game-1');

            expect(prismaMock.gameTurn.count).not.toHaveBeenCalled();
        });

        it('should not end game if both players have not completed attack turns', async () => {
            const mockGame = {
                id: 'game-1',
                phase: 'ATTACK',
                maxTurnsPerPhase: 5,
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
            };

            prismaMock.game.findUnique.mockResolvedValue(mockGame);
            prismaMock.gameTurn.count
                .mockResolvedValueOnce(5) // Player 1: 5 turns
                .mockResolvedValueOnce(3); // Player 2: 3 turns

            const determineWinnerSpy = jest.spyOn(GameService, 'determineWinner');

            await GameService.checkGameEnd('game-1');

            expect(determineWinnerSpy).not.toHaveBeenCalled();
        });

        it('should determine winner when both players complete attack turns', async () => {
            const mockGame = {
                id: 'game-1',
                phase: 'ATTACK',
                maxTurnsPerPhase: 5,
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
            };

            prismaMock.game.findUnique
                .mockResolvedValueOnce(mockGame)
                .mockResolvedValueOnce(mockGame);

            prismaMock.gameTurn.count
                .mockResolvedValueOnce(5) // Player 1: 5 turns
                .mockResolvedValueOnce(5); // Player 2: 5 turns

            prismaMock.gameTurn.findMany.mockResolvedValue([]);
            aiServiceMock.checkSecretRevealed
                .mockResolvedValueOnce(true)  // P1 succeeded
                .mockResolvedValueOnce(false); // P2 failed

            prismaMock.game.update.mockResolvedValue({});

            await GameService.checkGameEnd('game-1');

            expect(prismaMock.game.update).toHaveBeenCalled();
        });
    });

    describe('determineWinner', () => {
        const setupMockGame = () => ({
            id: 'game-1',
            playerOneId: 'player-1',
            playerTwoId: 'player-2',
            generatedSecret: 'secret123',
        });

        it('should declare Player 1 winner when only P1 reveals secret', async () => {
            const mockGame = setupMockGame();

            prismaMock.game.findUnique.mockResolvedValue(mockGame);
            prismaMock.gameTurn.findMany
                .mockResolvedValueOnce([{ id: 'turn-1' }]) // P1 turns
                .mockResolvedValueOnce([{ id: 'turn-2' }]); // P2 turns

            aiServiceMock.checkSecretRevealed
                .mockResolvedValueOnce(true)  // P1 succeeded
                .mockResolvedValueOnce(false); // P2 failed

            prismaMock.game.update.mockResolvedValue({});

            await GameService.determineWinner('game-1');

            expect(prismaMock.game.update).toHaveBeenCalledWith({
                where: { id: 'game-1' },
                data: {
                    status: 'FINISHED',
                    winnerId: 'player-1',
                    endReason: 'SECRET_REVEALED',
                },
            });
        });

        it('should declare Player 2 winner when only P2 reveals secret', async () => {
            const mockGame = setupMockGame();

            prismaMock.game.findUnique.mockResolvedValue(mockGame);
            prismaMock.gameTurn.findMany
                .mockResolvedValueOnce([{ id: 'turn-1' }])
                .mockResolvedValueOnce([{ id: 'turn-2' }]);

            aiServiceMock.checkSecretRevealed
                .mockResolvedValueOnce(false) // P1 failed
                .mockResolvedValueOnce(true);  // P2 succeeded

            prismaMock.game.update.mockResolvedValue({});

            await GameService.determineWinner('game-1');

            expect(prismaMock.game.update).toHaveBeenCalledWith({
                where: { id: 'game-1' },
                data: {
                    status: 'FINISHED',
                    winnerId: 'player-2',
                    endReason: 'SECRET_REVEALED',
                },
            });
        });

        it('should declare draw when both players reveal secret', async () => {
            const mockGame = setupMockGame();

            prismaMock.game.findUnique.mockResolvedValue(mockGame);
            prismaMock.gameTurn.findMany
                .mockResolvedValueOnce([{ id: 'turn-1' }])
                .mockResolvedValueOnce([{ id: 'turn-2' }]);

            aiServiceMock.checkSecretRevealed
                .mockResolvedValueOnce(true) // P1 succeeded
                .mockResolvedValueOnce(true); // P2 succeeded

            prismaMock.game.update.mockResolvedValue({});

            await GameService.determineWinner('game-1');

            expect(prismaMock.game.update).toHaveBeenCalledWith({
                where: { id: 'game-1' },
                data: {
                    status: 'FINISHED',
                    winnerId: undefined,
                    endReason: 'DRAW',
                },
            });
        });

        it('should declare draw when neither player reveals secret', async () => {
            const mockGame = setupMockGame();

            prismaMock.game.findUnique.mockResolvedValue(mockGame);
            prismaMock.gameTurn.findMany
                .mockResolvedValueOnce([{ id: 'turn-1' }])
                .mockResolvedValueOnce([{ id: 'turn-2' }]);

            aiServiceMock.checkSecretRevealed
                .mockResolvedValueOnce(false) // P1 failed
                .mockResolvedValueOnce(false); // P2 failed

            prismaMock.game.update.mockResolvedValue({});

            await GameService.determineWinner('game-1');

            expect(prismaMock.game.update).toHaveBeenCalledWith({
                where: { id: 'game-1' },
                data: {
                    status: 'FINISHED',
                    winnerId: undefined,
                    endReason: 'DRAW',
                },
            });
        });
    });

    describe('submitTurn', () => {
        it('should throw error when turn limit is reached', async () => {
            const mockGame = {
                id: 'game-1',
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
                phase: 'DEFENSE',
                maxTurnsPerPhase: 5,
                maxCharsPerMessage: 250,
                status: 'IN_PROGRESS',
                template: {},
            };

            prismaMock.game.findUnique.mockResolvedValue(mockGame);
            prismaMock.gameTurn.count.mockResolvedValue(5); // Already at max

            await expect(
                GameService.submitTurn('game-1', 'player-1', 'Test message')
            ).rejects.toThrow('Turn limit reached');
        });

        it('should create turn and get AI response when valid', async () => {
            const mockGame = {
                id: 'game-1',
                playerOneId: 'player-1',
                playerTwoId: 'player-2',
                phase: 'DEFENSE',
                maxTurnsPerPhase: 5,
                maxCharsPerMessage: 250,
                status: 'IN_PROGRESS',
                template: {},
            };

            const mockTurn = {
                id: 'turn-1',
                gameId: 'game-1',
                playerId: 'player-1',
                phase: 'DEFENSE',
                turnNumber: 3,
                playerMessage: 'Test message',
                aiResponse: 'AI says no!',
            };

            prismaMock.game.findUnique.mockResolvedValue(mockGame);
            prismaMock.gameTurn.count.mockResolvedValue(2); // Has 2 turns
            aiServiceMock.getResponse.mockResolvedValue('AI says no!');
            prismaMock.gameTurn.create.mockResolvedValue(mockTurn);

            // Mock phase transition check
            prismaMock.gameTurn.count
                .mockResolvedValueOnce(2) // Current count for validation
                .mockResolvedValueOnce(3) // P1 turns for phase check
                .mockResolvedValueOnce(2); // P2 turns for phase check

            const result = await GameService.submitTurn('game-1', 'player-1', 'Test message');

            expect(aiServiceMock.getResponse).toHaveBeenCalledWith(
                mockGame,
                'player-1',
                'Test message',
                'DEFENSE'
            );
            expect(prismaMock.gameTurn.create).toHaveBeenCalledWith({
                data: {
                    gameId: 'game-1',
                    playerId: 'player-1',
                    phase: 'DEFENSE',
                    turnNumber: 3,
                    playerMessage: 'Test message',
                    aiResponse: 'AI says no!',
                },
            });
            expect(result).toEqual(mockTurn);
        });
    });
});