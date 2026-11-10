package com.cinema.server;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/** Phase 14A read-only endpoint previews. Writes move to server in 14B. */
@RestController
@RequestMapping("/api")
public class CatalogController {
 private final JdbcTemplate jdbc;
 public CatalogController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 private List<Map<String,Object>> rows(String sql){return jdbc.queryForList(sql);}
 @GetMapping("/health") public Map<String,String> health(){return Map.of("status","UP","phase","14A");}
 @GetMapping("/cinemas") public List<Map<String,Object>> cinemas(){return rows("SELECT id,name,address FROM cinemas ORDER BY id");}
 @GetMapping("/rooms") public List<Map<String,Object>> rooms(){return rows("SELECT id,room_name,capacity,cinema_id FROM rooms ORDER BY id");}
 @GetMapping("/seats") public List<Map<String,Object>> seats(){return rows("SELECT id,seat_row,seat_number,seat_type,room_id FROM seats ORDER BY id");}
 @GetMapping("/showtimes") public List<Map<String,Object>> showtimes(){return rows("SELECT id,start_time,end_time,status,movie_id,room_id,ticket_price FROM showtimes ORDER BY id");}
 @PreAuthorize("hasRole('ADMIN')") @GetMapping("/customers") public List<Map<String,Object>> customers(){return rows("SELECT id,name,email,phone FROM customers ORDER BY id");}
 // Booking/ticket payloads must be owner-scoped before customer-facing endpoints are enabled.
 @PreAuthorize("hasRole('ADMIN')") @GetMapping("/bookings") public List<Map<String,Object>> bookings(){return rows("SELECT id,booking_date,total_amount,status,customer_id FROM bookings ORDER BY id");}
 @PreAuthorize("hasRole('ADMIN')") @GetMapping("/tickets") public List<Map<String,Object>> tickets(){return rows("SELECT id,price,status,booking_id,seat_id,showtime_id FROM tickets ORDER BY id");}
}
