package com.swiftdrop.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Startup safety guard for the 'dev' profile.
 *
 * Under the 'dev' profile, Hibernate's ddl-auto is set to 'update' and Flyway is disabled.
 * If someone runs core-api locally outside Docker while their DB_URL points to the shared
 * Supabase database (or any non-local database), Hibernate would silently mutate the shared
 * production/staging schema.
 *
 * This configuration runs during initial context setup and aborts startup immediately if a
 * non-local database URL is detected under the 'dev' profile.
 */
@Configuration
@Profile("dev")
public class DevDatabaseGuard {

    private static final Logger log = LoggerFactory.getLogger(DevDatabaseGuard.class);

    public DevDatabaseGuard(Environment environment) {
        String dbUrl = environment.getProperty("spring.datasource.url", "");
        if (!isSafeLocalDatabaseUrl(dbUrl)) {
            String errorMsg = "\n" +
                "========================================================================================\n" +
                "  SECURITY & SCHEMA SAFETY ERROR: UNSAFE DATABASE CONNECTION IN DEV PROFILE!\n" +
                "========================================================================================\n" +
                "  The 'dev' profile has 'spring.jpa.hibernate.ddl-auto: update' with Flyway disabled.\n" +
                "  Attempted to connect to a non-local database:\n" +
                "    " + dbUrl + "\n" +
                "\n" +
                "  Allowing this would cause Hibernate to silently rewrite the shared team schema!\n" +
                "  To connect to the shared database safely, use the 'docker' profile:\n" +
                "    - Run via Docker Compose: docker compose up -d\n" +
                "    - Or set SPRING_PROFILES_ACTIVE=docker\n" +
                "========================================================================================";
            log.error(errorMsg);
            throw new IllegalStateException("Refusing to boot under 'dev' profile with non-local database: " + dbUrl);
        }
    }

    public static boolean isSafeLocalDatabaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("jdbc:h2:") ||
               lower.contains("localhost") ||
               lower.contains("127.0.0.1") ||
               lower.contains("postgres:5432");
    }
}
