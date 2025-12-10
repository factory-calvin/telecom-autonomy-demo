package com.example.demo.controller;

import com.example.demo.model.SupportTicket;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.SupportTicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class SupportTicketController {

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

        return new PagedResponse(result.getContent().stream().map(TicketResponse::from).toList(), result.getNumber(),
                result.getTotalPages(), result.getTotalElements(), result.hasNext());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long id) {
        return repository.findById(id).map(ticket -> ResponseEntity.ok(TicketResponse.from(ticket)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@RequestBody TicketRequest request) {
        return customerRepository.findById(request.customerId()).map(customer -> {
            SupportTicket ticket = new SupportTicket(customer, request.subject(), request.description(),
                    SupportTicket.Priority.valueOf(request.priority()));
            return ResponseEntity.ok(TicketResponse.from(repository.save(ticket)));
        }).orElse(ResponseEntity.badRequest().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable Long id, @RequestBody TicketRequest request) {
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
            return ResponseEntity.ok(TicketResponse.from(repository.save(ticket)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
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
