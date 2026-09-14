package com.example.demo.repository;

import com.example.demo.model.Customer;
import com.example.demo.model.Plan;
import com.example.demo.model.SupportTicket;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class SupportTicketRepositoryIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SupportTicketRepository repository;

    @Test
    void filteredQueryPreservesOrderAndAuditableTimestamps() {
        Plan plan = new Plan("Test", BigDecimal.TEN, 10, 100, 100);
        entityManager.persist(plan);
        Customer customer = new Customer("Ada", "Lovelace", "ticket-repository@example.com", "555-0100", plan);
        entityManager.persist(customer);

        SupportTicket older = ticket(customer, SupportTicket.Priority.LOW, "2026-08-03T10:00:00Z");
        older.setAcknowledgedAt(Instant.parse("2026-08-04T10:00:00Z"));
        older.setPriorityEscalatedAt(Instant.parse("2026-08-18T10:00:01Z"));
        SupportTicket newer = ticket(customer, SupportTicket.Priority.LOW, "2026-08-04T10:00:00Z");
        repository.saveAllAndFlush(List.of(older, newer));
        entityManager.clear();

        List<SupportTicket> filtered = repository.findFiltered(SupportTicket.Priority.LOW, SupportTicket.Status.OPEN,
                customer.getId());

        assertEquals(List.of(newer.getId(), older.getId()), filtered.stream().map(SupportTicket::getId).toList());
        SupportTicket reloadedOlder = filtered.get(1);
        assertEquals(Instant.parse("2026-08-04T10:00:00Z"), reloadedOlder.getAcknowledgedAt());
        assertEquals(Instant.parse("2026-08-18T10:00:01Z"), reloadedOlder.getPriorityEscalatedAt());
    }

    private SupportTicket ticket(Customer customer, SupportTicket.Priority priority, String createdAt) {
        SupportTicket ticket = new SupportTicket(customer, "Subject", "Description", priority);
        ticket.setCreatedAt(Instant.parse(createdAt));
        return ticket;
    }
}
