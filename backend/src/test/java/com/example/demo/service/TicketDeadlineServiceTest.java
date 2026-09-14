package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.model.SupportTicket;
import com.example.demo.repository.SupportTicketRepository;
import com.example.demo.service.TicketDeadlineService.DeadlineCounts;
import com.example.demo.service.TicketDeadlineService.DeadlineState;
import com.example.demo.service.TicketDeadlineService.Projection;
import com.example.demo.service.TicketDeadlineService.ReconciliationResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketDeadlineServiceTest {

    private static final Instant MONDAY = Instant.parse("2026-08-03T10:00:00Z");
    private static final Instant FIXED_NOW = Instant.parse("2026-08-18T10:00:01Z");

    private final SupportTicketRepository repository = mock(SupportTicketRepository.class);
    private final TicketDeadlineService service = new TicketDeadlineService(repository,
            Clock.fixed(FIXED_NOW, ZoneOffset.UTC));

    @Test
    void projectionCalculatesWholeAgeAndExactWeekdayDeadlines() {
        SupportTicket ticket = ticket(SupportTicket.Status.OPEN, SupportTicket.Priority.MEDIUM, MONDAY);

        Projection projection = service.project(ticket, Instant.parse("2026-08-04T11:30:00Z"));

        assertEquals(25, projection.ageHours());
        assertEquals(1, projection.ageDays());
        assertEquals(Instant.parse("2026-08-05T10:00:00Z"), projection.acknowledgementDueAt());
        assertEquals(Instant.parse("2026-08-17T10:00:00Z"), projection.resolutionDueAt());
        assertEquals(DeadlineState.DUE_SOON, projection.deadlineState());
    }

    @Test
    void weekendTimeDoesNotConsumeBusinessDays() {
        assertEquals(Instant.parse("2026-08-04T15:00:00Z"),
                service.addBusinessDays(Instant.parse("2026-07-31T15:00:00Z"), 2));
    }

    @Test
    void exactlyTwentyFourHoursAndTheDueInstantAreDueSoon() {
        SupportTicket ticket = ticket(SupportTicket.Status.OPEN, SupportTicket.Priority.MEDIUM, MONDAY);

        assertEquals(DeadlineState.DUE_SOON,
                service.project(ticket, Instant.parse("2026-08-04T10:00:00Z")).deadlineState());
        assertEquals(DeadlineState.DUE_SOON,
                service.project(ticket, Instant.parse("2026-08-05T10:00:00Z")).deadlineState());
        assertEquals(DeadlineState.ACKNOWLEDGEMENT_OVERDUE,
                service.project(ticket, Instant.parse("2026-08-05T10:00:00.001Z")).deadlineState());
    }

    @Test
    void dueSoonWindowTreatsAWeekendAsNonBusinessTime() {
        SupportTicket ticket = ticket(SupportTicket.Status.OPEN, SupportTicket.Priority.MEDIUM,
                Instant.parse("2026-07-30T10:00:00Z"));

        assertEquals(DeadlineState.DUE_SOON,
                service.project(ticket, Instant.parse("2026-07-31T10:00:00Z")).deadlineState());
    }

    @Test
    void resolutionOverdueTakesPrecedenceOverMissingAcknowledgement() {
        SupportTicket ticket = ticket(SupportTicket.Status.OPEN, SupportTicket.Priority.LOW, MONDAY);

        assertEquals(DeadlineState.RESOLUTION_OVERDUE, service.project(ticket, FIXED_NOW).deadlineState());
    }

    @Test
    void resolvedTicketRetainsItsHistoricalResolutionBreach() {
        SupportTicket ticket = ticket(SupportTicket.Status.RESOLVED, SupportTicket.Priority.HIGH, MONDAY);
        ticket.setAcknowledgedAt(Instant.parse("2026-08-04T10:00:00Z"));
        ticket.setResolvedAt(Instant.parse("2026-08-17T10:00:01Z"));

        assertEquals(DeadlineState.RESOLUTION_OVERDUE, service.project(ticket, FIXED_NOW).deadlineState());
    }

    @Test
    void resolvedTicketProjectedBeforeItsResolutionIsBoundedByAsOf() {
        SupportTicket ticket = ticket(SupportTicket.Status.RESOLVED, SupportTicket.Priority.MEDIUM, MONDAY);
        ticket.setAcknowledgedAt(Instant.parse("2026-08-03T12:00:00Z"));
        ticket.setResolvedAt(Instant.parse("2026-08-17T10:00:01Z"));

        assertEquals(DeadlineState.ON_TRACK,
                service.project(ticket, Instant.parse("2026-08-10T10:00:00Z")).deadlineState());
    }

    @Test
    void currentCountsExcludeResolvedTicketsAndUseOneStatePerTicket() {
        SupportTicket acknowledgementOverdue = ticket(SupportTicket.Status.OPEN, SupportTicket.Priority.HIGH,
                Instant.parse("2026-08-12T10:00:00Z"));
        SupportTicket resolutionOverdue = ticket(SupportTicket.Status.IN_PROGRESS, SupportTicket.Priority.HIGH, MONDAY);
        resolutionOverdue.setAcknowledgedAt(Instant.parse("2026-08-04T10:00:00Z"));
        SupportTicket dueSoon = ticket(SupportTicket.Status.IN_PROGRESS, SupportTicket.Priority.HIGH,
                Instant.parse("2026-08-04T10:00:00Z"));
        dueSoon.setAcknowledgedAt(Instant.parse("2026-08-05T10:00:00Z"));
        SupportTicket resolved = ticket(SupportTicket.Status.RESOLVED, SupportTicket.Priority.HIGH,
                Instant.parse("2026-08-13T10:00:00Z"));
        resolved.setResolvedAt(Instant.parse("2026-08-14T10:00:00Z"));

        DeadlineCounts counts = service.countActive(
                List.of(acknowledgementOverdue, resolutionOverdue, dueSoon, resolved),
                Instant.parse("2026-08-17T10:00:01Z"));

        assertEquals(1, counts.acknowledgementOverdue());
        assertEquals(1, counts.resolutionOverdue());
        assertEquals(1, counts.dueSoon());
    }

    @Test
    void reconciliationEscalatesLowOnceAndPreservesUrgent() {
        SupportTicket low = ticket(SupportTicket.Status.OPEN, SupportTicket.Priority.LOW, MONDAY);
        SupportTicket urgent = ticket(SupportTicket.Status.OPEN, SupportTicket.Priority.URGENT, MONDAY);
        when(repository.findByStatusIn(List.of(SupportTicket.Status.OPEN, SupportTicket.Status.IN_PROGRESS)))
                .thenReturn(List.of(low, urgent));
        when(repository.save(low)).thenReturn(low);

        ReconciliationResult first = service.reconcile(FIXED_NOW);
        Instant escalatedAt = low.getPriorityEscalatedAt();
        ReconciliationResult second = service.reconcile(FIXED_NOW.plusSeconds(60));

        assertEquals(2, first.candidates());
        assertEquals(1, first.updated());
        assertEquals(0, second.updated());
        assertEquals(SupportTicket.Priority.HIGH, low.getPriority());
        assertEquals(FIXED_NOW, escalatedAt);
        assertSame(escalatedAt, low.getPriorityEscalatedAt());
        assertEquals(SupportTicket.Priority.URGENT, urgent.getPriority());
        assertNull(urgent.getPriorityEscalatedAt());
        verify(repository, times(1)).save(low);
    }

    @Test
    void reconciliationPreservesAnExistingEscalationTimestampWhenPriorityWasLowered() {
        SupportTicket ticket = ticket(SupportTicket.Status.OPEN, SupportTicket.Priority.LOW, MONDAY);
        Instant firstEscalation = Instant.parse("2026-08-18T09:00:00Z");
        ticket.setPriorityEscalatedAt(firstEscalation);
        when(repository.findByStatusIn(List.of(SupportTicket.Status.OPEN, SupportTicket.Status.IN_PROGRESS)))
                .thenReturn(List.of(ticket));

        service.reconcile(FIXED_NOW);

        assertEquals(SupportTicket.Priority.HIGH, ticket.getPriority());
        assertEquals(firstEscalation, ticket.getPriorityEscalatedAt());
        verify(repository).save(ticket);
    }

    private SupportTicket ticket(SupportTicket.Status status, SupportTicket.Priority priority, Instant createdAt) {
        Customer customer = new Customer();
        customer.setId(1L);
        SupportTicket ticket = new SupportTicket(customer, "Subject", "Description", priority);
        ticket.setId(1L);
        ticket.setStatus(status);
        ticket.setCreatedAt(createdAt);
        return ticket;
    }
}
