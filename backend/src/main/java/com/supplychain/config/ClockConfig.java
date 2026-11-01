package com.supplychain.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Services read time from this bean, so tests and the seeder can control it. */
@Configuration
public class ClockConfig {

    @Bean
    @Profile("!dev")
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @Profile("dev")
    AdjustableClock adjustableClock() {
        return new AdjustableClock(Clock.systemUTC());
    }
}
