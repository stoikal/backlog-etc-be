package com.stoikal.backlog_etc.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "gaming", name = "list_items")
@IdClass(ListItem.ListItemId.class)
public class ListItem {

    @Id
    @Column(name = "list_id")
    private UUID listId;

    @Id
    @Column(name = "game_id")
    private Integer gameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", insertable = false, updatable = false)
    private Game game;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ListItem() {}

    public UUID getListId() { return listId; }
    public Integer getGameId() { return gameId; }
    public Game getGame() { return game; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static class ListItemId implements Serializable {
        private UUID listId;
        private Integer gameId;

        public ListItemId() {}

        public ListItemId(UUID listId, Integer gameId) {
            this.listId = listId;
            this.gameId = gameId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ListItemId that)) return false;
            return Objects.equals(listId, that.listId) && Objects.equals(gameId, that.gameId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(listId, gameId);
        }
    }
}