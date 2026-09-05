# Authentication Implementation Guide

## Architecture

Access token (Bearer) + refresh token (HttpOnly cookie) pattern using JWT.

```
Client                          Server
  │                               │
  │  POST /api/v1/auth/login      │
  │  { email, password }          │
  │──────────────────────────────>│
  │                               │  Verify credentials (BCrypt)
  │                               │  Generate access token (JWT, 15min)
  │                               │  Generate refresh token (JWT, 7d)
  │                               │  Store SHA-256 hash of refresh token
  │  Set-Cookie: refreshToken=... │
  │  { accessToken: "..." }       │
  │<──────────────────────────────│
  │                               │
  │  GET /api/v1/hello         │
  │  Authorization: Bearer ...   │
  │──────────────────────────────>│
  │                               │  Validate access token
  │                               │  Extract email, set SecurityContext
  │  "Hello, world!"              │
  │<──────────────────────────────│
  │                               │
  │  POST /api/v1/auth/refresh    │
  │  Cookie: refreshToken=...     │
  │──────────────────────────────>│
  │                               │  Hash cookie value, match DB
  │                               │  Validate expiry, rotate: delete old + create new
  │  Set-Cookie: refreshToken=... │
  │  { accessToken: "..." }       │
  │<──────────────────────────────│
  │                               │
  │  POST /api/v1/auth/logout     │
  │  Cookie: refreshToken=...     │
  │──────────────────────────────>│
  │                               │  Hash cookie value, delete DB row
  │                               │  Set-Cookie: refreshToken=; Max-Age=0
  │<──────────────────────────────│
```

## Token Details

### Access Token
- **Payload**: `{ sub: email, iat, exp }`
- **Expiration**: 15 minutes (`jwt.access-expiration=900000`)
- **Transmission**: `Authorization: Bearer <token>` header
- **Validation**: JWT signature → extract email → load UserDetails → set SecurityContext

### Refresh Token
- **Payload**: `{ sub: email, jti: <UUID>, iat, exp }`
- **Expiration**: 7 days (`jwt.refresh-expiration=604800000`)
- **Transmission**: HttpOnly cookie, `Path=/api/v1/auth`, `SameSite=Strict`
- **Storage**: SHA-256 hash of token value in `identity.refresh_tokens` table
- **Rotation**: On each `/refresh`, old token is deleted and a new pair is issued
- **Scope**: Multi-device — each device has its own cookie/DB row; `/logout` deletes only that row

## Cookie Configuration

```
Set-Cookie: refreshToken=<value>; HttpOnly; Secure=${COOKIE_SECURE}; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800
```

`COOKIE_SECURE=true` in production, `false` in dev over plain HTTP.

## Directory Structure

```
src/main/java/com/stoikal/backlog_etc/
├── BacklogApplication.java
├── config/
│   ├── JwtConfig.java           @ConfigurationProperties(prefix = "jwt")
│   └── SecurityConfig.java      SecurityFilterChain, CORS, PasswordEncoder
├── controller/
│   ├── AuthController.java      POST /api/v1/auth/{register,login,refresh,logout}
│   └── HelloController.java     GET /api/v1/hello
├── dto/
│   ├── AuthResponse.java        { accessToken: String }
│   ├── LoginRequest.java        @Email @NotBlank email, @NotBlank password
│   └── RegisterRequest.java     @Email @NotBlank email, @NotBlank username, @Size(min=8) password
├── entity/
│   ├── RefreshToken.java        maps identity.refresh_tokens
│   └── User.java                maps identity.users
├── repository/
│   ├── RefreshTokenRepository.java
│   └── UserRepository.java
├── security/
│   ├── JwtAuthenticationFilter.java   OncePerRequestFilter
│   ├── JwtTokenProvider.java          JJWT 0.12.6 — generate, validate, parse
│   └── UserDetailsServiceImpl.java    UserDetailsService → findByEmail
└── service/
    ├── AuthService.java         register, login, refresh, logout
    └── TokenPair.java           internal record (accessToken, refreshToken)
```

## Endpoints

| Endpoint | Auth | Request | Response |
|----------|------|---------|----------|
| `POST /api/v1/auth/register` | Public | `{ email, username, password }` | `{ accessToken }` + Set-Cookie |
| `POST /api/v1/auth/login` | Public | `{ email, password }` | `{ accessToken }` + Set-Cookie |
| `POST /api/v1/auth/refresh` | Public | Cookie: `refreshToken` | `{ accessToken }` + Set-Cookie |
| `POST /api/v1/auth/logout` | Public | Cookie: `refreshToken` | `204 No Content` + Clear-Cookie |
| `GET /api/v1/hello` | Protected | `Authorization: Bearer <token>` | `"Hello, world!"` |
| Everything else | Protected | — | `401 Unauthorized` |

## Implementation Steps

### Step 1: Add Spring Security dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Step 2: Create JwtConfig

```java
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;
    private long accessExpiration;     // milliseconds
    private long refreshExpiration;    // milliseconds
    // getters + setters
}
```

### Step 3: Create entities

**User.java** — maps `identity.users`:
- `UUID id`, `String email`, `String username`, `String password`, `LocalDateTime createdAt`, `LocalDateTime updatedAt`

