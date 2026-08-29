package com.stoikal.backlog_etc.dto;

import java.util.UUID;

public class ListItemDto {
    private final String gameName;
    private final String status;

    public ListItemDto(String gameName, String status) {
        this.gameName = gameName;
        this.status = status;
    }

    public String getGameName() { return gameName; }
    public String getStatus() { return status; }
}