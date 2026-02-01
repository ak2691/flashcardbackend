package com.backend.promptvprompt.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Players
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_one_id", nullable = false)
    private User playerOne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_two_id")
    private User playerTwo;

    // Scenario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ScenarioTemplate template;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String generatedCharacter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String generatedSecret;

    // Summaries (generated after defense phase)
    @Column(columnDefinition = "TEXT")
    private String playerOneDefenseSummary;

    @Column(columnDefinition = "TEXT")
    private String playerTwoDefenseSummary;

    // Game state
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.WAITING_FOR_PLAYER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase phase = GamePhase.DEFENSE;

    // HP system
    @Builder.Default
    @Column(nullable = false)
    private Integer playerOneAiHp = 100;

    @Builder.Default
    @Column(nullable = false)
    private Integer playerTwoAiHp = 100;

    // Game config/limits
    @Builder.Default
    @Column(nullable = false)
    private Integer maxCharsPerMessage = 250;

    @Builder.Default
    @Column(nullable = false)
    private Integer maxTurnsPerPhase = 5;

    // Win conditions
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Enumerated(EnumType.STRING)
    private GameEndReason endReason;

    // Timestamps
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Transition timestamps
    @Builder.Default
    @Column(nullable = false)
    private Boolean isTransitioning = false;

    private LocalDateTime transitionEndsAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GameTurn> turns = new ArrayList<>();
}