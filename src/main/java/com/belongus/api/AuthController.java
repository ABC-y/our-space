package com.belongus.api;

import com.belongus.domain.AppUser;
import com.belongus.repository.AppUserRepository;
import com.belongus.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public AuthController(AppUserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("这个账号已被使用，请换一个");
        }

        AppUser user = userRepository.save(new AppUser(
                username,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        ));
        authService.startSession(user, response);
        return toResponse(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AppUser user = userRepository.findByUsername(request.username().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UnauthorizedException("账号或密码不正确"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("账号或密码不正确");
        }
        authService.startSession(user, response);
        return toResponse(user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.endSession(request, response);
    }

    @GetMapping("/me")
    public AuthResponse me(HttpServletRequest request) {
        return toResponse(authService.requireUser(request));
    }

    private AuthResponse toResponse(AppUser user) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }

    public record AuthResponse(Long id, String username, String displayName) {
    }
}
