package simulator;

import java.time.LocalDateTime;

public class TransactionEvent {
    public String transactionId;
    public String simulationRunId;
    public String customerId;
    public String accountNumber;
    public String accountType;
    public String transactionType;
    public String amount; // keep as string for precision
    public String balanceBefore;
    public String balanceAfter;
    public LocalDateTime timestamp;
    public String status;
    public String failureReason;

    public String toCsvLine() {
        return String.join(",",
                transactionId,
                simulationRunId,
                customerId,
                accountNumber,
                accountType == null ? "" : accountType,
                transactionType,
                amount,
                balanceBefore,
                balanceAfter,
                status,
                failureReason == null ? "" : failureReason,
                timestamp.toString()
        );
    }
}
