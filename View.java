package application;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

class View {
    int H = 420;         // Height of window pixels 
    int W = 500;         // Width of window pixels 

    // UI Components
    Label title;             // Title area
    TextField message;       // Message area (for numbers)
    TextArea reply;          // Reply area (for instructions/results)
    ScrollPane scrollPane;   // Scrollbars for TextArea  
    GridPane grid;           // Main layout grid

    // MVC components
    public Model model;
    public Controller controller;

    // Constructor
    public View() {
        Debug.trace("View::<constructor>");
    }

    // Start GUI
    public void start(Stage window) {
        Debug.trace("View::start");

        // Initialize layout
        grid = new GridPane();
        grid.setId("Layout");           // Assign CSS ID

        // Initialize controls
        title = new Label();            // Title label
        grid.add(title, 0, 0, 2, 1);    // Add to top, spanning two columns

        message = new TextField();      // Message field
        message.setEditable(false);     // Read-only
        grid.add(message, 0, 1, 2, 1);  // Add to second row, spanning two columns

        reply = new TextArea();         // Reply area
        reply.setEditable(false);       // Read-only
        scrollPane = new ScrollPane();  // Scroll pane
        scrollPane.setContent(reply);   // Embed TextArea
        grid.add(scrollPane, 0, 2, 2, 1); // Add to third row, spanning two columns

        // Numeric Buttons Layout
        VBox numericPane = new VBox();
        numericPane.setSpacing(10); // Vertical spacing
        numericPane.setAlignment(Pos.CENTER_LEFT); // Align left
        String[][] numericLabels = {
            {"7", "8", "9"},
            {"4", "5", "6"},
            {"1", "2", "3"},
            {"CLR", "0"}
        };
        for (String[] row : numericLabels) {
            HBox rowPane = new HBox();
            rowPane.setSpacing(10); // Horizontal spacing
            for (String label : row) {
                Button b = new Button(label);
                b.setPrefSize(60, 40);
                b.setOnAction(this::buttonClicked); // Set action handler
                rowPane.getChildren().add(b);
            }
            numericPane.getChildren().add(rowPane);
        }

        // Functional Buttons Layout
        VBox functionalPane = new VBox();
        functionalPane.setSpacing(10); // Vertical spacing
        functionalPane.setAlignment(Pos.CENTER_RIGHT); // Align right
        String[] functionalLabels = {"Dep", "W/D", "Bal", "Fin", "Trf"};
        for (String label : functionalLabels) {
            Button b = new Button(label);
            b.setPrefSize(80, 40); // Slightly larger for functional buttons
            b.setOnAction(this::buttonClicked); // Set action handler
            functionalPane.getChildren().add(b);
        }

        // Centered Enter Button
        HBox bottomPane = new HBox();
        bottomPane.setAlignment(Pos.CENTER); // Center align the Enter button
        Button enterButton = new Button("Ent");
        enterButton.setPrefSize(100, 50); // Larger size for emphasis
        enterButton.setOnAction(this::buttonClicked);
        bottomPane.getChildren().add(enterButton);

        // Combine Layouts in Grid
        grid.add(numericPane, 0, 3);    // Add numeric buttons to the left
        grid.add(functionalPane, 1, 3); // Add functional buttons to the right
        grid.add(bottomPane, 0, 4, 2, 1); // Span bottomPane across both columns

        // Create and set scene
        Scene scene = new Scene(grid, W, H);   
        scene.getStylesheets().add("atm.css"); // Apply CSS
        window.setScene(scene);
        window.show();
    }

    // Button click handler
    public void buttonClicked(ActionEvent event) {
        Button b = (Button) event.getSource();
        if (controller != null) {          
            String label = b.getText();   // Get button label
            Debug.trace("View::buttonClicked: label = " + label);
            controller.process(label);    // Pass to controller
        }
    }

    // Update display based on model
    public void update() {        
        if (model != null) {
            Debug.trace("View::update");
            String message1 = model.title;        // Get title
            title.setText(message1);              // Set title label
            String message2 = model.display1;     // Get message1
            message.setText(message2);            // Set message field
            String message3 = model.display2;     // Get message2
            reply.setText(message3);              // Set reply area
        }
    }
}
