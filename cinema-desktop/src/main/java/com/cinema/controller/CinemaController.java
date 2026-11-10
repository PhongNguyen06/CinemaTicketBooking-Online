package com.cinema.controller;

import com.cinema.AppContext;
import com.cinema.remote.RemoteSession;
import com.cinema.MainApp;
import entity.Cinema;
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

import java.util.List;
import java.util.Optional;

public class CinemaController {
    @FXML private Label greeting;
    @FXML private TableView<Cinema> cinemaTable;
    @FXML private TableColumn<Cinema, Number> idColumn;
    @FXML private TableColumn<Cinema, String> nameColumn;
    @FXML private TableColumn<Cinema, String> addressColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchType;
    private User currentUser;

    @FXML private void initialize() {
        idColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getId()));
        nameColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getName()));
        addressColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getAddress()));
        searchType.setItems(FXCollections.observableArrayList("Name", "Address"));
        searchType.getSelectionModel().selectFirst();
        refreshCinemas();
    }

    public void setUser(User user) {
        com.cinema.remote.RemoteUpdates.watch(greeting, "cinemas", () -> refreshCinemas());
        if (user == null || user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Administrator access required.");
        }
        currentUser = user;
        greeting.setText("Welcome, " + user.getUsername());
    }

    @FXML private void refreshCinemas() {
        searchField.clear();
        try {
            cinemaTable.setItems(FXCollections.observableArrayList(allCinemas()));
        } catch (Exception e) { alert(Alert.AlertType.ERROR,"Cannot load cinemas: " + e.getMessage()); }
    }

    @FXML private void searchCinemas() {
        String keyword = searchField.getText().trim();
        try {
            String lower=keyword.toLowerCase();
            List<Cinema> result = allCinemas().stream().filter(c -> ("Address".equals(searchType.getValue())
                    ? c.getAddress() : c.getName()).toLowerCase().contains(lower)).toList();
            cinemaTable.setItems(FXCollections.observableArrayList(result));
        } catch (Exception e) {alert(Alert.AlertType.ERROR,"Search failed: " + e.getMessage());}
    }

    @FXML private void addCinema() {
        if (!isAdmin()) return;
        Cinema cinema = showCinemaDialog("Add Cinema", null);
        if (cinema == null) return;
        try {
            if (RemoteSession.enabled()) RemoteSession.client().addCinema(cinema);
            else AppContext.cinemas().addCinema(cinema);
            refreshCinemas();
        } catch (Exception e) {alert(Alert.AlertType.ERROR,"Add failed: " + e.getMessage());}
    }

    @FXML private void editCinema() {
        if (!isAdmin()) return;
        Cinema selected = selectedCinema();
        if (selected == null) return;
        Cinema updated = showCinemaDialog("Edit Cinema", selected);
        if (updated == null) return;
        try {
            if (RemoteSession.enabled()) RemoteSession.client().updateCinema(updated);
            else AppContext.cinemas().updateCinema(updated);
            refreshCinemas();
        } catch (Exception e) {alert(Alert.AlertType.ERROR,"Update failed: " + e.getMessage());}
    }

    @FXML private void deleteCinema() {
        if (!isAdmin()) return;
        Cinema selected = selectedCinema();
        if (selected == null) return;
        // Prevent orphan references in the existing CSV-based data model.
        boolean hasRooms;
        try { hasRooms = allRooms().stream().anyMatch(room -> room.getCinema() != null
                        && room.getCinema().getId() == selected.getId()); }
        catch (Exception e) { alert(Alert.AlertType.ERROR,"Cannot check linked rooms: "+e.getMessage());return;}
        if (hasRooms) {
            alert(Alert.AlertType.WARNING, "This cinema still has rooms. Remove or move its rooms first.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete cinema '" + selected.getName() + "'?", ButtonType.YES, ButtonType.NO);
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            if (RemoteSession.enabled()) RemoteSession.client().deleteCinema(selected.getId());
            else AppContext.cinemas().deleteCinema(selected.getId());
            refreshCinemas();
        } catch (Exception e) {alert(Alert.AlertType.ERROR,"Delete failed: " + e.getMessage());}
    }

    private Cinema showCinemaDialog(String title, Cinema existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField name = new TextField(existing == null ? "" : existing.getName());
        TextField address = new TextField(existing == null ? "" : existing.getAddress());
        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(12); form.setPadding(new Insets(18));
        form.addRow(0, new Label("Cinema name:"), name);
        form.addRow(1, new Label("Address:"), address);
        dialog.getDialogPane().setContent(form);
        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) return null;
        String n = name.getText().trim(), a = address.getText().trim();
        if (n.isBlank() || a.isBlank() || (!RemoteSession.enabled() && (n.contains(",") || a.contains(",")))) {
            alert(Alert.AlertType.ERROR, "Name and address are required; CSV mode does not allow commas.");
            return null;
        }
        return new Cinema(existing == null ? 0 : existing.getId(), n, a);
    }

    private List<Cinema> allCinemas() throws Exception {
        return RemoteSession.enabled() ? RemoteSession.client().cinemas() : AppContext.cinemas().getAllCinemas();
    }
    private List<entity.Room> allRooms() throws Exception {
        return RemoteSession.enabled() ? RemoteSession.client().rooms() : AppContext.rooms().getAllRooms();
    }

    private Cinema selectedCinema() {
        Cinema selected = cinemaTable.getSelectionModel().getSelectedItem();
        if (selected == null) alert(Alert.AlertType.INFORMATION, "Select a cinema in the table first.");
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
