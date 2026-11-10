package com.cinema.controller;
import com.cinema.MainApp;
import com.cinema.remote.CinemaApiClient;
import com.cinema.remote.RemoteSession;
import entity.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RemoteBookingsController {
 @FXML private Label greeting,pageTitle;
 @FXML private TableView<CinemaApiClient.RemoteBooking> bookingsTable;
 @FXML private TableColumn<CinemaApiClient.RemoteBooking,Number> idCol,totalCol;
 @FXML private TableColumn<CinemaApiClient.RemoteBooking,String> dateCol,statusCol,customerCol;
 @FXML private ListView<String> ticketList;
 private User user;
 private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
 private String displayDate(String raw) {
   try {return LocalDateTime.parse(raw.replace(' ', 'T')).format(DISPLAY_DATE);}
   catch (Exception ex) {return raw;}
 }
 @FXML private void initialize(){
   idCol.setCellValueFactory(c->new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().id()));
   totalCol.setCellValueFactory(c->new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().totalAmount()));
   dateCol.setCellValueFactory(c->new javafx.beans.property.ReadOnlyStringWrapper(displayDate(c.getValue().date())));
   statusCol.setCellValueFactory(c->new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().status()));
   customerCol.setCellValueFactory(c->new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().customerName()));
   bookingsTable.getSelectionModel().selectedItemProperty().addListener((o,a,b)->loadTickets(b));
 }
 public void setUser(User u){
        com.cinema.remote.RemoteUpdates.watch(greeting, "reservations", () -> refresh());user=u;greeting.setText("Welcome, "+u.getUsername());pageTitle.setText(u.getRole()==enums.Role.ADMIN?"All Bookings":"My Bookings");refresh();}
 @FXML private void refresh(){try {bookingsTable.setItems(FXCollections.observableArrayList(RemoteSession.client().bookings()));ticketList.getItems().clear();}
   catch(Exception e){new Alert(Alert.AlertType.ERROR,"Could not refresh: "+e.getMessage()).showAndWait();}}
 private void loadTickets(CinemaApiClient.RemoteBooking b){ticketList.getItems().clear();if(b==null)return;
   try{for(var t:RemoteSession.client().bookingTickets(b.id()))ticketList.getItems().add("#"+t.id()+" | "+t.movie()+" | "+t.seat()+" | "+t.price()+" VND | "+t.status());}
   catch(Exception e){new Alert(Alert.AlertType.ERROR,e.getMessage()).showAndWait();}}
 @FXML private void cancelSelected(){var b=bookingsTable.getSelectionModel().getSelectedItem();if(b==null)return;
   if(new Alert(Alert.AlertType.CONFIRMATION,"Cancel booking #"+b.id()+"?",ButtonType.YES,ButtonType.NO).showAndWait().orElse(ButtonType.NO)!=ButtonType.YES)return;
   try{RemoteSession.client().cancelBooking(b.id());refresh();}catch(Exception e){new Alert(Alert.AlertType.ERROR,e.getMessage()).showAndWait();}}
 @FXML private void backToDashboard(){try{FXMLLoader l=new FXMLLoader(MainApp.class.getResource("/com/cinema/view/dashboard.fxml"));
   Scene sc=new Scene(l.load(),1100,700);sc.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
   ((DashboardController)l.getController()).setUser(user);((Stage)greeting.getScene().getWindow()).setScene(sc);
 }catch(Exception e){new Alert(Alert.AlertType.ERROR,e.getMessage()).showAndWait();}}
}
