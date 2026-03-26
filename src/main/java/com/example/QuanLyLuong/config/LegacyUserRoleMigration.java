package com.example.QuanLyLuong.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LegacyUserRoleMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.update("update users set role = 'ROLE_SYSTEM_ADMIN' where role = 'ROLE_ADMIN'");
        } catch (DataAccessException ignored) {
            // Fresh schema with the new enum no longer accepts ROLE_ADMIN in the predicate.
        }
    }
}