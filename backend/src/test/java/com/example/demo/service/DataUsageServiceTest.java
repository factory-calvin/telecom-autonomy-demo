package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.model.Plan;
import com.example.demo.model.UsageRecord;
import com.example.demo.repository.UsageRecordRepository;
import com.example.demo.service.DataUsageService.DataUsage;
import com.example.demo.service.DataUsageService.DataUsageState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataUsageServiceTest {

    private final UsageRecordRepository repository = mock(UsageRecordRepository.class);
    private final DataUsageService service = new DataUsageService(repository);

    @Test
    void calculatesAllThresholdBoundariesAndUnlimitedUsage() {
        List<Customer> customers = List.of(customer(1L, 10), customer(2L, 10), customer(3L, 10), customer(4L, 10),
                customer(5L, 10), customer(6L, null));
        when(repository.sumQuantityByCustomerForCycle(any(), eq(UsageRecord.Type.DATA), any(), any())).thenReturn(
                List.of(new Object[] {1L, new BigDecimal("7990")}, new Object[] {2L, new BigDecimal("8000")},
                        new Object[] {3L, new BigDecimal("9990")}, new Object[] {4L, new BigDecimal("10000")},
                        new Object[] {5L, new BigDecimal("10200")}, new Object[] {6L, new BigDecimal("12500")}));

        Map<Long, DataUsage> result = service.calculate(customers, Instant.parse("2026-09-12T06:00:00Z"));

        assertUsage(result.get(1L), "7.99", "79.9", DataUsageState.WITHIN_LIMIT);
        assertUsage(result.get(2L), "8", "80", DataUsageState.AT_RISK);
        assertUsage(result.get(3L), "9.99", "99.9", DataUsageState.AT_RISK);
        assertUsage(result.get(4L), "10", "100", DataUsageState.OVER_LIMIT);
        assertUsage(result.get(5L), "10.2", "102", DataUsageState.OVER_LIMIT);
        assertEquals(new BigDecimal("12.5"), result.get(6L).usedDataGb());
        assertNull(result.get(6L).dataLimitGb());
        assertNull(result.get(6L).percentage());
        assertEquals(DataUsageState.UNLIMITED, result.get(6L).state());
    }

    @Test
    void queriesOnlyDataWithinTheHalfOpenUtcCalendarMonth() {
        Customer customer = customer(1L, 10);
        when(repository.sumQuantityByCustomerForCycle(any(), eq(UsageRecord.Type.DATA), any(), any()))
                .thenReturn(Collections.emptyList());
        ArgumentCaptor<Instant> start = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> end = ArgumentCaptor.forClass(Instant.class);

        service.calculate(List.of(customer), Instant.parse("2026-09-30T23:59:59Z"));

        verify(repository).sumQuantityByCustomerForCycle(eq(List.of(1L)), eq(UsageRecord.Type.DATA), start.capture(),
                end.capture());
        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), start.getValue());
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), end.getValue());
    }

    private static void assertUsage(DataUsage usage, String usedGb, String percentage, DataUsageState state) {
        assertEquals(0, new BigDecimal(usedGb).compareTo(usage.usedDataGb()));
        assertEquals(10, usage.dataLimitGb());
        assertEquals(0, new BigDecimal(percentage).compareTo(usage.percentage()));
        assertEquals(state, usage.state());
    }

    private static Customer customer(Long id, Integer dataLimitGb) {
        Plan plan = new Plan(dataLimitGb == null ? "Unlimited" : "Finite", BigDecimal.TEN, dataLimitGb, null, null);
        plan.setId(id);
        Customer customer = new Customer("First", "Last", "customer" + id + "@example.com", "555-000" + id, plan);
        customer.setId(id);
        return customer;
    }
}
