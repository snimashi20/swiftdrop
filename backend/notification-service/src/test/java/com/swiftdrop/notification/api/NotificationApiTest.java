package com.swiftdrop.notification.api;

import com.swiftdrop.notification.domain.NotificationStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The notifications API over the real security chain and a real database.
 *
 * <p>No RabbitMQ container is needed, so the listener is switched off for this context.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class NotificationApiTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;

    @Value("${app.jwt.secret}")
    String jwtSecret;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("without a token the API answers 401")
    void withoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/notifications/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("a made-up token is not accepted")
    void invalidTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/notifications/me").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the feed shows one item per status change and never another customer's rows")
    void feedGroupsChannelsAndIsolatesCustomers() throws Exception {
        TestUser mine = user("CUSTOMER");
        TestUser other = user("CUSTOMER");
        Instant now = Instant.now();

        String older = tracking();
        String newer = tracking();
        logEvent(mine.id(), older, NotificationStatus.SENT, now.minusSeconds(60));
        logEvent(mine.id(), newer, NotificationStatus.SENT, now.minusSeconds(10));
        logEvent(other.id(), tracking(), NotificationStatus.SENT, now);

        // Two status changes, four rows: the feed must report two items, newest first.
        mvc.perform(get("/api/v1/notifications/me").header("Authorization", mine.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].trackingNumber").value(newer))
                .andExpect(jsonPath("$.content[1].trackingNumber").value(older))
                .andExpect(jsonPath("$.content[0].message")
                        .value("Parcel " + newer + " is now in transit."))
                .andExpect(jsonPath("$.content[0].read").value(false));
    }

    @Test
    @DisplayName("the unread count counts status changes, and read-all clears it")
    void unreadCountAndReadAll() throws Exception {
        TestUser mine = user("CUSTOMER");
        Instant now = Instant.now();
        logEvent(mine.id(), tracking(), NotificationStatus.SENT, now.minusSeconds(30));
        logEvent(mine.id(), tracking(), NotificationStatus.SENT, now);

        mvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", mine.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        // Four rows carry those two status changes.
        mvc.perform(post("/api/v1/notifications/me/read-all").header("Authorization", mine.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(4));

        mvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", mine.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        mvc.perform(get("/api/v1/notifications/me?unreadOnly=true").header("Authorization", mine.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("marking one notification read covers both channels of that status change")
    void markOneReadCoversBothChannels() throws Exception {
        TestUser mine = user("CUSTOMER");
        UUID eventId = logEvent(mine.id(), tracking(), NotificationStatus.SENT, Instant.now());
        Long feedItemId = representativeRowId(eventId);

        mvc.perform(post("/api/v1/notifications/" + feedItemId + "/read")
                        .header("Authorization", mine.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        mvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", mine.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @DisplayName("another customer's notification cannot be marked read and looks absent")
    void cannotMarkSomeoneElsesNotification() throws Exception {
        TestUser mine = user("CUSTOMER");
        TestUser other = user("CUSTOMER");
        UUID theirEvent = logEvent(other.id(), tracking(), NotificationStatus.SENT, Instant.now());
        Long theirRowId = representativeRowId(theirEvent);

        mvc.perform(post("/api/v1/notifications/" + theirRowId + "/read")
                        .header("Authorization", mine.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("a customer may not read the admin log")
    void customerCannotUseAdminLog() throws Exception {
        TestUser mine = user("CUSTOMER");

        mvc.perform(get("/api/v1/notifications/admin").header("Authorization", mine.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an admin sees every channel row, masked, and can filter by status")
    void adminSeesChannelRowsAndFilters() throws Exception {
        TestUser admin = user("ADMIN");
        TestUser customer = user("CUSTOMER");
        String trackingNumber = tracking();
        Instant now = Instant.now();

        logEvent(customer.id(), trackingNumber, NotificationStatus.SENT, now.minusSeconds(20));
        insertRow(UUID.randomUUID(), customer.id(), trackingNumber, "EMAIL", "s***@example.com",
                NotificationStatus.FAILED, now);

        // Lower case on purpose: the API normalises the tracking number.
        mvc.perform(get("/api/v1/notifications/admin")
                        .param("trackingNumber", trackingNumber.toLowerCase(Locale.ROOT))
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get("/api/v1/notifications/admin")
                        .param("trackingNumber", trackingNumber)
                        .param("status", "FAILED")
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].recipient").value("s***@example.com"))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));

        mvc.perform(get("/api/v1/notifications/admin")
                        .param("trackingNumber", trackingNumber)
                        .param("type", "SMS")
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].recipient").value("*******567"));
    }

    @Test
    @DisplayName("a dispatcher may read the admin log")
    void dispatcherCanUseAdminLog() throws Exception {
        TestUser dispatcher = user("DISPATCHER");

        mvc.perform(get("/api/v1/notifications/admin").header("Authorization", dispatcher.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a page size above the limit is refused")
    void tooLargePageSizeIsRefused() throws Exception {
        TestUser mine = user("CUSTOMER");

        mvc.perform(get("/api/v1/notifications/me?size=500").header("Authorization", mine.bearer()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("a disabled account cannot use the API")
    void disabledAccountIsRejected() throws Exception {
        TestUser mine = user("CUSTOMER");
        jdbc.update("UPDATE users SET enabled = FALSE WHERE id = ?", mine.id());

        mvc.perform(get("/api/v1/notifications/me").header("Authorization", mine.bearer()))
                .andExpect(status().isUnauthorized());
    }

    // ----- helpers -----

    private record TestUser(Long id, String email, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private TestUser user(String role) {
        String email = "u" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Long id = jdbc.queryForObject(
                "INSERT INTO users (email, full_name, phone_number, role) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, email, "Test User", "0771234567", role);
        return new TestUser(id, email, token(email, role, id));
    }

    /** Signed exactly as core-api signs it, with the shared app.jwt.secret. */
    private String token(String email, String role, Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000))
                .signWith(key)
                .compact();
    }

    /** One status change as the consumer writes it: an e-mail row and an SMS row. */
    private UUID logEvent(Long customerId, String trackingNumber, NotificationStatus status, Instant createdAt) {
        UUID eventId = UUID.randomUUID();
        insertRow(eventId, customerId, trackingNumber, "EMAIL", "s***@example.com", status, createdAt);
        insertRow(eventId, customerId, trackingNumber, "SMS", "*******567", status, createdAt);
        return eventId;
    }

    private void insertRow(UUID eventId, Long customerId, String trackingNumber, String type,
                           String recipient, NotificationStatus status, Instant createdAt) {
        jdbc.update("""
                INSERT INTO notification_log
                    (event_id, parcel_id, tracking_number, old_status, new_status,
                     customer_id, type, recipient, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                eventId, 42L, trackingNumber, "PICKED_UP", "IN_TRANSIT",
                customerId, type, recipient, status.name(), Timestamp.from(createdAt));
    }

    /** The row the feed returns for an event: the lowest id of that event. */
    private Long representativeRowId(UUID eventId) {
        return jdbc.queryForObject(
                "SELECT MIN(id) FROM notification_log WHERE event_id = ?", Long.class, eventId);
    }

    /** A unique 12-character tracking number, the width of the column. */
    private static String tracking() {
        return ("SD" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .toUpperCase(Locale.ROOT);
    }
}
