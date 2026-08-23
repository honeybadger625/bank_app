package com.example.banking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/events")
public class EventController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping
    public ResponseEntity<?> ingest(@RequestBody Map<String, Object> event) {
        // Minimal insert into simulation_transactions created by Flyway
        try {
            String sql = "INSERT INTO simulation_transactions(run_id, ts, account_id, event_type, amount, status, raw_json) VALUES (?, datetime('now'), ?, ?, ?, ?, ?)";
            Object runId = event.getOrDefault("runId", "cli");
            Object accountId = event.getOrDefault("accountId", null);
            Object type = event.getOrDefault("type", "UNKNOWN");
            Object amount = event.getOrDefault("amount", null);
            Object status = event.getOrDefault("status", "OK");
            String raw = event.toString();
            jdbcTemplate.update(sql, runId, accountId, type, amount, status, raw);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
