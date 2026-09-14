package com.example.demo.controller;

import com.example.demo.model.SupportTicket;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.SupportTicketRepository;
import com.example.demo.service.TicketDeadlineService;
import com.example.demo.service.TicketDeadlineService.DeadlineState;
import com.example.demo.service.TicketDeadlineService.Projection;
import com.example.demo.service.TicketDeadlineService.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class SupportTicketController {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketController.class);

    private final SupportTicketRepository repository;
    private final CustomerRepository customerRepository;
    private final TicketDeadlineService deadlineService;

    public SupportTicketController(SupportTicketRepository repository, CustomerRepository customerRepository,
            TicketDeadlineService deadlineService) {
        this.repository = repository;
        this.customerRepository = customerRepository;
        this.deadlineService = deadlineService;
    }

    @GetMapping
    public PagedResponse getTickets(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status, @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String deadlineState, @RequestParam(required = false) Instant asOf) {
        log.info("Fetching tickets: page={}, size={}, priority={}, status={}, customerId={}, deadlineState={}", page,
                size, priority, status, customerId, deadlineState);
        SupportTicket.Priority priorityEnum = parseEnum(priority, SupportTicket.Priority.class, "priority");
        SupportTicket.Status statusEnum = parseEnum(status, SupportTicket.Status.class, "status");
        DeadlineState deadlineStateEnum = parseEnum(deadlineState, DeadlineState.class, "deadlineState");
        Instant effectiveAsOf = asOf != null ? asOf : deadlineService.now();

        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100));
        if (deadlineStateEnum == null) {
            Page<SupportTicket> result;
            boolean hasFilters = priorityEnum != null || statusEnum != null || customerId != null;
            if (hasFilters) {
                result = repository.findFiltered(priorityEnum, statusEnum, customerId, pageRequest);
            } else {
                result = repository.findAllByOrderByCreatedAtDesc(pageRequest);
            }
            return pagedResponse(result.getContent(), result.getNumber(), result.getTotalPages(),
                    result.getTotalElements(), result.hasNext(), effectiveAsOf);
        }

        List<SupportTicket> filtered = repository.findFiltered(priorityEnum, statusEnum, customerId).stream()
                .filter(ticket -> deadlineService.project(ticket, effectiveAsOf).deadlineState() == deadlineStateEnum)
                .toList();
        int pageSize = pageRequest.getPageSize();
        int fromIndex = Math.min(page * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / pageSize);
        return pagedResponse(filtered.subList(fromIndex, toIndex), page, totalPages, filtered.size(),
                toIndex < filtered.size(), effectiveAsOf);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long id,
            @RequestParam(required = false) Instant asOf) {
        log.info("Fetching ticket with id={}", id);
        Instant effectiveAsOf = asOf != null ? asOf : deadlineService.now();
        return repository.findById(id).map(ticket -> {
            log.debug("Found ticket: subject={}", ticket.getSubject());
            return ResponseEntity.ok(TicketResponse.from(ticket, deadlineService.project(ticket, effectiveAsOf)));
        }).orElseGet(() -> {
            log.warn("Ticket not found: id={}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@RequestBody TicketRequest request) {
        log.info("Creating ticket: customerId={}, subject={}, priority={}", request.customerId(), request.subject(),
                request.priority());
        return customerRepository.findById(request.customerId()).map(customer -> {
            SupportTicket ticket = new SupportTicket(customer, request.subject(), request.description(),
                    SupportTicket.Priority.valueOf(request.priority()));
            ticket.setCreatedAt(deadlineService.now());
            SupportTicket saved = repository.save(ticket);
            log.info("Ticket created: id={}, subject={}", saved.getId(), saved.getSubject());
            return ResponseEntity.ok(TicketResponse.from(saved, deadlineService.project(saved, deadlineService.now())));
        }).orElseGet(() -> {
            log.warn("Customer not found for ticket creation: customerId={}", request.customerId());
            return ResponseEntity.badRequest().build();
        });
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable Long id, @RequestBody TicketRequest request) {
        log.info("Updating ticket: id={}", id);
        return repository.findById(id).map(ticket -> {
            ticket.setSubject(request.subject());
            ticket.setDescription(request.description());
            if (request.priority() != null) {
                ticket.setPriority(SupportTicket.Priority.valueOf(request.priority()));
            }
            if (request.status() != null) {
                SupportTicket.Status previousStatus = ticket.getStatus();
                SupportTicket.Status newStatus = SupportTicket.Status.valueOf(request.status());
                ticket.setStatus(newStatus);
                Instant changedAt = deadlineService.now();
                if (previousStatus == SupportTicket.Status.OPEN && newStatus == SupportTicket.Status.IN_PROGRESS
                        && ticket.getAcknowledgedAt() == null) {
                    ticket.setAcknowledgedAt(changedAt);
                }
                if (newStatus == SupportTicket.Status.RESOLVED || newStatus == SupportTicket.Status.CLOSED) {
                    if (ticket.getResolvedAt() == null) {
                        ticket.setResolvedAt(changedAt);
                    }
                } else {
                    ticket.setResolvedAt(null);
                }
            }
            SupportTicket saved = repository.save(ticket);
            log.info("Ticket updated: id={}, status={}", saved.getId(), saved.getStatus());
            return ResponseEntity.ok(TicketResponse.from(saved, deadlineService.project(saved, deadlineService.now())));
        }).orElseGet(() -> {
            log.warn("Ticket not found for update: id={}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @PostMapping("/reconcile-deadlines")
    public ReconciliationResponse reconcileDeadlines() {
        log.info("Manually reconciling ticket deadlines");
        ReconciliationResult result = deadlineService.reconcile();
        return new ReconciliationResponse(result.candidates(), result.updated(), result.reconciledAt().toString());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        log.info("Deleting ticket: id={}", id);
        if (!repository.existsById(id)) {
            log.warn("Ticket not found for deletion: id={}", id);
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        log.info("Ticket deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }

    public record TicketRequest(Long customerId, String subject, String description, String priority, String status) {
    }

    public record PagedResponse(List<TicketResponse> content, int page, int totalPages, long totalElements,
            boolean hasNext, String as_of) {
    }

    public record TicketResponse(Long id, Long customer_id, String customer_name, String subject, String description,
            String priority, String status, String created_at, String acknowledged_at, String resolved_at,
            String priority_escalated_at, long age_hours, long age_days, String acknowledgement_due_at,
            String resolution_due_at, String deadline_state) {
        public static TicketResponse from(SupportTicket ticket, Projection projection) {
            return new TicketResponse(ticket.getId(), ticket.getCustomer().getId(),
                    ticket.getCustomer().getFirstName() + " " + ticket.getCustomer().getLastName(), ticket.getSubject(),
                    ticket.getDescription(), ticket.getPriority().name(), ticket.getStatus().name(),
                    ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null,
                    ticket.getAcknowledgedAt() != null ? ticket.getAcknowledgedAt().toString() : null,
                    ticket.getResolvedAt() != null ? ticket.getResolvedAt().toString() : null,
                    ticket.getPriorityEscalatedAt() != null ? ticket.getPriorityEscalatedAt().toString() : null,
                    projection.ageHours(), projection.ageDays(), projection.acknowledgementDueAt().toString(),
                    projection.resolutionDueAt().toString(), projection.deadlineState().name());
        }
    }

    public record ReconciliationResponse(int candidates, int updated, String reconciled_at) {
    }

    private PagedResponse pagedResponse(List<SupportTicket> tickets, int page, int totalPages, long totalElements,
            boolean hasNext, Instant asOf) {
        List<TicketResponse> content = tickets.stream()
                .map(ticket -> TicketResponse.from(ticket, deadlineService.project(ticket, asOf))).toList();
        log.debug("Found {} tickets (page {} of {})", content.size(), page, totalPages);
        return new PagedResponse(content, page, totalPages, totalElements, hasNext, asOf.toString());
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, String parameter) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + parameter, exception);
        }
    }
}
