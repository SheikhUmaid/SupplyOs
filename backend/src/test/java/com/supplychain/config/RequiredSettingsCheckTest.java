package com.supplychain.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RequiredSettingsCheckTest {

    @Test
    void namesEverySettingThatIsMissingOrBlank() {
        var environment = new MockEnvironment().withProperty("JWT_SECRET", "   ");
        assertThat(RequiredSettingsCheck.findMissing(environment)).containsExactly("DB_PASSWORD", "JWT_SECRET");
    }

    @Test
    void passesWhenEverythingIsSet() {
        var environment = new MockEnvironment()
                .withProperty("DB_PASSWORD", "password")
                .withProperty("JWT_SECRET", "secret");
        assertThat(RequiredSettingsCheck.findMissing(environment)).isEmpty();
    }
}
