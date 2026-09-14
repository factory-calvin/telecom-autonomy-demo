package com.example.demo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SupportTicketSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("acknowledged_at");
        addColumnIfMissing("priority_escalated_at");
    }

    private void addColumnIfMissing(String column) {
        boolean exists = jdbcTemplate.queryForList("PRAGMA table_info(support_tickets)").stream()
                .anyMatch(row -> column.equals(row.get("name")));
        if (!exists) {
            jdbcTemplate.execute("ALTER TABLE support_tickets ADD COLUMN " + column + " TIMESTAMP");
        }
    }
}
