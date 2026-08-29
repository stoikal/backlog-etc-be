package com.stoikal.backlog_etc.service;

import com.stoikal.backlog_etc.dto.AuthResponse;
import com.stoikal.backlog_etc.dto.LoginRequest;
import com.stoikal.backlog_etc.dto.RegisterRequest;
import com.stoikal.backlog_etc.entity.RefreshToken;
import com.stoikal.backlog_etc.entity.User;
import com.stoikal.backlog_etc.repository.RefreshTokenRepository;
import com.stoikal.backlog_etc.repository.UserRepository;
import com.stoikal.backlog_etc.security.AuthUser;
import com.stoikal.backlog_etc.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User user = new User(request.getEmail(), request.getUsername(),
                passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return generateAuthResult(user);
    }

    public AuthResult login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return generateAuthResult(user);
    }

    @Transactional
    public AuthResult refresh(String submittedRefreshToken) {
        if (!jwtUtil.isValid(submittedRefreshToken) || !jwtUtil.isRefreshToken(submittedRefreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String tokenHash = hashToken(submittedRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Refresh token not found or already used"));

        refreshTokenRepository.delete(storedToken);

        if (storedToken.isExpired()) {
            throw new RuntimeException("Refresh token expired");
        }

        String email = jwtUtil.extractEmail(submittedRefreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return generateAuthResult(user);
    }

    private AuthResult generateAuthResult(User user) {
        AuthUser userDetails = new AuthUser(user.getId(), user.getEmail(), user.getPassword());

        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String rawRefreshToken = jwtUtil.generateRefreshToken(userDetails);

        saveRefreshToken(user, rawRefreshToken);

        return new AuthResult(
                new AuthResponse(accessToken, user.getEmail(), user.getUsername()),
                rawRefreshToken);
    }

    private void saveRefreshToken(User user, String submittedRefreshToken) {
        String hash = hashToken(submittedRefreshToken);
        long expiresMs = System.currentTimeMillis() + jwtUtil.getRefreshExpiration();
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(expiresMs), ZoneId.systemDefault());
        refreshTokenRepository.save(new RefreshToken(user, hash, expiresAt));
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record AuthResult(AuthResponse authResponse, String refreshToken) {}
}