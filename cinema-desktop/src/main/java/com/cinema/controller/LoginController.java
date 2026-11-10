package com.cinema.controller;

import com.cinema.AppContext;
import com.cinema.MainApp;
import com.cinema.remote.RemoteSession;
import enums.Role;
import entity.Customer;
import entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import java.util.Optional;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password."); return;
        }
        User user;
        if (RemoteSession.enabled()) {
            try {
                var response = RemoteSession.client().login(username, password);
                com.cinema.remote.RemoteUpdates.start(RemoteSession.client());
                user = new User(0,response.username(),"", Role.valueOf(response.role()),null);
            } catch (Exception ex) {
                messageLabel.setText("Server login failed: " + ex.getMessage()); return;
            }
        } else {
            user = AppContext.users().login(username, password);
        }
        if (user == null) { messageLabel.setText("Invalid username or password."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            DashboardController controller = loader.getController();
            controller.setUser(user);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene); stage.setTitle("Cinema - " + user.getRole());
        } catch (Exception ex) { messageLabel.setText("Could not open dashboard: " + ex.getMessage()); }
    }

    @FXML private void handleRegister() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register Customer Account");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10); form.setPadding(new Insets(15));
        TextField username = new TextField(), name = new TextField(), email = new TextField(), phone = new TextField();
        PasswordField password = new PasswordField(), confirm = new PasswordField();
        Control[] fields = {username,password,confirm,name,email,phone};
        String[] labels = {"Username","Password","Confirm password","Full name","Email","Phone"};
        for (int i=0;i<fields.length;i++) {form.add(new Label(labels[i]),0,i);form.add(fields[i],1,i);}
        Label error = new Label();
        error.setWrapText(true);
        error.setStyle("-fx-text-fill: #e35e6b;");
        form.add(error,0,fields.length,2,1);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String u=username.getText().trim(), p=password.getText(), n=name.getText().trim();
            String e=email.getText().trim(), ph=phone.getText().trim();
            if (!u.matches("[A-Za-z0-9_]{4,20}")) {error.setText("Username must be 4-20 letters, digits or underscores.");event.consume();return;}
            if (p.length()<12 || p.length()>128 || !p.equals(confirm.getText())) {
                error.setText("Password must be 12-128 characters and match confirmation.");event.consume();return;
            }
            if (n.length()<2 || n.length()>100) {error.setText("Full name must be 2-100 characters.");event.consume();return;}
            if (!e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                error.setText("Please enter a valid email address.");event.consume();return;
            }
            if (!ph.matches("0[0-9]{9}")) {error.setText("Phone must contain 10 digits starting with 0.");event.consume();return;}
            try {
                if (RemoteSession.enabled()) {
                    RemoteSession.client().register(u,p,n,e,ph);
                } else {
                    if (AppContext.users().findByUsername(u)!=null) {error.setText("Username already exists.");event.consume();return;}
                    if (AppContext.customers().findByEmail(e)!=null) {error.setText("Email already exists.");event.consume();return;}
                    if (AppContext.customers().findByPhone(ph)!=null) {error.setText("Phone already exists.");event.consume();return;}
                    Customer c=new Customer(0,n,e,ph);
                    AppContext.customers().addCustomer(c);
                    AppContext.users().registerUser(u,p,c);
                }
                messageLabel.setText("Account created! You can now log in.");
                usernameField.setText(u);
                passwordField.clear();
            } catch (Exception ex) {
                String msg=ex.getMessage();
                if(msg==null) msg="Please try again.";
                if(msg.contains("Username already exists")) msg="Username already exists.";
                else if(msg.contains("Email already exists")) msg="Email already exists.";
                else if(msg.contains("Phone number already exists")) msg="Phone number already exists.";
                else if(msg.contains("Duplicate value")) msg="Username, email or phone already in use.";
                else if(msg.contains("HTTP 404") || msg.contains("HTTP 403")) msg="Registration blocked (HTTP 403/404). Deploy the updated cinema-server to Render, then verify /api/auth/registration-status.";
                error.setText(msg);
                event.consume();
            }
        });
        dialog.showAndWait();
    }
}
