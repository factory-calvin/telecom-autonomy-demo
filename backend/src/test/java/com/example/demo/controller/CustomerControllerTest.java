package com.example.demo.controller;

import com.example.demo.model.Customer;
import com.example.demo.model.Plan;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final PlanRepository planRepository = mock(PlanRepository.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerController(repository, planRepository)).build();
    }

    @Test
    void getAllCustomersFlattensPlanOntoTheResponse() throws Exception {
        when(repository.findAll()).thenReturn(List.of(customer(1L, plan(2L, "Standard", "40.00"))));

        mockMvc.perform(get("/api/customers")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].first_name").value("First1"))
                .andExpect(jsonPath("$[0].last_name").value("Last1")).andExpect(jsonPath("$[0].plan_id").value(2))
                .andExpect(jsonPath("$[0].plan_name").value("Standard"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getAllCustomersToleratesAMissingPlan() throws Exception {
        when(repository.findAll()).thenReturn(List.of(customer(1L, null)));

        mockMvc.perform(get("/api/customers")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plan_id").value(nullValue()))
                .andExpect(jsonPath("$[0].plan_name").value(nullValue()));
    }

    @Test
    void getCustomerReturnsNotFoundForUnknownId() throws Exception {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/customers/404")).andExpect(status().isNotFound());
    }

    @Test
    void createCustomerRejectsAnUnknownPlan() throws Exception {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        String body = """
                {"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","phone":"555-0100","planId":99}
                """;

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any(Customer.class));
    }

    @Test
    void createCustomerStartsActiveWithTheGivenBalance() throws Exception {
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan(2L, "Standard", "40.00")));
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        String body = """
                {"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","phone":"555-0100",
                 "planId":2,"balance":12.34}
                """;

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.balance").value(12.34))
                .andExpect(jsonPath("$.plan_name").value("Standard"));
    }

    @Test
    void updateCustomerAppliesAStatusTransition() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(customer(1L, plan(2L, "Standard", "40.00"))));
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String body = """
                {"firstName":"First1","lastName":"Last1","email":"customer1@example.com","phone":"555-0001",
                 "status":"SUSPENDED"}
                """;

        mockMvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void updateCustomerReturnsNotFoundForUnknownId() throws Exception {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        String body = """
                {"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","phone":"555-0100"}
                """;

        mockMvc.perform(put("/api/customers/404").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCustomerReturnsNoContentWhenPresent() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/customers/1")).andExpect(status().isNoContent());

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteCustomerReturnsNotFoundWhenMissing() throws Exception {
        when(repository.existsById(404L)).thenReturn(false);

        mockMvc.perform(delete("/api/customers/404")).andExpect(status().isNotFound());

        verify(repository, never()).deleteById(anyLong());
    }

    private static Plan plan(Long id, String name, String price) {
        Plan plan = new Plan(name, new BigDecimal(price), 10, 500, 500);
        plan.setId(id);
        return plan;
    }

    private static Customer customer(Long id, Plan plan) {
        Customer customer = new Customer("First" + id, "Last" + id, "customer" + id + "@example.com", "555-000" + id,
                plan);
        customer.setId(id);
        return customer;
    }
}
