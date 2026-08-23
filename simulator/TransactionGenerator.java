package simulator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.io.IOException;

import Bank.*;
import Data.FileIO;
import Exceptions.*;

public class TransactionGenerator {
    private final SimulationConfig cfg;
    private final List<SimulatedUser> users;
    private final TransactionEventWriter writer;
    private final Random rnd = new Random();

    public TransactionGenerator(SimulationConfig cfg, List<SimulatedUser> users, TransactionEventWriter writer) {
        this.cfg = cfg;
        this.users = users;
        this.writer = writer;
    }

    public void run(String simulationRunId) throws IOException {
        // Bank state should already be loaded by caller; do not reload here
        int successes = 0;
        int failures = 0;
        for (int i = 0; i < cfg.numberOfTransactions; i++) {
            SimulatedUser u = users.get(rnd.nextInt(users.size()));
            String txnType = pickTxnType(u.profile);
            BigDecimal amount = pickAmount(u.profile, txnType);
            TransactionEvent ev = new TransactionEvent();
            ev.transactionId = "TXN-" + UUID.randomUUID().toString();
            ev.simulationRunId = simulationRunId;
            ev.customerId = u.customerId;
            ev.accountNumber = u.accountNumber;
            ev.accountType = u.accountType;
            ev.transactionType = txnType;
            ev.amount = amount.toPlainString();
            ev.timestamp = LocalDateTime.now();

            // balance before/after using Bank API
            BankAccount acc = FileIO.bank.findAccount(u.accountNumber);
            if (acc == null) {
                ev.status = "FAILED";
                ev.failureReason = "ACCOUNT_NOT_FOUND";
                ev.balanceBefore = "";
                ev.balanceAfter = "";
                writer.writeEvent(ev);
                failures++;
            } else {
                BigDecimal before = BigDecimal.valueOf(acc.getbalance());
                ev.balanceBefore = before.toPlainString();
                try {
                    if (txnType.equals("DEPOSIT")) {
                        FileIO.bank.deposit(u.accountNumber, amount.doubleValue());
                    } else if (txnType.equals("WITHDRAWAL")) {
                        FileIO.bank.withdraw(u.accountNumber, amount.doubleValue());
                    } else if (txnType.equals("BALANCE_CHECK")) {
                        // no-op
                    }
                    BankAccount accAfter = FileIO.bank.findAccount(u.accountNumber);
                    BigDecimal after = BigDecimal.valueOf(accAfter.getbalance());
                    ev.balanceAfter = after.toPlainString();
                    ev.status = "SUCCESS";
                    ev.failureReason = "";
                    writer.writeEvent(ev);
                    successes++;
                } catch (InvalidAmount ia) {
                    ev.status = "FAILED";
                    ev.failureReason = "INVALID_AMOUNT";
                    ev.balanceAfter = ev.balanceBefore;
                    writer.writeEvent(ev);
                    failures++;
                } catch (AccNotFound anf) {
                    ev.status = "FAILED";
                    ev.failureReason = "ACCOUNT_NOT_FOUND";
                    ev.balanceAfter = ev.balanceBefore;
                    writer.writeEvent(ev);
                    failures++;
                } catch (MaxBalance mb) {
                    ev.status = "FAILED";
                    ev.failureReason = "INSUFFICIENT_FUNDS";
                    ev.balanceAfter = ev.balanceBefore;
                    writer.writeEvent(ev);
                    failures++;
                } catch (MaxWithdraw mw) {
                    ev.status = "FAILED";
                    ev.failureReason = "MAX_WITHDRAW_LIMIT";
                    ev.balanceAfter = ev.balanceBefore;
                    writer.writeEvent(ev);
                    failures++;
                } catch (Exception ex) {
                    ev.status = "FAILED";
                    ev.failureReason = "UNKNOWN_ERROR";
                    ev.balanceAfter = ev.balanceBefore;
                    writer.writeEvent(ev);
                    failures++;
                }
            }

            // delay
            try { Thread.sleep(cfg.minDelayMs + rnd.nextInt(Math.max(1, cfg.maxDelayMs - cfg.minDelayMs + 1))); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        writer.close();

        System.out.println("Simulation Complete");
        System.out.println("Successful Transactions: " + successes);
        System.out.println("Failed Transactions: " + failures);
        System.out.println("Total Events: " + cfg.numberOfTransactions);
    }

    private String pickTxnType(CustomerProfile p) {
        int r = rnd.nextInt(100);
        switch (p) {
            case STUDENT:
                if (r < 60) return "WITHDRAWAL";
                if (r < 85) return "DEPOSIT";
                return "BALANCE_CHECK";
            case PROFESSIONAL:
                if (r < 50) return "DEPOSIT";
                if (r < 85) return "WITHDRAWAL";
                return "BALANCE_CHECK";
            case FAMILY:
                if (r < 40) return "WITHDRAWAL";
                if (r < 80) return "DEPOSIT";
                return "BALANCE_CHECK";
            case RETIREE:
                if (r < 20) return "WITHDRAWAL";
                if (r < 60) return "DEPOSIT";
                return "BALANCE_CHECK";
            default:
                return "BALANCE_CHECK";
        }
    }

    private BigDecimal pickAmount(CustomerProfile p, String txnType) {
        // Use BigDecimal for precision, then convert for bank calls
        switch (p) {
            case STUDENT:
                if (txnType.equals("DEPOSIT")) return BigDecimal.valueOf(20 + rnd.nextInt(200));
                return BigDecimal.valueOf(1 + rnd.nextInt(100));
            case PROFESSIONAL:
                if (txnType.equals("DEPOSIT")) return BigDecimal.valueOf(500 + rnd.nextInt(5000));
                return BigDecimal.valueOf(20 + rnd.nextInt(500));
            case FAMILY:
                if (txnType.equals("DEPOSIT")) return BigDecimal.valueOf(200 + rnd.nextInt(2000));
                return BigDecimal.valueOf(10 + rnd.nextInt(800));
            case RETIREE:
                if (txnType.equals("DEPOSIT")) return BigDecimal.valueOf(100 + rnd.nextInt(1000));
                return BigDecimal.valueOf(5 + rnd.nextInt(300));
            default:
                return BigDecimal.valueOf(10);
        }
    }
}
