import { jest } from '@jest/globals';
import { mockDeep, mockReset } from 'jest-mock-extended';

const matchmakingServiceMock = mockDeep();

const gameServiceMock = mockDeep();


jest.unstable_mockModule('../services/gameService.js', () => ({
    default: gameServiceMock
}));
jest.unstable_mockModule('../services/matchmakingService.js', () => ({
    default: matchmakingServiceMock
}));

const { default: setupGameSocket } = await import('../sockets/gamesocket.js');

describe('GameSocket', () => {
    let mockIo;
    let mockSocket;
    beforeEach(() => {

        mockSocket = {
            id: 'socket-123',
            emit: jest.fn(),
            on: jest.fn(),
            join: jest.fn()
        };


        mockIo = {
            on: jest.fn(),
            emit: jest.fn(),
            to: jest.fn().mockReturnThis(),
            sockets: {
                sockets: {
                    get: jest.fn((socketId) => {

                        if (socketId === 1 || socketId === 2) {
                            return mockSocket;
                        }
                        return undefined;
                    })
                }
            }
        };


        mockReset(matchmakingServiceMock);
        mockReset(gameServiceMock);
        jest.clearAllMocks();
    });
    describe('connection handling', () => {
        it('should set up connection listener', () => {
            setupGameSocket(mockIo);

            expect(mockIo.on).toHaveBeenCalledWith('connection', expect.any(Function));
        });

        it('should set up joinQueue listener on socket connection', () => {
            // Capture the connection callback
            let connectionCallback;
            mockIo.on.mockImplementation((event, callback) => {
                if (event === 'connection') {
                    connectionCallback = callback;
                }
            });

            setupGameSocket(mockIo);

            // Simulate a connection
            connectionCallback(mockSocket);

            expect(mockSocket.on).toHaveBeenCalledWith('joinQueue', expect.any(Function));
        });
    });
    describe('joinQueue event', () => {
        let joinQueueCallback;

        beforeEach(() => {
            // Setup to capture callbacks
            mockIo.on.mockImplementation((event, callback) => {
                if (event === 'connection') {
                    // Immediately trigger connection with mockSocket
                    callback(mockSocket);
                }
            });

            mockSocket.on.mockImplementation((event, callback) => {
                if (event === 'joinQueue') {
                    joinQueueCallback = callback;
                }
            });

            setupGameSocket(mockIo);
        });

        it('should add player to queue when no match is found', async () => {
            const userId = 'user-1';

            // Mock: no match found (only 1 player in queue)
            matchmakingServiceMock.addPlayer.mockReturnValue(null);
            matchmakingServiceMock.queue = { length: 1 };

            await joinQueueCallback({ userId });

            expect(matchmakingServiceMock.addPlayer).toHaveBeenCalledWith(userId, 'socket-123');
            expect(mockSocket.emit).toHaveBeenCalledWith('queueJoined', { position: 1 });
            expect(gameServiceMock.createGameFromMatch).not.toHaveBeenCalled();
        });

        it('should create game and notify both players when match is found', async () => {
            const userId = 'user-2';
            const match = { 'playerOne': { 'playerId': 1, 'socketId': 1 }, 'playerTwo': { 'playerId': 2, 'socketId': 2 } };
            const mockGame = { id: 'game-123' };

            // Mock: match found (2 players)
            matchmakingServiceMock.addPlayer.mockReturnValue(match);
            gameServiceMock.createGameFromMatch.mockResolvedValue(mockGame);

            await joinQueueCallback({ userId });

            expect(matchmakingServiceMock.addPlayer).toHaveBeenCalledWith(userId, 'socket-123');
            expect(gameServiceMock.createGameFromMatch).toHaveBeenCalledWith(1, 2);

            // Should emit gameFound to both players
            expect(mockIo.sockets.sockets.get).toHaveBeenCalledWith(1);
            expect(mockIo.sockets.sockets.get).toHaveBeenCalledWith(2);
            expect(mockSocket.join).toHaveBeenCalledWith(`game-${mockGame.id}`);
            expect(mockIo.emit).toHaveBeenCalledWith('gameFound', mockGame);

            // Should NOT emit queueJoined when match is found
            expect(mockSocket.emit).not.toHaveBeenCalledWith('queueJoined', expect.anything());
        });

        it('should handle multiple players joining queue sequentially', async () => {
            // First player joins - no match
            matchmakingServiceMock.addPlayer.mockReturnValueOnce(null);
            matchmakingServiceMock.queue = { length: 1 };

            await joinQueueCallback({ userId: 'user-1' });

            expect(mockSocket.emit).toHaveBeenCalledWith('queueJoined', { position: 1 });

            // Second player joins - match found
            const match = { 'playerOne': { 'playerId': 1 }, 'playerTwo': { 'playerId': 2 } };
            const mockGame = { id: 'game-456' };
            matchmakingServiceMock.addPlayer.mockReturnValueOnce(match);
            gameServiceMock.createGameFromMatch.mockResolvedValue(mockGame);

            await joinQueueCallback({ userId: 'user-2' });

            expect(gameServiceMock.createGameFromMatch).toHaveBeenCalledWith(1, 2);
            expect(mockIo.emit).toHaveBeenCalledWith('gameFound', mockGame);
        });


    });

})



