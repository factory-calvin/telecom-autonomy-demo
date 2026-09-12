package com.example.demo.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsageRecordIndexIntegrationTest {

    private Path databasePath;
    private Connection connection;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        databasePath = Files.createTempFile("usage-record-index-", ".db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                CREATE TABLE usage_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id BIGINT NOT NULL,
                    type VARCHAR(255) NOT NULL,
                    quantity NUMERIC(10,2) NOT NULL,
                    cost NUMERIC(10,2) NOT NULL,
                    recorded_at TIMESTAMP NOT NULL
                )
                """);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
        Files.deleteIfExists(databasePath);
    }

    @Test
    void initializerAddsIndexToExistingDatabaseIdempotently() {
        DatabaseIndexInitializer initializer = new DatabaseIndexInitializer(jdbcTemplate);

        initializer.run(null);
        initializer.run(null);

        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = ?", Integer.class,
                DatabaseIndexInitializer.CURRENT_CYCLE_INDEX);
        assertEquals(1, indexCount);
    }

    @Test
    void currentCycleAggregateUsesIndexAndPreservesHalfOpenResults() {
        DatabaseIndexInitializer initializer = new DatabaseIndexInitializer(jdbcTemplate);
        initializer.run(null);
        insertUsage(1, "DATA", "500", "2026-08-31T23:59:59Z");
        insertUsage(1, "DATA", "8500", "2026-09-01T00:00:00Z");
        insertUsage(1, "SMS", "999", "2026-09-15T00:00:00Z");
        insertUsage(2, "DATA", "10200", "2026-09-30T23:59:59Z");
        insertUsage(2, "DATA", "700", "2026-10-01T00:00:00Z");

        String aggregateSql = """
                SELECT customer_id, SUM(quantity)
                FROM usage_records
                WHERE customer_id IN (1, 2)
                  AND type = 'DATA'
                  AND recorded_at >= '2026-09-01T00:00:00Z'
                  AND recorded_at < '2026-10-01T00:00:00Z'
                GROUP BY customer_id
                """;
        List<UsageTotal> totals = jdbcTemplate.query(aggregateSql,
                (resultSet, rowNumber) -> new UsageTotal(resultSet.getLong(1), resultSet.getBigDecimal(2)));

        assertEquals(List.of(new UsageTotal(1, new BigDecimal("8500")), new UsageTotal(2, new BigDecimal("10200"))),
                totals);

        List<String> plan = jdbcTemplate.query("EXPLAIN QUERY PLAN " + aggregateSql,
                (resultSet, rowNumber) -> resultSet.getString("detail"));
        assertTrue(
                plan.stream().anyMatch(
                        detail -> detail.contains("USING INDEX " + DatabaseIndexInitializer.CURRENT_CYCLE_INDEX)),
                () -> "Expected current-cycle index in query plan, got: " + plan);
    }

    private void insertUsage(long customerId, String type, String quantity, String recordedAt) {
        jdbcTemplate.update("""
                INSERT INTO usage_records (customer_id, type, quantity, cost, recorded_at)
                VALUES (?, ?, ?, 0, ?)
                """, customerId, type, new BigDecimal(quantity), Instant.parse(recordedAt).toString());
    }

    private record UsageTotal(long customerId, BigDecimal quantity) {
    }
}
