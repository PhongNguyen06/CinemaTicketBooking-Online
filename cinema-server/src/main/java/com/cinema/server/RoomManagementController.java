package com.cinema.server;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestController
@RequestMapping("/api/rooms")
public class RoomManagementController {
 private final JdbcTemplate jdbc;
 public RoomManagementController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record Input(@NotBlank String roomName,@Min(1) int capacity,@NotNull Long cinemaId) {}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
 public synchronized Map<String,Object> add(@Valid @RequestBody Input input){
  long id=jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM rooms",Long.class);
  jdbc.update("INSERT INTO rooms(id,room_name,capacity,cinema_id) VALUES(?,?,?,?)",id,input.roomName().trim(),input.capacity(),input.cinemaId());
  return payload(id,input);
 }
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
 public Map<String,Object> update(@PathVariable long id,@Valid @RequestBody Input input){
  Integer existingSeats=jdbc.queryForObject("SELECT COUNT(*) FROM seats WHERE room_id=?",Integer.class,id);
  if(existingSeats!=null && existingSeats>input.capacity()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Room capacity lower than seat count");
  if(jdbc.update("UPDATE rooms SET room_name=?,capacity=?,cinema_id=? WHERE id=?",input.roomName().trim(),input.capacity(),input.cinemaId(),id)==0)
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found");
  return payload(id,input);
 }
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable long id){
  if(jdbc.update("DELETE FROM rooms WHERE id=?",id)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found");
 }
 private Map<String,Object> payload(long id,Input i){return Map.of("id",id,"room_name",i.roomName().trim(),"capacity",i.capacity(),"cinema_id",i.cinemaId());}
}
