package com.cinema.controller;

import com.cinema.AppContext;
import com.cinema.MainApp;
import entity.Seat;
import entity.Showtime;
import entity.User;
import enums.Role;
import enums.ShowtimeStatus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SeatSelectionController {
    @FXML private Label greeting;
    @FXML private Label selectionLabel;
    @FXML private Label totalLabel;
    @FXML private Label infoLabel;
    @FXML private ComboBox<Showtime> showtimeCombo;
    @FXML private FlowPane seatsPane;
    private User currentUser;
    private final Set<Long> selectedSeatIds = new LinkedHashSet<>();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private void initialize() {
        showtimeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Showtime s) {
                return s == null ? "" : s.getMovie().getTitle() + " | " + s.getRoom().getRoomName() + " | " + s.getStartTime().format(FORMAT);
            }
            @Override public Showtime fromString(String text) { return null; }
        });
        showtimeCombo.setItems(FXCollections.observableArrayList(AppContext.showtimes().getAllShowtimes().stream()
                .filter(s -> s.getRoom() != null && s.getMovie() != null && s.getStatus() != ShowtimeStatus.CANCELLED && s.getStatus() != ShowtimeStatus.ENDED
                        && s.getStartTime() != null && s.getStartTime().isAfter(java.time.LocalDateTime.now()))
                .sorted(Comparator.comparing(Showtime::getStartTime)).toList()));
        if (!showtimeCombo.getItems().isEmpty()) showtimeCombo.getSelectionModel().selectFirst();
        updateSeats();
    }
    public void setUser(User user) {
        if (user == null || user.getRole() != Role.USER) throw new IllegalArgumentException("Customer only");
        currentUser = user;
        greeting.setText("Welcome, " + user.getUsername());
    }
    @FXML private void updateSeats() {
        if (repository.StorageIO.mysqlEnabled()) AppContext.reloadBookingsAndTickets();
        selectedSeatIds.clear(); seatsPane.getChildren().clear();
        Showtime s = showtimeCombo.getValue();
        if (s == null) { infoLabel.setText("No upcoming showtimes. Ask Admin to create one."); updateTotal(); return; }
        infoLabel.setText(s.getMovie().getTitle() + " • " + s.getRoom().getRoomName() + " • " + s.getTicketPrice() + " VND / seat");
        var seats = AppContext.seats().findByRoom(s.getRoom().getId()).stream()
                .sorted(Comparator.comparing(Seat::getRow).thenComparingInt(Seat::getNumber)).toList();
        for (Seat seat : seats) {
            boolean booked = repository.StorageIO.mysqlEnabled()
                    ? repository.MySqlBookingStore.isSeatBooked(s.getId(), seat.getId())
                    : AppContext.tickets().isSeatBooked(seat, s);
            Button button = new Button(seat.getRow() + seat.getNumber());
            button.setMinWidth(60); button.setPrefHeight(42);
            button.getStyleClass().add(booked ? "seat-booked" : seat.getType() == enums.SeatType.VIP ? "seat-vip" : "seat-free");
            button.setDisable(booked);
            button.setOnAction(e -> {
                if (!selectedSeatIds.add(seat.getId())) selectedSeatIds.remove(seat.getId());
                button.getStyleClass().removeAll("seat-free", "seat-vip", "seat-chosen");
                button.getStyleClass().add(selectedSeatIds.contains(seat.getId()) ? "seat-chosen" : seat.getType() == enums.SeatType.VIP ? "seat-vip" : "seat-free");
                updateTotal();
            });
            seatsPane.getChildren().add(button);
        }
        if (seats.isEmpty()) infoLabel.setText("This room has no seats. Admin needs to add seats.");
        updateTotal();
    }
    private void updateTotal() {
        Showtime s = showtimeCombo.getValue();
        String names = selectedSeatIds.stream().map(id -> AppContext.seats().findById(id))
                .filter(v -> v != null).map(v -> v.getRow() + v.getNumber()).collect(Collectors.joining(", "));
        selectionLabel.setText(names.isEmpty() ? "Selected: none" : "Selected: " + names);
        totalLabel.setText("Estimated total: " + (s == null ? 0L : (long) s.getTicketPrice() * selectedSeatIds.size()) + " VND");
    }
    @FXML private void confirmBooking() {
        if (selectedSeatIds.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Please select at least one seat.").showAndWait();
            return;
        }
        Showtime showtime = showtimeCombo.getValue();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                selectionLabel.getText() + "\n" + totalLabel.getText() + "\n\nConfirm booking?",
                ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            entity.Booking booking = com.cinema.BookingWorkflow.confirm(currentUser, showtime, selectedSeatIds);
            new Alert(Alert.AlertType.INFORMATION,
                    "Booking #" + booking.getId() + " confirmed!\n" +
                    "Total: " + (long) booking.getTotalAmount() + " VND.\nOpen My Bookings to view your tickets.")
                    .showAndWait();
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        } finally {
            updateSeats();
        }
    }
    @FXML private void backToDashboard() {
        if (currentUser == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            ((DashboardController) loader.getController()).setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene); stage.setTitle("Cinema - CUSTOMER");
        } catch (Exception e) { new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait(); }
    }
}
