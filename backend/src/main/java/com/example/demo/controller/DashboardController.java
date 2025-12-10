package com.example.demo.controller;

import com.example.demo.model.Customer;
import com.example.demo.model.Device;
import com.example.demo.model.SupportTicket;
import com.example.demo.repository.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final DeviceRepository deviceRepository;
    private final SupportTicketRepository ticketRepository;

    public DashboardController(CustomerRepository customerRepository, PlanRepository planRepository,
            DeviceRepository deviceRepository, SupportTicketRepository ticketRepository) {
        this.customerRepository = customerRepository;
        this.planRepository = planRepository;
        this.deviceRepository = deviceRepository;
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/stats")
    public DashboardStats getStats() {
        long activeCustomers = customerRepository.countByStatus(Customer.Status.ACTIVE);

        BigDecimal monthlyRevenue = customerRepository.findByStatus(Customer.Status.ACTIVE).stream()
                .filter(c -> c.getPlan() != null).map(c -> c.getPlan().getMonthlyPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long openTickets = ticketRepository
                .countByStatusIn(List.of(SupportTicket.Status.OPEN, SupportTicket.Status.IN_PROGRESS));

        long devicesInUse = deviceRepository.countByStatus(Device.Status.ASSIGNED);

        return new DashboardStats(activeCustomers, monthlyRevenue, openTickets, devicesInUse);
    }

    @GetMapping("/customers-by-plan")
    public List<ChartData> getCustomersByPlan() {
        return customerRepository.countByPlanName().stream()
                .map(row -> new ChartData((String) row[0], ((Number) row[1]).longValue())).toList();
    }

    @GetMapping("/devices-by-status")
    public List<ChartData> getDevicesByStatus() {
        return deviceRepository.countByStatus().stream()
                .map(row -> new ChartData(((Device.Status) row[0]).name(), ((Number) row[1]).longValue())).toList();
    }

    @GetMapping("/tickets-by-status")
    public List<ChartData> getTicketsByStatus() {
        return ticketRepository.countByStatus().stream()
                .map(row -> new ChartData(((SupportTicket.Status) row[0]).name(), ((Number) row[1]).longValue()))
                .toList();
    }

    @GetMapping("/revenue-by-plan")
    public List<RevenueData> getRevenueByPlan() {
        Map<String, Long> customerCounts = customerRepository.findByStatus(Customer.Status.ACTIVE).stream()
                .filter(c -> c.getPlan() != null)
                .collect(Collectors.groupingBy(c -> c.getPlan().getName(), Collectors.counting()));

        return planRepository.findAll().stream().filter(plan -> customerCounts.containsKey(plan.getName()))
                .map(plan -> new RevenueData(plan.getName(),
                        plan.getMonthlyPrice().multiply(BigDecimal.valueOf(customerCounts.get(plan.getName())))))
                .toList();
    }

    public record DashboardStats(long active_customers, BigDecimal monthly_revenue, long open_tickets,
            long devices_in_use) {
    }

    public record ChartData(String name, long value) {
    }

    public record RevenueData(String name, BigDecimal revenue) {
    }
}
