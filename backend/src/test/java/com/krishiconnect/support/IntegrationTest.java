package com.krishiconnect.support;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base class for tests that need the real database. One MySQL 8 container is started for the
 * whole test run (Testcontainers needs Docker) and every test starts from empty tables.
 */
@SpringBootTest(properties = {
        "app.seed.demo-data=false",
        // the expiry job is called by hand in tests, so it doesn't fire in the middle of one
        "app.booking.expiry-check-ms=3600000",
        "app.booking.pending-minutes=30",
        // tests never call the real OSRM server
        "app.routing.enabled=false",
        // enough connections for every thread in the concurrency tests to hit MySQL at once
        "spring.datasource.hikari.maximum-pool-size=30",
        "app.admin.email=admin@test.local",
        "app.admin.password=admin-password"
})
@AutoConfigureMockMvc
@Import({TestData.class, IntegrationTest.ClockConfig.class})
public abstract class IntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("krishiconnect")
            .withUsername("krishi")
            .withPassword("krishi");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @TestConfiguration
    static class ClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.now(), ZoneId.of("Asia/Kolkata"));
        }

        @Bean
        @Primary
        FakeRoutingClient fakeRoutingClient() {
            return new FakeRoutingClient();
        }
    }

    @Autowired
    protected TestData data;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected FakeRoutingClient routing;

    @BeforeEach
    void cleanDatabase() {
        clock.reset();
        routing.reset();
        data.clean();
    }
}