**RefreshToken.java** — maps `identity.refresh_tokens`:
- `UUID id`, `User user` (@ManyToOne), `String tokenHash`, `LocalDateTime expiresAt`, `LocalDateTime createdAt`

### Step 4: Create repositories

**UserRepository** extends `JpaRepository<User, UUID>`:
- `Optional<User> findByEmail(String email)`
- `boolean existsByEmail(String email)`

**RefreshTokenRepository** extends `JpaRepository<RefreshToken, UUID>`:
- `Optional<RefreshToken> findByTokenHash(String tokenHash)`
- `void deleteByUserId(UUID userId)`

### Step 5: Create DTOs

```java
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String username,
    @NotBlank @Size(min = 8) String password
) {}

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

public record AuthResponse(String accessToken) {}
```

### Step 6: Create JwtTokenProvider

Uses JJWT 0.12.6 API:

```java
public String generateAccessToken(String email) {
    return Jwts.builder()
        .subject(email)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessExpiration()))
        .signWith(getSigningKey())
        .compact();
}

public String generateRefreshToken(String email) {
    return Jwts.builder()
        .subject(email)
        .id(UUID.randomUUID().toString())   // jti for rotation tracking
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshExpiration()))
        .signWith(getSigningKey())
        .compact();
}

public String getEmailFromToken(String token) { ... }
public boolean validateToken(String token) { ... }
public String getTokenId(String token) { ... }   // jti claim
```

### Step 7: Create UserDetailsServiceImpl

```java
public class UserDetailsServiceImpl implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(), List.of()
        );
    }
}
```

### Step 8: Create JwtAuthenticationFilter

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getEmailFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
```

### Step 9: Create SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(POST, "/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() { ... }
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) { ... }
}
```

Add a package-private record in the `service` package for the token pair returned internally:

```java
package com.stoikal.backlog_etc.service;

public record TokenPair(String accessToken, String refreshToken) {}
```

### Step 10: Create AuthService

```java
public TokenPair register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) throw ...;
    User user = new User(request.email(), request.username(), passwordEncoder.encode(request.password()));
    userRepository.save(user);
    return generateTokenPair(user);
}

public TokenPair login(LoginRequest request) {
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    User user = userRepository.findByEmail(request.email()).orElseThrow();
    return generateTokenPair(user);
}

public TokenPair refresh(String refreshTokenValue) {
    // Validate JWT, hash value, match in DB, check expiry
    // Delete old row, generate new pair, save new hash
}

public void logout(String refreshTokenValue) {
    // Hash value, delete DB row
}

private TokenPair generateTokenPair(User user) {
    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
    String tokenHash = sha256(refreshToken);
    refreshTokenRepository.save(new RefreshToken(user, tokenHash, ...));
    return new TokenPair(accessToken, refreshToken);  // refresh token returned only to service layer
}
```

Note: `AuthService.refresh()` and `AuthService.logout()` receive the raw refresh token value and hash it internally.

### Step 11: Create AuthController

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        var tokenPair = authService.register(request);
        addRefreshTokenCookie(response, tokenPair.refreshToken());
        return ResponseEntity.ok(new AuthResponse(tokenPair.accessToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var tokenPair = authService.login(request);
        addRefreshTokenCookie(response, tokenPair.refreshToken());
        return ResponseEntity.ok(new AuthResponse(tokenPair.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue("refreshToken") String refreshToken, HttpServletResponse response) {
        var tokenPair = authService.refresh(refreshToken);
        addRefreshTokenCookie(response, tokenPair.refreshToken());
        return ResponseEntity.ok(new AuthResponse(tokenPair.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("refreshToken") String refreshToken, HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
            .httpOnly(true)
            .secure(jwtConfig.isSecure())
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(Duration.ofMillis(jwtConfig.getRefreshExpiration()))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(jwtConfig.isSecure())
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
```

### Step 12: Update HelloController

```java
@GetMapping("/api/v1/hello")
public String hello() { return "Hello, world!"; }
```

### Step 13: Add cookie secure property to application.properties

```properties
jwt.secret=${JWT_SECRET}
jwt.access-expiration=900000
jwt.refresh-expiration=604800000
jwt.cookie-secure=${COOKIE_SECURE:false}
```

## Entity Definitions

### User Entity

`@Table(schema = "identity", name = "users")`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID (PK) | `@GeneratedValue(Strategy.UUID)` |
| email | String (unique) | `@Column(nullable = false, unique = true)` |
| username | String | `@Column(nullable = false)` |
| password | String (hashed) | `@Column(nullable = false)` |
| createdAt | LocalDateTime | `@CreatedDate` |
| updatedAt | LocalDateTime | `@LastModifiedDate` |

### RefreshToken Entity

`@Table(schema = "identity", name = "refresh_tokens")`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID (PK) | `@GeneratedValue(Strategy.UUID)` |
| user | User (ManyToOne) | `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "user_id")` |
| tokenHash | String | `@Column(nullable = false)` |
| expiresAt | LocalDateTime | `@Column(nullable = false)` |
| createdAt | LocalDateTime | |