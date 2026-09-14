package com.example.demo.repository;

import com.example.demo.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByStatus(SupportTicket.Status status);

    long countByStatusIn(List<SupportTicket.Status> statuses);

    List<SupportTicket> findByStatusIn(List<SupportTicket.Status> statuses);

    @Query("SELECT t.status, COUNT(t) FROM SupportTicket t GROUP BY t.status")
    List<Object[]> countByStatus();

    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE " + "(:priority IS NULL OR t.priority = :priority) AND "
            + "(:status IS NULL OR t.status = :status) AND " + "(:customerId IS NULL OR t.customer.id = :customerId) "
            + "ORDER BY t.createdAt DESC")
    Page<SupportTicket> findFiltered(@Param("priority") SupportTicket.Priority priority,
            @Param("status") SupportTicket.Status status, @Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE " + "(:priority IS NULL OR t.priority = :priority) AND "
            + "(:status IS NULL OR t.status = :status) AND " + "(:customerId IS NULL OR t.customer.id = :customerId) "
            + "ORDER BY t.createdAt DESC")
    List<SupportTicket> findFiltered(@Param("priority") SupportTicket.Priority priority,
            @Param("status") SupportTicket.Status status, @Param("customerId") Long customerId);

    String DEADLINE_FILTER_CTE = """
            WITH params AS (
                SELECT unixepoch(:asOf, 'subsec') * 1000.0 AS as_of
            ),
            normalized AS (
                SELECT t.id,
                       CASE typeof(t.created_at)
                           WHEN 'integer' THEN CAST(t.created_at AS REAL)
                           ELSE unixepoch(t.created_at, 'subsec') * 1000.0
                       END AS created_at,
                       CASE typeof(t.resolved_at)
                           WHEN 'integer' THEN CAST(t.resolved_at AS REAL)
                           ELSE unixepoch(t.resolved_at, 'subsec') * 1000.0
                       END AS resolved_at,
                       t.acknowledged_at,
                       t.status,
                       p.as_of
                FROM support_tickets t
                CROSS JOIN params p
                WHERE (:priority IS NULL OR t.priority = :priority)
                  AND (:status IS NULL OR t.status = :status)
                  AND (:customerId IS NULL OR t.customer_id = :customerId)
            ),
            projected AS (
                SELECT id,
                       CASE
                           WHEN status IN ('OPEN', 'IN_PROGRESS') THEN as_of
                           WHEN resolved_at IS NULL THEN NULL
                           ELSE min(resolved_at, as_of)
                       END AS evaluation_time,
                       created_at + CASE strftime('%w', created_at / 1000.0, 'unixepoch')
                           WHEN '0' THEN 172800000
                           WHEN '4' THEN 345600000
                           WHEN '5' THEN 345600000
                           WHEN '6' THEN 259200000
                           ELSE 172800000
                       END AS acknowledgement_due,
                       created_at + CASE strftime('%w', created_at / 1000.0, 'unixepoch')
                           WHEN '0' THEN 1036800000
                           WHEN '6' THEN 1123200000
                           ELSE 1209600000
                       END AS resolution_due,
                       acknowledged_at,
                       status,
                       as_of
                FROM normalized
            ),
            classified AS (
                SELECT id,
                       CASE
                           WHEN evaluation_time > resolution_due THEN 'RESOLUTION_OVERDUE'
                           WHEN acknowledged_at IS NULL
                                AND evaluation_time > acknowledgement_due
                               THEN 'ACKNOWLEDGEMENT_OVERDUE'
                           WHEN status IN ('OPEN', 'IN_PROGRESS')
                                AND as_of <= CASE WHEN acknowledged_at IS NULL
                                    THEN acknowledgement_due
                                    ELSE resolution_due
                                END
                                AND as_of >= CASE
                                    WHEN acknowledged_at IS NULL THEN
                                        acknowledgement_due -
                                            CASE strftime('%w', acknowledgement_due / 1000.0, 'unixepoch')
                                                WHEN '1' THEN 259200000
                                                ELSE 86400000
                                            END
                                    ELSE resolution_due -
                                        CASE strftime('%w', resolution_due / 1000.0, 'unixepoch')
                                            WHEN '1' THEN 259200000
                                            ELSE 86400000
                                        END
                                END
                               THEN 'DUE_SOON'
                           ELSE 'ON_TRACK'
                       END AS deadline_state
                FROM projected
            )
            """;

    @Query(value = DEADLINE_FILTER_CTE + """
            SELECT t.*
            FROM support_tickets t
            JOIN classified c ON c.id = t.id
            WHERE c.deadline_state = :deadlineState
            ORDER BY t.created_at DESC
            """, countQuery = DEADLINE_FILTER_CTE + """
            SELECT count(*)
            FROM classified
            WHERE deadline_state = :deadlineState
            """, nativeQuery = true)
    Page<SupportTicket> findFilteredByDeadline(@Param("priority") String priority, @Param("status") String status,
            @Param("customerId") Long customerId, @Param("deadlineState") String deadlineState,
            @Param("asOf") String asOf, Pageable pageable);
}
