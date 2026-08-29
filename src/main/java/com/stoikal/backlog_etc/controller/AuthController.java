package com.stoikal.backlog_etc.controller;

import com.stoikal.backlog_etc.dto.AuthResponse;
import com.stoikal.backlog_etc.dto.LoginRequest;
import com.stoikal.backlog_etc.dto.RegisterRequest;
import com.stoikal.backlog_etc.service.AuthService;
import com.stoikal.backlog_etc.service.AuthService.AuthResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final long refreshExpiration;

    public AuthController(AuthService authService,
                          @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.authService = authService;
        this.refreshExpiration = refreshExpiration;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthResult result = authService.register(request);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthResult result = authService.login(request);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                                HttpServletResponse response) {
        String refreshToken = extractRefreshCookie(request);
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        AuthResult result = authService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(result.authResponse());
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge((int) (refreshExpiration / 1000));
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}