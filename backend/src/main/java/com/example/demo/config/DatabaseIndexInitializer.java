package com.example.demo.config;

import com.example.demo.model.UsageRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + UsageRecord.CURRENT_CYCLE_INDEX
                + " ON usage_records (type, customer_id, recorded_at)");
    }
}
