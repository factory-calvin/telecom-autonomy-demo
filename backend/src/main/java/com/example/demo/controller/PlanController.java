package com.example.demo.controller;

import com.example.demo.model.Plan;
import com.example.demo.repository.PlanRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanRepository repository;

    public PlanController(PlanRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PlanResponse> getAllPlans() {
        return repository.findAll().stream().map(PlanResponse::from).toList();
    }

    @GetMapping("/active")
    public List<PlanResponse> getActivePlans() {
        return repository.findByIsActiveTrue().stream().map(PlanResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlan(@PathVariable Long id) {
        return repository.findById(id).map(plan -> ResponseEntity.ok(PlanResponse.from(plan)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public PlanResponse createPlan(@RequestBody PlanRequest request) {
        Plan plan = new Plan(request.name(), request.monthlyPrice(), request.dataLimitGb(), request.minutesLimit(),
                request.smsLimit());
        return PlanResponse.from(repository.save(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable Long id, @RequestBody PlanRequest request) {
        return repository.findById(id).map(plan -> {
            plan.setName(request.name());
            plan.setMonthlyPrice(request.monthlyPrice());
            plan.setDataLimitGb(request.dataLimitGb());
            plan.setMinutesLimit(request.minutesLimit());
            plan.setSmsLimit(request.smsLimit());
            if (request.isActive() != null) {
                plan.setIsActive(request.isActive());
            }
            return ResponseEntity.ok(PlanResponse.from(repository.save(plan)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record PlanRequest(String name, BigDecimal monthlyPrice, Integer dataLimitGb, Integer minutesLimit,
            Integer smsLimit, Boolean isActive) {
    }

    public record PlanResponse(Long id, String name, BigDecimal monthly_price, Integer data_limit_gb,
            Integer minutes_limit, Integer sms_limit, Boolean is_active) {
        public static PlanResponse from(Plan plan) {
            return new PlanResponse(plan.getId(), plan.getName(), plan.getMonthlyPrice(), plan.getDataLimitGb(),
                    plan.getMinutesLimit(), plan.getSmsLimit(), plan.getIsActive());
        }
    }
}
