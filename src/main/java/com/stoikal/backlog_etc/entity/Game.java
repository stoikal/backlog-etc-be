package com.stoikal.backlog_etc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "gaming", name = "games")
public class Game {

    @Id
    private Integer id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String name;

    @Column(name = "first_release_date")
    private Long firstReleaseDate;

    public Game() {}

    public Integer getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getName() { return name; }
    public Long getFirstReleaseDate() { return firstReleaseDate; }
}