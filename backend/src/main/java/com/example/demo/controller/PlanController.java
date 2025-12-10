package com.example.demo.controller;

import com.example.demo.model.Plan;
import com.example.demo.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);

    private final PlanRepository repository;

    public PlanController(PlanRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PlanResponse> getAllPlans() {
        log.info("Fetching all plans");
        List<PlanResponse> plans = repository.findAll().stream().map(PlanResponse::from).toList();
        log.debug("Found {} plans", plans.size());
        return plans;
    }

    @GetMapping("/active")
    public List<PlanResponse> getActivePlans() {
        log.info("Fetching active plans");
        List<PlanResponse> plans = repository.findByIsActiveTrue().stream().map(PlanResponse::from).toList();
        log.debug("Found {} active plans", plans.size());
        return plans;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlan(@PathVariable Long id) {
        log.info("Fetching plan with id={}", id);
        return repository.findById(id).map(plan -> {
            log.debug("Found plan: {}", plan.getName());
            return ResponseEntity.ok(PlanResponse.from(plan));
        }).orElseGet(() -> {
            log.warn("Plan not found: id={}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @PostMapping
    public PlanResponse createPlan(@RequestBody PlanRequest request) {
        log.info("Creating plan: name={}", request.name());
        Plan plan = new Plan(request.name(), request.monthlyPrice(), request.dataLimitGb(), request.minutesLimit(),
                request.smsLimit());
        Plan saved = repository.save(plan);
        log.info("Plan created: id={}, name={}", saved.getId(), saved.getName());
        return PlanResponse.from(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable Long id, @RequestBody PlanRequest request) {
        log.info("Updating plan: id={}", id);
        return repository.findById(id).map(plan -> {
            plan.setName(request.name());
            plan.setMonthlyPrice(request.monthlyPrice());
            plan.setDataLimitGb(request.dataLimitGb());
            plan.setMinutesLimit(request.minutesLimit());
            plan.setSmsLimit(request.smsLimit());
            if (request.isActive() != null) {
                plan.setIsActive(request.isActive());
            }
            Plan saved = repository.save(plan);
            log.info("Plan updated: id={}, name={}", saved.getId(), saved.getName());
            return ResponseEntity.ok(PlanResponse.from(saved));
        }).orElseGet(() -> {
            log.warn("Plan not found for update: id={}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        log.info("Deleting plan: id={}", id);
        if (!repository.existsById(id)) {
            log.warn("Plan not found for deletion: id={}", id);
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        log.info("Plan deleted: id={}", id);
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
