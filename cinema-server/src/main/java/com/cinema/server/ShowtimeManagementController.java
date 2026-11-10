package com.cinema.server;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeManagementController {
    private final JdbcTemplate jdbc;
    public ShowtimeManagementController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Input(@NotNull LocalDateTime startTime,
                        @NotNull LocalDateTime endTime,
                        @NotNull @Pattern(regexp="SCHEDULED|STARTED|ENDED|CANCELLED") String status,
                        @NotNull @Min(1) Long movieId,
                        @NotNull @Min(1) Long roomId,
                        @Min(1) int ticketPrice) {}

    @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public synchronized Map<String,Object> add(@Valid @RequestBody Input input) {
        validate(input, null);
        long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM showtimes", Long.class);
        jdbc.update("INSERT INTO showtimes(id,start_time,end_time,status,movie_id,room_id,ticket_price) VALUES(?,?,?,?,?,?,?)",
                id,input.startTime().toString(),input.endTime().toString(),input.status(),input.movieId(),input.roomId(),input.ticketPrice());
        return payload(id,input);
    }

    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public synchronized Map<String,Object> update(@PathVariable long id,@Valid @RequestBody Input input) {
        ensureEditable(id);
        validate(input,id);
        int count=jdbc.update("UPDATE showtimes SET start_time=?,end_time=?,status=?,movie_id=?,room_id=?,ticket_price=? WHERE id=?",
                input.startTime().toString(),input.endTime().toString(),input.status(),input.movieId(),input.roomId(),input.ticketPrice(),id);
        if(count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Showtime not found");
        return payload(id,input);
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public synchronized void delete(@PathVariable long id) {
        ensureEditable(id);
        if(jdbc.update("DELETE FROM showtimes WHERE id=?",id)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Showtime not found");
    }

    private void ensureEditable(long id) {
        Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM showtimes WHERE id=?",Integer.class,id);
        if(exists==null || exists==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Showtime not found");
        Integer tickets=jdbc.queryForObject("SELECT COUNT(*) FROM tickets WHERE showtime_id=?",Integer.class,id);
        if(tickets!=null && tickets>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Showtime has ticket history");
    }

    private void validate(Input input, Long excludeId) {
        if(!input.endTime().isAfter(input.startTime()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"End time must be after start time");
        Integer movie=jdbc.queryForObject("SELECT COUNT(*) FROM movies WHERE id=?",Integer.class,input.movieId());
        Integer room=jdbc.queryForObject("SELECT COUNT(*) FROM rooms WHERE id=?",Integer.class,input.roomId());
        if(movie==null || movie==0 || room==null || room==0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Movie or room not found");
        // Existing data uses ISO-like varchar(40) timestamps; parse in Java to avoid DB string-format pitfalls.
        var existing=jdbc.queryForList("SELECT id,start_time,end_time,status FROM showtimes WHERE room_id=?",input.roomId());
        if(!"CANCELLED".equals(input.status())) for(var row:existing) {
            if(excludeId!=null && ((Number)row.get("id")).longValue()==excludeId) continue;
            if("CANCELLED".equals(row.get("status"))) continue;
            LocalDateTime starts=LocalDateTime.parse(row.get("start_time").toString().replace(' ','T'));
            LocalDateTime ends=LocalDateTime.parse(row.get("end_time").toString().replace(' ','T'));
            if(input.startTime().isBefore(ends) && starts.isBefore(input.endTime()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Showtime overlaps another screening in this room");
        }
    }
    private Map<String,Object> payload(long id,Input i) {
        return Map.of("id",id,"start_time",i.startTime().toString(),"end_time",i.endTime().toString(),
                "status",i.status(),"movie_id",i.movieId(),"room_id",i.roomId(),"ticket_price",i.ticketPrice());
    }
}
