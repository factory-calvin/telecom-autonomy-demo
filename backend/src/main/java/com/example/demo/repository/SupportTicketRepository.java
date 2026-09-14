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
}
