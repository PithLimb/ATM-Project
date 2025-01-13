package application;

//The model represents all the actual content and functionality of the app
//For the ATM, it keeps track of the information shown in the display
//(the title and two message boxes), and the interaction with the bank, executes
//commands provided by the controller and tells the view to update when
//something changes
public class Model {
    // ATM states
    final String ACCOUNT_NO = "account_no";
    final String PASSWORD = "password";
    final String LOGGED_IN = "logged_in";
    final String TRANSFER_ACCOUNT_NO = "transfer_account_no";
    final String TRANSFER_AMOUNT = "transfer_amount";

    // Model variables
    String state = ACCOUNT_NO;      // Current state
    int  number = 0;                // Current number input
    Bank  bank = null;              // Bank object
    int accNumber = -1;             // Entered account number
    int accPasswd = -1;             // Entered password
    int transferAccNumber = -1;     // Destination account number
    double transferAmount = 0.0;    // Transfer amount
    // Display variables
    String title = "Bank ATM";      // Title message
    String display1 = null;         // Message 1
    String display2 = null;         // Message 2

    // MVC components
    public View view;
    public Controller controller;

    // Constructor
    public Model(Bank b) {
        Debug.trace("Model::<constructor>");          
        bank = b;
    }

    // Initialize ATM state
    public void initialise(String message) {
        setState(ACCOUNT_NO);
        number = 0;
        accNumber = -1;
        accPasswd = -1;
        transferAccNumber = -1;
        transferAmount = 0.0;
        display1 = message; 
        display2 =  "Enter your account number\n" +
                    "Followed by \"Ent\"";
    }

    // Change state with debugging
    public void setState(String newState) {
        if (!state.equals(newState)) {
            String oldState = state;
            state = newState;
            Debug.trace("Model::setState: changed state from " + oldState + " to " + newState);
        }
    }

    // Process number key
    public void processNumber(String label) {
        char c = label.charAt(0);
        number = number * 10 + (c - '0'); // Build number 
        display1 = "" + number;
        display();  // Update GUI
    }

    // Process Clear button
    public void processClear() {
        number = 0;
        display1 = "";
        display();  // Update GUI
    }

    // Process Enter button
    public void processEnter() {
        switch (state) {
            case ACCOUNT_NO:
                accNumber = number;
                number = 0;
                setState(PASSWORD);
                display1 = "";
                display2 = "Now enter your password\n" +
                           "Followed by \"Ent\"";
                break;
            case PASSWORD:
                accPasswd = number;
                number = 0;
                display1 = "";
                if (bank.login(accNumber, accPasswd)) {
                    setState(LOGGED_IN);
                    display2 = "Accepted\n" +
                               "Now enter the transaction you require";
                } else {
                    initialise("Unknown account/password");
                }
                break;
            case TRANSFER_ACCOUNT_NO:
                processTransferAccountNo();
                break;
            case TRANSFER_AMOUNT:
                processTransferAmount();
                break;
            case LOGGED_IN:     
            default: 
                // Do nothing in any other state (ie logged in)
        }  
        display();  // Update GUI
    }

    // Process Withdraw button
    public void processWithdraw() {
        if (state.equals(LOGGED_IN)) {            
            if (bank.withdraw(number)) {
                display2 = "Withdrawn: " + number;
            } else {
                BankAccount currentAccount = bank.getAccount();
                double available = currentAccount.getBalance() + currentAccount.getOverdraftLimit();
                display2 = "Insufficient funds. Available to withdraw: " + available;
            }
            number = 0;
            display1 = "";           
        } else {
            initialise("You are not logged in");
        }
        display();  // Update GUI
    }

    // Process Deposit button
    public void processDeposit() {
        if (state.equals(LOGGED_IN)) {
            if (bank.deposit(number)) {
                display2 = "Deposited: " + number;
            } else {
                display2 = "Deposit failed";
            }
            display1 = "";
            number = 0;
        } else {
            initialise("You are not logged in");
        }
        display();  // Update GUI
    }

    // Process Balance button
    public void processBalance() {
        if (state.equals(LOGGED_IN)) {
            double balance = bank.getBalance();
            BankAccount currentAccount = bank.getAccount(); // Assuming you have a getter for account
            double overdraft = currentAccount.getOverdraftLimit();
            display2 = "Your balance is: " + balance + "\n" +
                       "Overdraft limit: " + overdraft;
            number = 0;
            display1 = "";
        } else {
            initialise("You are not logged in");
        }
        display();  // Update GUI
    }

    // Process Finish (Logout) button
    public void processFinish() {
        if (state.equals(LOGGED_IN)) {
            setState(ACCOUNT_NO);
            number = 0;
            accNumber = -1;
            accPasswd = -1;
            transferAccNumber = -1;
            transferAmount = 0.0;
            display2 = "Welcome: Enter your account number";
            bank.logout();
        } else {
            initialise("You are not logged in");
        }
        display();  // Update GUI
    }

    // Process Unknown Key
    public void processUnknownKey(String action) {
        Debug.trace("Model::processUnknownKey: unknown button \"" + action + "\", re-initialising");
        initialise("Invalid command");
        display();
    }

    // Initiate the transfer process
    public void processTransferInitiate() {
        if (state.equals(LOGGED_IN)) {
            setState(TRANSFER_ACCOUNT_NO);
            number = 0;
            transferAccNumber = -1;
            transferAmount = 0.0;
            display1 = "";
            display2 = "Enter destination account number\nFollowed by \"Ent\"";
            display();
        } else {
            initialise("You are not logged in");
        }
    }

    // Process the destination account number input
    public void processTransferAccountNo() {
        transferAccNumber = number;
        number = 0;
        setState(TRANSFER_AMOUNT);
        display1 = "";
        display2 = "Enter transfer amount\nFollowed by \"Ent\"";
        display();
    }

    // Process the transfer amount input and execute transfer
    public void processTransferAmount() {
        transferAmount = number;
        number = 0;
        display1 = "";
        
        if (bank.transfer(transferAccNumber, transferAmount)) {
            display2 = "Transferred " + transferAmount + " to Account " + transferAccNumber;
        } else {
            display2 = "Transfer failed: Insufficient funds or invalid account.";
        }
        
        setState(LOGGED_IN);
        display();
    }

    // Update View
    public void display() {
        Debug.trace("Model::display");
        view.update();
    }
}
