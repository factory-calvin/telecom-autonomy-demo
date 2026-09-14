package com.example.demo.service;

import com.example.demo.model.SupportTicket;
import com.example.demo.repository.SupportTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class TicketDeadlineService {

    public enum DeadlineState {
        ON_TRACK, DUE_SOON, ACKNOWLEDGEMENT_OVERDUE, RESOLUTION_OVERDUE
    }

    public record Projection(long ageHours, long ageDays, Instant acknowledgementDueAt, Instant resolutionDueAt,
            DeadlineState deadlineState) {
    }

    public record DeadlineCounts(long acknowledgementOverdue, long resolutionOverdue, long dueSoon) {
    }

    public record ReconciliationResult(int candidates, int updated, Instant reconciledAt) {
    }

    private static final Logger log = LoggerFactory.getLogger(TicketDeadlineService.class);
    private static final int ACKNOWLEDGEMENT_BUSINESS_DAYS = 2;
    private static final int RESOLUTION_BUSINESS_DAYS = 10;
    private final SupportTicketRepository repository;
    private final Clock clock;

    @Autowired
    public TicketDeadlineService(SupportTicketRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public TicketDeadlineService(SupportTicketRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Instant now() {
        return clock.instant();
    }

    public Projection project(SupportTicket ticket, Instant asOf) {
        Instant createdAt = ticket.getCreatedAt();
        Instant acknowledgementDueAt = addBusinessDays(createdAt, ACKNOWLEDGEMENT_BUSINESS_DAYS);
        Instant resolutionDueAt = addBusinessDays(createdAt, RESOLUTION_BUSINESS_DAYS);
        Instant ageEnd = ticket.getResolvedAt() != null && ticket.getResolvedAt().isBefore(asOf)
                ? ticket.getResolvedAt()
                : asOf;
        Duration age = ageEnd.isBefore(createdAt) ? Duration.ZERO : Duration.between(createdAt, ageEnd);

        return new Projection(age.toHours(), age.toDays(), acknowledgementDueAt, resolutionDueAt,
                state(ticket, asOf, acknowledgementDueAt, resolutionDueAt));
    }

    public DeadlineCounts countActive(List<SupportTicket> tickets, Instant asOf) {
        long acknowledgementOverdue = 0;
        long resolutionOverdue = 0;
        long dueSoon = 0;
        for (SupportTicket ticket : tickets) {
            if (!isActive(ticket)) {
                continue;
            }
            switch (project(ticket, asOf).deadlineState()) {
                case ACKNOWLEDGEMENT_OVERDUE -> acknowledgementOverdue++;
                case RESOLUTION_OVERDUE -> resolutionOverdue++;
                case DUE_SOON -> dueSoon++;
                case ON_TRACK -> {
                }
            }
        }
        return new DeadlineCounts(acknowledgementOverdue, resolutionOverdue, dueSoon);
    }

    @Transactional
    public ReconciliationResult reconcile() {
        return reconcile(now());
    }

    @Transactional
    public ReconciliationResult reconcile(Instant asOf) {
        List<SupportTicket> overdue = repository
                .findByStatusIn(List.of(SupportTicket.Status.OPEN, SupportTicket.Status.IN_PROGRESS)).stream()
                .filter(ticket -> project(ticket, asOf).deadlineState() == DeadlineState.RESOLUTION_OVERDUE).toList();

        int updated = 0;
        for (SupportTicket ticket : overdue) {
            if ((ticket.getPriority() == SupportTicket.Priority.LOW
                    || ticket.getPriority() == SupportTicket.Priority.MEDIUM)) {
                ticket.setPriority(SupportTicket.Priority.HIGH);
                if (ticket.getPriorityEscalatedAt() == null) {
                    ticket.setPriorityEscalatedAt(asOf);
                }
                repository.save(ticket);
                updated++;
            }
        }
        log.info("Ticket deadline reconciliation completed: candidates={}, updated={}, asOf={}", overdue.size(),
                updated, asOf);
        return new ReconciliationResult(overdue.size(), updated, asOf);
    }

    public Instant addBusinessDays(Instant start, int businessDays) {
        ZonedDateTime result = start.atZone(ZoneOffset.UTC);
        int remaining = Math.abs(businessDays);
        int direction = businessDays < 0 ? -1 : 1;
        while (remaining > 0) {
            result = result.plusDays(direction);
            if (isBusinessDay(result.getDayOfWeek())) {
                remaining--;
            }
        }
        return result.toInstant();
    }

    private DeadlineState state(SupportTicket ticket, Instant asOf, Instant acknowledgementDueAt,
            Instant resolutionDueAt) {
        Instant evaluationTime = evaluationTime(ticket, asOf);
        if (evaluationTime != null && evaluationTime.isAfter(resolutionDueAt)) {
            return DeadlineState.RESOLUTION_OVERDUE;
        }
        if (ticket.getAcknowledgedAt() == null && evaluationTime != null
                && evaluationTime.isAfter(acknowledgementDueAt)) {
            return DeadlineState.ACKNOWLEDGEMENT_OVERDUE;
        }
        if (!isActive(ticket)) {
            return DeadlineState.ON_TRACK;
        }

        Instant nextDeadline = ticket.getAcknowledgedAt() == null ? acknowledgementDueAt : resolutionDueAt;
        Instant dueSoonStartsAt = addBusinessDays(nextDeadline, -1);
        if (!asOf.isBefore(dueSoonStartsAt) && !asOf.isAfter(nextDeadline)) {
            return DeadlineState.DUE_SOON;
        }
        return DeadlineState.ON_TRACK;
    }

    private Instant evaluationTime(SupportTicket ticket, Instant asOf) {
        if (isActive(ticket)) {
            return asOf;
        }
        Instant resolvedAt = ticket.getResolvedAt();
        if (resolvedAt == null) {
            return null;
        }
        // A projection asked for a past asOf must not see a resolution that happened
        // later.
        return resolvedAt.isBefore(asOf) ? resolvedAt : asOf;
    }

    private boolean isActive(SupportTicket ticket) {
        return ticket.getStatus() == SupportTicket.Status.OPEN
                || ticket.getStatus() == SupportTicket.Status.IN_PROGRESS;
    }

    private boolean isBusinessDay(DayOfWeek day) {
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }
}
