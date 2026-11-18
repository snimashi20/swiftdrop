package com.swiftdrop.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevDatabaseGuardTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:h2:mem:swiftdropdb;DB_CLOSE_DELAY=-1",
        "jdbc:postgresql://localhost:5432/swiftdrop",
        "jdbc:postgresql://127.0.0.1:5432/swiftdrop",
        "jdbc:postgresql://postgres:5432/swiftdrop"
    })
    @DisplayName("Allows safe local database URLs under dev profile")
    void allowsSafeLocalDatabaseUrls(String safeUrl) {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.url", safeUrl);

        assertThatCode(() -> new DevDatabaseGuard(env)).doesNotThrowAnyException();
        assertThat(DevDatabaseGuard.isSafeLocalDatabaseUrl(safeUrl)).isTrue();
    }

    @Test
    @DisplayName("Allows null or blank database URL")
    void allowsNullOrBlankDatabaseUrl() {
        MockEnvironment env = new MockEnvironment();

        assertThatCode(() -> new DevDatabaseGuard(env)).doesNotThrowAnyException();
        assertThat(DevDatabaseGuard.isSafeLocalDatabaseUrl(null)).isTrue();
        assertThat(DevDatabaseGuard.isSafeLocalDatabaseUrl("   ")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require",
        "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require",
        "jdbc:postgresql://ep-xyz-123.neon.tech/swiftdrop?sslmode=require",
        "jdbc:postgresql://192.168.1.100:5432/swiftdrop",
        "jdbc:postgresql://production-db.internal:5432/swiftdrop"
    })
    @DisplayName("Fails fast when dev profile encounters non-local or cloud database URL")
    void rejectsRemoteOrCloudDatabaseUrls(String unsafeUrl) {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.url", unsafeUrl);

        assertThatThrownBy(() -> new DevDatabaseGuard(env))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Refusing to boot under 'dev' profile with non-local database");

        assertThat(DevDatabaseGuard.isSafeLocalDatabaseUrl(unsafeUrl)).isFalse();
    }
}
