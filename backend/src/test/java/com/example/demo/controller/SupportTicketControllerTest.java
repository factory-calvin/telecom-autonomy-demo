package com.example.demo.controller;

import com.example.demo.model.Customer;
import com.example.demo.model.SupportTicket;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.SupportTicketRepository;
import com.example.demo.service.TicketDeadlineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupportTicketControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-18T10:00:01Z");

    private final SupportTicketRepository repository = mock(SupportTicketRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final TicketDeadlineService deadlineService = new TicketDeadlineService(repository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SupportTicketController(repository, customerRepository, deadlineService)).build();
    }

    @Test
    void deadlineFilterRunsBeforePaginationAndReportsFilteredMetadata() throws Exception {
        SupportTicket first = ticket(1L, SupportTicket.Status.OPEN, "2026-08-03T10:00:00Z");
        SupportTicket second = ticket(2L, SupportTicket.Status.IN_PROGRESS, "2026-08-03T11:00:00Z");
        second.setAcknowledgedAt(Instant.parse("2026-08-04T11:00:00Z"));
        SupportTicket onTrack = ticket(3L, SupportTicket.Status.OPEN, "2026-08-18T09:00:00Z");
        when(repository.findFilteredByDeadline(isNull(), isNull(), isNull(), eq("RESOLUTION_OVERDUE"),
                eq(NOW.toString()), eq(PageRequest.of(1, 1))))
                .thenReturn(new PageImpl<>(List.of(second), PageRequest.of(1, 1), 2));

        mockMvc.perform(get("/api/tickets").param("deadlineState", "RESOLUTION_OVERDUE").param("page", "1")
                .param("size", "1").param("asOf", NOW.toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].deadline_state").value("RESOLUTION_OVERDUE"))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.as_of").value(NOW.toString()));

        verify(repository, never()).save(any(SupportTicket.class));
    }

    @Test
    void unknownDeadlineStateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tickets").param("deadlineState", "LATE")).andExpect(status().isBadRequest());
    }

    @Test
    void openToInProgressSetsAcknowledgementOnlyOnce() throws Exception {
        SupportTicket ticket = ticket(1L, SupportTicket.Status.OPEN, "2026-08-18T09:00:00Z");
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenReturn(ticket);

        String body = """
                {"customerId":1,"subject":"Subject","description":"Description","priority":"MEDIUM",
                 "status":"IN_PROGRESS"}
                """;
        mockMvc.perform(put("/api/tickets/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acknowledged_at").value(NOW.toString()));

        Instant firstAcknowledgement = ticket.getAcknowledgedAt();
        mockMvc.perform(put("/api/tickets/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acknowledged_at").value(NOW.toString()));
        org.junit.jupiter.api.Assertions.assertSame(firstAcknowledgement, ticket.getAcknowledgedAt());
    }

    @Test
    void reopeningAResolvedTicketClearsResolvedAt() throws Exception {
        SupportTicket ticket = ticket(1L, SupportTicket.Status.RESOLVED, "2026-08-03T10:00:00Z");
        ticket.setResolvedAt(Instant.parse("2026-08-10T10:00:00Z"));
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenReturn(ticket);

        String body = """
                {"customerId":1,"subject":"Subject","description":"Description","priority":"MEDIUM","status":"OPEN"}
                """;
        mockMvc.perform(put("/api/tickets/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolved_at").doesNotExist());
    }

    @Test
    void repeatedResolvedUpdatePreservesTheFirstResolutionTimestamp() throws Exception {
        SupportTicket ticket = ticket(1L, SupportTicket.Status.RESOLVED, "2026-08-03T10:00:00Z");
        Instant firstResolution = Instant.parse("2026-08-10T10:00:00Z");
        ticket.setResolvedAt(firstResolution);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenReturn(ticket);

        String body = """
                {"customerId":1,"subject":"Subject","description":"Description","priority":"MEDIUM",
                 "status":"RESOLVED"}
                """;
        mockMvc.perform(put("/api/tickets/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolved_at").value(firstResolution.toString()));
    }

    private SupportTicket ticket(Long id, SupportTicket.Status status, String createdAt) {
        Customer customer = new Customer();
        customer.setId(7L);
        customer.setFirstName("Ada");
        customer.setLastName("Lovelace");
        SupportTicket ticket = new SupportTicket(customer, "Subject", "Description", SupportTicket.Priority.MEDIUM);
        ticket.setId(id);
        ticket.setStatus(status);
        ticket.setCreatedAt(Instant.parse(createdAt));
        return ticket;
    }
}
