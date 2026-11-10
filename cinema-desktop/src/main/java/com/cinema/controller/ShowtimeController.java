package com.cinema.controller;

import com.cinema.AppContext;
import com.cinema.MainApp;
import com.cinema.remote.RemoteSession;
import entity.Movie;
import entity.Room;
import entity.Showtime;
import entity.User;
import enums.Role;
import enums.ShowtimeStatus;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ShowtimeController {
    @FXML private Label greeting;
    @FXML private TableView<Showtime> showtimeTable;
    @FXML private TableColumn<Showtime, Number> idColumn;
    @FXML private TableColumn<Showtime, String> movieColumn;
    @FXML private TableColumn<Showtime, String> roomColumn;
    @FXML private TableColumn<Showtime, String> startColumn;
    @FXML private TableColumn<Showtime, String> endColumn;
    @FXML private TableColumn<Showtime, Number> priceColumn;
    @FXML private TableColumn<Showtime, String> statusColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchType;
    private User currentUser;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private void initialize() {
        idColumn.setCellValueFactory(r -> new ReadOnlyObjectWrapper<>(r.getValue().getId()));
        movieColumn.setCellValueFactory(r -> new ReadOnlyStringWrapper(r.getValue().getMovie() == null ? "Missing movie" : r.getValue().getMovie().getTitle()));
        roomColumn.setCellValueFactory(r -> new ReadOnlyStringWrapper(roomLabel(r.getValue().getRoom())));
        startColumn.setCellValueFactory(r -> new ReadOnlyStringWrapper(r.getValue().getStartTime().format(DISPLAY_FORMAT)));
        endColumn.setCellValueFactory(r -> new ReadOnlyStringWrapper(r.getValue().getEndTime().format(DISPLAY_FORMAT)));
        priceColumn.setCellValueFactory(r -> new ReadOnlyObjectWrapper<>(r.getValue().getTicketPrice()));
        statusColumn.setCellValueFactory(r -> new ReadOnlyStringWrapper(r.getValue().getStatus().name()));
        searchType.setItems(FXCollections.observableArrayList("Movie", "Room", "Cinema", "Status"));
        searchType.getSelectionModel().selectFirst();
        refreshShowtimes();
    }

    public void setUser(User user) {
        com.cinema.remote.RemoteUpdates.watch(greeting, "showtimes", () -> refreshShowtimes());
        com.cinema.remote.RemoteUpdates.watch(greeting, "movies", () -> refreshShowtimes());
        com.cinema.remote.RemoteUpdates.watch(greeting, "rooms", () -> refreshShowtimes());
        if (user == null || user.getRole() != Role.ADMIN) throw new IllegalArgumentException("Administrator access required.");
        currentUser = user;
        greeting.setText("Welcome, " + user.getUsername());
    }

    private String roomLabel(Room room) {
        if (room == null) return "Missing room";
        return (room.getCinema() == null ? "Unknown cinema" : room.getCinema().getName()) + " / " + room.getRoomName();
    }

    @FXML private void refreshShowtimes() {
        searchField.clear();
        showtimeTable.setItems(FXCollections.observableArrayList(allShowtimes()));
    }

    @FXML private void searchShowtimes() {
        String q = searchField.getText().trim().toLowerCase();
        String type = searchType.getValue();
        List<Showtime> matched = allShowtimes().stream().filter(s -> {
            return switch (type == null ? "Movie" : type) {
                case "Room" -> s.getRoom() != null && s.getRoom().getRoomName().toLowerCase().contains(q);
                case "Cinema" -> s.getRoom() != null && s.getRoom().getCinema() != null && s.getRoom().getCinema().getName().toLowerCase().contains(q);
                case "Status" -> s.getStatus() != null && s.getStatus().name().toLowerCase().contains(q);
                default -> s.getMovie() != null && s.getMovie().getTitle().toLowerCase().contains(q);
            };
        }).toList();
        showtimeTable.setItems(FXCollections.observableArrayList(matched));
    }

    @FXML private void addShowtime() {
        if (!isAdmin()) return;
        Showtime added = showDialog("Add Showtime", null);
        if (added == null) return;
        try { if(RemoteSession.enabled()) RemoteSession.client().addShowtime(added); else AppContext.showtimes().addShowtime(added); } catch(Exception e){alert(Alert.AlertType.ERROR,e.getMessage());return;}
        refreshShowtimes();
    }

    @FXML private void editShowtime() {
        if (!isAdmin()) return;
        Showtime existing = selected();
        if (existing == null) return;
        if (hasAssociatedTickets(existing.getId())) {
            alert(Alert.AlertType.WARNING, "This showtime has ticket records. Editing is blocked to preserve ticket history.");
            return;
        }
        Showtime edited = showDialog("Edit Showtime", existing);
        if (edited == null) return;
        try { if(RemoteSession.enabled()) RemoteSession.client().updateShowtime(edited); else AppContext.showtimes().updateShowtime(edited); } catch(Exception e){alert(Alert.AlertType.ERROR,e.getMessage());return;}
        refreshShowtimes();
    }

    @FXML private void deleteShowtime() {
        if (!isAdmin()) return;
        Showtime selected = selected();
        if (selected == null) return;
        if (hasAssociatedTickets(selected.getId())) {
            alert(Alert.AlertType.WARNING, "This showtime has ticket records. It cannot be deleted.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete showtime #" + selected.getId() + "?", ButtonType.YES, ButtonType.NO);
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try { if(RemoteSession.enabled()) RemoteSession.client().deleteShowtime(selected.getId()); else AppContext.showtimes().deleteShowtime(selected.getId()); } catch(Exception e){alert(Alert.AlertType.ERROR,e.getMessage());return;}
        refreshShowtimes();
    }

    // Preserves foreign keys in tickets.csv, including cancelled/historical tickets.
    private boolean hasAssociatedTickets(long showtimeId) {
        if(RemoteSession.enabled()) return false; // Server enforces ticket-history restrictions. 
        try (var lines = Files.lines(Path.of("database", "tickets.csv"))) {
            return lines.map(line -> line.split(",", -1))
                    .anyMatch(fields -> fields.length > 5 && fields[5].trim().equals(String.valueOf(showtimeId)));
        } catch (java.nio.file.NoSuchFileException ex) {
            return false;
        } catch (IOException ex) {
            alert(Alert.AlertType.ERROR, "Cannot check ticket data: " + ex.getMessage());
            return true; // fail closed to protect references
        }
    }

    private Showtime showDialog(String title, Showtime existing) {
        if (allMovies().isEmpty() || allRooms().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Create a movie and a room before adding showtimes.");
            return null;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ComboBox<Movie> movie = new ComboBox<>(FXCollections.observableArrayList(allMovies()));
        movie.setConverter(new StringConverter<>() {
            @Override public String toString(Movie m) { return m == null ? "" : m.getId() + " - " + m.getTitle(); }
            @Override public Movie fromString(String s) { return null; }
        });
        ComboBox<Room> room = new ComboBox<>(FXCollections.observableArrayList(allRooms()));
        room.setConverter(new StringConverter<>() {
            @Override public String toString(Room r) { return r == null ? "" : r.getId() + " - " + roomLabel(r); }
            @Override public Room fromString(String s) { return null; }
        });
        movie.setPrefWidth(290);
        room.setPrefWidth(290);
        DatePicker date = new DatePicker(existing == null ? LocalDate.now() : existing.getStartTime().toLocalDate());
        TextField start = new TextField(existing == null ? "18:00" : existing.getStartTime().format(TIME_FORMAT));
        TextField end = new TextField(existing == null ? "" : existing.getEndTime().format(TIME_FORMAT));
        TextField price = new TextField(existing == null ? "70000" : Integer.toString(existing.getTicketPrice()));
        ComboBox<ShowtimeStatus> status = new ComboBox<>(FXCollections.observableArrayList(ShowtimeStatus.values()));
        status.setValue(existing == null ? ShowtimeStatus.SCHEDULED : existing.getStatus());
        if (existing != null) {
            movie.setValue(existing.getMovie());
            room.setValue(existing.getRoom());
        }
        Label hint = new Label("Date applies to start; end time may pass midnight. HH:mm, 24-hour format.");
        hint.setWrapText(true);
        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(12); form.setPadding(new Insets(15));
        form.addRow(0, new Label("Movie:"), movie);
        form.addRow(1, new Label("Room / Cinema:"), room);
        form.addRow(2, new Label("Start date:"), date);
        form.addRow(3, new Label("Start time:"), start);
        form.addRow(4, new Label("End time:"), end);
        form.addRow(5, new Label("Ticket price (VND):"), price);
        form.addRow(6, new Label("Status:"), status);
        form.add(hint, 0, 7, 2, 1);
        dialog.getDialogPane().setContent(form);
        while (true) {
            if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return null;
            try {
                if (movie.getValue() == null || room.getValue() == null || date.getValue() == null || status.getValue() == null)
                    throw new IllegalArgumentException("Select a movie, room, date and status.");
                LocalDateTime begins = LocalDateTime.of(date.getValue(), LocalTime.parse(start.getText().trim(), TIME_FORMAT));
                LocalDateTime ends;
                if (end.getText().isBlank()) {
                    ends = begins.plusMinutes(movie.getValue().getDuration());
                } else {
                    ends = LocalDateTime.of(date.getValue(), LocalTime.parse(end.getText().trim(), TIME_FORMAT));
                    if (!ends.isAfter(begins)) ends = ends.plusDays(1);
                }
                int amount = Integer.parseInt(price.getText().trim());
                if (amount <= 0) throw new IllegalArgumentException("Ticket price must be positive.");
                if (!ends.isAfter(begins)) throw new IllegalArgumentException("End must be after start.");
                // Check intersection of [start,end) for all active screenings in the same room.
                for (Showtime other : allShowtimes()) {
                    if (existing != null && other.getId() == existing.getId()) continue;
                    if (other.getRoom() != null && other.getRoom().getId() == room.getValue().getId()
                            && other.getStatus() != ShowtimeStatus.CANCELLED && status.getValue() != ShowtimeStatus.CANCELLED
                            && begins.isBefore(other.getEndTime()) && other.getStartTime().isBefore(ends)) {
                        throw new IllegalArgumentException("Overlapping showtime in the same room: #" + other.getId());
                    }
                }
                return new Showtime(existing == null ? 0 : existing.getId(), begins, ends,
                        status.getValue(), movie.getValue(), room.getValue(), amount);
            } catch (DateTimeParseException ex) {
                alert(Alert.AlertType.ERROR, "Enter a valid time in HH:mm format (for example 18:30).");
            } catch (IllegalArgumentException ex) {
                alert(Alert.AlertType.ERROR, ex.getMessage());
            }
        }
    }

    private Showtime selected() {
        Showtime selected = showtimeTable.getSelectionModel().getSelectedItem();
        if (selected == null) alert(Alert.AlertType.INFORMATION, "Select a showtime first.");
        return selected;
    }
    private List<Movie> allMovies() {
        try {return RemoteSession.enabled()?RemoteSession.client().movies():AppContext.movies().getAllMovies();}
        catch(Exception e){alert(Alert.AlertType.ERROR,"Cannot load movies: "+e.getMessage());return List.of();}
    }
    private List<Room> allRooms() {
        try {return RemoteSession.enabled()?RemoteSession.client().rooms():AppContext.rooms().getAllRooms();}
        catch(Exception e){alert(Alert.AlertType.ERROR,"Cannot load rooms: "+e.getMessage());return List.of();}
    }
    private List<Showtime> allShowtimes() {
        try {return RemoteSession.enabled()?RemoteSession.client().showtimes():AppContext.showtimes().getAllShowtimes();}
        catch(Exception e){alert(Alert.AlertType.ERROR,"Cannot load showtimes: "+e.getMessage());return List.of();}
    }
    private boolean isAdmin() { return currentUser != null && currentUser.getRole() == Role.ADMIN; }
    private void alert(Alert.AlertType type, String msg) { new Alert(type, msg).showAndWait(); }

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
