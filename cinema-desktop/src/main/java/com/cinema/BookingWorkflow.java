package com.cinema;

import entity.*;
import enums.*;
import repository.BookingRepository;
import repository.TicketRepository;
import repository.StorageIO;
import repository.MySqlBookingStore;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/** Single-process CSV booking workflow. Migrate to DB transactions for multi-user deployments. */
public final class BookingWorkflow {
    private static final Object LOCK = new Object();
    private static final Path BOOKINGS = Path.of("database", "bookings.csv");
    private static final Path TICKETS = Path.of("database", "tickets.csv");

    private BookingWorkflow() {}

    public static Booking confirm(User user, Showtime showtime, Collection<Long> requestedIds) {
        synchronized (LOCK) {
            if (StorageIO.mysqlEnabled()) return MySqlBookingStore.confirm(user, showtime, requestedIds);
            if (user == null || user.getRole() != Role.USER || user.getCustomer() == null)
                throw new IllegalArgumentException("Customer account is required.");
            if (showtime == null || showtime.getRoom() == null || showtime.getStartTime() == null
                    || !showtime.getStartTime().isAfter(LocalDateTime.now())
                    || showtime.getStatus() != ShowtimeStatus.SCHEDULED || showtime.getTicketPrice() <= 0)
                throw new IllegalArgumentException("This showtime is not available for booking.");
            if (requestedIds == null || requestedIds.isEmpty())
                throw new IllegalArgumentException("Please select at least one seat.");
            List<Long> ids = new ArrayList<>(requestedIds);
            if (new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("Duplicate seats in selection.");
            List<Seat> seats = new ArrayList<>();
            for (long id : ids) {
                Seat seat = AppContext.seats().findById(id);
                if (seat == null || seat.getRoom() == null || seat.getRoom().getId() != showtime.getRoom().getId())
                    throw new IllegalArgumentException("Invalid seat for selected showtime.");
                if (AppContext.tickets().isSeatBooked(seat, showtime))
                    throw new IllegalArgumentException("One or more seats have already been booked. Please refresh.");
                seats.add(seat);
            }
            List<Booking> bookings = AppContext.bookings().getAllBookings();
            List<Ticket> tickets = AppContext.tickets().getAllTickets();
            long bookingId = bookings.stream().mapToLong(Booking::getId).max().orElse(0) + 1;
            long ticketId = tickets.stream().mapToLong(Ticket::getId).max().orElse(0) + 1;
            Booking booking = new Booking(bookingId, LocalDateTime.now(),
                    (double) showtime.getTicketPrice() * seats.size(), BookingStatus.CONFIRMED, user.getCustomer());
            List<Ticket> additions = new ArrayList<>();
            for (Seat seat : seats) additions.add(new Ticket(ticketId++, showtime.getTicketPrice(),
                    TicketStatus.BOOKED, booking, seat, showtime));
            Snapshot snapshot = new Snapshot();
            try {
                bookings.add(booking);
                tickets.addAll(additions);
                new BookingRepository().save(bookings);
                new TicketRepository().save(tickets);
                return booking;
            } catch (RuntimeException ex) {
                tickets.removeAll(additions);
                bookings.remove(booking);
                snapshot.restore(ex);
                throw new IllegalStateException("Booking could not be saved; please retry. " + ex.getMessage(), ex);
            }
        }
    }

    public static void cancel(Booking booking, User user) {
        synchronized (LOCK) {
            if (StorageIO.mysqlEnabled()) { MySqlBookingStore.cancel(booking, user); return; }
            if (booking == null || user == null) throw new IllegalArgumentException("No booking selected.");
            boolean admin = user.getRole() == Role.ADMIN;
            if (!admin && (user.getCustomer() == null || booking.getCustomer() == null
                    || user.getCustomer().getId() != booking.getCustomer().getId()))
                throw new IllegalArgumentException("You cannot cancel this booking.");
            if (booking.getStatus() == BookingStatus.CANCELLED) throw new IllegalArgumentException("Booking already cancelled.");
            List<Ticket> related = AppContext.tickets().findByBooking(booking.getId());
            if (related.stream().anyMatch(t -> t.getShowtime() != null
                    && t.getShowtime().getStartTime() != null
                    && !t.getShowtime().getStartTime().isAfter(LocalDateTime.now())))
                throw new IllegalArgumentException("Cannot cancel a booking after the showtime starts.");
            Snapshot snapshot = new Snapshot();
            BookingStatus originalBookingStatus = booking.getStatus();
            Map<Ticket, TicketStatus> original = new IdentityHashMap<>();
            related.forEach(t -> original.put(t, t.getStatus()));
            try {
                booking.setStatus(BookingStatus.CANCELLED);
                related.forEach(t -> t.setStatus(TicketStatus.CANCELLED));
                new BookingRepository().save(AppContext.bookings().getAllBookings());
                new TicketRepository().save(AppContext.tickets().getAllTickets());
            } catch (RuntimeException ex) {
                booking.setStatus(originalBookingStatus);
                original.forEach(Ticket::setStatus);
                snapshot.restore(ex);
                throw new IllegalStateException("Cancellation could not be saved: " + ex.getMessage(), ex);
            }
        }
    }

    private static final class Snapshot {
        private final byte[] oldBookings;
        private final byte[] oldTickets;
        Snapshot() {
            try {
                oldBookings = Files.exists(BOOKINGS) ? Files.readAllBytes(BOOKINGS) : null;
                oldTickets = Files.exists(TICKETS) ? Files.readAllBytes(TICKETS) : null;
            } catch (IOException ex) { throw new IllegalStateException("Could not prepare file backup", ex); }
        }
        void restore(RuntimeException parent) {
            try { restoreFile(BOOKINGS, oldBookings); } catch (IOException ex) { parent.addSuppressed(ex); }
            try { restoreFile(TICKETS, oldTickets); } catch (IOException ex) { parent.addSuppressed(ex); }
        }
        private void restoreFile(Path file, byte[] bytes) throws IOException {
            if (bytes == null) Files.deleteIfExists(file);
            else Files.write(file, bytes);
        }
    }
}
