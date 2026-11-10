package com.cinema;

import com.cinema.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/cinema/view/login.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 650);
        scene.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
        stage.setTitle("Cinema Ticket Booking System");
        stage.setMinWidth(850);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
