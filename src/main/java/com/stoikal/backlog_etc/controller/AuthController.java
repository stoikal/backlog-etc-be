package com.stoikal.backlog_etc.controller;

import com.stoikal.backlog_etc.config.JwtConfig;
import com.stoikal.backlog_etc.dto.AuthResponse;
import com.stoikal.backlog_etc.dto.RegisterRequest;
import com.stoikal.backlog_etc.service.AuthService;
import com.stoikal.backlog_etc.service.TokenPair;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    public AuthController(AuthService authService, JwtConfig jwtConfig) {
        this.authService = authService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        TokenPair tokenPair = authService.register(request);
        addRefreshTokenCookie(response, tokenPair.refreshToken());
        return ResponseEntity.ok(new AuthResponse(tokenPair.accessToken()));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(jwtConfig.isCookieSecure())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofMillis(jwtConfig.getRefreshExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    }
}
