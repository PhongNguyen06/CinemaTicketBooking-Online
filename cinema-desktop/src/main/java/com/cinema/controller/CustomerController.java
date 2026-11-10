package com.cinema.controller;

import com.cinema.AppContext;
import com.cinema.MainApp;
import entity.Booking;
import entity.Customer;
import entity.User;
import enums.Role;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerController {
    @FXML private Label greeting;
    @FXML private Label countLabel;
    @FXML private ComboBox<String> searchType;
    @FXML private TextField searchField;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Number> idColumn;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, Number> bookingIdColumn;
    @FXML private TableColumn<Booking, String> bookingDateColumn;
    @FXML private TableColumn<Booking, String> bookingStatusColumn;
    @FXML private TableColumn<Booking, Number> bookingTotalColumn;
    private User currentUser;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private void initialize() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        emailColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEmail()));
        phoneColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPhone()));
        bookingIdColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        bookingDateColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getBookingDate() == null ? "" : c.getValue().getBookingDate().format(DATE)));
        bookingStatusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus().name()));
        bookingTotalColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTotalAmount()));
        searchType.setItems(FXCollections.observableArrayList("Name", "Phone", "Email"));
        searchType.getSelectionModel().selectFirst();
        customerTable.getSelectionModel().selectedItemProperty().addListener((obs, previous, current) -> showBookings(current));
    }

    public void setUser(User user) {
        if (user == null || user.getRole() != Role.ADMIN) throw new IllegalArgumentException("Admin access required");
        currentUser = user;
        greeting.setText("Welcome, " + user.getUsername());
        refreshCustomers();
    }

    @FXML private void refreshCustomers() {
        List<Customer> customers = AppContext.customers().getAllCustomers();
        customerTable.setItems(FXCollections.observableArrayList(customers));
        countLabel.setText("Customers: " + customers.size());
        bookingTable.getItems().clear();
    }

    @FXML private void searchCustomers() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) { refreshCustomers(); return; }
        List<Customer> results;
        switch (searchType.getValue()) {
            case "Phone" -> results = AppContext.customers().findByPhone(query);
            case "Email" -> results = AppContext.customers().getAllCustomers().stream()
                    .filter(c -> c.getEmail().toLowerCase().contains(query.toLowerCase())).toList();
            default -> results = AppContext.customers().findByName(query);
        }
        customerTable.setItems(FXCollections.observableArrayList(results));
        countLabel.setText("Matches: " + results.size());
        bookingTable.getItems().clear();
    }

    private void showBookings(Customer customer) {
        bookingTable.setItems(FXCollections.observableArrayList(customer == null
                ? List.of() : AppContext.bookings().findByCustomer(customer.getId())));
    }

    @FXML private void addCustomer() {
        if (!isAdmin()) return;
        Customer created = showCustomerForm(null);
        if (created == null) return;
        AppContext.customers().addCustomer(created);
        refreshCustomers();
    }

    @FXML private void editCustomer() {
        if (!isAdmin()) return;
        Customer selected = selectedCustomer();
        if (selected == null) return;
        Customer updated = showCustomerForm(selected);
        if (updated == null) return;
        AppContext.customers().updateCustomer(updated);
        refreshCustomers();
        customerTable.getSelectionModel().select(selected);
        customerTable.refresh();
    }

    @FXML private void deleteCustomer() {
        if (!isAdmin()) return;
        Customer selected = selectedCustomer();
        if (selected == null) return;
        if (!AppContext.bookings().findByCustomer(selected.getId()).isEmpty()) {
            warn("This customer has booking history. Deletion is blocked to preserve records."); return;
        }
        if (AppContext.users().getAllUsers().stream().anyMatch(u -> u.getCustomer() != null && u.getCustomer().getId() == selected.getId())) {
            warn("This customer has a login account. Deletion is blocked to protect the account."); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete customer " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        AppContext.customers().deleteCustomer(selected.getId());
        refreshCustomers();
    }

    private Customer selectedCustomer() {
        Customer customer = customerTable.getSelectionModel().getSelectedItem();
        if (customer == null) warn("Please select a customer first.");
        return customer;
    }

    private Customer showCustomerForm(Customer existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Customer" : "Edit Customer");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField name = new TextField(existing == null ? "" : existing.getName());
        TextField email = new TextField(existing == null ? "" : existing.getEmail());
        TextField phone = new TextField(existing == null ? "" : existing.getPhone());
        GridPane form = new GridPane(); form.setHgap(10); form.setVgap(10); form.setPadding(new Insets(16));
        form.addRow(0, new Label("Name"), name);
        form.addRow(1, new Label("Email"), email);
        form.addRow(2, new Label("Phone"), phone);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validate(existing, name.getText(), email.getText(), phone.getText());
            if (error != null) { warn(error); event.consume(); }
        });
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return null;
        return new Customer(existing == null ? 0 : existing.getId(),
                name.getText().trim(), email.getText().trim(), phone.getText().trim());
    }

    private String validate(Customer existing, String rawName, String rawEmail, String rawPhone) {
        String name = rawName.trim(), email = rawEmail.trim(), phone = rawPhone.trim();
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) return "All fields are required.";
        if (name.contains(",") || email.contains(",") || phone.contains(",") || name.contains("\n") || email.contains("\n") || phone.contains("\n"))
            return "Commas and line breaks cannot be stored in CSV fields.";
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) return "Invalid email address.";
        if (!phone.matches("^[0-9+() .-]{9,18}$")) return "Invalid phone number.";
        for (Customer customer : AppContext.customers().getAllCustomers()) {
            if (existing != null && customer.getId() == existing.getId()) continue;
            if (customer.getEmail().equalsIgnoreCase(email)) return "This email address is already used.";
        }
        return null;
    }

    private boolean isAdmin() { return currentUser != null && currentUser.getRole() == Role.ADMIN; }
    private void warn(String message) { new Alert(Alert.AlertType.WARNING, message).showAndWait(); }

    @FXML private void backToDashboard() {
        if (!isAdmin()) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            ((DashboardController) loader.getController()).setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene); stage.setTitle("Cinema - Admin Dashboard");
        } catch (Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
    }
}
