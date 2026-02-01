package com.backend.promptvprompt.controllers;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.backend.promptvprompt.DTO.Game.GameData;
import com.backend.promptvprompt.DTO.Game.GameResponse;
import com.backend.promptvprompt.DTO.Game.TransitionData;
import com.backend.promptvprompt.DTO.Game.TurnData;
import com.backend.promptvprompt.DTO.Matchmaking.GameFoundResponse;
import com.backend.promptvprompt.DTO.Matchmaking.JoinGameRoomRequest;
import com.backend.promptvprompt.DTO.Matchmaking.JoinQueueRequest;
import com.backend.promptvprompt.DTO.Matchmaking.Match;
import com.backend.promptvprompt.DTO.Matchmaking.QueueJoinedResponse;
import com.backend.promptvprompt.exceptions.InvalidCredentialsException;
import com.backend.promptvprompt.models.Game;
import com.backend.promptvprompt.models.GameTurn;
import com.backend.promptvprompt.services.GameService;
import com.backend.promptvprompt.services.JwtService;
import com.backend.promptvprompt.services.MatchmakingService;

@Controller
public class GameSocketController {
	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private MatchmakingService matchmakingService;

	@Autowired
	private GameService gameService;

	@Autowired
	private JwtService jwtService;

	private static final Logger logger = LoggerFactory.getLogger(GameSocketController.class);

	@MessageMapping("/joinQueue")
	public void joinQueue(
			Principal principal) {

		String user = principal.getName();

		System.out.println("DEBUG: THIS IS THE USERID: " + user);

		Match match = matchmakingService.addPlayer(user, user);

		if (match != null) {
			// Create game from match
			Game game = gameService.createGameFromMatch(
					match.getPlayerOne().getPlayerId(),
					match.getPlayerTwo().getPlayerId());

			// Send game found event to both players
			GameFoundResponse response = new GameFoundResponse(game);

			messagingTemplate.convertAndSendToUser(
					match.getPlayerOne().getSocketId(),
					"/queue/gameFound",
					response);

			messagingTemplate.convertAndSendToUser(
					match.getPlayerTwo().getSocketId(),
					"/queue/gameFound",
					response);
			System.out.println("Game found: " + game.getId());

		} else {
			// Send queue joined event
			QueueJoinedResponse response = new QueueJoinedResponse(
					matchmakingService.getQueueSize());
			messagingTemplate.convertAndSendToUser(
					user,
					"/queue/queueJoined",
					response);

		}
	}

	@MessageMapping("/game/joinGameRoom")
	public void joinGameRoom(@Payload JoinGameRoomRequest request,
			Principal principal) {

		String gameId = request.getGameId();
		String userId = principal.getName();

		Game game = gameService.getGame(gameId);
		if (game == null) {
			messagingTemplate.convertAndSendToUser(userId, "/queue/game-response",
					new GameResponse("error", "GAME_NOT_FOUND", "Game not found", null));
			return;
		}
		if (!game.getPlayerOne().getId().equals(userId)
				&& !game.getPlayerTwo().getId().equals(userId)) {
			messagingTemplate.convertAndSendToUser(userId, "/queue/game-response",
					new GameResponse("error", "NOT_A_PLAYER", "You are spectating", null));
			return;
		}
		TransitionData transitionData = null;

		if (game.getIsTransitioning() && game.getTransitionEndsAt() != null) {
			Duration duration = Duration.between(LocalDateTime.now(), game.getTransitionEndsAt());

			int remainingSeconds = (int) duration.getSeconds();

			if (remainingSeconds > 0) {
				transitionData = new TransitionData(
						true,
						remainingSeconds,
						game.getPhase().name());
			} else {
				gameService.endTransition(gameId);
			}
		}

		int myMessageCount = gameService.getTurnCount(gameId, userId, game.getPhase());

		String opponentId = game.getPlayerOne().getId().equals(userId)
				? game.getPlayerTwo().getId()
				: game.getPlayerOne().getId();

		int opponentMessageCount = gameService.getTurnCount(gameId, opponentId, game.getPhase());

		List<GameTurn> gameTurns = gameService.getTurns(gameId, userId, game.getPhase());

		boolean isGameComplete = gameService.checkGameEnd(gameId);
		String phase = game.getPhase().name();
		GameData gameData = new GameData(
				myMessageCount,
				opponentMessageCount,
				gameTurns,
				phase,
				isGameComplete,
				transitionData);
		messagingTemplate.convertAndSendToUser(userId, "/queue/game-response",
				new GameResponse("success", "", "", gameData));

	}

	@MessageMapping("/game/{gameId}/submit-turn")
	public void submitTurn(@Payload TurnData turnData, Principal principal, @DestinationVariable String gameId) {
		String userId = turnData.getUserId();
		Game game = gameService.getGame(gameId);

	}
}
