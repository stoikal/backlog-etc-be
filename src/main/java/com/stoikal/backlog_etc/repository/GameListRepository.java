package com.stoikal.backlog_etc.repository;

import com.stoikal.backlog_etc.entity.GameList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameListRepository extends JpaRepository<GameList, UUID> {
    Page<GameList> findByUserId(UUID userId, Pageable pageable);
}