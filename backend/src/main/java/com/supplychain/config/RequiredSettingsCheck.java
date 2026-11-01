package com.supplychain.config;

import java.util.List;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Stops startup with a clear message when a required secret is missing. Spring otherwise keeps an
 * unresolved ${DB_PASSWORD} as literal text and sends it to MySQL, which reports "Access denied".
 */
public class RequiredSettingsCheck implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final List<String> REQUIRED = List.of("DB_PASSWORD", "JWT_SECRET");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        List<String> missing = findMissing(event.getEnvironment());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required settings: " + String.join(", ", missing)
                    + ". Set them in backend/.env (see .env.example) or as environment variables.");
        }
    }

    static List<String> findMissing(Environment environment) {
        return REQUIRED.stream()
                .filter(name -> !StringUtils.hasText(environment.getProperty(name)))
                .toList();
    }
}
