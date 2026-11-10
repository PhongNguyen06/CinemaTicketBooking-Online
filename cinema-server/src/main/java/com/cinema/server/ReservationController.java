package com.cinema.server;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final JdbcTemplate jdbc;
    public ReservationController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Request(@NotNull @Min(1) Long showtimeId, @NotEmpty List<@NotNull @Min(1) Long> seatIds) {}

    private AuthSessionStore.Session session(Authentication authentication) {
        return (AuthSessionStore.Session) authentication.getPrincipal();
    }
    private long customer(Authentication authentication) {
        var user=session(authentication);
        if (!"USER".equals(user.role()) || user.customerId()==null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A linked customer account is required");
        return user.customerId();
    }
    /** This checks the required migration rather than silently allowing unsafe bookings. */
    private void requireSeatGuard() {
        Integer constraints = jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.statistics
            WHERE table_schema=DATABASE() AND table_name='tickets'
              AND index_name='uq_one_active_ticket_per_showtime_seat' AND non_unique=0
            """, Integer.class);
        if (constraints==null || constraints==0)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Run sql/stage14f_booking_migration.sql before enabling online bookings");
    }
    @GetMapping("/showtimes/{showtimeId}/seats")
    public List<Map<String,Object>> seats(@PathVariable long showtimeId) {
        Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM showtimes WHERE id=?", Integer.class,showtimeId);
        if (exists==null || exists==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Showtime not found");
        return jdbc.queryForList("""
            SELECT s.id,s.seat_row,s.seat_number,s.seat_type,
              CASE WHEN EXISTS(SELECT 1 FROM tickets t WHERE t.showtime_id=? AND t.seat_id=s.id AND t.status='BOOKED')
                THEN 1 ELSE 0 END AS booked
            FROM seats s JOIN showtimes st ON st.room_id=s.room_id
            WHERE st.id=? ORDER BY s.seat_row,s.seat_number
            """,showtimeId,showtimeId);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public Map<String,Object> book(@Valid @RequestBody Request input,Authentication authentication) {
        long customerId=customer(authentication);
        if(input.seatIds().size()>30 || new HashSet<>(input.seatIds()).size()!=input.seatIds().size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select 1-30 unique seats");
        requireSeatGuard();
        // Lock the screening row. All reservations for one screening serialize at this point.
        var showtime=jdbc.query("SELECT room_id,ticket_price,start_time,status FROM showtimes WHERE id=? FOR UPDATE",
                (rs,n)->new Screening(rs.getLong(1),rs.getInt(2),rs.getString(3),rs.getString(4)),input.showtimeId());
        if(showtime.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Showtime not found");
        var s=showtime.get(0);
        if(!"SCHEDULED".equals(s.status()) || !LocalDateTime.parse(s.startTime().replace(' ','T')).isAfter(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Showtime is not available");
        if(s.price()<=0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid showtime ticket price");
        String slots=String.join(",",Collections.nCopies(input.seatIds().size(),"?"));
        List<Object> args=new ArrayList<>(); args.add(s.roomId()); args.addAll(input.seatIds());
        Integer actual=jdbc.queryForObject("SELECT COUNT(*) FROM seats WHERE room_id=? AND id IN ("+slots+")",Integer.class,args.toArray());
        if(actual==null || actual!=input.seatIds().size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seat does not belong to this screening room");
        args=new ArrayList<>();args.add(input.showtimeId());args.addAll(input.seatIds());
        Integer occupied=jdbc.queryForObject("SELECT COUNT(*) FROM tickets WHERE status='BOOKED' AND showtime_id=? AND seat_id IN ("+slots+")",Integer.class,args.toArray());
        if(occupied!=null && occupied>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"One or more seats are already booked");
        long amount=(long)s.price()*input.seatIds().size();
        GeneratedKeyHolder bookingKey=new GeneratedKeyHolder();
        jdbc.update(connection->{var ps=connection.prepareStatement(
            "INSERT INTO bookings(booking_date,total_amount,status,customer_id) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,LocalDateTime.now().toString());ps.setLong(2,amount);ps.setString(3,"CONFIRMED");ps.setLong(4,customerId);return ps;},bookingKey);
        if(bookingKey.getKey()==null) throw new IllegalStateException("Booking key missing");
        long bookingId=bookingKey.getKey().longValue();
        for(long seatId:input.seatIds()) {
            jdbc.update("INSERT INTO tickets(price,status,booking_id,seat_id,showtime_id) VALUES(?,'BOOKED',?,?,?)",
                s.price(),bookingId,seatId,input.showtimeId());
        }
        return Map.of("id",bookingId,"status","CONFIRMED","totalAmount",amount,"ticketCount",input.seatIds().size());
    }
    private record Screening(long roomId,int price,String startTime,String status) {}
    @GetMapping
    public List<Map<String,Object>> all(Authentication authentication) {
        var u=session(authentication);
        if("ADMIN".equals(u.role())) return jdbc.queryForList("""
            SELECT b.id,b.booking_date,b.total_amount,b.status,b.customer_id,c.name AS customer_name
            FROM bookings b LEFT JOIN customers c ON c.id=b.customer_id ORDER BY b.id DESC
            """);
        long customerId=customer(authentication);
        return jdbc.queryForList("""
            SELECT b.id,b.booking_date,b.total_amount,b.status,b.customer_id,c.name AS customer_name
            FROM bookings b LEFT JOIN customers c ON c.id=b.customer_id
            WHERE b.customer_id=? ORDER BY b.id DESC
            """,customerId);
    }
    @GetMapping("/{bookingId}/tickets")
    public List<Map<String,Object>> tickets(@PathVariable long bookingId,Authentication authentication) {
        requireOwnerOrAdmin(bookingId,authentication);
        return jdbc.queryForList("""
            SELECT t.id,t.price,t.status,t.booking_id,t.seat_id,t.showtime_id,
                   s.seat_row,s.seat_number,m.title AS movie_title
            FROM tickets t JOIN seats s ON s.id=t.seat_id
             JOIN showtimes st ON st.id=t.showtime_id JOIN movies m ON m.id=st.movie_id
            WHERE t.booking_id=? ORDER BY t.id
            """,bookingId);
    }
    private void requireOwnerOrAdmin(long bookingId,Authentication auth) {
        var u=session(auth);
        var rows=jdbc.query("SELECT customer_id FROM bookings WHERE id=?",
            (rs,n)->rs.getLong(1),bookingId);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found");
        if(!"ADMIN".equals(u.role()) && (u.customerId()==null || rows.get(0)!=u.customerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your booking");
    }
    @PostMapping("/{bookingId}/cancel") @Transactional
    public Map<String,Object> cancel(@PathVariable long bookingId,Authentication authentication) {
        requireOwnerOrAdmin(bookingId,authentication);
        // Lock screening first, consistent with booking's lock order.
        var ids=jdbc.query("SELECT DISTINCT showtime_id FROM tickets WHERE booking_id=?",(rs,n)->rs.getLong(1),bookingId);
        ids.stream().sorted().forEach(id->jdbc.queryForList("SELECT id FROM showtimes WHERE id=? FOR UPDATE",id));
        var bookings=jdbc.query("SELECT status FROM bookings WHERE id=? FOR UPDATE",(rs,n)->rs.getString(1),bookingId);
        if(bookings.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found");
        if(!"CONFIRMED".equals(bookings.get(0))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking already cancelled");
        Integer past=jdbc.queryForObject("""
            SELECT COUNT(*) FROM tickets t JOIN showtimes s ON s.id=t.showtime_id
            WHERE t.booking_id=? AND s.start_time<=?
            """,Integer.class,bookingId,LocalDateTime.now().toString());
        if(past!=null && past>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Screening has already begun");
        jdbc.update("UPDATE bookings SET status='CANCELLED' WHERE id=?",bookingId);
        jdbc.update("UPDATE tickets SET status='CANCELLED' WHERE booking_id=? AND status='BOOKED'",bookingId);
        return Map.of("id",bookingId,"status","CANCELLED");
    }
}
