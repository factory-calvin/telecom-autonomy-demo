package com.example.demo.repository;

import com.example.demo.config.DatabaseIndexInitializer;
import com.example.demo.model.Customer;
import com.example.demo.model.Plan;
import com.example.demo.model.UsageRecord;
import com.example.demo.service.DataUsageService;
import com.example.demo.service.DataUsageService.DataUsage;
import com.example.demo.service.DataUsageService.DataUsageState;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UsageRecordRepositoryIntegrationTest {

    private static final Instant CYCLE_START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant CYCLE_END = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UsageRecordRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void currentCycleAggregateUsesIndexAndPreservesUsageSemantics() {
        Plan finitePlan = persistPlan("Finite", 10);
        Plan unlimitedPlan = persistPlan("Unlimited", null);
        Customer atRisk = persistCustomer("at-risk@example.com", finitePlan);
        Customer overLimit = persistCustomer("over-limit@example.com", finitePlan);
        Customer unlimited = persistCustomer("unlimited@example.com", unlimitedPlan);

        repository.saveAll(List.of(usage(atRisk, UsageRecord.Type.DATA, "500", "2026-08-31T23:59:59Z"),
                usage(atRisk, UsageRecord.Type.DATA, "8500", "2026-09-01T00:00:00Z"),
                usage(atRisk, UsageRecord.Type.SMS, "999", "2026-09-15T00:00:00Z"),
                usage(overLimit, UsageRecord.Type.DATA, "10200", "2026-09-30T23:59:59Z"),
                usage(overLimit, UsageRecord.Type.DATA, "700", "2026-10-01T00:00:00Z"),
                usage(unlimited, UsageRecord.Type.DATA, "12500", "2026-09-15T00:00:00Z")));
        repository.flush();

        List<Customer> customers = List.of(atRisk, overLimit, unlimited);
        Map<Long, DataUsage> result = new DataUsageService(repository).calculate(customers,
                Instant.parse("2026-09-12T00:00:00Z"));

        assertUsage(result.get(atRisk.getId()), "8.5", "85", DataUsageState.AT_RISK);
        assertUsage(result.get(overLimit.getId()), "10.2", "102", DataUsageState.OVER_LIMIT);
        DataUsage unlimitedUsage = result.get(unlimited.getId());
        assertEquals(0, new BigDecimal("12.5").compareTo(unlimitedUsage.usedDataGb()));
        assertNull(unlimitedUsage.percentage());
        assertEquals(DataUsageState.UNLIMITED, unlimitedUsage.state());

        List<String> plan = jdbcTemplate.query("""
                EXPLAIN QUERY PLAN
                SELECT customer_id, SUM(quantity)
                FROM usage_records
                WHERE customer_id IN (?, ?, ?)
                  AND type = ?
                  AND recorded_at >= ?
                  AND recorded_at < ?
                GROUP BY customer_id
                """, (resultSet, rowNumber) -> resultSet.getString("detail"), atRisk.getId(), overLimit.getId(),
                unlimited.getId(), UsageRecord.Type.DATA.name(), Timestamp.from(CYCLE_START),
                Timestamp.from(CYCLE_END));
        assertTrue(
                plan.stream().anyMatch(
                        detail -> detail.contains("USING INDEX " + DatabaseIndexInitializer.CURRENT_CYCLE_INDEX)),
                () -> "Expected current-cycle index in query plan, got: " + plan);
    }

    private Plan persistPlan(String name, Integer dataLimitGb) {
        Plan plan = new Plan(name, BigDecimal.TEN, dataLimitGb, null, null);
        entityManager.persist(plan);
        return plan;
    }

    private Customer persistCustomer(String email, Plan plan) {
        Customer customer = new Customer("First", "Last", email, "555-" + email, plan);
        entityManager.persist(customer);
        return customer;
    }

    private UsageRecord usage(Customer customer, UsageRecord.Type type, String quantity, String recordedAt) {
        return new UsageRecord(customer, type, new BigDecimal(quantity), BigDecimal.ZERO, Instant.parse(recordedAt));
    }

    private void assertUsage(DataUsage usage, String usedGb, String percentage, DataUsageState state) {
        assertEquals(0, new BigDecimal(usedGb).compareTo(usage.usedDataGb()));
        assertEquals(0, new BigDecimal(percentage).compareTo(usage.percentage()));
        assertEquals(state, usage.state());
    }
}
