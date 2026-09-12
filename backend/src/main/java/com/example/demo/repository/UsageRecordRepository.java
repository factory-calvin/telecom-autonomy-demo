package com.example.demo.repository;

import com.example.demo.model.UsageRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    List<UsageRecord> findByCustomerId(Long customerId);

    Page<UsageRecord> findAllByOrderByRecordedAtDesc(Pageable pageable);

    @Query("SELECT u.type, FUNCTION('date', u.recordedAt), SUM(u.quantity) FROM UsageRecord u WHERE u.recordedAt >= :since GROUP BY u.type, FUNCTION('date', u.recordedAt) ORDER BY FUNCTION('date', u.recordedAt)")
    List<Object[]> sumByTypeAndDate(@Param("since") Instant since);

    @Query("SELECT u.customer.id, SUM(u.quantity) FROM UsageRecord u "
            + "WHERE u.customer.id IN :customerIds AND u.type = :type "
            + "AND u.recordedAt >= :cycleStart AND u.recordedAt < :cycleEnd GROUP BY u.customer.id")
    List<Object[]> sumQuantityByCustomerForCycle(@Param("customerIds") List<Long> customerIds,
            @Param("type") UsageRecord.Type type, @Param("cycleStart") Instant cycleStart,
            @Param("cycleEnd") Instant cycleEnd);

    @Query("SELECT u FROM UsageRecord u WHERE " + "(:type IS NULL OR u.type = :type) AND "
            + "(:customerId IS NULL OR u.customer.id = :customerId) AND "
            + "(:dateFrom IS NULL OR u.recordedAt >= :dateFrom) AND " + "(:dateTo IS NULL OR u.recordedAt <= :dateTo) "
            + "ORDER BY u.recordedAt DESC")
    Page<UsageRecord> findFiltered(@Param("type") UsageRecord.Type type, @Param("customerId") Long customerId,
            @Param("dateFrom") Instant dateFrom, @Param("dateTo") Instant dateTo, Pageable pageable);
}
