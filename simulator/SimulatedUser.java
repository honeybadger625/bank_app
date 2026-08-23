package simulator;

public class SimulatedUser {
    public final String customerId;
    public final String firstName;
    public final String lastName;
    public final int age;
    public final CustomerProfile profile;
    public String accountNumber;
    public String accountType;

    public SimulatedUser(String customerId, String firstName, String lastName, int age, CustomerProfile profile) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.profile = profile;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return customerId + " | " + getFullName() + " | " + profile + " | " + accountNumber;
    }
}
