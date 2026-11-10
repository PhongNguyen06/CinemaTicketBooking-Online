package com.cinema.controller;
import com.cinema.MainApp;
import com.cinema.remote.RemoteSession;
import com.cinema.remote.CinemaApiClient;
import entity.Showtime;
import entity.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RemoteSeatSelectionController {
 @FXML private Label greeting,infoLabel,selectionLabel,totalLabel;
 @FXML private ComboBox<Showtime> showtimeCombo;
 @FXML private FlowPane seatsPane;
 private User user;
 private static final DateTimeFormatter SHOWTIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
 private final Set<Long> selected = new LinkedHashSet<>();
 @FXML private void initialize() {
   showtimeCombo.setConverter(new StringConverter<>() {
      public String toString(Showtime s){return s==null?"":s.getMovie().getTitle()+" | "+s.getRoom().getRoomName()+" | "+s.getStartTime().format(SHOWTIME_FORMAT);}
      public Showtime fromString(String text){return null;}
   });
   try {
      showtimeCombo.setItems(FXCollections.observableArrayList(RemoteSession.client().showtimes().stream()
          .filter(s->s.getRoom()!=null && s.getMovie()!=null && s.getStartTime().isAfter(LocalDateTime.now()) && s.getStatus()==enums.ShowtimeStatus.SCHEDULED).toList()));
      showtimeCombo.getSelectionModel().selectFirst();
   } catch(Exception e){infoLabel.setText("Cannot load showtimes: "+e.getMessage());}
   refresh();
 }
 public void setUser(User u){
        com.cinema.remote.RemoteUpdates.watch(greeting, "seats", () -> refresh());
        com.cinema.remote.RemoteUpdates.watch(greeting, "showtimes", () -> refreshShowtimesLive());user=u;greeting.setText("Welcome, "+u.getUsername());}
 private void refreshShowtimesLive() {
    Showtime prior=showtimeCombo.getValue();
    try {
      var list=RemoteSession.client().showtimes().stream()
        .filter(s->s.getRoom()!=null && s.getMovie()!=null && s.getStartTime().isAfter(LocalDateTime.now())
            && s.getStatus()==enums.ShowtimeStatus.SCHEDULED).toList();
      showtimeCombo.setItems(FXCollections.observableArrayList(list));
      var restore=list.stream().filter(s->prior!=null && s.getId()==prior.getId()).findFirst().orElse(null);
      if(restore!=null)showtimeCombo.setValue(restore);else showtimeCombo.getSelectionModel().selectFirst();
      refresh();
    } catch(Exception e) { infoLabel.setText("Cannot update showtimes: "+e.getMessage()); }
 }
 @FXML private void refresh() {
    seatsPane.getChildren().clear();selected.clear();updateTotal();
    Showtime st=showtimeCombo.getValue();
    if(st==null){infoLabel.setText("No upcoming showtimes");return;}
    infoLabel.setText(st.getMovie().getTitle()+" — "+st.getTicketPrice()+" VND per seat");
    try {
       for(CinemaApiClient.RemoteSeat seat:RemoteSession.client().availableSeats(st.getId())) {
          Button b=new Button(seat.row()+seat.number());b.setMinWidth(65);b.setPrefHeight(42);
          b.setDisable(seat.booked()); b.getStyleClass().add(seat.booked()?"seat-booked":"VIP".equals(seat.type())?"seat-vip":"seat-free");
          b.setOnAction(e->{if(!selected.add(seat.id()))selected.remove(seat.id());
              b.getStyleClass().removeAll("seat-chosen","seat-vip","seat-free");
              b.getStyleClass().add(selected.contains(seat.id())?"seat-chosen":"VIP".equals(seat.type())?"seat-vip":"seat-free");updateTotal();});
          seatsPane.getChildren().add(b);
       }
    } catch(Exception e){infoLabel.setText("Could not load seats: "+e.getMessage());}
 }
 private void updateTotal(){selectionLabel.setText("Selected seats: "+selected.size());Showtime s=showtimeCombo.getValue();
   totalLabel.setText("Estimated total: "+(s==null?0:(long)s.getTicketPrice()*selected.size())+" VND");}
 @FXML private void confirmBooking(){
    Showtime st=showtimeCombo.getValue();if(st==null || selected.isEmpty()){alert("Select seats first",false);return;}
    if(new Alert(Alert.AlertType.CONFIRMATION,"Confirm "+selected.size()+" tickets?",ButtonType.YES,ButtonType.NO).showAndWait().orElse(ButtonType.NO)!=ButtonType.YES)return;
    try {var result=RemoteSession.client().bookSeats(st.getId(),new ArrayList<>(selected));alert("Booking #"+result.id()+" confirmed. Total: "+result.totalAmount()+" VND",true);}
    catch(Exception e){alert("Booking failed: "+e.getMessage(),false);}finally{refresh();}
 }
 private void alert(String msg,boolean success){new Alert(success?Alert.AlertType.INFORMATION:Alert.AlertType.ERROR,msg).showAndWait();}
 @FXML private void backToDashboard(){
   try {FXMLLoader l=new FXMLLoader(MainApp.class.getResource("/com/cinema/view/dashboard.fxml"));Scene sc=new Scene(l.load(),1100,700);
      sc.getStylesheets().add(MainApp.class.getResource("/com/cinema/css/style.css").toExternalForm());
      ((DashboardController)l.getController()).setUser(user);Stage stage=(Stage)greeting.getScene().getWindow();stage.setScene(sc);
   }catch(Exception e){alert(e.getMessage(),false);}
 }
}
