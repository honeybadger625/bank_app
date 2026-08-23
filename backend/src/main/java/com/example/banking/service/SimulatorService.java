package com.example.banking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class SimulatorService {

    private final BankService bankService;
    private final JdbcTemplate jdbcTemplate;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, String> runStatus = new ConcurrentHashMap<>();

    @Autowired
    public SimulatorService(BankService bankService, JdbcTemplate jdbcTemplate) {
        this.bankService = bankService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public String startSimulation(int transactions, int minDelayMs, int maxDelayMs) {
        String runId = UUID.randomUUID().toString();
        runStatus.put(runId, "RUNNING");
        executor.submit(() -> {
            Random rnd = new Random();
            List<com.example.banking.model.Account> accounts = bankService.listAccounts();
            int successes = 0, failures = 0;
            for (int i = 0; i < transactions; i++) {
                if (accounts.isEmpty()) break;
                com.example.banking.model.Account a = accounts.get(rnd.nextInt(accounts.size()));
                boolean deposit = rnd.nextBoolean();
                BigDecimal amount = BigDecimal.valueOf(1 + rnd.nextInt(200));
                boolean ok;
                String type;
                if (deposit) {
                    ok = bankService.deposit(a.getId(), amount);
                    type = "DEPOSIT";
                } else {
                    ok = bankService.withdraw(a.getId(), amount);
                    type = "WITHDRAWAL";
                }
                if (ok) successes++; else failures++;
                // write event
                try {
                    String sql = "INSERT INTO simulation_transactions(run_id, ts, account_id, event_type, amount, status, raw_json) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    String status = ok ? "SUCCESS" : "FAILED";
                    String raw = String.format("{\"accountId\":\"%s\",\"type\":\"%s\",\"amount\":%s}", a.getId(), type, amount.toPlainString());
                    jdbcTemplate.update(sql, runId, LocalDateTime.now().toString(), a.getId(), type, amount, status, raw);
                } catch (Exception e) {
                    // swallow
                }

                try { Thread.sleep(minDelayMs + rnd.nextInt(Math.max(1, maxDelayMs - minDelayMs + 1))); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
            runStatus.put(runId, "COMPLETED: success=" + successes + ",fail=" + failures);
        });
        return runId;
    }

    public String getStatus(String runId) {
        return runStatus.getOrDefault(runId, "UNKNOWN");
    }
}
