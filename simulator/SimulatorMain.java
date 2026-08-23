package simulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Bank.*;
import Data.FileIO;

public class SimulatorMain {
    public static void main(String[] args) throws Exception {
        SimulationConfig cfg = SimulationConfig.fromArgs(args);
        String apiUrl = null;
        String dbPath = null;
        boolean persistBank = false;
        for (int i = 0; i < args.length; i++) {
            if ("--api".equals(args[i]) && i + 1 < args.length) apiUrl = args[++i];
            else if ("--db".equals(args[i]) && i + 1 < args.length) dbPath = args[++i];
            else if ("--persistBank".equals(args[i])) persistBank = true;
        }
        System.out.println("====================================");
        System.out.println("Bank Customer Simulator");
        System.out.println("====================================");
        String simulationRunId = "SIM-" + java.time.LocalDate.now().toString().replaceAll("-", "") + "-" + (int)(Math.random()*1000);

        System.out.println();
        System.out.println("Simulation Run: " + simulationRunId);
        System.out.println("Customers: " + cfg.numberOfUsers);
        System.out.println("Transactions Requested: " + cfg.numberOfTransactions);
        System.out.println();

        // Ensure bank state loaded
        FileIO.Read();

        List<SimulatedUser> users = createUsers(cfg.numberOfUsers);

        TransactionEventWriter writer = null;
        try {
            if (apiUrl != null) {
                writer = new ApiTransactionEventWriter(apiUrl);
                System.out.println("Using API writer -> " + apiUrl);
            } else if (dbPath != null) {
                writer = new SqliteTransactionEventWriter(dbPath);
                System.out.println("Using SQLite DB -> " + dbPath);
            } else {
                writer = new CsvTransactionEventWriter("simulation_output/simulation_transactions.csv");
                System.out.println("Using CSV writer -> simulation_output/simulation_transactions.csv");
            }

            TransactionGenerator gen = new TransactionGenerator(cfg, users, writer);
            System.out.println("Starting simulation...");
            gen.run(simulationRunId);
        } finally {
            if (writer != null) writer.close();
        }

        if (persistBank) {
            try { FileIO.Write(); System.out.println("Bank state persisted."); }
            catch (Exception e) { System.err.println("Failed to persist bank: " + e.getMessage()); }
        }
    }

    public static List<SimulatedUser> createUsers(int n) {
        List<SimulatedUser> users = new ArrayList<>();
        String[] first = {"Alex","Jamie","Taylor","Jordan","Casey","Riley","Morgan","Avery","Parker","Quinn"};
        String[] last = {"Smith","Johnson","Brown","Taylor","Anderson","Thomas","Jackson","White","Harris","Martin"};
        Random rnd = new Random();
        CustomerProfile[] profiles = CustomerProfile.values();

        for (int i = 0; i < n; i++) {
            String cid = String.format("CUST-%03d", i+1);
            String fn = first[rnd.nextInt(first.length)];
            String ln = last[rnd.nextInt(last.length)];
            int age = 18 + rnd.nextInt(60);
            CustomerProfile prof = profiles[rnd.nextInt(profiles.length)];
            SimulatedUser u = new SimulatedUser(cid, fn, ln, age, prof);

            // create matching account using domain classes
            try {
                BankAccount acc = null;
                double initBal = 100 + rnd.nextInt(5000);
                switch (prof) {
                    case STUDENT:
                        acc = new StudentAccount(u.getFullName(), Math.max(initBal, 100), "Local University");
                        u.accountType = "STUDENT";
                        break;
                    case PROFESSIONAL:
                        acc = new CurrentAccount(u.getFullName(), Math.max(initBal, 5000), "TL-"+rnd.nextInt(9999));
                        u.accountType = "CURRENT";
                        break;
                    case FAMILY:
                    case RETIREE:
                    default:
                        acc = new SavingsAccount(u.getFullName(), Math.max(initBal, 2000), 1000);
                        u.accountType = "SAVINGS";
                        break;
                }
                int idx = FileIO.bank.addAccount(acc);
                BankAccount stored = FileIO.bank.getAccounts()[idx];
                u.accountNumber = stored.getAccNum();
            } catch (Exception e) {
                // fallback: create a plain savings account
                try {
                    BankAccount acc = new SavingsAccount(u.getFullName(), 2000, 1000);
                    int idx = FileIO.bank.addAccount(acc);
                    u.accountNumber = FileIO.bank.getAccounts()[idx].getAccNum();
                    u.accountType = "SAVINGS";
                } catch (Exception ex) {
                    // ignore - very unlikely
                }
            }

            users.add(u);
        }

        return users;
    }
}
