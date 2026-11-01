package com.supplychain.auth;

import com.supplychain.auth.AuthDtos.LoginRequest;
import com.supplychain.auth.AuthDtos.RegisterRequest;
import com.supplychain.auth.AuthDtos.UserResponse;
import com.supplychain.security.AuthCookie;
import com.supplychain.security.AuthenticatedUser;
import com.supplychain.security.JwtService;
import com.supplychain.user.User;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthCookie authCookie;

    public AuthController(AuthService authService, JwtService jwtService, AuthCookie authCookie) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.authCookie = authCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withSession(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        return withSession(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookie.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(authService.getUser(principal.id()));
    }

    private ResponseEntity<UserResponse> withSession(HttpStatus status, User user) {
        String token = jwtService.issue(user.getId());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, authCookie.create(token, jwtService.getExpiry()).toString())
                .body(UserResponse.from(user));
    }
}
