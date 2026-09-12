package com.example.demo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndexInitializer implements ApplicationRunner {

    public static final String CURRENT_CYCLE_INDEX = "idx_usage_records_type_customer_recorded_at";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + CURRENT_CYCLE_INDEX
                + " ON usage_records (type, customer_id, recorded_at)");
    }
}
