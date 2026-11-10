package com.cinema.server;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
@RestController
@RequestMapping("/api/seats")
public class SeatManagementController {
 private final JdbcTemplate jdbc;
 public SeatManagementController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record Input(@NotBlank String seatRow,@Min(1) int seatNumber,@Pattern(regexp="NORMAL|VIP") String seatType,@NotNull Long roomId) {}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
 @Transactional
 public synchronized Map<String,Object> add(@Valid @RequestBody Input i){
  validate(i,null);
  Integer capacity=jdbc.query("SELECT capacity FROM rooms WHERE id=?",rs->rs.next()?rs.getInt(1):null,i.roomId());
  if(capacity==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found");
  Integer seats=jdbc.queryForObject("SELECT COUNT(*) FROM seats WHERE room_id=?",Integer.class,i.roomId());
  if(seats!=null && seats>=capacity) throw new ResponseStatusException(HttpStatus.CONFLICT,"Room is full");
  long id=jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM seats",Long.class);
  jdbc.update("INSERT INTO seats(id,seat_row,seat_number,seat_type,room_id) VALUES(?,?,?,?,?)",id,i.seatRow().trim(),i.seatNumber(),i.seatType(),i.roomId());
  return payload(id,i);
 }
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
 @Transactional
 public synchronized Map<String,Object> update(@PathVariable long id,@Valid @RequestBody Input i){
  validate(i,id);
  Integer linked=jdbc.queryForObject("SELECT COUNT(*) FROM tickets WHERE seat_id=?",Integer.class,id);
  if(linked!=null && linked>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Seat has ticket history");
  if(jdbc.update("UPDATE seats SET seat_row=?,seat_number=?,seat_type=?,room_id=? WHERE id=?",i.seatRow().trim(),i.seatNumber(),i.seatType(),i.roomId(),id)==0)
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Seat not found");
  return payload(id,i);
 }
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
 @Transactional
 public void delete(@PathVariable long id){
  if(jdbc.update("DELETE FROM seats WHERE id=?",id)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Seat not found");
 }
 private void validate(Input i, Long excludeId) {
  if(!i.seatRow().trim().matches("[A-Za-z]{1,3}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid seat row");
  Integer capacity=jdbc.query("SELECT capacity FROM rooms WHERE id=?",rs->rs.next()?rs.getInt(1):null,i.roomId());
  if(capacity==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found");
  Integer duplicate=jdbc.queryForObject("SELECT COUNT(*) FROM seats WHERE room_id=? AND UPPER(seat_row)=UPPER(?) AND seat_number=? AND (? IS NULL OR id<>?)",Integer.class,i.roomId(),i.seatRow().trim(),i.seatNumber(),excludeId,excludeId);
  if(duplicate!=null && duplicate>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Seat already exists in room");
  Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM seats WHERE room_id=? AND (? IS NULL OR id<>?)",Integer.class,i.roomId(),excludeId,excludeId);
  if(count!=null && count>=capacity) throw new ResponseStatusException(HttpStatus.CONFLICT,"Room capacity is full");
 }
 private Map<String,Object> payload(long id,Input i){return Map.of("id",id,"seat_row",i.seatRow().trim(),"seat_number",i.seatNumber(),"seat_type",i.seatType(),"room_id",i.roomId());}
}
