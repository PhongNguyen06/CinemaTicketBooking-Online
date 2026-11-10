package entity;

import enums.TicketStatus;

public class Ticket {
    private long id;
    private int price;
    private TicketStatus status;
    private Booking booking;
    private Seat seat;
    private Showtime showtime;

    public Ticket() {
    }

    public Ticket(long id, int price, TicketStatus status,
                  Booking booking, Seat seat, Showtime showtime) {
        this.id = id;
        this.price = price;
        this.status = status;
        this.booking = booking;
        this.seat = seat;
        this.showtime = showtime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }
}