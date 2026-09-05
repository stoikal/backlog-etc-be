package com.stoikal.backlog_etc.service;

import com.stoikal.backlog_etc.config.JwtConfig;
import com.stoikal.backlog_etc.dto.RegisterRequest;
import com.stoikal.backlog_etc.entity.RefreshToken;
import com.stoikal.backlog_etc.entity.User;
import com.stoikal.backlog_etc.repository.RefreshTokenRepository;
import com.stoikal.backlog_etc.repository.UserRepository;
import com.stoikal.backlog_etc.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtConfig = jwtConfig;
    }

    public TokenPair register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(
                request.email(),
                request.username(),
                passwordEncoder.encode(request.password())
        );

        userRepository.save(user);
        return generateTokenPair(user);
    }



    private TokenPair generateTokenPair(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        refreshTokenRepository.save(new RefreshToken(
                user,
                sha256(refreshToken),
                LocalDateTime.now().plusNanos(jwtConfig.getRefreshExpiration() * 1_000_000L)
        ));

        return new TokenPair(accessToken, refreshToken);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat
                    .of()
                    .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
