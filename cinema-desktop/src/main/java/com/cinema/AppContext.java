package com.cinema;

import service.BookingService;
import service.TicketService;
import service.CinemaService;
import service.CustomerService;
import service.MovieService;
import service.RoomService;
import service.SeatService;
import service.ShowtimeService;
import service.UserService;

public final class AppContext {
    private static final CinemaService cinemaService = new CinemaService();
    private static final RoomService roomService = new RoomService(cinemaService.getAllCinemas());
    private static final SeatService seatService = new SeatService(roomService.getAllRooms());
    private static final CustomerService customerService = new CustomerService();
    private static final MovieService movieService = new MovieService();
    private static final ShowtimeService showtimeService = new ShowtimeService(movieService.getAllMovies(), roomService.getAllRooms());
    private static final UserService userService = new UserService(customerService.getAllCustomers());
    private static final BookingService bookingService = new BookingService(customerService.getAllCustomers());
    private static final TicketService ticketService = new TicketService(bookingService.getAllBookings(), seatService.getAllSeats(), showtimeService.getAllShowtimes());
    public static void reloadBookingsAndTickets() {
        if (!repository.StorageIO.mysqlEnabled()) return;
        // Keep list instances because other services/controllers hold references to them.
        var refreshedBookings = new repository.BookingRepository().findAll(customerService.getAllCustomers());
        bookingService.getAllBookings().clear();
        bookingService.getAllBookings().addAll(refreshedBookings);
        var refreshedTickets = new repository.TicketRepository().findAll(
                bookingService.getAllBookings(), seatService.getAllSeats(), showtimeService.getAllShowtimes());
        ticketService.getAllTickets().clear();
        ticketService.getAllTickets().addAll(refreshedTickets);
    }
    public static BookingService bookings() { return bookingService; }
    public static TicketService tickets() { return ticketService; }
    private AppContext() {}
    public static RoomService rooms() { return roomService; }
    public static SeatService seats() { return seatService; }
    public static ShowtimeService showtimes() { return showtimeService; }
    public static CinemaService cinemas() { return cinemaService; }
    public static CustomerService customers() { return customerService; }
    public static MovieService movies() { return movieService; }
    public static UserService users() { return userService; }
}
