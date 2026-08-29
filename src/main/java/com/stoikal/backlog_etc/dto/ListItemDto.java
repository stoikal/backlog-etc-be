package com.stoikal.backlog_etc.dto;

public class ListItemDto {
    private final String name;
    private final String status;

    public ListItemDto(String gameName, String status) {
        this.name = gameName;
        this.status = status;
    }

    public String getName() { return name; }
    public String getStatus() { return status; }
}