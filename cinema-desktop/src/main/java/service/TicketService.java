package service;

import entity.Booking;
import entity.Seat;
import entity.Showtime;
import entity.Ticket;
import enums.TicketStatus;
import repository.TicketRepository;

import java.util.ArrayList;
import java.util.List;

public class TicketService {

    private final TicketRepository repository;
    private final List<Ticket> tickets;

    public TicketService(
            List<Booking> bookings,
            List<Seat> seats,
            List<Showtime> showtimes) {

        repository = new TicketRepository();
        tickets = repository.findAll(bookings, seats, showtimes);
    }

    public void addTicket(Ticket ticket) {
        ticket.setId(getNextId());
        if (ticket.getSeat() != null
                && ticket.getShowtime() != null
                && isSeatBooked(ticket.getSeat(), ticket.getShowtime())) {
            System.out.println("Seat is already booked.");
            return;
        }

        tickets.add(ticket);
        repository.save(tickets);
    }

    public List<Ticket> getAllTickets() {
        return tickets;
    }

    public Ticket findById(long id) {
        for (Ticket ticket : tickets) {
            if (ticket.getId() == id) {
                return ticket;
            }
        }
        return null;
    }

    public List<Ticket> findByShowtime(long showtimeId) {
        List<Ticket> result = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (ticket.getShowtime() != null
                    && ticket.getShowtime().getId() == showtimeId) {
                result.add(ticket);
            }
        }

        return result;
    }

    public List<Ticket> findByBooking(long bookingId) {
        List<Ticket> result = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (ticket.getBooking() != null
                    && ticket.getBooking().getId() == bookingId) {
                result.add(ticket);
            }
        }

        return result;
    }

    public boolean isSeatBooked(Seat seat, Showtime showtime) {
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.BOOKED
                    && ticket.getSeat() != null
                    && ticket.getShowtime() != null
                    && ticket.getSeat().getId() == seat.getId()
                    && ticket.getShowtime().getId() == showtime.getId()) {
                return true;
            }
        }

        return false;
    }

    public void cancelTicket(long id) {
        Ticket ticket = findById(id);

        if (ticket != null) {
            ticket.setStatus(TicketStatus.CANCELLED);
            repository.save(tickets);
        }
    }

    private long getNextId() {
        long maxId = 0;

        for (Ticket ticket : tickets) {
            if (ticket.getId() > maxId) {
                maxId = ticket.getId();
            }
        }

        return maxId + 1;
    }
}
