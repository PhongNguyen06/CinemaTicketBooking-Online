package com.cinema.controller;

import com.cinema.AppContext;
import com.cinema.BookingWorkflow;
import com.cinema.MainApp;
import entity.*;
import enums.Role;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class BookingsController {
    @FXML private Label greeting;
    @FXML private Label pageTitle;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking,Number> idColumn;
    @FXML private TableColumn<Booking,String> dateColumn;
    @FXML private TableColumn<Booking,String> customerColumn;
    @FXML private TableColumn<Booking,String> statusColumn;
    @FXML private TableColumn<Booking,Number> totalColumn;
    @FXML private TableView<Ticket> ticketTable;
    @FXML private TableColumn<Ticket,Number> ticketIdColumn;
    @FXML private TableColumn<Ticket,String> movieColumn;
    @FXML private TableColumn<Ticket,String> seatColumn;
    @FXML private TableColumn<Ticket,String> ticketStatusColumn;
    @FXML private TableColumn<Ticket,Number> priceColumn;
    private User currentUser;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private void initialize() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        dateColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getBookingDate() == null ? "" : c.getValue().getBookingDate().format(TIME)));
        customerColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCustomer() == null ? "" : c.getValue().getCustomer().getName()));
        statusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus().name()));
        totalColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTotalAmount()));
        ticketIdColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        movieColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getShowtime() == null || c.getValue().getShowtime().getMovie() == null ? "" : c.getValue().getShowtime().getMovie().getTitle()));
        seatColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSeat() == null ? "" : c.getValue().getSeat().getRow() + c.getValue().getSeat().getNumber()));
        ticketStatusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus().name()));
        priceColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getPrice()));
        bookingTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            ticketTable.setItems(FXCollections.observableArrayList(selected == null ? java.util.List.of() : AppContext.tickets().findByBooking(selected.getId())));
        });
    }

    public void setUser(User user) {
        currentUser = user;
        boolean admin = user.getRole() == Role.ADMIN;
        pageTitle.setText(admin ? "Booking & Ticket Management" : "My Bookings & Tickets");
        greeting.setText("Welcome, " + user.getUsername());
        refresh();
    }
    @FXML private void refresh() {
        if (currentUser == null) return;
        if (repository.StorageIO.mysqlEnabled()) AppContext.reloadBookingsAndTickets();
        java.util.List<Booking> all = currentUser.getRole() == Role.ADMIN
                ? AppContext.bookings().getAllBookings()
                : currentUser.getCustomer() == null ? java.util.List.of()
                : AppContext.bookings().findByCustomer(currentUser.getCustomer().getId());
        bookingTable.setItems(FXCollections.observableArrayList(all));
        ticketTable.getItems().clear();
    }
    @FXML private void cancelSelected() {
        Booking booking = bookingTable.getSelectionModel().getSelectedItem();
        if (booking == null) { new Alert(Alert.AlertType.INFORMATION, "Select a booking first.").showAndWait(); return; }
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel booking #" + booking.getId() + " and its tickets?", ButtonType.YES, ButtonType.NO);
        if (dialog.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            BookingWorkflow.cancel(booking, currentUser);
            new Alert(Alert.AlertType.INFORMATION, "Booking cancelled.").showAndWait();
            refresh();
        } catch (RuntimeException ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
    }
    @FXML private void backToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            ((DashboardController) loader.getController()).setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene); stage.setTitle("Cinema - " + currentUser.getRole());
        } catch (Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
    }
}
