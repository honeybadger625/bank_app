package com.example.banking.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final JdbcTemplate jdbcTemplate;

    public TransactionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String account,
            @RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        if (account == null || account.isBlank()) {
            return jdbcTemplate.queryForList("SELECT id, run_id, ts, account_id, event_type, amount, status, raw_json FROM simulation_transactions ORDER BY id DESC LIMIT ?", safeLimit);
        }
        return jdbcTemplate.queryForList("SELECT id, run_id, ts, account_id, event_type, amount, status, raw_json FROM simulation_transactions WHERE account_id = ? ORDER BY id DESC LIMIT ?", account, safeLimit);
    }
}