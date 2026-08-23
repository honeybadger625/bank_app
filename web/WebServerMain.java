package web;

import simulator.CsvTransactionEventWriter;
import simulator.SqliteTransactionEventWriter;
import simulator.TransactionEventWriter;

public class WebServerMain {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String db = null;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) port = Integer.parseInt(args[++i]);
            else if ("--db".equals(args[i]) && i + 1 < args.length) db = args[++i];
        }

        TransactionEventWriter writer;
        if (db != null) {
            writer = new SqliteTransactionEventWriter(db);
            System.out.println("Using SQLite DB: " + db);
        } else {
            writer = new CsvTransactionEventWriter("simulation_output/simulation_transactions.csv");
            System.out.println("Using CSV output");
        }

        WebServer ws = new WebServer(port, writer);
        ws.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { writer.close(); } catch (Exception e) {}
            ws.stop(1);
        }));
    }
}
