import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

class Transaction {
    private final int txnId;
    private final String type;
    private final double amount;
    private final double balanceAfter;
    private final String timestamp;
    private final String remarks;

    public Transaction(int txnId, String type, double amount,
                       double balanceAfter, String remarks) {
        this.txnId = txnId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.remarks = remarks;
        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    public String toString() {
        return String.format("%-6d %-16s %-7s %10.2f %11.2f %s",
                txnId, timestamp, type, amount, balanceAfter, remarks);
    }
}

class Account {
    public static final double MIN_BALANCE = 500.0;
    private static int txnCounter = 1000;

    private final String accountNo;
    private final String holderName;
    private String pin;
    private double balance;
    private int failedAttempts = 0;
    private boolean locked = false;
    private final ArrayList<Transaction> history = new ArrayList<>();

    public Account(String accountNo, String holderName, String pin, double openingBalance) {
        this.accountNo = accountNo;
        this.holderName = holderName;
        this.pin = pin;
        this.balance = openingBalance;
        record("CREDIT", openingBalance, "Account opening");
    }

    public String getAccountNo() { return accountNo; }
    public String getHolderName() { return holderName; }
    public double getBalance() { return balance; }
    public boolean isLocked() { return locked; }

    public boolean verifyPin(String entered) {
        if (locked) return false;
        if (pin.equals(entered)) {
            failedAttempts = 0;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= 3) locked = true;
        return false;
    }

    public int attemptsLeft() { return Math.max(0, 3 - failedAttempts); }

    public void changePin(String newPin) { pin = newPin; }

    public void deposit(double amount, String remarks) {
        balance += amount;
        record("CREDIT", amount, remarks);
    }

    public boolean withdraw(double amount, String remarks) {
        if (amount <= 0 || balance - amount < MIN_BALANCE) return false;
        balance -= amount;
        record("DEBIT", amount, remarks);
        return true;
    }

    private void record(String type, double amount, String remarks) {
        history.add(new Transaction(++txnCounter, type, amount, balance, remarks));
    }

    public void printStatement(int count) {
        System.out.printf("%-6s %-16s %-7s %10s %11s %s%n",
                "TxnID", "Date & Time", "Type", "Amount", "Balance", "Remarks");
        int start = Math.max(0, history.size() - count);
        for (int i = start; i < history.size(); i++)
            System.out.println(history.get(i));
    }
}

public class InternetBankingSystem {
    private static final Scanner sc = new Scanner(System.in);
    private static final ArrayList<Account> accounts = new ArrayList<>();
    private static int nextAccountNo = 1003;

    private static Account findAccount(String accNo) {
        for (Account a : accounts)
            if (a.getAccountNo().equals(accNo)) return a;
        return null;
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static double readAmount(String prompt) {
        while (true) {
            try {
                double v = Double.parseDouble(readLine(prompt));
                if (v > 0) return v;
                System.out.println("Amount must be greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static void registerAccount() {
        String name = readLine("Account holder name: ");
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        String pin = readLine("Set a 4-digit PIN: ");
        if (!pin.matches("\\d{4}")) {
            System.out.println("PIN must contain exactly 4 digits.");
            return;
        }
        double opening = readAmount("Opening deposit (min Rs. 500): ");
        if (opening < Account.MIN_BALANCE) {
            System.out.println("Opening deposit is below the minimum balance.");
            return;
        }
        String accNo = String.valueOf(nextAccountNo++);
        accounts.add(new Account(accNo, name, pin, opening));
        System.out.println("Account created. Your account number is " + accNo);
    }

    private static void login() {
        String accNo = readLine("Account number: ");
        Account acc = findAccount(accNo);
        if (acc == null) {
            System.out.println("Account not found.");
            return;
        }
        if (acc.isLocked()) {
            System.out.println("Account is locked. Contact the bank branch.");
            return;
        }
        String pin = readLine("PIN: ");
        if (!acc.verifyPin(pin)) {
            if (acc.isLocked())
                System.out.println("Too many wrong attempts. Account locked.");
            else
                System.out.println("Incorrect PIN. Attempts left: " + acc.attemptsLeft());
            return;
        }
        System.out.println("Login successful. Welcome, " + acc.getHolderName() + "!");
        customerMenu(acc);
    }

    private static void transfer(Account from) {
        String toNo = readLine("Beneficiary account number: ");
        Account to = findAccount(toNo);
        if (to == null) {
            System.out.println("Beneficiary account not found.");
            return;
        }
        if (to == from) {
            System.out.println("Cannot transfer to your own account.");
            return;
        }
        double amt = readAmount("Amount to transfer: Rs. ");
        if (from.withdraw(amt, "Transfer to " + toNo)) {
            to.deposit(amt, "Transfer from " + from.getAccountNo());
            System.out.printf("Rs. %.2f transferred to %s (%s).%n",
                    amt, toNo, to.getHolderName());
        } else {
            System.out.println("Transfer failed: insufficient balance "
                    + "(minimum Rs. 500 must remain).");
        }
    }

    private static void customerMenu(Account acc) {
        int choice;
        do {
            System.out.println("\n----- INTERNET BANKING : CUSTOMER MENU -----");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Fund Transfer");
            System.out.println("5. Mini Statement (last 5)");
            System.out.println("6. Change PIN");
            System.out.println("7. Logout");
            choice = readInt("Enter choice: ");
            switch (choice) {
                case 1:
                    System.out.printf("Available balance: Rs. %.2f%n", acc.getBalance());
                    break;
                case 2:
                    acc.deposit(readAmount("Deposit amount: Rs. "), "Cash deposit");
                    System.out.printf("Deposit successful. Balance: Rs. %.2f%n", acc.getBalance());
                    break;
                case 3:
                    if (acc.withdraw(readAmount("Withdrawal amount: Rs. "), "Withdrawal"))
                        System.out.printf("Please collect cash. Balance: Rs. %.2f%n", acc.getBalance());
                    else
                        System.out.println("Insufficient balance (minimum Rs. 500 must remain).");
                    break;
                case 4: transfer(acc); break;
                case 5: acc.printStatement(5); break;
                case 6:
                    String oldPin = readLine("Current PIN: ");
                    if (!acc.verifyPin(oldPin)) {
                        System.out.println("Incorrect PIN.");
                        break;
                    }
                    String newPin = readLine("New 4-digit PIN: ");
                    if (newPin.matches("\\d{4}")) {
                        acc.changePin(newPin);
                        System.out.println("PIN changed successfully.");
                    } else {
                        System.out.println("PIN must contain exactly 4 digits.");
                    }
                    break;
                case 7: System.out.println("Logged out securely."); break;
                default: System.out.println("Invalid choice. Try again.");
            }
        } while (choice != 7);
    }

    public static void main(String[] args) {
        accounts.add(new Account("1001", "Arun Kumar", "1234", 10000));
        accounts.add(new Account("1002", "Priya Devi", "4321", 5000));
        int choice;
        do {
            System.out.println("\n===== INTERNET BANKING SYSTEM =====");
            System.out.println("1. Register New Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            choice = readInt("Enter choice: ");
            switch (choice) {
                case 1: registerAccount(); break;
                case 2: login(); break;
                case 3: System.out.println("Thank you for banking with us."); break;
                default: System.out.println("Invalid choice. Try again.");
            }
        } while (choice != 3);
        sc.close();
    }
}