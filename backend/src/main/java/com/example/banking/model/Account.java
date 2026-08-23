package com.example.banking.model;

import java.math.BigDecimal;

public class Account {
    private String id;
    private String ownerName;
    private BigDecimal balance;
    private String accountType;

    public Account() {}

    public Account(String id, String ownerName, BigDecimal balance, String accountType) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance == null ? BigDecimal.ZERO : balance;
        this.accountType = accountType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
}
