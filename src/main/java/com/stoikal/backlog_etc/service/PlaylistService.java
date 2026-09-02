package com.stoikal.backlog_etc.service;

import com.stoikal.backlog_etc.dto.ListDto;
import com.stoikal.backlog_etc.dto.ListItemDto;
import com.stoikal.backlog_etc.dto.ListPageResponse;
import com.stoikal.backlog_etc.entity.GameList;
import com.stoikal.backlog_etc.entity.GameStatus;
import com.stoikal.backlog_etc.entity.ListItem;
import com.stoikal.backlog_etc.repository.GameListRepository;
import com.stoikal.backlog_etc.repository.GameStatusRepository;
import com.stoikal.backlog_etc.repository.ListItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PlaylistService {

    private final GameListRepository gameListRepository;
    private final ListItemRepository listItemRepository;
    private final GameStatusRepository gameStatusRepository;

    public PlaylistService(GameListRepository gameListRepository,
                            ListItemRepository listItemRepository,
                            GameStatusRepository gameStatusRepository) {
        this.gameListRepository = gameListRepository;
        this.listItemRepository = listItemRepository;
        this.gameStatusRepository = gameStatusRepository;
    }

    private static final Map<String, String> SORT_MAP = Map.of(
        "created_at", "createdAt"
    );

    private static final Set<String> PROGRESS_SORT_KEYS = Set.of("finished", "progress");

    public ListPageResponse getUserLists(UUID userId, int page, int limit, String sortBy, String sortDir) {
        Page<GameList> listPage;

        if (sortBy != null && PROGRESS_SORT_KEYS.contains(sortBy)) {
            Pageable pageable = PageRequest.of(page - 1, limit);
            listPage = gameListRepository.findByUserIdOrderedByProgress(userId, pageable);
        } else {
            String field = SORT_MAP.getOrDefault(sortBy, sortBy != null ? sortBy : "createdAt");
            Sort sort = Sort.by(
                    "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                    field
            ).and(Sort.by(Sort.Direction.ASC, "id"));
            Pageable pageable = PageRequest.of(page - 1, limit, sort);
            listPage = gameListRepository.findByUserId(userId, pageable);
        }

        Set<Integer> gameIds = new HashSet<>();
        Map<UUID, List<ListItem>> itemsByListId = new HashMap<>();
        for (GameList gl : listPage.getContent()) {
            List<ListItem> items = listItemRepository.findByListId(gl.getId());
            itemsByListId.put(gl.getId(), items);
            items.forEach(item -> gameIds.add(item.getGameId()));
        }

        Map<Integer, String> statusByGameId = gameStatusRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(GameStatus::getGameId, GameStatus::getStatus));

        List<ListDto> data = listPage.getContent().stream()
                .map(gl -> {
                    List<ListItemDto> itemDtos = itemsByListId.getOrDefault(gl.getId(), List.of())
                            .stream()
                            .map(item -> {
                            Long releaseTs = item.getGame().getFirstReleaseDate();
                            Integer year = releaseTs != null
                                    ? Instant.ofEpochSecond(releaseTs).atZone(ZoneId.of("UTC")).getYear()
                                    : null;
                            return new ListItemDto(
                                    item.getGame().getName(),
                                    statusByGameId.getOrDefault(item.getGameId(), "todo"),
                                    year);
                        })
                            .sorted(Comparator.comparing((ListItemDto dto) -> "finished".equals(dto.getStatus()) ? 1 : 0)
                                    .thenComparing(ListItemDto::getName))
                            .toList();
                    return new ListDto(gl.getId(), gl.getTitle(), itemDtos);
                })
                .toList();

        ListPageResponse.Pagination pagination = new ListPageResponse.Pagination(
                page, limit, listPage.getTotalElements(), listPage.getTotalPages());

        return new ListPageResponse(data, pagination);
    }
}