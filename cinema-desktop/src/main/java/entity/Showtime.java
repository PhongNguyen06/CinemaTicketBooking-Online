package entity;

import enums.ShowtimeStatus;

import java.time.LocalDateTime;

public class Showtime {
    private long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ShowtimeStatus status;
    private int ticketPrice;
    private Movie movie;
    private Room room;

    public Showtime() {
    }

    public Showtime(long id, LocalDateTime startTime, LocalDateTime endTime,
                    ShowtimeStatus status, Movie movie, Room room, int ticketPrice) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.movie = movie;
        this.ticketPrice = ticketPrice;
        this.room = room;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public ShowtimeStatus getStatus() {
        return status;
    }

    public void setStatus(ShowtimeStatus status) {
        this.status = status;
    }

    public int getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(int ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}