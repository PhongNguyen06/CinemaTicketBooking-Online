package com.cinema.server;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/movies")
public class MovieController {
 public record Movie(long id,String title,int duration,String genre,String description) {}
 public record MovieInput(@NotBlank @Size(max=255) String title,@Min(1) int duration,
  @Size(max=150) String genre,String description) {}
 private final JdbcTemplate jdbc;
 public MovieController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping public List<Movie> all() {
  return jdbc.query("SELECT id,title,duration,genre,description FROM movies ORDER BY id",
   (rs,n)->new Movie(rs.getLong(1),rs.getString(2),rs.getInt(3),rs.getString(4),rs.getString(5)));
 }
 @GetMapping("/{id}") public Movie one(@PathVariable long id) {
  return jdbc.query("SELECT id,title,duration,genre,description FROM movies WHERE id=?",
   (rs,n)->new Movie(rs.getLong(1),rs.getString(2),rs.getInt(3),rs.getString(4),rs.getString(5)),id)
   .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Movie not found"));
 }
 // Single backend instance in 14A; existing Stage 13 schema uses explicitly allocated IDs.
 // Before horizontally scaling, migrate IDs to AUTO_INCREMENT/sequence allocation.
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
 public synchronized Movie add(@Valid @RequestBody MovieInput m) {
  Long next=jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM movies",Long.class);
  long id=next==null?1:next;
  jdbc.update("INSERT INTO movies(id,title,duration,genre,description) VALUES(?,?,?,?,?)",
    id,m.title().trim(),m.duration(),m.genre(),m.description());
  return new Movie(id,m.title().trim(),m.duration(),m.genre(),m.description());
 }
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
 public Movie update(@PathVariable long id,@Valid @RequestBody MovieInput m) {
  int count=jdbc.update("UPDATE movies SET title=?,duration=?,genre=?,description=? WHERE id=?",
   m.title().trim(),m.duration(),m.genre(),m.description(),id);
  if(count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Movie not found");
  return new Movie(id,m.title().trim(),m.duration(),m.genre(),m.description());
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')")
 public void delete(@PathVariable long id) {
  try {if(jdbc.update("DELETE FROM movies WHERE id=?",id)==0)
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Movie not found");}
  catch(org.springframework.dao.DataIntegrityViolationException ex) {
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Movie has linked showtimes");}
 }
}
