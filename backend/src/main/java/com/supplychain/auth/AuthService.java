package com.supplychain.auth;

import com.supplychain.auth.AuthDtos.LoginRequest;
import com.supplychain.auth.AuthDtos.RegisterRequest;
import com.supplychain.exception.ApiException;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String BAD_CREDENTIALS = "Incorrect email or password.";
    private static final String EMAIL_TAKEN = "An account with this email already exists.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    /** Checked when an email is unknown, so response time doesn't reveal which emails have accounts. */
    private final String decoyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.decoyHash = passwordEncoder.encode("decoy-password-for-unknown-emails");
    }

    /** Public registration can only ever create customers. */
    @Transactional
    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, EMAIL_TAKEN);
        }
        User user = new User(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                blankToNull(request.phone()),
                UserRole.CUSTOMER,
                clock.instant());
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Two simultaneous registrations both passed the check above; the unique key caught one.
            throw new ApiException(HttpStatus.CONFLICT, EMAIL_TAKEN);
        }
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(decoyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        return user.filter(found -> passwordMatches && found.isEnabled())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS));
    }

    @Transactional(readOnly = true)
    public User getUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue."));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
