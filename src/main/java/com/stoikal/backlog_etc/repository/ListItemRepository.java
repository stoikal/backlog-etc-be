package com.stoikal.backlog_etc.repository;

import com.stoikal.backlog_etc.entity.ListItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListItemRepository extends JpaRepository<ListItem, ListItem.ListItemId> {
    List<ListItem> findByListId(UUID listId);
}