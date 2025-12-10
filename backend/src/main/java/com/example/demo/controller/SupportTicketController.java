package com.example.demo.controller;

import com.example.demo.model.SupportTicket;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.SupportTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class SupportTicketController {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketController.class);

    private final SupportTicketRepository repository;
    private final CustomerRepository customerRepository;

    public SupportTicketController(SupportTicketRepository repository, CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public PagedResponse getTickets(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status, @RequestParam(required = false) Long customerId) {
        log.info("Fetching tickets: page={}, size={}, priority={}, status={}, customerId={}", page, size, priority,
                status, customerId);
        SupportTicket.Priority priorityEnum = (priority != null && !priority.isEmpty())
                ? SupportTicket.Priority.valueOf(priority)
                : null;
        SupportTicket.Status statusEnum = (status != null && !status.isEmpty())
                ? SupportTicket.Status.valueOf(status)
                : null;

        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100));
        Page<SupportTicket> result;

        boolean hasFilters = priorityEnum != null || statusEnum != null || customerId != null;
        if (hasFilters) {
            result = repository.findFiltered(priorityEnum, statusEnum, customerId, pageRequest);
        } else {
            result = repository.findAllByOrderByCreatedAtDesc(pageRequest);
        }

        log.debug("Found {} tickets (page {} of {})", result.getNumberOfElements(), result.getNumber(),
                result.getTotalPages());
        return new PagedResponse(result.getContent().stream().map(TicketResponse::from).toList(), result.getNumber(),
                result.getTotalPages(), result.getTotalElements(), result.hasNext());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long id) {
        log.info("Fetching ticket with id={}", id);
        return repository.findById(id).map(ticket -> {
            log.debug("Found ticket: subject={}", ticket.getSubject());
            return ResponseEntity.ok(TicketResponse.from(ticket));
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
            SupportTicket saved = repository.save(ticket);
            log.info("Ticket created: id={}, subject={}", saved.getId(), saved.getSubject());
            return ResponseEntity.ok(TicketResponse.from(saved));
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
                SupportTicket.Status newStatus = SupportTicket.Status.valueOf(request.status());
                ticket.setStatus(newStatus);
                if (newStatus == SupportTicket.Status.RESOLVED || newStatus == SupportTicket.Status.CLOSED) {
                    ticket.setResolvedAt(Instant.now());
                }
            }
            SupportTicket saved = repository.save(ticket);
            log.info("Ticket updated: id={}, status={}", saved.getId(), saved.getStatus());
            return ResponseEntity.ok(TicketResponse.from(saved));
        }).orElseGet(() -> {
            log.warn("Ticket not found for update: id={}", id);
            return ResponseEntity.notFound().build();
        });
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
            boolean hasNext) {
    }

    public record TicketResponse(Long id, Long customer_id, String customer_name, String subject, String description,
            String priority, String status, String created_at, String resolved_at) {
        public static TicketResponse from(SupportTicket ticket) {
            return new TicketResponse(ticket.getId(), ticket.getCustomer().getId(),
                    ticket.getCustomer().getFirstName() + " " + ticket.getCustomer().getLastName(), ticket.getSubject(),
                    ticket.getDescription(), ticket.getPriority().name(), ticket.getStatus().name(),
                    ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null,
                    ticket.getResolvedAt() != null ? ticket.getResolvedAt().toString() : null);
        }
    }
}
