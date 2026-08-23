package simulator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiTransactionEventWriter implements TransactionEventWriter {
    private final String apiUrl; // base URL, e.g. http://localhost:8080

    public ApiTransactionEventWriter(String apiUrl) {
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length()-1) : apiUrl;
    }

    @Override
    public void writeEvent(TransactionEvent e) throws IOException {
        URL u = new URL(apiUrl + "/events");
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        String json = toJson(e);
        byte[] out = json.getBytes("UTF-8");
        conn.setFixedLengthStreamingMode(out.length);
        conn.connect();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(out);
            os.flush();
        }
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("API returned HTTP " + code);
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String toJson(TransactionEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"transactionId\":\"").append(esc(e.transactionId)).append('\"');
        sb.append(",\"simulationRunId\":\"").append(esc(e.simulationRunId)).append('\"');
        sb.append(",\"customerId\":\"").append(esc(e.customerId)).append('\"');
        sb.append(",\"accountNumber\":\"").append(esc(e.accountNumber)).append('\"');
        sb.append(",\"accountType\":\"").append(esc(e.accountType)).append('\"');
        sb.append(",\"transactionType\":\"").append(esc(e.transactionType)).append('\"');
        sb.append(",\"amount\":\"").append(esc(e.amount)).append('\"');
        sb.append(",\"balanceBefore\":\"").append(esc(e.balanceBefore)).append('\"');
        sb.append(",\"balanceAfter\":\"").append(esc(e.balanceAfter)).append('\"');
        sb.append(",\"status\":\"").append(esc(e.status)).append('\"');
        sb.append(",\"failureReason\":\"").append(esc(e.failureReason)).append('\"');
        sb.append(",\"timestamp\":\"").append(esc(e.timestamp == null ? "" : e.timestamp.toString())).append('\"');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        // nothing to close
    }
}
