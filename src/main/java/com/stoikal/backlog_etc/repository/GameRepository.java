package com.stoikal.backlog_etc.repository;

import com.stoikal.backlog_etc.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Integer> {
}