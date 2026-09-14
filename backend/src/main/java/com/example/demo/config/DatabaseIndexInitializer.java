package com.example.demo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndexInitializer implements ApplicationRunner {

    public static final String CURRENT_CYCLE_INDEX = "idx_usage_records_type_customer_recorded_at";
    public static final String TICKET_DEADLINE_INDEX = "idx_support_tickets_deadline_filter";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + CURRENT_CYCLE_INDEX
                + " ON usage_records (type, customer_id, recorded_at)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + TICKET_DEADLINE_INDEX
                + " ON support_tickets (created_at DESC, status, acknowledged_at, resolved_at, priority, customer_id)");
    }
}
