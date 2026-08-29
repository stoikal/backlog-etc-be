package com.stoikal.backlog_etc.repository;

import com.stoikal.backlog_etc.entity.GameList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface GameListRepository extends JpaRepository<GameList, UUID> {
    Page<GameList> findByUserId(UUID userId, Pageable pageable);

    @Query(value = """
        SELECT gl.* FROM gaming.lists gl
        LEFT JOIN gaming.list_items li ON li.list_id = gl.id
        LEFT JOIN gaming.games_statuses gs ON gs.game_id = li.game_id AND gs.user_id = :userId
        WHERE gl.user_id = :userId
        GROUP BY gl.id, gl.user_id, gl.title, gl.created_at
        ORDER BY COUNT(CASE WHEN gs.status = 'finished' THEN 1 END) ASC,
                 COUNT(CASE WHEN gs.status IS NULL OR gs.status != 'finished' THEN 1 END) DESC
        """,
        countQuery = "SELECT COUNT(*) FROM gaming.lists WHERE user_id = :userId",
        nativeQuery = true)
    Page<GameList> findByUserIdOrderedByProgress(UUID userId, Pageable pageable);
}