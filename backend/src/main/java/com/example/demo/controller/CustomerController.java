package com.example.demo.controller;

import com.example.demo.model.Customer;
import com.example.demo.model.Plan;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.PlanRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository repository;
    private final PlanRepository planRepository;

    public CustomerController(CustomerRepository repository, PlanRepository planRepository) {
        this.repository = repository;
        this.planRepository = planRepository;
    }

    @GetMapping
    public List<CustomerResponse> getAllCustomers() {
        return repository.findAll().stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return repository.findById(id).map(customer -> ResponseEntity.ok(CustomerResponse.from(customer)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CustomerRequest request) {
        Plan plan = planRepository.findById(request.planId()).orElse(null);
        if (plan == null) {
            return ResponseEntity.badRequest().build();
        }
        Customer customer = new Customer(request.firstName(), request.lastName(), request.email(), request.phone(),
                plan);
        if (request.balance() != null) {
            customer.setBalance(request.balance());
        }
        return ResponseEntity.ok(CustomerResponse.from(repository.save(customer)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id,
            @RequestBody CustomerRequest request) {
        return repository.findById(id).map(customer -> {
            customer.setFirstName(request.firstName());
            customer.setLastName(request.lastName());
            customer.setEmail(request.email());
            customer.setPhone(request.phone());
            if (request.planId() != null) {
                planRepository.findById(request.planId()).ifPresent(customer::setPlan);
            }
            if (request.status() != null) {
                customer.setStatus(Customer.Status.valueOf(request.status()));
            }
            if (request.balance() != null) {
                customer.setBalance(request.balance());
            }
            return ResponseEntity.ok(CustomerResponse.from(repository.save(customer)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record CustomerRequest(String firstName, String lastName, String email, String phone, Long planId,
            String status, BigDecimal balance) {
    }

    public record CustomerResponse(Long id, String first_name, String last_name, String email, String phone,
            Long plan_id, String plan_name, String status, BigDecimal balance, String activated_at, String created_at) {
        public static CustomerResponse from(Customer customer) {
            return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName(),
                    customer.getEmail(), customer.getPhone(),
                    customer.getPlan() != null ? customer.getPlan().getId() : null,
                    customer.getPlan() != null ? customer.getPlan().getName() : null, customer.getStatus().name(),
                    customer.getBalance(),
                    customer.getActivatedAt() != null ? customer.getActivatedAt().toString() : null,
                    customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : null);
        }
    }
}
