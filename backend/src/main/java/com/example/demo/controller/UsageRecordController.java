package com.example.demo.controller;

import com.example.demo.model.UsageRecord;
import com.example.demo.repository.UsageRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/usage")
public class UsageRecordController {

    private final UsageRecordRepository repository;

    public UsageRecordController(UsageRecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public PagedResponse getUsageRecords(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String type,
            @RequestParam(required = false) Long customerId, @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        UsageRecord.Type typeEnum = (type != null && !type.isEmpty()) ? UsageRecord.Type.valueOf(type) : null;
        Instant from = (dateFrom != null && !dateFrom.isEmpty())
                ? LocalDate.parse(dateFrom).atStartOfDay().toInstant(ZoneOffset.UTC)
                : null;
        Instant to = (dateTo != null && !dateTo.isEmpty())
                ? LocalDate.parse(dateTo).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : null;

        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100));
        Page<UsageRecord> result;

        boolean hasFilters = typeEnum != null || customerId != null || from != null || to != null;
        if (hasFilters) {
            result = repository.findFiltered(typeEnum, customerId, from, to, pageRequest);
        } else {
            result = repository.findAllByOrderByRecordedAtDesc(pageRequest);
        }

        return new PagedResponse(result.getContent().stream().map(UsageRecordResponse::from).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements(), result.hasNext());
    }

    @GetMapping("/customer/{customerId}")
    public List<UsageRecordResponse> getByCustomer(@PathVariable Long customerId) {
        return repository.findByCustomerId(customerId).stream().map(UsageRecordResponse::from).toList();
    }

    public record PagedResponse(List<UsageRecordResponse> content, int page, int totalPages, long totalElements,
            boolean hasNext) {
    }

    public record UsageRecordResponse(Long id, Long customer_id, String customer_name, String type, BigDecimal quantity,
            BigDecimal cost, String recorded_at) {
        public static UsageRecordResponse from(UsageRecord record) {
            return new UsageRecordResponse(record.getId(), record.getCustomer().getId(),
                    record.getCustomer().getFirstName() + " " + record.getCustomer().getLastName(),
                    record.getType().name(), record.getQuantity(), record.getCost(),
                    record.getRecordedAt() != null ? record.getRecordedAt().toString() : null);
        }
    }
}
