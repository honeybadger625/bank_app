package simulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvTransactionEventWriter implements TransactionEventWriter {
    private final FileWriter fw;

    public CsvTransactionEventWriter(String path) throws IOException {
        File f = new File(path);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        boolean existed = f.exists();
        this.fw = new FileWriter(f, true);
        if (!existed) {
            fw.write("transaction_id,simulation_run_id,customer_id,account_number,account_type,transaction_type,amount,balance_before,balance_after,status,failure_reason,timestamp\n");
            fw.flush();
        }
    }

    @Override
    public void writeEvent(TransactionEvent e) throws IOException {
        fw.write(e.toCsvLine() + "\n");
        fw.flush();
    }

    @Override
    public void close() throws IOException {
        fw.close();
    }
}
