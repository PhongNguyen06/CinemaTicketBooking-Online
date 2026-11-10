package com.cinema.controller;

import com.cinema.AppContext;
import com.cinema.MainApp;
import com.cinema.remote.RemoteSession;
import entity.Movie;
import entity.User;
import enums.Role;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import java.util.Optional;

public class DashboardController {
    @FXML private Label greeting;
    @FXML private Label screenTitle;
    @FXML private TableView<Movie> movieTable;
    @FXML private TableColumn<Movie,Number> idColumn;
    @FXML private TableColumn<Movie,String> titleColumn;
    @FXML private TableColumn<Movie,Number> durationColumn;
    @FXML private TableColumn<Movie,String> genreColumn;
    @FXML private TableColumn<Movie,String> descriptionColumn;
    @FXML private Button addMovieButton;
    @FXML private Button deleteMovieButton;
    @FXML private Button cinemaNavButton;
    @FXML private Button roomNavButton;
    @FXML private Button showtimeNavButton;
    @FXML private Button seatNavButton;
    @FXML private Button selectSeatNavButton;
    @FXML private Button bookingsNavButton;
    @FXML private Button customerNavButton;
    @FXML private Label dashboardStats;
    private User currentUser;

    @FXML private void initialize() {
        idColumn.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        titleColumn.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().getTitle()));
        durationColumn.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue().getDuration()));
        genreColumn.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().getGenre()));
        descriptionColumn.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().getDescription()));
        if (!RemoteSession.enabled()) refreshMovies();
    }
    public void setUser(User user) {
        com.cinema.remote.RemoteUpdates.watch(greeting, "movies", () -> refreshMovies());
        currentUser=user;
        boolean admin=user.getRole()==Role.ADMIN;
        screenTitle.setText(admin ? "ADMIN MENU" : "CUSTOMER MENU");
        greeting.setText("Welcome, " + user.getUsername());
        addMovieButton.setVisible(admin); addMovieButton.setManaged(admin);
        deleteMovieButton.setVisible(admin); deleteMovieButton.setManaged(admin);
        cinemaNavButton.setVisible(admin); cinemaNavButton.setManaged(admin);
        roomNavButton.setVisible(admin); roomNavButton.setManaged(admin);
        showtimeNavButton.setVisible(admin); showtimeNavButton.setManaged(admin);
        seatNavButton.setVisible(admin); seatNavButton.setManaged(admin);
        selectSeatNavButton.setVisible(!admin); selectSeatNavButton.setManaged(!admin);
        bookingsNavButton.setText(admin ? "Booking & Ticket Management" : "My Bookings");
        customerNavButton.setVisible(admin); customerNavButton.setManaged(admin);
        dashboardStats.setVisible(admin); dashboardStats.setManaged(admin);
        if (RemoteSession.enabled()) {
            // The remaining legacy screens still access repositories directly.
            // Hide them until their server endpoints and controllers are migrated.
            for (Button button : new Button[]{customerNavButton}) {
                button.setVisible(false); button.setManaged(false);
            }
            dashboardStats.setText("Online mode — shared movies, cinemas, rooms, seats and showtimes (refresh to sync)");
            dashboardStats.setVisible(true);dashboardStats.setManaged(true);
        } else if (admin) refreshDashboardStats();
        refreshMovies();
    }
    @FXML private void refreshMovies() {
        try {
            movieTable.setItems(FXCollections.observableArrayList(RemoteSession.enabled()
                    ? RemoteSession.client().movies() : AppContext.movies().getAllMovies()));
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,"Could not load movies: " + ex.getMessage()).showAndWait();
        }
    }
    @FXML private void addMovie() {
        if (currentUser==null || currentUser.getRole()!=Role.ADMIN) return;
        Dialog<ButtonType> dialog=new Dialog<>(); dialog.setTitle("Add Movie");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField title=new TextField(), duration=new TextField(), genre=new TextField();
        TextArea description=new TextArea(); description.setPrefRowCount(3);
        GridPane form=new GridPane(); form.setHgap(10);form.setVgap(10);form.setPadding(new Insets(16));
        form.addRow(0,new Label("Title"),title);form.addRow(1,new Label("Duration (minutes)"),duration);
        form.addRow(2,new Label("Genre"),genre);form.addRow(3,new Label("Description"),description);
        dialog.getDialogPane().setContent(form);
        Optional<ButtonType> response=dialog.showAndWait();
        if (response.isEmpty() || response.get()!=ButtonType.OK) return;
        try {
            int minutes=Integer.parseInt(duration.getText().trim());
            if (minutes<=0 || title.getText().isBlank() || genre.getText().isBlank()) throw new IllegalArgumentException();
            Movie movie = new Movie(0,title.getText().trim(),minutes,genre.getText().trim(),description.getText().trim());
            if (RemoteSession.enabled()) RemoteSession.client().addMovie(movie);
            else AppContext.movies().addMovie(movie);
            refreshMovies();
        } catch (Exception e) { new Alert(Alert.AlertType.ERROR,"Unable to add movie: " + e.getMessage()).showAndWait(); }
    }
    @FXML private void deleteMovie() {
        if (currentUser==null || currentUser.getRole()!=Role.ADMIN) return;
        Movie selected=movieTable.getSelectionModel().getSelectedItem();
        if (selected==null) {new Alert(Alert.AlertType.INFORMATION,"Select a movie first.").showAndWait();return;}
        if (!RemoteSession.enabled() && !AppContext.showtimes().findByMovie(selected.getId()).isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "This movie has showtimes. Remove the showtimes first.").showAndWait();
            return;
        }
        Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Delete movie: "+selected.getTitle()+"?",ButtonType.YES,ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO)==ButtonType.YES) {
            try {
                if (RemoteSession.enabled()) RemoteSession.client().deleteMovie(selected.getId());
                else AppContext.movies().deleteMovie(selected.getId());
                refreshMovies();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR,"Unable to delete movie: " + e.getMessage()).showAndWait();
            }
        }
    }
    @FXML private void openCinemaManagement() {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/cinema.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            CinemaController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Cinema - Cinema Management");
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot open cinema management: " + ex.getMessage()).showAndWait();
        }
    }
    @FXML private void openRoomManagement() {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/room.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            RoomController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Cinema - Room Management");
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot open room management: " + ex.getMessage()).showAndWait();
        }
    }
    @FXML private void openShowtimeManagement() {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/showtime.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            ShowtimeController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Cinema - Showtime Management");
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot open showtime management: " + ex.getMessage()).showAndWait();
        }
    }
    @FXML private void openSeatManagement() {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) return;
        openSeatScreen("/com/cinema/view/seat.fxml", "Cinema - Seat Management");
    }
    @FXML private void openSeatSelection() {
        if (currentUser == null || currentUser.getRole() != Role.USER) return;
        openSeatScreen(RemoteSession.enabled() ? "/com/cinema/view/remote-seat-selection.fxml" : "/com/cinema/view/seat-selection.fxml", "Cinema - Seat Selection");
    }
    private void openSeatScreen(String resource, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(resource));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            if (loader.getController() instanceof SeatController c) c.setUser(currentUser);
            if (loader.getController() instanceof SeatSelectionController c) c.setUser(currentUser);
            if (loader.getController() instanceof RemoteSeatSelectionController c) c.setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot open seat screen: " + ex.getMessage()).showAndWait();
        }
    }
    @FXML private void openBookings() {
        if (currentUser == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(RemoteSession.enabled() ? "/com/cinema/view/remote-bookings.fxml" : "/com/cinema/view/bookings.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            if (loader.getController() instanceof BookingsController c) c.setUser(currentUser);
            if (loader.getController() instanceof RemoteBookingsController c) c.setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Cinema - Bookings");
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot open bookings: " + ex.getMessage()).showAndWait();
        }
    }
    @FXML private void refreshDashboardStats() {
        dashboardStats.setText("Movies: " + AppContext.movies().getAllMovies().size()
                + "   |   Cinemas: " + AppContext.cinemas().getAllCinemas().size()
                + "   |   Rooms: " + AppContext.rooms().getAllRooms().size()
                + "   |   Customers: " + AppContext.customers().getAllCustomers().size()
                + "   |   Bookings: " + AppContext.bookings().getAllBookings().size());
    }
    @FXML private void openCustomerManagement() {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/customer.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            ((CustomerController) loader.getController()).setUser(currentUser);
            Stage stage = (Stage) greeting.getScene().getWindow();
            stage.setScene(scene); stage.setTitle("Cinema - Customer Management");
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot open customers: " + ex.getMessage()).showAndWait();
        }
    }
    @FXML private void changePassword() {
        if (RemoteSession.enabled()) {
            new Alert(Alert.AlertType.INFORMATION,"Online password change is not yet migrated.").showAndWait();return;
        }
        if (currentUser == null) return;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Update password for " + currentUser.getUsername());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        PasswordField oldPassword = new PasswordField();
        PasswordField newPassword = new PasswordField();
        PasswordField confirmPassword = new PasswordField();
        oldPassword.setPromptText("Current password");
        newPassword.setPromptText("At least 12 characters");
        confirmPassword.setPromptText("Repeat new password");
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10); form.setPadding(new Insets(16));
        form.addRow(0, new Label("Current password"), oldPassword);
        form.addRow(1, new Label("New password"), newPassword);
        form.addRow(2, new Label("Confirm password"), confirmPassword);
        dialog.getDialogPane().setContent(form);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        if (!newPassword.getText().equals(confirmPassword.getText())) {
            new Alert(Alert.AlertType.ERROR, "New passwords do not match.").showAndWait(); return;
        }
        try {
            boolean changed = AppContext.users().changePassword(currentUser,
                    oldPassword.getText(), newPassword.getText());
            if (changed) {
                new Alert(Alert.AlertType.INFORMATION, "Password updated successfully. "+
                        "Use the new password next time you sign in.").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR,
                        "Current password is incorrect or the new password is unchanged.").showAndWait();
            }
        } catch (IllegalArgumentException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.ERROR, "Password could not be saved. Please try again.").showAndWait();
        }
    }

    @FXML private void logout() {
        Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Are you sure you want to logout?",ButtonType.YES,ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO)!=ButtonType.YES)return;
        try {
            if (RemoteSession.enabled()) RemoteSession.client().logout();
            FXMLLoader loader=new FXMLLoader(MainApp.class.getResource("/com/cinema/view/login.fxml"));
            Scene scene=new Scene(loader.load(),1000,650);
            scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
            Stage stage=(Stage)greeting.getScene().getWindow(); stage.setScene(scene);stage.setTitle("Cinema Ticket Booking System");
        } catch(Exception ex){new Alert(Alert.AlertType.ERROR,ex.getMessage()).showAndWait();}
    }
}
