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
@RequestMapping("/api/cinemas")
public class CinemaManagementController {
 private final JdbcTemplate jdbc;
 public CinemaManagementController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record Input(@NotBlank String name,@NotBlank String address) {}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
 public synchronized Map<String,Object> add(@Valid @RequestBody Input input){
  long id=jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM cinemas",Long.class);
  jdbc.update("INSERT INTO cinemas(id,name,address) VALUES(?,?,?)",id,input.name().trim(),input.address().trim());
  return Map.of("id",id,"name",input.name().trim(),"address",input.address().trim());
 }
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
 public Map<String,Object> update(@PathVariable long id,@Valid @RequestBody Input input){
  if(jdbc.update("UPDATE cinemas SET name=?, address=? WHERE id=?",input.name().trim(),input.address().trim(),id)==0)
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cinema not found");
  return Map.of("id",id,"name",input.name().trim(),"address",input.address().trim());
 }
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable long id){
  if(jdbc.update("DELETE FROM cinemas WHERE id=?",id)==0)
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cinema not found");
 }
}
