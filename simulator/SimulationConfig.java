package simulator;

public class SimulationConfig {
    public int numberOfUsers = 25;
    public int numberOfTransactions = 500;
    public int minDelayMs = 100;
    public int maxDelayMs = 1000;

    public SimulationConfig() {}

    public static SimulationConfig fromArgs(String[] args) {
        SimulationConfig cfg = new SimulationConfig();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--users") && i + 1 < args.length) {
                cfg.numberOfUsers = Integer.parseInt(args[++i]);
            } else if (a.equals("--transactions") && i + 1 < args.length) {
                cfg.numberOfTransactions = Integer.parseInt(args[++i]);
            } else if (a.equals("--minDelayMs") && i + 1 < args.length) {
                cfg.minDelayMs = Integer.parseInt(args[++i]);
            } else if (a.equals("--maxDelayMs") && i + 1 < args.length) {
                cfg.maxDelayMs = Integer.parseInt(args[++i]);
            }
        }
        return cfg;
    }
}
