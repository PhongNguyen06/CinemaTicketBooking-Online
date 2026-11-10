package com.cinema.controller;

import com.cinema.AppContext;
import com.cinema.remote.RemoteSession;
import com.cinema.MainApp;
import entity.Cinema;
import entity.Room;
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
import javafx.util.StringConverter;

import java.util.List;

public class RoomController {
    @FXML private Label greeting;
    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, Number> idColumn;
    @FXML private TableColumn<Room, String> nameColumn;
    @FXML private TableColumn<Room, Number> capacityColumn;
    @FXML private TableColumn<Room, String> cinemaColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchType;
    @FXML private ComboBox<Cinema> cinemaFilter;

    private User currentUser;

    private final StringConverter<Cinema> cinemaConverter = new StringConverter<>() {
        @Override public String toString(Cinema c) { return c == null ? "" : c.getId() + " - " + c.getName(); }
        @Override public Cinema fromString(String text) { return null; }
    };

    @FXML private void initialize() {
        idColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getId()));
        nameColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getRoomName()));
        capacityColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getCapacity()));
        cinemaColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().getCinema() == null ? "Unknown cinema" : row.getValue().getCinema().getName()));
        searchType.setItems(FXCollections.observableArrayList("Name", "Cinema"));
        searchType.getSelectionModel().selectFirst();
        cinemaFilter.setConverter(cinemaConverter);
        refreshRooms();
    }

    public void setUser(User user) {
        com.cinema.remote.RemoteUpdates.watch(greeting, "rooms", () -> refreshRooms());
        com.cinema.remote.RemoteUpdates.watch(greeting, "cinemas", () -> refreshRooms());
        if (user == null || user.getRole() != Role.ADMIN) throw new IllegalArgumentException("Administrator access required.");
        currentUser = user;
        greeting.setText("Welcome, " + user.getUsername());
    }

    @FXML private void refreshRooms() {
        searchField.clear();
        try {
            cinemaFilter.setItems(FXCollections.observableArrayList(allCinemas()));
            cinemaFilter.getSelectionModel().clearSelection();
            roomTable.setItems(FXCollections.observableArrayList(allRooms()));
        } catch (Exception e) {alert(Alert.AlertType.ERROR,"Cannot load rooms: "+e.getMessage());}
    }

    @FXML private void searchRooms() {
        String term = searchField.getText().trim();
        List<Room> matches;
        try {
            List<Room> rooms = allRooms();
            Cinema cinema = cinemaFilter.getValue();
            if (cinema != null) rooms = rooms.stream().filter(r -> r.getCinema()!=null && r.getCinema().getId()==cinema.getId()).toList();
            String query = term.toLowerCase();
            matches = rooms.stream().filter(r -> ("Cinema".equals(searchType.getValue())
                ? (r.getCinema()==null ? "" : r.getCinema().getName()) : r.getRoomName())
                    .toLowerCase().contains(query)).toList();
        } catch (Exception e) {alert(Alert.AlertType.ERROR,"Search failed: "+e.getMessage());return;}
        roomTable.setItems(FXCollections.observableArrayList(matches));
    }

    @FXML private void addRoom() {
        if (!isAdmin()) return;
        Room room = showRoomDialog("Add Room", null);
        if (room == null) return;
        try {
            if (RemoteSession.enabled()) RemoteSession.client().addRoom(room);
            else AppContext.rooms().addRoom(room);
            refreshRooms();
        } catch (Exception e) {alert(Alert.AlertType.ERROR,"Add failed: "+e.getMessage());}
    }

    @FXML private void editRoom() {
        if (!isAdmin()) return;
        Room existing = selectedRoom();
        if (existing == null) return;
        Room updated = showRoomDialog("Edit Room", existing);
        if (updated == null) return;
        try {
            if (RemoteSession.enabled()) {
                // The server validates capacity against the actual database seat count.
                RemoteSession.client().updateRoom(updated);
            } else {
                int seatCount = AppContext.seats().findByRoom(existing.getId()).size();
                if (updated.getCapacity() < seatCount) {
                    alert(Alert.AlertType.WARNING, "Capacity cannot be lower than the " + seatCount + " seats already assigned.");
                    return;
                }
                AppContext.rooms().updateRoom(updated);
            }
            refreshRooms();
        } catch (Exception e) {alert(Alert.AlertType.ERROR,"Update failed: "+e.getMessage());}
    }

    @FXML private void deleteRoom() {
        if (!isAdmin()) return;
        Room selected = selectedRoom();
        if (selected == null) return;
        if (!RemoteSession.enabled()) {
            if (!AppContext.seats().findByRoom(selected.getId()).isEmpty()) {
                alert(Alert.AlertType.WARNING,"This room has seats.");return;
            }
            if (!AppContext.showtimes().findByRoom(selected.getId()).isEmpty()) {
                alert(Alert.AlertType.WARNING,"This room has showtimes.");return;
            }
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete room '" + selected.getRoomName() + "'?", ButtonType.YES, ButtonType.NO);
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            if (RemoteSession.enabled()) RemoteSession.client().deleteRoom(selected.getId());
            else AppContext.rooms().deleteRoom(selected.getId());
            refreshRooms();
        } catch (Exception e) {alert(Alert.AlertType.ERROR,"Delete failed: "+e.getMessage());}
    }

    private Room showRoomDialog(String title, Room existing) {
        List<Cinema> cinemas;
        try { cinemas = allCinemas(); }
        catch (Exception e) {alert(Alert.AlertType.ERROR,"Cannot load cinemas: "+e.getMessage());return null;}
        if (cinemas.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Create a cinema first in Cinema Management.");
            return null;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField name = new TextField(existing == null ? "" : existing.getRoomName());
        TextField capacity = new TextField(existing == null ? "" : String.valueOf(existing.getCapacity()));
        ComboBox<Cinema> cinema = new ComboBox<>(FXCollections.observableArrayList(cinemas));
        cinema.setConverter(cinemaConverter);
        cinema.setPrefWidth(260);
        if (existing != null && existing.getCinema() != null) {
            cinemas.stream().filter(c -> c.getId() == existing.getCinema().getId())
                    .findFirst().ifPresent(cinema::setValue);
        }
        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(12); form.setPadding(new Insets(18));
        form.addRow(0, new Label("Room name:"), name);
        form.addRow(1, new Label("Capacity:"), capacity);
        form.addRow(2, new Label("Cinema:"), cinema);
        dialog.getDialogPane().setContent(form);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return null;
        try {
            int count = Integer.parseInt(capacity.getText().trim());
            String roomName = name.getText().trim();
            if (count <= 0 || roomName.isBlank() || roomName.contains(",") || cinema.getValue() == null)
                throw new IllegalArgumentException();
            return new Room(existing == null ? 0 : existing.getId(), roomName, count, cinema.getValue());
        } catch (IllegalArgumentException ex) {
            alert(Alert.AlertType.ERROR, "Enter a name (without commas), positive integer capacity, and select a cinema.");
            return null;
        }
    }

    private List<Cinema> allCinemas() throws Exception {
        return RemoteSession.enabled() ? RemoteSession.client().cinemas() : AppContext.cinemas().getAllCinemas();
    }
    private List<Room> allRooms() throws Exception {
        return RemoteSession.enabled() ? RemoteSession.client().rooms() : AppContext.rooms().getAllRooms();
    }

    private Room selectedRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) alert(Alert.AlertType.INFORMATION, "Select a room from the table first.");
        return selected;
    }

    private boolean isAdmin() { return currentUser != null && currentUser.getRole() == Role.ADMIN; }
    private void alert(Alert.AlertType type, String message) { new Alert(type, message).showAndWait(); }

    @FXML private void backToDashboard() {
        if (!isAdmin()) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            DashboardController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Cinema - ADMIN");
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Cannot go back: " + ex.getMessage());
        }
    }
}
