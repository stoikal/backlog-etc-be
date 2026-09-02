package com.stoikal.backlog_etc.controller;

import com.stoikal.backlog_etc.dto.ListPageResponse;
import com.stoikal.backlog_etc.security.AuthUser;
import com.stoikal.backlog_etc.service.PlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gaming/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping
    public ResponseEntity<ListPageResponse> getPlaylists(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        ListPageResponse response = playlistService.getUserLists(
                authUser.getUserId(), page, limit, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }
}