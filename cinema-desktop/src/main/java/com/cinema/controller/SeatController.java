package com.cinema.controller;

import com.cinema.AppContext;
import com.cinema.MainApp;
import com.cinema.remote.RemoteSession;
import entity.Room;
import entity.Seat;
import entity.User;
import enums.Role;
import enums.SeatType;
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
import javafx.util.StringConverter;
import java.util.List;

public class SeatController {
    @FXML private Label greeting;
    @FXML private ComboBox<Room> roomFilter;
    @FXML private TableView<Seat> seatTable;
    @FXML private TableColumn<Seat, Number> idColumn;
    @FXML private TableColumn<Seat, String> rowColumn;
    @FXML private TableColumn<Seat, Number> numberColumn;
    @FXML private TableColumn<Seat, String> typeColumn;
    @FXML private TableColumn<Seat, String> roomColumn;
    private User currentUser;

    private final StringConverter<Room> roomConverter = new StringConverter<>() {
        @Override public String toString(Room r) {
            return r == null ? "All rooms" : r.getRoomName() + " - " + (r.getCinema() == null ? "Unknown cinema" : r.getCinema().getName());
        }
        @Override public Room fromString(String text) { return null; }
    };

    @FXML private void initialize() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        rowColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRow()));
        numberColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNumber()));
        typeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getType().name()));
        roomColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(roomConverter.toString(c.getValue().getRoom())));
        roomFilter.setConverter(roomConverter);
        roomFilter.setItems(FXCollections.observableArrayList(allRooms()));
        refreshSeats();
    }
    public void setUser(User user) {
        com.cinema.remote.RemoteUpdates.watch(greeting, "seats", () -> refreshSeats());
        if (user == null || user.getRole() != Role.ADMIN) throw new IllegalArgumentException("Admin only");
        currentUser = user;
        greeting.setText("Welcome, " + user.getUsername());
    }
    @FXML private void refreshSeats() {
        Room room = roomFilter.getValue();
        List<Seat> seats = room == null ? allSeats() : allSeats().stream().filter(s -> s.getRoom()!=null && s.getRoom().getId()==room.getId()).toList();
        seatTable.setItems(FXCollections.observableArrayList(seats));
    }
    @FXML private void addSeat() {
        if (!isAdmin()) return;
        Seat seat = dialog("Add Seat", null);
        if (seat == null) return;
        if (!validSeat(seat, 0)) return;
        try { if (RemoteSession.enabled()) RemoteSession.client().addSeat(seat); else AppContext.seats().addSeat(seat); } catch(Exception e) {alert(Alert.AlertType.ERROR,e.getMessage());return;}
        refreshSeats();
    }
    @FXML private void editSeat() {
        if (!isAdmin()) return;
        Seat old = selected(); if (old == null) return;
        if (hasTicketHistory(old)) { alert(Alert.AlertType.WARNING, "This seat is referenced by tickets. Editing is blocked."); return; }
        Seat updated = dialog("Edit Seat", old);
        if (updated == null || !validSeat(updated, old.getId())) return;
        try { if (RemoteSession.enabled()) RemoteSession.client().updateSeat(updated); else AppContext.seats().updateSeat(updated); } catch(Exception e) {alert(Alert.AlertType.ERROR,e.getMessage());return;}
        refreshSeats();
    }
    @FXML private void deleteSeat() {
        if (!isAdmin()) return;
        Seat seat = selected(); if (seat == null) return;
        if (hasTicketHistory(seat)) { alert(Alert.AlertType.WARNING, "This seat has ticket history and cannot be deleted."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete seat " + seat.getRow() + seat.getNumber() + "?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try { if (RemoteSession.enabled()) RemoteSession.client().deleteSeat(seat.getId()); else AppContext.seats().deleteSeat(seat.getId()); } catch(Exception e) {alert(Alert.AlertType.ERROR,e.getMessage());return;}
        refreshSeats();
    }
    private boolean validSeat(Seat candidate, long excludeId) {
        long roomId = candidate.getRoom().getId();
        for (Seat seat : allSeats().stream().filter(s -> s.getRoom()!=null && s.getRoom().getId()==roomId).toList()) {
            if (seat.getId() != excludeId && seat.getRow().equalsIgnoreCase(candidate.getRow()) && seat.getNumber() == candidate.getNumber()) {
                alert(Alert.AlertType.ERROR, "This row and seat number already exist in the selected room."); return false;
            }
        }
        long count = allSeats().stream().filter(s -> s.getRoom()!=null && s.getRoom().getId()==roomId).toList().stream().filter(s -> s.getId() != excludeId).count();
        if (count >= candidate.getRoom().getCapacity()) {
            alert(Alert.AlertType.WARNING, "Room capacity is full. Increase capacity before adding or moving seats."); return false;
        }
        return true;
    }
    private boolean hasTicketHistory(Seat seat) {
        return !RemoteSession.enabled() && AppContext.tickets().getAllTickets().stream().anyMatch(t -> t.getSeat() != null && t.getSeat().getId() == seat.getId());
    }
    private Seat dialog(String title, Seat old) {
        if (allRooms().isEmpty()) { alert(Alert.AlertType.WARNING, "Create a room first."); return null; }
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField row = new TextField(old == null ? "" : old.getRow());
        TextField number = new TextField(old == null ? "" : String.valueOf(old.getNumber()));
        ComboBox<SeatType> type = new ComboBox<>(FXCollections.observableArrayList(SeatType.values()));
        type.setValue(old == null ? SeatType.NORMAL : old.getType());
        ComboBox<Room> room = new ComboBox<>(FXCollections.observableArrayList(allRooms()));
        room.setConverter(roomConverter); room.setPrefWidth(270);
        room.setValue(old == null ? roomFilter.getValue() : old.getRoom());
        GridPane form = new GridPane(); form.setHgap(10); form.setVgap(12); form.setPadding(new Insets(16));
        form.addRow(0, new Label("Row (A, B...):"), row);
        form.addRow(1, new Label("Number:"), number);
        form.addRow(2, new Label("Type:"), type);
        form.addRow(3, new Label("Room:"), room);
        dialog.getDialogPane().setContent(form);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return null;
        try {
            int n = Integer.parseInt(number.getText().trim());
            String r = row.getText().trim().toUpperCase();
            if (!r.matches("[A-Z]{1,3}") || n <= 0 || room.getValue() == null || type.getValue() == null) throw new IllegalArgumentException();
            return new Seat(old == null ? 0 : old.getId(), r, n, type.getValue(), room.getValue());
        } catch (IllegalArgumentException ex) {
            alert(Alert.AlertType.ERROR, "Enter row letters A-Z, a positive seat number, type and room."); return null;
        }
    }
    private Seat selected() {
        Seat seat = seatTable.getSelectionModel().getSelectedItem();
        if (seat == null) alert(Alert.AlertType.INFORMATION, "Select a seat first.");
        return seat;
    }
    private List<Room> allRooms() {
        try {return RemoteSession.enabled()?RemoteSession.client().rooms():AppContext.rooms().getAllRooms();}
        catch(Exception e){alert(Alert.AlertType.ERROR,"Cannot load rooms: "+e.getMessage());return List.of();}
    }
    private List<Seat> allSeats() {
        try {return RemoteSession.enabled()?RemoteSession.client().seats():AppContext.seats().getAllSeats();}
        catch(Exception e){alert(Alert.AlertType.ERROR,"Cannot load seats: "+e.getMessage());return List.of();}
    }
    private boolean isAdmin() { return currentUser != null && currentUser.getRole() == Role.ADMIN; }
    private void alert(Alert.AlertType type, String msg) { new Alert(type, msg).showAndWait(); }
    @FXML private void backToDashboard() {
        if (!isAdmin()) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            ((DashboardController) loader.getController()).setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene); stage.setTitle("Cinema - ADMIN");
        } catch (Exception e) { alert(Alert.AlertType.ERROR, e.getMessage()); }
    }
}
