package com.supplychain.seed;

import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.user.UserRole;
import java.time.Clock;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Creates the first administrator in development, since public registration can't create one. */
@Component
@Profile("dev")
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String email;
    private final String password;
    private final boolean enabled;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${SEED_ADMIN_EMAIL:}") String email,
            @Value("${SEED_ADMIN_PASSWORD:}") String password,
            @Value("${app.seed.enabled:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("SEED_ADMIN_EMAIL or SEED_ADMIN_PASSWORD is not set, so no administrator was created.");
            return;
        }
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(new User(
                    "Administrator", email, passwordEncoder.encode(password), null, UserRole.ADMIN, clock.instant()));
            log.info("Created the development administrator account.");
        }
    }
}
