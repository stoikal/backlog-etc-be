package com.stoikal.backlog_etc.repository;

import com.stoikal.backlog_etc.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameStatusRepository extends JpaRepository<GameStatus, GameStatus.GameStatusId> {
    List<GameStatus> findByUserId(UUID userId);
}