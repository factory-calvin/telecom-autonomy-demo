package com.example.demo.repository;

import com.example.demo.model.Customer;
import com.example.demo.model.Plan;
import com.example.demo.model.SupportTicket;
import com.example.demo.service.TicketDeadlineService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class SupportTicketRepositoryIntegrationTest {

    private static final Instant AS_OF = Instant.parse("2026-08-18T10:00:01Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SupportTicketRepository repository;

    private TicketDeadlineService deadlineService;

    @BeforeEach
    void setUp() {
        deadlineService = new TicketDeadlineService(repository, Clock.fixed(AS_OF, ZoneOffset.UTC));
    }

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

    @Test
    void deadlineQueryMatchesCanonicalBusinessDaySemanticsForEveryStateAndFilter() {
        Plan plan = new Plan("Deadline", BigDecimal.TEN, 10, 100, 100);
        entityManager.persist(plan);
        Customer customer = new Customer("Grace", "Hopper", "deadline-query@example.com", "555-0101", plan);
        Customer otherCustomer = new Customer("Katherine", "Johnson", "deadline-other@example.com", "555-0102", plan);
        entityManager.persist(customer);
        entityManager.persist(otherCustomer);

        List<SupportTicket> tickets = List.of(
                ticket(customer, SupportTicket.Priority.LOW, SupportTicket.Status.OPEN, "2026-08-03T10:00:00Z", null,
                        null),
                ticket(customer, SupportTicket.Priority.MEDIUM, SupportTicket.Status.OPEN, "2026-08-13T10:00:00Z", null,
                        null),
                ticket(customer, SupportTicket.Priority.HIGH, SupportTicket.Status.OPEN, "2026-08-17T10:00:00Z", null,
                        null),
                ticket(customer, SupportTicket.Priority.URGENT, SupportTicket.Status.OPEN, "2026-08-18T09:00:00Z", null,
                        null),
                ticket(customer, SupportTicket.Priority.LOW, SupportTicket.Status.IN_PROGRESS, "2026-08-04T10:00:00Z",
                        "2026-08-05T10:00:00Z", null),
                ticket(customer, SupportTicket.Priority.MEDIUM, SupportTicket.Status.RESOLVED, "2026-08-03T10:00:00Z",
                        "2026-08-04T10:00:00Z", "2026-08-17T10:00:00Z"),
                ticket(customer, SupportTicket.Priority.HIGH, SupportTicket.Status.RESOLVED, "2026-08-03T10:00:00Z",
                        "2026-08-04T10:00:00Z", "2026-08-17T10:00:01Z"),
                ticket(customer, SupportTicket.Priority.URGENT, SupportTicket.Status.OPEN, "2026-08-07T10:00:00Z",
                        "2026-08-10T10:00:00Z", null),
                ticket(otherCustomer, SupportTicket.Priority.LOW, SupportTicket.Status.OPEN, "2026-08-03T10:00:00Z",
                        null, null));
        repository.saveAllAndFlush(tickets);
        entityManager.clear();

        for (TicketDeadlineService.DeadlineState state : TicketDeadlineService.DeadlineState.values()) {
            assertDeadlineQueryMatchesService(state, null, null, customer.getId());
        }
        assertDeadlineQueryMatchesService(TicketDeadlineService.DeadlineState.RESOLUTION_OVERDUE,
                SupportTicket.Priority.LOW, null, customer.getId());
        assertDeadlineQueryMatchesService(TicketDeadlineService.DeadlineState.ACKNOWLEDGEMENT_OVERDUE, null,
                SupportTicket.Status.OPEN, customer.getId());
        assertDeadlineQueryMatchesService(TicketDeadlineService.DeadlineState.ON_TRACK, SupportTicket.Priority.URGENT,
                SupportTicket.Status.OPEN, customer.getId());
    }

    @Test
    void productionSizedDeadlineQueryLoadsOnlyTheRequestedPage() {
        Plan plan = new Plan("Scale", BigDecimal.TEN, 10, 100, 100);
        entityManager.persist(plan);
        Customer customer = new Customer("Margaret", "Hamilton", "deadline-scale@example.com", "555-0103", plan);
        entityManager.persist(customer);

        List<SupportTicket> batch = new ArrayList<>();
        for (int index = 0; index < 5_000; index++) {
            batch.add(ticket(customer, SupportTicket.Priority.MEDIUM, SupportTicket.Status.OPEN, "2026-07-01T10:00:00Z",
                    null, null));
            if (batch.size() == 250) {
                repository.saveAll(batch);
                repository.flush();
                entityManager.clear();
                batch.clear();
            }
        }

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        Page<SupportTicket> result = repository.findFilteredByDeadline(null, null, customer.getId(),
                TicketDeadlineService.DeadlineState.RESOLUTION_OVERDUE.name(), AS_OF.toString(), PageRequest.of(0, 50));

        assertEquals(50, result.getContent().size());
        assertEquals(5_000, result.getTotalElements());
        assertTrue(result.hasNext());
        long ticketLoads = statistics.getEntityStatistics(SupportTicket.class.getName()).getLoadCount();
        assertTrue(ticketLoads <= 50, () -> "Expected at most one page of ticket loads but loaded " + ticketLoads);
    }

    private void assertDeadlineQueryMatchesService(TicketDeadlineService.DeadlineState state,
            SupportTicket.Priority priority, SupportTicket.Status status, Long customerId) {
        List<Long> expected = repository.findFiltered(priority, status, customerId).stream()
                .filter(ticket -> deadlineService.project(ticket, AS_OF).deadlineState() == state)
                .map(SupportTicket::getId).toList();
        Page<SupportTicket> actual = repository.findFilteredByDeadline(priority != null ? priority.name() : null,
                status != null ? status.name() : null, customerId, state.name(), AS_OF.toString(),
                PageRequest.of(0, 100));

        assertEquals(expected, actual.getContent().stream().map(SupportTicket::getId).toList(), state.name());
        assertEquals(expected.size(), actual.getTotalElements(), state.name());
    }

    private SupportTicket ticket(Customer customer, SupportTicket.Priority priority, String createdAt) {
        SupportTicket ticket = new SupportTicket(customer, "Subject", "Description", priority);
        ticket.setCreatedAt(Instant.parse(createdAt));
        return ticket;
    }

    private SupportTicket ticket(Customer customer, SupportTicket.Priority priority, SupportTicket.Status status,
            String createdAt, String acknowledgedAt, String resolvedAt) {
        SupportTicket ticket = ticket(customer, priority, createdAt);
        ticket.setStatus(status);
        ticket.setAcknowledgedAt(acknowledgedAt != null ? Instant.parse(acknowledgedAt) : null);
        ticket.setResolvedAt(resolvedAt != null ? Instant.parse(resolvedAt) : null);
        return ticket;
    }
}
