package com.stoikal.backlog_etc.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "gaming", name = "games_statuses")
@IdClass(GameStatus.GameStatusId.class)
public class GameStatus {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "game_id")
    private Integer gameId;

    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public GameStatus() {}

    public UUID getUserId() { return userId; }
    public Integer getGameId() { return gameId; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static class GameStatusId implements Serializable {
        private UUID userId;
        private Integer gameId;

        public GameStatusId() {}

        public GameStatusId(UUID userId, Integer gameId) {
            this.userId = userId;
            this.gameId = gameId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GameStatusId that)) return false;
            return Objects.equals(userId, that.userId) && Objects.equals(gameId, that.gameId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, gameId);
        }
    }
}