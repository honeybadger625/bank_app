package com.example.banking.service;

import com.example.banking.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class BankService {

    private final JdbcTemplate jdbcTemplate;
    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    @Autowired
    public BankService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Account> mapper = new RowMapper<>() {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Account(
                    rs.getString("id"),
                    rs.getString("owner_name"),
                    rs.getBigDecimal("balance"),
                    rs.getString("account_type")
            );
        }
    };

    public List<Account> listAccounts() {
        rw.readLock().lock();
        try {
            return jdbcTemplate.query("SELECT id, owner_name, balance, account_type FROM accounts", mapper);
        } finally {
            rw.readLock().unlock();
        }
    }

    public Optional<Account> getAccount(String id) {
        rw.readLock().lock();
        try {
            List<Account> list = jdbcTemplate.query("SELECT id, owner_name, balance, account_type FROM accounts WHERE id = ?", new Object[]{id}, mapper);
            if (list.isEmpty()) return Optional.empty();
            return Optional.of(list.get(0));
        } finally {
            rw.readLock().unlock();
        }
    }

    public void createAccount(Account account) {
        rw.writeLock().lock();
        try {
            jdbcTemplate.update("INSERT INTO accounts(id, owner_name, balance, account_type) VALUES (?, ?, ?, ?)",
                    account.getId(), account.getOwnerName(), account.getBalance(), account.getAccountType());
        } finally {
            rw.writeLock().unlock();
        }
    }

    public boolean deposit(String id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        rw.writeLock().lock();
        try {
            int updated = jdbcTemplate.update("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, id);
            return updated > 0;
        } finally {
            rw.writeLock().unlock();
        }
    }

    public boolean withdraw(String id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        rw.writeLock().lock();
        try {
            // ensure sufficient funds
            BigDecimal bal = jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE id = ?", new Object[]{id}, BigDecimal.class);
            if (bal == null || bal.compareTo(amount) < 0) return false;
            int updated = jdbcTemplate.update("UPDATE accounts SET balance = balance - ? WHERE id = ?", amount, id);
            return updated > 0;
        } finally {
            rw.writeLock().unlock();
        }
    }
}
