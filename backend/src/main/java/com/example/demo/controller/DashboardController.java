package com.example.demo.controller;

import com.example.demo.model.Customer;
import com.example.demo.model.Device;
import com.example.demo.model.SupportTicket;
import com.example.demo.repository.*;
import com.example.demo.service.DataUsageService;
import com.example.demo.service.DataUsageService.DataUsage;
import com.example.demo.service.DataUsageService.DataUsageState;
import com.example.demo.service.TicketDeadlineService;
import com.example.demo.service.TicketDeadlineService.DeadlineCounts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final DeviceRepository deviceRepository;
    private final SupportTicketRepository ticketRepository;
    private final DataUsageService dataUsageService;
    private final TicketDeadlineService ticketDeadlineService;

    public DashboardController(CustomerRepository customerRepository, PlanRepository planRepository,
            DeviceRepository deviceRepository, SupportTicketRepository ticketRepository,
            DataUsageService dataUsageService, TicketDeadlineService ticketDeadlineService) {
        this.customerRepository = customerRepository;
        this.planRepository = planRepository;
        this.deviceRepository = deviceRepository;
        this.ticketRepository = ticketRepository;
        this.dataUsageService = dataUsageService;
        this.ticketDeadlineService = ticketDeadlineService;
    }

    @GetMapping("/stats")
    public DashboardStats getStats(@RequestParam(required = false) Instant asOf) {
        log.info("Fetching dashboard stats");
        Instant effectiveAsOf = asOf != null ? asOf : ticketDeadlineService.now();
        long activeCustomers = customerRepository.countByStatus(Customer.Status.ACTIVE);

        BigDecimal monthlyRevenue = customerRepository.findByStatus(Customer.Status.ACTIVE).stream()
                .filter(c -> c.getPlan() != null).map(c -> c.getPlan().getMonthlyPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long openTickets = ticketRepository
                .countByStatusIn(List.of(SupportTicket.Status.OPEN, SupportTicket.Status.IN_PROGRESS));

        long devicesInUse = deviceRepository.countByStatus(Device.Status.ASSIGNED);

        Map<Long, DataUsage> usageByCustomer = dataUsageService.calculate(customerRepository.findAll());
        long atRiskCustomers = usageByCustomer.values().stream()
                .filter(usage -> usage.state() == DataUsageState.AT_RISK).count();
        long overLimitCustomers = usageByCustomer.values().stream()
                .filter(usage -> usage.state() == DataUsageState.OVER_LIMIT).count();
        DeadlineCounts deadlineCounts = ticketDeadlineService.countActive(
                ticketRepository.findByStatusIn(List.of(SupportTicket.Status.OPEN, SupportTicket.Status.IN_PROGRESS)),
                effectiveAsOf);

        log.debug(
                "Dashboard stats: activeCustomers={}, monthlyRevenue={}, openTickets={}, devicesInUse={}, acknowledgementOverdue={}, resolutionOverdue={}, dueSoon={}",
                activeCustomers, monthlyRevenue, openTickets, devicesInUse, deadlineCounts.acknowledgementOverdue(),
                deadlineCounts.resolutionOverdue(), deadlineCounts.dueSoon());
        return new DashboardStats(activeCustomers, monthlyRevenue, openTickets, devicesInUse, atRiskCustomers,
                overLimitCustomers, deadlineCounts.acknowledgementOverdue(), deadlineCounts.resolutionOverdue(),
                deadlineCounts.dueSoon(), effectiveAsOf.toString());
    }

    @GetMapping("/customers-by-plan")
    public List<ChartData> getCustomersByPlan() {
        log.info("Fetching customers by plan chart data");
        List<ChartData> data = customerRepository.countByPlanName().stream()
                .map(row -> new ChartData((String) row[0], ((Number) row[1]).longValue())).toList();
        log.debug("Found {} plan categories", data.size());
        return data;
    }

    @GetMapping("/devices-by-status")
    public List<ChartData> getDevicesByStatus() {
        log.info("Fetching devices by status chart data");
        List<ChartData> data = deviceRepository.countByStatus().stream()
                .map(row -> new ChartData(((Device.Status) row[0]).name(), ((Number) row[1]).longValue())).toList();
        log.debug("Found {} device status categories", data.size());
        return data;
    }

    @GetMapping("/tickets-by-status")
    public List<ChartData> getTicketsByStatus() {
        log.info("Fetching tickets by status chart data");
        List<ChartData> data = ticketRepository.countByStatus().stream()
                .map(row -> new ChartData(((SupportTicket.Status) row[0]).name(), ((Number) row[1]).longValue()))
                .toList();
        log.debug("Found {} ticket status categories", data.size());
        return data;
    }

    @GetMapping("/revenue-by-plan")
    public List<RevenueData> getRevenueByPlan() {
        log.info("Fetching revenue by plan chart data");
        Map<String, Long> customerCounts = customerRepository.findByStatus(Customer.Status.ACTIVE).stream()
                .filter(c -> c.getPlan() != null)
                .collect(Collectors.groupingBy(c -> c.getPlan().getName(), Collectors.counting()));

        List<RevenueData> data = planRepository.findAll().stream()
                .filter(plan -> customerCounts.containsKey(plan.getName()))
                .map(plan -> new RevenueData(plan.getName(),
                        plan.getMonthlyPrice().multiply(BigDecimal.valueOf(customerCounts.get(plan.getName())))))
                .toList();
        log.debug("Found {} plans with revenue data", data.size());
        return data;
    }

    public record DashboardStats(long active_customers, BigDecimal monthly_revenue, long open_tickets,
            long devices_in_use, long at_risk_customers, long over_limit_customers,
            long acknowledgement_overdue_tickets, long resolution_overdue_tickets, long due_soon_tickets,
            String as_of) {
    }

    public record ChartData(String name, long value) {
    }

    public record RevenueData(String name, BigDecimal revenue) {
    }
}
