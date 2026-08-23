package com.example.banking.controller;

import com.example.banking.model.Account;
import com.example.banking.service.BankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountsController {

    private final BankService bankService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AccountsController(BankService bankService, JdbcTemplate jdbcTemplate) {
        this.bankService = bankService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<List<Account>> list() {
        return ResponseEntity.ok(bankService.listAccounts());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String owner = (String) body.getOrDefault("ownerName", "unknown");
        String type = (String) body.getOrDefault("accountType", "CHECKING");
        String id = (String) body.getOrDefault("id", UUID.randomUUID().toString());
        Account a = new Account(id, owner, BigDecimal.ZERO, type);
        bankService.createAccount(a);
        return ResponseEntity.accepted().body(a);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return bankService.getAccount(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<?> deposit(@PathVariable String id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.getOrDefault("amount", "0").toString());
        Account before = bankService.getAccount(id).orElse(null);
        boolean ok = bankService.deposit(id, amount);
        if (!ok) return ResponseEntity.badRequest().body(Map.of("error", "deposit failed"));
        recordEvent(id, "DEPOSIT", amount, before, "SUCCESS", "");
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable String id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.getOrDefault("amount", "0").toString());
        Account before = bankService.getAccount(id).orElse(null);
        boolean ok = bankService.withdraw(id, amount);
        if (!ok) return ResponseEntity.badRequest().body(Map.of("error", "withdraw failed"));
        recordEvent(id, "WITHDRAWAL", amount, before, "SUCCESS", "");
        return ResponseEntity.accepted().build();
    }

    private void recordEvent(String id, String type, BigDecimal amount, Account before, String status, String reason) {
        Account after = bankService.getAccount(id).orElse(before);
        String raw = String.format("{\"accountId\":\"%s\",\"type\":\"%s\",\"amount\":%s}", id, type, amount.toPlainString());
        jdbcTemplate.update("INSERT INTO simulation_transactions(run_id, ts, account_id, event_type, amount, status, raw_json) VALUES (?, datetime('now'), ?, ?, ?, ?, ?)",
                "manual", id, type, amount, status, raw);
    }
}
