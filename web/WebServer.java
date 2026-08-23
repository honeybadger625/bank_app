package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import simulator.*;
import Data.FileIO;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class WebServer {
    private final HttpServer server;
    private final TransactionEventWriter writer;

    public WebServer(int port, TransactionEventWriter writer) throws IOException {
        this.writer = writer;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", new HealthHandler());
        server.createContext("/events", new EventsHandler());
        server.createContext("/", new RootHandler());
        server.createContext("/simulate/start", new SimulateHandler());
        server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
        System.out.println("WebServer started on " + server.getAddress());
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] resp = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    class EventsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            // Expect JSON created by ApiTransactionEventWriter
            TransactionEvent e = parseJsonToEvent(body);
            try {
                writer.writeEvent(e);
                exchange.sendResponseHeaders(201, -1);
            } catch (IOException ex) {
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }

    class SimulateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            // Start an embedded simulator run in a background thread
            new Thread(() -> {
                try {
                    SimulationConfig cfg = SimulationConfig.fromArgs(new String[]{});
                    FileIO.Read();
                    java.util.List<SimulatedUser> users = simulator.SimulatorMain.createUsers(cfg.numberOfUsers);
                    TransactionGenerator gen = new TransactionGenerator(cfg, users, writer);
                    gen.run("WEB-SIM-" + System.currentTimeMillis());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            exchange.sendResponseHeaders(202, -1);
        }
    }

    class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Banking Simulator</title></head><body>" +
                    "<h1>Banking Simulator</h1>" +
                    "<p><a href=\"/health\">Health</a></p>" +
                    "<p><button id=\"start\">Start Embedded Simulation</button></p>" +
                    "<pre id=\"out\"></pre>" +
                    "<script>document.getElementById('start').onclick = async () => {fetch('/simulate/start',{method:'POST'});document.getElementById('out').textContent='Simulation started';};</script>" +
                    "</body></html>";
            byte[] resp = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    private TransactionEvent parseJsonToEvent(String body) {
        TransactionEvent e = new TransactionEvent();
        // Very small and forgiving parser; expects simple flat JSON with string values
        String s = body.trim();
        s = s.replaceAll("^[\\{\\s]*|[\\}\\s]*$", "");
        String[] parts = s.split(",(?=\\\")");
        for (String p : parts) {
            String[] kv = p.split(":", 2);
            if (kv.length < 2) continue;
            String k = kv[0].trim().replaceAll("\"", "");
            String v = kv[1].trim();
            if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length()-1);
            v = v.replaceAll("\\\\\"", "\"").replaceAll("\\\\", "\\");
            switch (k) {
                case "transactionId": e.transactionId = v; break;
                case "simulationRunId": e.simulationRunId = v; break;
                case "customerId": e.customerId = v; break;
                case "accountNumber": e.accountNumber = v; break;
                case "accountType": e.accountType = v; break;
                case "transactionType": e.transactionType = v; break;
                case "amount": e.amount = v; break;
                case "balanceBefore": e.balanceBefore = v; break;
                case "balanceAfter": e.balanceAfter = v; break;
                case "status": e.status = v; break;
                case "failureReason": e.failureReason = v; break;
                case "timestamp": e.timestamp = java.time.LocalDateTime.parse(v); break;
            }
        }
        return e;
    }
}
