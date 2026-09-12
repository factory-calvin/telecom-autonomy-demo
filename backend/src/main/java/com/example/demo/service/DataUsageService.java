package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.model.UsageRecord;
import com.example.demo.repository.UsageRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataUsageService {

    private static final Logger log = LoggerFactory.getLogger(DataUsageService.class);
    private static final BigDecimal MEGABYTES_PER_GIGABYTE = BigDecimal.valueOf(1000);
    private static final BigDecimal AT_RISK_PERCENTAGE = BigDecimal.valueOf(80);
    private static final BigDecimal OVER_LIMIT_PERCENTAGE = BigDecimal.valueOf(100);

    private final UsageRecordRepository usageRecordRepository;

    public DataUsageService(UsageRecordRepository usageRecordRepository) {
        this.usageRecordRepository = usageRecordRepository;
    }

    public Map<Long, DataUsage> calculate(List<Customer> customers) {
        return calculate(customers, Instant.now());
    }

    public Map<Long, DataUsage> calculate(List<Customer> customers, Instant referenceTime) {
        if (customers.isEmpty()) {
            return Collections.emptyMap();
        }

        Instant cycleStart = referenceTime.atZone(ZoneOffset.UTC).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
                .toInstant();
        Instant cycleEnd = cycleStart.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
        List<Long> customerIds = customers.stream().map(Customer::getId).toList();

        Map<Long, BigDecimal> usageByCustomer = new HashMap<>();
        usageRecordRepository.sumQuantityByCustomerForCycle(customerIds, UsageRecord.Type.DATA, cycleStart, cycleEnd)
                .forEach(row -> usageByCustomer.put(((Number) row[0]).longValue(), (BigDecimal) row[1]));

        Map<Long, DataUsage> result = new HashMap<>();
        for (Customer customer : customers) {
            BigDecimal usedMegabytes = usageByCustomer.getOrDefault(customer.getId(), BigDecimal.ZERO);
            result.put(customer.getId(), project(customer, usedMegabytes));
        }

        log.debug("Calculated current-cycle data usage: cycleStart={}, cycleEnd={}, customers={}", cycleStart, cycleEnd,
                result.size());
        return result;
    }

    private DataUsage project(Customer customer, BigDecimal usedMegabytes) {
        BigDecimal usedGigabytes = usedMegabytes.divide(MEGABYTES_PER_GIGABYTE, 3, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        if (customer.getPlan() == null) {
            return new DataUsage(usedGigabytes, null, null, DataUsageState.WITHIN_LIMIT);
        }

        Integer dataLimitGb = customer.getPlan().getDataLimitGb();
        if (dataLimitGb == null) {
            return new DataUsage(usedGigabytes, null, null, DataUsageState.UNLIMITED);
        }

        BigDecimal limitMegabytes = MEGABYTES_PER_GIGABYTE.multiply(BigDecimal.valueOf(dataLimitGb));
        BigDecimal percentage = usedMegabytes.multiply(BigDecimal.valueOf(100))
                .divide(limitMegabytes, 2, RoundingMode.HALF_UP).stripTrailingZeros();
        BigDecimal unroundedPercentageDividend = usedMegabytes.multiply(BigDecimal.valueOf(100));
        DataUsageState state;
        if (unroundedPercentageDividend.compareTo(limitMegabytes.multiply(OVER_LIMIT_PERCENTAGE)) >= 0) {
            state = DataUsageState.OVER_LIMIT;
        } else if (unroundedPercentageDividend.compareTo(limitMegabytes.multiply(AT_RISK_PERCENTAGE)) >= 0) {
            state = DataUsageState.AT_RISK;
        } else {
            state = DataUsageState.WITHIN_LIMIT;
        }

        return new DataUsage(usedGigabytes, dataLimitGb, percentage, state);
    }

    public enum DataUsageState {
        WITHIN_LIMIT, AT_RISK, OVER_LIMIT, UNLIMITED
    }

    public record DataUsage(BigDecimal usedDataGb, Integer dataLimitGb, BigDecimal percentage, DataUsageState state) {
    }
}
