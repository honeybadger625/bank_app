package simulator;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class SqliteTransactionEventWriter implements TransactionEventWriter {
    private final Connection conn;
    private final PreparedStatement insertStmt;

    public SqliteTransactionEventWriter(String dbPath) throws IOException {
        try {
            File f = new File(dbPath);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            String url = "jdbc:sqlite:" + dbPath;
            this.conn = DriverManager.getConnection(url);
            this.conn.setAutoCommit(false);
            createTableIfNotExists();
            this.insertStmt = conn.prepareStatement(
                "INSERT INTO simulation_transactions(" +
                "transaction_id,simulation_run_id,customer_id,account_number,account_type," +
                "transaction_type,amount,balance_before,balance_after,status,failure_reason,timestamp) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)"
            );
        } catch (SQLException ex) {
            throw new IOException("Failed to open SQLite DB", ex);
        }
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS simulation_transactions (" +
            "transaction_id TEXT PRIMARY KEY," +
            "simulation_run_id TEXT," +
            "customer_id TEXT," +
            "account_number TEXT," +
            "account_type TEXT," +
            "transaction_type TEXT," +
            "amount TEXT," +
            "balance_before TEXT," +
            "balance_after TEXT," +
            "status TEXT," +
            "failure_reason TEXT," +
            "timestamp TEXT" +
            ")";
        try (Statement s = conn.createStatement()) {
            s.execute(sql);
            conn.commit();
        }
    }

    @Override
    public void writeEvent(TransactionEvent e) throws IOException {
        try {
            insertStmt.setString(1, e.transactionId);
            insertStmt.setString(2, e.simulationRunId);
            insertStmt.setString(3, e.customerId);
            insertStmt.setString(4, e.accountNumber);
            insertStmt.setString(5, e.accountType);
            insertStmt.setString(6, e.transactionType);
            insertStmt.setString(7, e.amount);
            insertStmt.setString(8, e.balanceBefore);
            insertStmt.setString(9, e.balanceAfter);
            insertStmt.setString(10, e.status);
            insertStmt.setString(11, e.failureReason == null ? "" : e.failureReason);
            insertStmt.setString(12, e.timestamp == null ? "" : e.timestamp.toString());
            insertStmt.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            throw new IOException("Failed to insert event", ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (insertStmt != null) insertStmt.close();
            if (conn != null) conn.close();
        } catch (SQLException ex) {
            throw new IOException("Failed to close DB", ex);
        }
    }
}
