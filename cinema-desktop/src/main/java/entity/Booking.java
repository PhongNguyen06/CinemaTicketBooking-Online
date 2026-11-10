package entity;

import enums.BookingStatus;

import java.time.LocalDateTime;

public class Booking {
    private long id;
    private LocalDateTime bookingDate;
    private double totalAmount;
    private BookingStatus status;
    private Customer customer;

    public Booking() {
    }

    public Booking(long id, LocalDateTime bookingDate, double totalAmount,
                   BookingStatus status, Customer customer) {
        this.id = id;
        this.bookingDate = bookingDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.customer = customer;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}