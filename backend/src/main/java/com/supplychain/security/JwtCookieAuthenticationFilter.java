package com.supplychain.security;

import com.supplychain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request from its session cookie. The user is loaded on every request, so a disabled
 * account or a changed role takes effect immediately rather than when the token expires.
 */
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtCookieAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        readToken(request)
                .flatMap(jwtService::verify)
                .flatMap(userRepository::findById)
                .filter(user -> user.isEnabled())
                .ifPresent(user -> {
                    var principal = new AuthenticatedUser(user.getId(), user.getRole());
                    var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, List.of(authority)));
                });
        chain.doFilter(request, response);
    }

    private Optional<String> readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> AuthCookie.NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
