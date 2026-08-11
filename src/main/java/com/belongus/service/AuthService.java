package com.belongus.service;

import com.belongus.api.UnauthorizedException;
import com.belongus.domain.AppSession;
import com.belongus.domain.AppUser;
import com.belongus.repository.AppSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {
    public static final String USER_ATTRIBUTE = "belongUsCurrentUser";
    private static final String COOKIE_NAME = "belong_us_session";
    private static final Duration SESSION_DURATION = Duration.ofDays(30);

    private final AppSessionRepository sessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthService(AppSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public Optional<AppUser> findAuthenticatedUser(HttpServletRequest request) {
        String token = cookieValue(request);
        if (token == null) {
            return Optional.empty();
        }

        String tokenHash = hashToken(token);
        Optional<AppSession> session = sessionRepository.findByTokenHash(tokenHash);
        if (session.isEmpty()) {
            return Optional.empty();
        }
        if (session.get().isExpired()) {
            sessionRepository.deleteByTokenHash(tokenHash);
            return Optional.empty();
        }
        return Optional.of(session.get().getUser());
    }

    public AppUser requireUser(HttpServletRequest request) {
        Object user = request.getAttribute(USER_ATTRIBUTE);
        if (user instanceof AppUser appUser) {
            return appUser;
        }
        throw new UnauthorizedException("请先登录后再继续");
    }

    public void startSession(AppUser user, HttpServletResponse response) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessionRepository.save(new AppSession(user, hashToken(token), LocalDateTime.now().plus(SESSION_DURATION)));
        addCookie(response, token, SESSION_DURATION);
    }

    public void endSession(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieValue(request);
        if (token != null) {
            sessionRepository.deleteByTokenHash(hashToken(token));
        }
        addCookie(response, "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String cookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
