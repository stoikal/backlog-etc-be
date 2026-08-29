package com.stoikal.backlog_etc.dto;

import java.util.List;
import java.util.UUID;

public class ListDto {
    private final UUID id;
    private final String title;
    private final List<ListItemDto> items;

    public ListDto(UUID id, String title, List<ListItemDto> items) {
        this.id = id;
        this.title = title;
        this.items = items;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public List<ListItemDto> getItems() { return items; }
}