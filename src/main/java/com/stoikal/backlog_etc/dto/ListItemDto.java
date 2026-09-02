package com.stoikal.backlog_etc.dto;

public class ListItemDto {
    private final String name;
    private final String status;
    private final Integer releaseYear;

    public ListItemDto(String gameName, String status, Integer releaseYear) {
        this.name = gameName;
        this.status = status;
        this.releaseYear = releaseYear;
    }

    public String getName() { return name; }
    public String getStatus() { return status; }
    public Integer getReleaseYear() { return releaseYear; }
}