package com.example.demo;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private final PlanRepository planRepository;
    private final CustomerRepository customerRepository;
    private final DeviceRepository deviceRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final SupportTicketRepository ticketRepository;
    private final Random random = new Random(42);

    public DataSeeder(PlanRepository planRepository, CustomerRepository customerRepository,
            DeviceRepository deviceRepository, UsageRecordRepository usageRecordRepository,
            SupportTicketRepository ticketRepository) {
        this.planRepository = planRepository;
        this.customerRepository = customerRepository;
        this.deviceRepository = deviceRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void run(String... args) {
        // Skip if data already exists (e.g., seeded by Python script)
        if (planRepository.count() > 0) {
            System.out.println("Database already seeded, skipping Java DataSeeder");
            return;
        }

        System.out.println("No existing data found, running Java DataSeeder...");
        List<Plan> plans = createPlans();
        List<Customer> customers = createCustomers(plans);
        List<Device> devices = createDevices(customers);
        createUsageRecords(customers);
        createSupportTickets(customers);
    }

    private List<Plan> createPlans() {
        List<Plan> plans = List.of(new Plan("Basic", new BigDecimal("29.99"), 5, 500, 500),
                new Plan("Standard", new BigDecimal("49.99"), 15, 1000, null),
                new Plan("Premium", new BigDecimal("69.99"), 30, null, null),
                new Plan("Unlimited", new BigDecimal("89.99"), null, null, null),
                new Plan("Family Share", new BigDecimal("119.99"), 50, null, null));
        return planRepository.saveAll(plans);
    }

    private List<Customer> createCustomers(List<Plan> plans) {
        String[][] customerData = {{"Sarah", "Johnson", "sarah.johnson@email.com", "+1-555-0101"},
                {"Michael", "Chen", "michael.chen@email.com", "+1-555-0102"},
                {"Emily", "Rodriguez", "emily.rodriguez@email.com", "+1-555-0103"},
                {"James", "Williams", "james.williams@email.com", "+1-555-0104"},
                {"Jessica", "Brown", "jessica.brown@email.com", "+1-555-0105"},
                {"David", "Martinez", "david.martinez@email.com", "+1-555-0106"},
                {"Ashley", "Garcia", "ashley.garcia@email.com", "+1-555-0107"},
                {"Christopher", "Lee", "christopher.lee@email.com", "+1-555-0108"},
                {"Amanda", "Wilson", "amanda.wilson@email.com", "+1-555-0109"},
                {"Matthew", "Anderson", "matthew.anderson@email.com", "+1-555-0110"},
                {"Stephanie", "Taylor", "stephanie.taylor@email.com", "+1-555-0111"},
                {"Daniel", "Thomas", "daniel.thomas@email.com", "+1-555-0112"},
                {"Nicole", "Moore", "nicole.moore@email.com", "+1-555-0113"},
                {"Andrew", "Jackson", "andrew.jackson@email.com", "+1-555-0114"},
                {"Jennifer", "White", "jennifer.white@email.com", "+1-555-0115"},
                {"Joshua", "Harris", "joshua.harris@email.com", "+1-555-0116"},
                {"Elizabeth", "Clark", "elizabeth.clark@email.com", "+1-555-0117"},
                {"Ryan", "Lewis", "ryan.lewis@email.com", "+1-555-0118"},
                {"Megan", "Robinson", "megan.robinson@email.com", "+1-555-0119"},
                {"Brandon", "Walker", "brandon.walker@email.com", "+1-555-0120"},
                {"Lauren", "Hall", "lauren.hall@email.com", "+1-555-0121"},
                {"Justin", "Allen", "justin.allen@email.com", "+1-555-0122"},
                {"Samantha", "Young", "samantha.young@email.com", "+1-555-0123"},
                {"Kevin", "King", "kevin.king@email.com", "+1-555-0124"},
                {"Rachel", "Wright", "rachel.wright@email.com", "+1-555-0125"}};

        List<Customer> customers = new java.util.ArrayList<>();
        Customer.Status[] statuses = {Customer.Status.ACTIVE, Customer.Status.ACTIVE, Customer.Status.ACTIVE,
                Customer.Status.ACTIVE, Customer.Status.SUSPENDED, Customer.Status.CANCELLED};

        for (int i = 0; i < customerData.length; i++) {
            String[] data = customerData[i];
            Customer customer = new Customer(data[0], data[1], data[2], data[3], plans.get(i % plans.size()));
            customer.setStatus(statuses[i % statuses.length]);
            customer.setBalance(new BigDecimal(random.nextInt(100)).setScale(2));
            customer.setCreatedAt(Instant.now().minus(random.nextInt(180), ChronoUnit.DAYS));
            customers.add(customer);
        }

        return customerRepository.saveAll(customers);
    }

    private List<Device> createDevices(List<Customer> customers) {
        String[] models = {"iPhone 15 Pro", "iPhone 15", "Samsung Galaxy S24", "Samsung Galaxy A54", "Google Pixel 8",
                "OnePlus 12", "Motorola Edge", "iPhone 14"};

        List<Device> devices = new java.util.ArrayList<>();
        List<Customer> activeCustomers = customers.stream().filter(c -> c.getStatus() == Customer.Status.ACTIVE)
                .toList();

        for (int i = 0; i < 35; i++) {
            String imei = String.format("35%013d", 1000000000000L + i);
            String simNumber = String.format("8901%016d", 1000000000000000L + i);
            Device device = new Device(imei, models[i % models.length], simNumber);

            if (i < activeCustomers.size()) {
                device.setCustomer(activeCustomers.get(i));
                device.setStatus(Device.Status.ASSIGNED);
                device.setAssignedAt(Instant.now().minus(random.nextInt(90), ChronoUnit.DAYS));
            } else if (i < 30) {
                device.setStatus(Device.Status.AVAILABLE);
            } else if (i < 33) {
                device.setStatus(Device.Status.DAMAGED);
            } else {
                device.setStatus(Device.Status.LOST);
            }
            devices.add(device);
        }

        return deviceRepository.saveAll(devices);
    }

    private void createUsageRecords(List<Customer> customers) {
        List<Customer> activeCustomers = customers.stream().filter(c -> c.getStatus() == Customer.Status.ACTIVE)
                .toList();

        List<UsageRecord> records = new java.util.ArrayList<>();
        UsageRecord.Type[] types = UsageRecord.Type.values();

        for (int day = 0; day < 30; day++) {
            Instant recordDate = Instant.now().minus(day, ChronoUnit.DAYS);

            for (Customer customer : activeCustomers) {
                for (UsageRecord.Type type : types) {
                    if (random.nextDouble() > 0.3) {
                        BigDecimal quantity;
                        BigDecimal cost;

                        switch (type) {
                            case CALL -> {
                                quantity = new BigDecimal(random.nextInt(60) + 1);
                                cost = quantity.multiply(new BigDecimal("0.05"));
                            }
                            case DATA -> {
                                quantity = new BigDecimal(random.nextInt(500) + 50);
                                cost = quantity.multiply(new BigDecimal("0.01"));
                            }
                            case SMS -> {
                                quantity = new BigDecimal(random.nextInt(20) + 1);
                                cost = quantity.multiply(new BigDecimal("0.02"));
                            }
                            default -> {
                                quantity = BigDecimal.ONE;
                                cost = BigDecimal.ZERO;
                            }
                        }

                        records.add(new UsageRecord(customer, type, quantity,
                                cost.setScale(2, java.math.RoundingMode.HALF_UP), recordDate));
                    }
                }
            }
        }

        usageRecordRepository.saveAll(records);
    }

    private void createSupportTickets(List<Customer> customers) {
        String[][] ticketData = {{"Billing inquiry", "I have a question about my last bill"},
                {"Service outage", "Cannot make calls in my area"},
                {"Plan upgrade request", "Would like to upgrade to unlimited plan"},
                {"Device issue", "Phone screen is cracked"},
                {"International roaming", "Need to enable international roaming"},
                {"Data usage concern", "Data seems to be used faster than expected"},
                {"Account security", "Need to update my security settings"},
                {"New SIM request", "Lost my SIM card, need replacement"},
                {"Voicemail setup", "Help setting up voicemail"}, {"Payment issue", "Payment failed, need assistance"},
                {"Coverage question", "Checking coverage in rural area"},
                {"Family plan setup", "Adding family members to plan"},
                {"Contract renewal", "Questions about contract renewal"},
                {"Number transfer", "Want to transfer number from another carrier"},
                {"App not working", "FactoryFone app keeps crashing"}};

        SupportTicket.Priority[] priorities = SupportTicket.Priority.values();
        SupportTicket.Status[] statuses = SupportTicket.Status.values();

        List<Customer> activeCustomers = customers.stream().filter(c -> c.getStatus() == Customer.Status.ACTIVE)
                .toList();

        List<SupportTicket> tickets = new java.util.ArrayList<>();

        for (int i = 0; i < ticketData.length; i++) {
            Customer customer = activeCustomers.get(i % activeCustomers.size());
            SupportTicket ticket = new SupportTicket(customer, ticketData[i][0], ticketData[i][1],
                    priorities[i % priorities.length]);
            ticket.setStatus(statuses[i % statuses.length]);
            ticket.setCreatedAt(Instant.now().minus(random.nextInt(14), ChronoUnit.DAYS));

            if (ticket.getStatus() == SupportTicket.Status.RESOLVED
                    || ticket.getStatus() == SupportTicket.Status.CLOSED) {
                ticket.setResolvedAt(Instant.now().minus(random.nextInt(3), ChronoUnit.DAYS));
            }

            tickets.add(ticket);
        }

        ticketRepository.saveAll(tickets);
    }
}
