package com.backend.promptvprompt.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.backend.promptvprompt.models.Game;
import com.backend.promptvprompt.models.GameEndReason;
import com.backend.promptvprompt.models.GamePhase;
import com.backend.promptvprompt.models.GameStatus;
import com.backend.promptvprompt.models.GameTurn;
import com.backend.promptvprompt.models.ScenarioTemplate;
import com.backend.promptvprompt.models.User;
import com.backend.promptvprompt.repos.GameRepo;
import com.backend.promptvprompt.repos.GameTurnRepo;
import com.backend.promptvprompt.repos.ScenarioTemplateRepo;
import com.backend.promptvprompt.repos.UserRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepo gameRepo;
    private final GameTurnRepo gameTurnRepo;
    private final ScenarioTemplateRepo scenarioTemplateRepo;
    private final UserRepo userRepo;
    private final AiService aiService;
    private final Random random = new Random();

    @Transactional
    public Game createGameFromMatch(String playerOneId, String playerTwoId) {
        try {
            // Fetch actual User objects
            User playerOne = userRepo.findById(playerOneId)
                    .orElseThrow(() -> new RuntimeException("Player one not found"));
            User playerTwo = userRepo.findById(playerTwoId)
                    .orElseThrow(() -> new RuntimeException("Player two not found"));

            ScenarioTemplate template = generateTemplate();
            String character = generateCharacter(template);
            String secret = generateSecret(template);

            Game game = Game.builder()
                    .playerOne(playerOne)
                    .playerTwo(playerTwo)
                    .template(template)
                    .generatedCharacter(character)
                    .generatedSecret(secret)
                    .status(GameStatus.DEFENSE_PHASE)
                    .phase(GamePhase.DEFENSE)
                    .build();

            return gameRepo.save(game);
        } catch (Exception e) {
            throw new RuntimeException("Error creating game from match: " + e.getMessage(), e);
        }
    }

    public GamePhase getPhase(String gameId) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        return game.getPhase();
    }

    @Transactional
    public GameTurn submitTurn(String gameId, String playerId, String message) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        validateTurn(game, playerId, message);

        int turnCount = getTurnCount(gameId, playerId, game.getPhase());

        if (turnCount >= game.getMaxTurnsPerPhase()) {
            throw new IllegalStateException("Turn limit reached");
        }

        String aiResponse = aiService.getResponse(game, playerId, message, game.getPhase());
        User player = userRepo.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        GameTurn turn = GameTurn.builder()
                .game(game)
                .player(player)
                .phase(game.getPhase())
                .turnNumber(turnCount + 1)
                .playerMessage(message)
                .aiResponse(aiResponse)
                .build();

        GameTurn savedTurn = gameTurnRepo.save(turn);

        checkPhaseTransition(gameId);
        checkGameEnd(gameId);

        return savedTurn;
    }

    void validateTurn(Game game, String playerId, String message) {
        if (game.getStatus() != GameStatus.ATTACK_PHASE &&
                game.getStatus() != GameStatus.DEFENSE_PHASE) {
            throw new IllegalStateException("Game not in progress");
        }

        String trimmedMessage = message.trim();
        if (trimmedMessage.length() > game.getMaxCharsPerMessage()) {
            throw new IllegalArgumentException(
                    String.format("Message exceeds %d characters", game.getMaxCharsPerMessage()));
        }

        if (!playerId.equals(game.getPlayerOne().getId()) &&
                !playerId.equals(game.getPlayerTwo().getId())) {
            throw new IllegalArgumentException("Player not in this game");
        }
    }

    public int getTurnCount(String gameId, String playerId, GamePhase phase) {
        return gameTurnRepo.countByGameIdAndPlayerIdAndPhase(gameId, playerId, phase);
    }

    public List<GameTurn> getTurns(String gameId, String playerId, GamePhase phase) {
        return gameTurnRepo.findByGameIdAndPlayerIdAndPhaseOrderByTurnNumberAsc(
                gameId, playerId, phase);
    }

    @Transactional
    public void checkPhaseTransition(String gameId) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (game.getPhase() != GamePhase.DEFENSE) {
            return;
        }

        int p1Turns = getTurnCount(gameId, game.getPlayerOne().getId(), GamePhase.DEFENSE);
        int p2Turns = getTurnCount(gameId, game.getPlayerTwo().getId(), GamePhase.DEFENSE);

        if (p1Turns >= game.getMaxTurnsPerPhase() &&
                p2Turns >= game.getMaxTurnsPerPhase()) {
            transitionToAttack(gameId);
        }
    }

    @Transactional
    public void transitionToAttack(String gameId) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        String p1Summary = generateDefenseSummary(gameId, game.getPlayerOne().getId());
        String p2Summary = generateDefenseSummary(gameId, game.getPlayerTwo().getId());

        game.setStatus(GameStatus.ATTACK_PHASE);
        game.setPhase(GamePhase.ATTACK);
        game.setPlayerOneDefenseSummary(p1Summary);
        game.setPlayerTwoDefenseSummary(p2Summary);
        game.setIsTransitioning(true);
        game.setTransitionEndsAt(LocalDateTime.now().plusSeconds(5));

        gameRepo.save(game);
    }

    @Transactional
    public void endTransition(String gameId) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        game.setIsTransitioning(false);
        game.setTransitionEndsAt(null);

        gameRepo.save(game);
    }

    public String generateDefenseSummary(String gameId, String playerId) {
        List<GameTurn> turns = gameTurnRepo
                .findByGameIdAndPlayerIdAndPhaseOrderByTurnNumberAsc(
                        gameId, playerId, GamePhase.DEFENSE);

        return aiService.summarizeDefense(turns);
    }

    @Transactional
    public boolean checkGameEnd(String gameId) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (game.getPhase() != GamePhase.ATTACK) {
            return false;
        }

        int p1AttackTurns = getTurnCount(gameId, game.getPlayerOne().getId(), GamePhase.ATTACK);
        int p2AttackTurns = getTurnCount(gameId, game.getPlayerTwo().getId(), GamePhase.ATTACK);

        if (p1AttackTurns >= game.getMaxTurnsPerPhase() &&
                p2AttackTurns >= game.getMaxTurnsPerPhase()) {
            determineWinner(gameId);
            return true;
        }
        return false;
    }

    @Transactional
    public void determineWinner(String gameId) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        User winner = null;
        GameEndReason endReason = GameEndReason.DRAW;

        List<GameTurn> p1AttackTurns = gameTurnRepo
                .findByGameIdAndPlayerIdAndPhaseOrderByTurnNumberAsc(
                        gameId, game.getPlayerOne().getId(), GamePhase.ATTACK);

        List<GameTurn> p2AttackTurns = gameTurnRepo
                .findByGameIdAndPlayerIdAndPhaseOrderByTurnNumberAsc(
                        gameId, game.getPlayerTwo().getId(), GamePhase.ATTACK);

        boolean p1Succeeded = aiService.checkSecretRevealed(
                game, p1AttackTurns);

        boolean p2Succeeded = aiService.checkSecretRevealed(
                game, p2AttackTurns);

        if (p1Succeeded && !p2Succeeded) {
            winner = game.getPlayerOne();
            endReason = GameEndReason.FULL_CONVICTION;
        } else if (p2Succeeded && !p1Succeeded) {
            winner = game.getPlayerTwo();
            endReason = GameEndReason.FULL_CONVICTION;
        } else if (p1Succeeded && p2Succeeded) {
            winner = null;
            endReason = GameEndReason.DRAW;
        } else {
            winner = null;
            endReason = GameEndReason.DRAW;
        }

        game.setStatus(GameStatus.COMPLETED);
        game.setWinner(winner);
        game.setEndReason(endReason);

        gameRepo.save(game);
    }

    public Game getGame(String gameId) {
        return gameRepo.findByIdWithTemplateAndTurns(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
    }

    String generateCharacter(ScenarioTemplate template) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) template.getVariables().get("role");
        int randomIndex = random.nextInt(roles.size());
        return roles.get(randomIndex);
    }

    String generateSecret(ScenarioTemplate template) {
        @SuppressWarnings("unchecked")
        List<String> passwords = (List<String>) template.getVariables().get("password");
        int randomIndex = random.nextInt(passwords.size());
        return passwords.get(randomIndex);
    }

    ScenarioTemplate generateTemplate() {
        long count = scenarioTemplateRepo.count();
        if (count == 0) {
            throw new RuntimeException("No templates available");
        }

        int randomIndex = random.nextInt((int) count);
        return scenarioTemplateRepo.findRandomTemplate(randomIndex)
                .orElseThrow(() -> new RuntimeException("No template found"));
    }
}