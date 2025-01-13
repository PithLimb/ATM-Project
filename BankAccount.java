package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BankAccount {
    private int accNumber;
    private int accPasswd;
    private double balance;
    private double overdraftLimit; // New Field

    // Database connection details
    // Use double backslashes in Java strings
    private static final String CONNECTION_URL = 
            "jdbc:sqlserver://LAPTOP-GAE1PC33;databaseName=ATMDB;integratedSecurity=true;trustServerCertificate=true;";

    // Static block to load the JDBC driver
    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Debug.trace("JDBC Driver Registered!");
        } catch (ClassNotFoundException e) {
            Debug.trace("JDBC Driver not found: " + e.getMessage());
        }
    }

    // Constructor
    public BankAccount(int a, int p, double b, double o) { // Added 'double o' for overdraft
        this.accNumber = a;
        this.accPasswd = p;
        this.balance = b;
        this.overdraftLimit = o;
    }

    // Getters
    public int getAccNumber() {
        return accNumber;
    }

    public int getAccPasswd() {
        return accPasswd;
    }

    public double getBalance() {
        return balance;
    }
    
    public double getOverdraftLimit() { // New Getter
        return overdraftLimit;
    }

    // Withdraw money from the account
    public boolean withdraw(double amount) {
        Debug.trace("BankAccount::withdraw: amount = " + amount);

        if (amount > 0) {
            // Check if withdrawal exceeds balance + overdraft limit
            if ((balance - amount) >= -overdraftLimit) { // Allow overdraft up to the limit
                balance -= amount;
                updateBalanceInDB();
                Debug.trace("BankAccount::withdraw: Withdrawal successful. New balance = " + balance);
                return true;
            } else {
                Debug.trace("BankAccount::withdraw: Insufficient funds. Overdraft limit reached.");
            }
        } else {
            Debug.trace("BankAccount::withdraw: Invalid withdrawal amount.");
        }
        return false;
    }

    // Deposit money into the account
    public boolean deposit(double amount) {
        Debug.trace("BankAccount::deposit: amount = " + amount);

        if (amount > 0) {
            balance += amount;
            updateBalanceInDB();
            Debug.trace("BankAccount::deposit: Deposit successful. New balance = " + balance);
            return true;
        }
        Debug.trace("BankAccount::deposit: Invalid deposit amount.");
        return false;
    }

    // Update balance in the database
    private void updateBalanceInDB() {
        String updateSQL = "UPDATE BankAccounts SET Balance = ? WHERE AccountID = ?";

        try (Connection con = DriverManager.getConnection(CONNECTION_URL);
             PreparedStatement ps = con.prepareStatement(updateSQL)) {

            ps.setDouble(1, balance);
            ps.setInt(2, accNumber);
            ps.executeUpdate();

            Debug.trace("BankAccount::updateBalanceInDB: Account " + accNumber + " new balance " + balance);

        } catch (SQLException e) {
            Debug.trace("BankAccount::updateBalanceInDB: SQLException: " + e.getMessage());
        }
    }

    // Static method to fetch a BankAccount from the database
    public static BankAccount fetchAccount(int accNumber, int accPasswd) {
        String selectSQL = "SELECT * FROM BankAccounts WHERE AccountID = ? AND Password = ?";

        try (Connection con = DriverManager.getConnection(CONNECTION_URL);
             PreparedStatement ps = con.prepareStatement(selectSQL)) {

            ps.setInt(1, accNumber);
            ps.setInt(2, accPasswd);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double bal = rs.getDouble("Balance");
                double overdraft = rs.getDouble("OverdraftLimit"); // Retrieve OverdraftLimit
                Debug.trace("BankAccount::fetchAccount: Found AccountID " + accNumber + " with balance " + bal + " and overdraft " + overdraft);
                return new BankAccount(accNumber, accPasswd, bal, overdraft);
            }

        } catch (SQLException e) {
            Debug.trace("BankAccount::fetchAccount: SQLException: " + e.getMessage());
        }

        Debug.trace("BankAccount::fetchAccount: Account not found.");
        return null;
    }

    /**
     * Transfer amount to another account
     * @param destinationAccNumber The account number to transfer to
     * @param amount The amount to transfer
     * @return true if transfer is successful, false otherwise
     */
    public boolean transfer(int destinationAccNumber, double amount) {
        Debug.trace("BankAccount::transfer: To Account " + destinationAccNumber + ", Amount: " + amount);
        
        if (amount <= 0) {
            Debug.trace("BankAccount::transfer: Invalid transfer amount.");
            return false;
        }
        
        if ((balance - amount) < -overdraftLimit) {
            Debug.trace("BankAccount::transfer: Insufficient funds. Overdraft limit reached.");
            return false;
        }
        
        // Start transfer
        Connection con = null;
        PreparedStatement psCheck = null;
        PreparedStatement psDeduct = null;
        PreparedStatement psAdd = null;
        ResultSet rs = null;
        
        try {
            con = DriverManager.getConnection(CONNECTION_URL);
            con.setAutoCommit(false); // Begin transaction
            
            // Check if destination account exists
            String checkSQL = "SELECT Balance FROM BankAccounts WHERE AccountID = ?";
            psCheck = con.prepareStatement(checkSQL);
            psCheck.setInt(1, destinationAccNumber);
            rs = psCheck.executeQuery();
            
            if (!rs.next()) {
                Debug.trace("BankAccount::transfer: Destination account does not exist.");
                con.rollback();
                return false;
            }
            
            // Deduct from current account
            String deductSQL = "UPDATE BankAccounts SET Balance = Balance - ? WHERE AccountID = ?";
            psDeduct = con.prepareStatement(deductSQL);
            psDeduct.setDouble(1, amount);
            psDeduct.setInt(2, this.accNumber);
            psDeduct.executeUpdate();
            
            // Add to destination account
            String addSQL = "UPDATE BankAccounts SET Balance = Balance + ? WHERE AccountID = ?";
            psAdd = con.prepareStatement(addSQL);
            psAdd.setDouble(1, amount);
            psAdd.setInt(2, destinationAccNumber);
            psAdd.executeUpdate();
            
            con.commit(); // Commit transaction
            
            // Update in-memory balance
            this.balance -= amount;
            // Removed: updateBalanceInDB();
            
            Debug.trace("BankAccount::transfer: Transfer successful.");
            return true;
            
        } catch (SQLException e) {
            Debug.trace("BankAccount::transfer: SQLException: " + e.getMessage());
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                Debug.trace("BankAccount::transfer: Rollback failed: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (psCheck != null) psCheck.close();
                if (psDeduct != null) psDeduct.close();
                if (psAdd != null) psAdd.close();
                if (con != null) con.setAutoCommit(true);
                if (con != null) con.close();
            } catch (SQLException e) {
                Debug.trace("BankAccount::transfer: Failed to close resources: " + e.getMessage());
            }
        }
    }
}
