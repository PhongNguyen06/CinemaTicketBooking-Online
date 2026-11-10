package repository;

import com.cinema.AppContext;
import entity.*;
import enums.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Atomic MySQL booking/cancellation path. The named lock coordinates this application's writers. */
public final class MySqlBookingStore {
    private static final String LOCK_NAME = "cinema_booking_write_v1";
    private MySqlBookingStore() {}

    public static boolean isSeatBooked(long showtimeId, long seatId) {
        try (Connection c = StorageIO.connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM tickets WHERE showtime_id=? AND seat_id=? AND status='BOOKED' LIMIT 1")) {
            ps.setLong(1, showtimeId);
            ps.setLong(2, seatId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException ex) { throw new IllegalStateException("Cannot check MySQL seat availability", ex); }
    }

    private static void lock(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT GET_LOCK(?, 10)")) {
            ps.setString(1, LOCK_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt(1) != 1)
                    throw new SQLException("Booking system is busy; try again.");
            }
        }
    }
    private static void unlock(Connection c) {
        try (PreparedStatement ps = c.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            ps.setString(1, LOCK_NAME); ps.executeQuery();
        } catch (SQLException ignored) { /* Connection close releases MySQL named locks. */ }
    }
    private static long nextId(Connection c, String table) throws SQLException {
        // Only constant, application-defined table names reach this method.
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM " + table)) {
            rs.next(); return rs.getLong(1);
        }
    }
    public static Booking confirm(User user, Showtime showtime, Collection<Long> requestedIds) {
        if (user == null || user.getRole() != Role.USER || user.getCustomer() == null)
            throw new IllegalArgumentException("Customer account is required.");
        if (showtime == null || showtime.getRoom() == null || showtime.getStartTime() == null
                || !showtime.getStartTime().isAfter(LocalDateTime.now())
                || showtime.getStatus() != ShowtimeStatus.SCHEDULED)
            throw new IllegalArgumentException("This showtime is not available for booking.");
        if (requestedIds == null || requestedIds.isEmpty())
            throw new IllegalArgumentException("Please select at least one seat.");
        List<Long> ids = new ArrayList<>(requestedIds);
        if (ids.contains(null) || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException("Invalid or duplicated seats.");

        try (Connection c = StorageIO.connection()) {
            lock(c);
            try {
                c.setAutoCommit(false);
                try {
                    int ticketPrice;
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT status, start_time, ticket_price, room_id FROM showtimes WHERE id=? FOR UPDATE")) {
                        ps.setLong(1, showtime.getId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || !"SCHEDULED".equals(rs.getString(1))
                                    || !LocalDateTime.parse(rs.getString(2)).isAfter(LocalDateTime.now())
                                    || rs.getLong(4) != showtime.getRoom().getId())
                                throw new IllegalArgumentException("Showtime is no longer available.");
                            ticketPrice = rs.getInt(3);
                            if (ticketPrice <= 0) throw new IllegalArgumentException("Invalid ticket price.");
                        }
                    }
                    List<Seat> seats = new ArrayList<>();
                    try (PreparedStatement validate = c.prepareStatement("SELECT room_id FROM seats WHERE id=?");
                         PreparedStatement taken = c.prepareStatement(
                                 "SELECT 1 FROM tickets WHERE showtime_id=? AND seat_id=? AND status='BOOKED' LIMIT 1")) {
                        for (long id : ids) {
                            Seat seat = AppContext.seats().findById(id);
                            validate.setLong(1, id);
                            try (ResultSet rs = validate.executeQuery()) {
                                if (!rs.next() || rs.getLong(1) != showtime.getRoom().getId() || seat == null)
                                    throw new IllegalArgumentException("Invalid seat for this room.");
                            }
                            taken.setLong(1, showtime.getId()); taken.setLong(2, id);
                            try (ResultSet rs = taken.executeQuery()) {
                                if (rs.next()) throw new IllegalArgumentException("Seat already booked. Refresh seats.");
                            }
                            seats.add(seat);
                        }
                    }
                    long bookingId = nextId(c, "bookings");
                    long ticketId = nextId(c, "tickets");
                    LocalDateTime now = LocalDateTime.now();
                    Booking booking = new Booking(bookingId, now, (double)ticketPrice * seats.size(),
                            BookingStatus.CONFIRMED, user.getCustomer());
                    List<Ticket> tickets = new ArrayList<>();
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO bookings(id, booking_date, total_amount, status, customer_id) VALUES(?,?,?,?,?)")) {
                        ps.setLong(1, bookingId); ps.setString(2, now.toString());
                        ps.setBigDecimal(3, java.math.BigDecimal.valueOf(booking.getTotalAmount()));
                        ps.setString(4, "CONFIRMED"); ps.setLong(5, user.getCustomer().getId());
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO tickets(id, price, status, booking_id, seat_id, showtime_id) VALUES(?,?,?,?,?,?)")) {
                        for (Seat seat : seats) {
                            ps.setLong(1, ticketId); ps.setInt(2, ticketPrice); ps.setString(3, "BOOKED");
                            ps.setLong(4, bookingId); ps.setLong(5, seat.getId()); ps.setLong(6, showtime.getId());
                            ps.addBatch();
                            tickets.add(new Ticket(ticketId++, ticketPrice, TicketStatus.BOOKED, booking, seat, showtime));
                        }
                        ps.executeBatch();
                    }
                    c.commit();
                    // Update in-process cache after commit. Other instances refresh on screen navigation.
                    AppContext.bookings().getAllBookings().add(booking);
                    AppContext.tickets().getAllTickets().addAll(tickets);
                    return booking;
                } catch (Exception ex) {
                    c.rollback();
                    if (ex instanceof IllegalArgumentException e) throw e;
                    throw new IllegalStateException("MySQL booking transaction failed: " + ex.getMessage(), ex);
                }
            } finally { unlock(c); }
        } catch (SQLException ex) { throw new IllegalStateException("Cannot complete MySQL booking: " + ex.getMessage(), ex); }
    }

    public static void cancel(Booking booking, User user) {
        if (booking == null || user == null) throw new IllegalArgumentException("No booking selected.");
        boolean admin = user.getRole() == Role.ADMIN;
        if (!admin && (user.getCustomer() == null || booking.getCustomer() == null
                || user.getCustomer().getId() != booking.getCustomer().getId()))
            throw new IllegalArgumentException("You cannot cancel this booking.");
        try (Connection c = StorageIO.connection()) {
            lock(c);
            try {
                c.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = c.prepareStatement("SELECT status, customer_id FROM bookings WHERE id=? FOR UPDATE")) {
                        ps.setLong(1, booking.getId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new IllegalArgumentException("Booking no longer exists.");
                            if (!admin && rs.getLong(2) != user.getCustomer().getId())
                                throw new IllegalArgumentException("You cannot cancel this booking.");
                            if ("CANCELLED".equals(rs.getString(1))) throw new IllegalArgumentException("Booking already cancelled.");
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT s.start_time FROM tickets t JOIN showtimes s ON t.showtime_id=s.id WHERE t.booking_id=?")) {
                        ps.setLong(1, booking.getId());
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                if (!LocalDateTime.parse(rs.getString(1)).isAfter(LocalDateTime.now()))
                                    throw new IllegalArgumentException("Cannot cancel after showtime starts.");
                            }
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement("UPDATE bookings SET status='CANCELLED' WHERE id=?")) {
                        ps.setLong(1, booking.getId()); ps.executeUpdate();
                    }
                    try (PreparedStatement ps = c.prepareStatement("UPDATE tickets SET status='CANCELLED' WHERE booking_id=?")) {
                        ps.setLong(1, booking.getId()); ps.executeUpdate();
                    }
                    c.commit();
                    booking.setStatus(BookingStatus.CANCELLED);
                    AppContext.tickets().findByBooking(booking.getId()).forEach(t -> t.setStatus(TicketStatus.CANCELLED));
                } catch (Exception ex) {
                    c.rollback();
                    if (ex instanceof IllegalArgumentException e) throw e;
                    throw new IllegalStateException("Cancellation failed: " + ex.getMessage(), ex);
                }
            } finally { unlock(c); }
        } catch (SQLException ex) { throw new IllegalStateException("Cannot cancel booking: " + ex.getMessage(), ex); }
    }
}
