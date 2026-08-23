import simulator.*;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import Data.FileIO;
import Bank.*;

public class SimulatorTests {
    public static void main(String[] args) throws Exception {
        // Basic smoke tests
        SimulationConfig cfg = new SimulationConfig();
        cfg.numberOfUsers = 5;
        cfg.numberOfTransactions = 20;

        FileIO.Read();
        List<SimulatedUser> users = new ArrayList<>();
        users.add(new SimulatedUser("CUST-TEST-1","Test","User",21,CustomerProfile.STUDENT));
        // Create one account for test user
        BankAccount acc = new SavingsAccount("Test User", 5000, 1000);
        int idx = FileIO.bank.addAccount(acc);
        users.get(0).accountNumber = FileIO.bank.getAccounts()[idx].getAccNum();

        CsvTransactionEventWriter writer = new CsvTransactionEventWriter("data/simulation_transactions_test.csv");
        TransactionGenerator gen = new TransactionGenerator(cfg, users, writer);
        gen.run("SIM-TEST-001");

        File f = new File("data/simulation_transactions_test.csv");
        if (!f.exists()) throw new RuntimeException("CSV not created");
        System.out.println("Tests completed successfully");
    }
}
