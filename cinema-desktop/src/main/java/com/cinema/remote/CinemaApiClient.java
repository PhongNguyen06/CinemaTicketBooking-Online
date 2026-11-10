package com.cinema.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Movie;
import entity.Cinema;
import entity.Room;
import entity.Seat;
import entity.Showtime;
import enums.SeatType;
import enums.ShowtimeStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** Accesses only the Spring Boot API, never the database. */
public final class CinemaApiClient {
    public record LoginResponse(String token, String username, String role, Long customerId) {}
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI base;
    private String bearer;

    public CinemaApiClient(String baseUrl) {
        if (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://localhost:")
                && !baseUrl.startsWith("http://127.0.0.1:"))
            throw new IllegalArgumentException("Remote API must use HTTPS (except localhost)");
        this.base = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
    }
    public LoginResponse login(String username, String password) throws IOException, InterruptedException {
        LoginResponse response = mapper.readValue(request("POST", "api/auth/login",
                mapper.writeValueAsString(new LoginCredentials(username,password))), LoginResponse.class);
        bearer = response.token();
        return response;
    }
    public void register(String username, String password, String name, String email, String phone)
            throws IOException, InterruptedException {
        request("POST", "api/auth/register", mapper.writeValueAsString(
                new RegisterCredentials(username, password, name, email, phone)));
    }
    private record RegisterCredentials(String username, String password, String name, String email, String phone) {}
    /** WebSocket keeps authentication out of URL parameters. */
    public java.net.URI updatesUri() {
        String scheme = "https".equalsIgnoreCase(base.getScheme()) ? "wss" : "ws";
        return java.net.URI.create(scheme + "://" + base.getRawAuthority() + base.getRawPath()).resolve("ws/updates");
    }
    public String sessionToken() {return bearer;}
    private record LoginCredentials(String username,String password) {}
    public List<Movie> movies() throws IOException, InterruptedException {
        return mapper.readValue(request("GET", "api/movies", null),new TypeReference<List<Movie>>(){});
    }
    public void addMovie(Movie movie) throws IOException, InterruptedException {
        request("POST", "api/movies", mapper.writeValueAsString(new MovieInput(movie.getTitle(),movie.getDuration(),movie.getGenre(),movie.getDescription())));
    }
    private record MovieInput(String title,int duration,String genre,String description) {}
    public void deleteMovie(long id) throws IOException, InterruptedException { request("DELETE", "api/movies/"+id,null); }
    // Catalog mapping is explicit because the API uses SQL column names (room_name, cinema_id).
    public List<Cinema> cinemas() throws IOException, InterruptedException {
        return mapper.readValue(request("GET", "api/cinemas", null), new TypeReference<List<Cinema>>(){});
    }
    public void addCinema(Cinema cinema) throws IOException, InterruptedException {
        request("POST", "api/cinemas", mapper.writeValueAsString(Map.of("name",cinema.getName(), "address",cinema.getAddress())));
    }
    public void updateCinema(Cinema cinema) throws IOException, InterruptedException {
        request("PUT", "api/cinemas/" + cinema.getId(), mapper.writeValueAsString(Map.of("name",cinema.getName(),"address",cinema.getAddress())));
    }
    public void deleteCinema(long id) throws IOException, InterruptedException {
        request("DELETE", "api/cinemas/" + id,null);
    }
    private record RoomRow(long id, @com.fasterxml.jackson.annotation.JsonProperty("room_name") String room_name,
                           int capacity, @com.fasterxml.jackson.annotation.JsonProperty("cinema_id") long cinema_id) {}
    public List<Room> rooms() throws IOException, InterruptedException {
        List<Cinema> cinemas = cinemas();
        Map<Long,Cinema> byId = new HashMap<>();
        for (Cinema cinema : cinemas) byId.put(cinema.getId(),cinema);
        List<RoomRow> rows = mapper.readValue(request("GET","api/rooms",null),new TypeReference<List<RoomRow>>(){});
        return rows.stream().map(r->new Room(r.id(),r.room_name(),r.capacity(),byId.get(r.cinema_id()))).toList();
    }
    private Map<String,Object> roomInput(Room room) {
        return Map.of("roomName",room.getRoomName(),"capacity",room.getCapacity(),"cinemaId",room.getCinema().getId());
    }
    public void addRoom(Room room) throws IOException, InterruptedException {
        request("POST","api/rooms",mapper.writeValueAsString(roomInput(room)));
    }
    public void updateRoom(Room room) throws IOException, InterruptedException {
        request("PUT","api/rooms/"+room.getId(),mapper.writeValueAsString(roomInput(room)));
    }
    public void deleteRoom(long id) throws IOException, InterruptedException {
        request("DELETE","api/rooms/"+id,null);
    }
    private record SeatRow(long id, @com.fasterxml.jackson.annotation.JsonProperty("seat_row") String seat_row,
            @com.fasterxml.jackson.annotation.JsonProperty("seat_number") int seat_number,
            @com.fasterxml.jackson.annotation.JsonProperty("seat_type") String seat_type,
            @com.fasterxml.jackson.annotation.JsonProperty("room_id") long room_id) {}
    public List<Seat> seats() throws IOException, InterruptedException {
        Map<Long,Room> roomMap=new HashMap<>();
        for(Room room:rooms()) roomMap.put(room.getId(),room);
        List<SeatRow> rows=mapper.readValue(request("GET","api/seats",null),new TypeReference<List<SeatRow>>(){});
        return rows.stream().map(r->new Seat(r.id(),r.seat_row(),r.seat_number(),SeatType.valueOf(r.seat_type()),roomMap.get(r.room_id()))).toList();
    }
    private Map<String,Object> seatInput(Seat s) {
        return Map.of("seatRow",s.getRow(),"seatNumber",s.getNumber(),"seatType",s.getType().name(),"roomId",s.getRoom().getId());
    }
    public void addSeat(Seat s) throws IOException, InterruptedException {request("POST","api/seats",mapper.writeValueAsString(seatInput(s)));}
    public void updateSeat(Seat s) throws IOException, InterruptedException {request("PUT","api/seats/"+s.getId(),mapper.writeValueAsString(seatInput(s)));}
    public void deleteSeat(long id) throws IOException, InterruptedException {request("DELETE","api/seats/"+id,null);}

    private record ShowtimeRow(long id,
            @com.fasterxml.jackson.annotation.JsonProperty("start_time") String start_time,
            @com.fasterxml.jackson.annotation.JsonProperty("end_time") String end_time,
            String status,
            @com.fasterxml.jackson.annotation.JsonProperty("movie_id") long movie_id,
            @com.fasterxml.jackson.annotation.JsonProperty("room_id") long room_id,
            @com.fasterxml.jackson.annotation.JsonProperty("ticket_price") int ticket_price) {}
    public List<Showtime> showtimes() throws IOException, InterruptedException {
        Map<Long,Movie> movieMap=new HashMap<>();
        for(Movie movie:movies()) movieMap.put(movie.getId(),movie);
        Map<Long,Room> roomMap=new HashMap<>();
        for(Room room:rooms()) roomMap.put(room.getId(),room);
        List<ShowtimeRow> rows=mapper.readValue(request("GET","api/showtimes",null),new TypeReference<List<ShowtimeRow>>(){});
        return rows.stream().map(r->new Showtime(r.id(),LocalDateTime.parse(r.start_time().replace(' ','T')),
            LocalDateTime.parse(r.end_time().replace(' ','T')),ShowtimeStatus.valueOf(r.status()),
            movieMap.get(r.movie_id()),roomMap.get(r.room_id()),r.ticket_price())).toList();
    }
    private Map<String,Object> showtimeInput(Showtime s) {
        return Map.of("startTime",s.getStartTime().toString(),"endTime",s.getEndTime().toString(),
                "status",s.getStatus().name(),"movieId",s.getMovie().getId(),"roomId",s.getRoom().getId(),"ticketPrice",s.getTicketPrice());
    }
    public void addShowtime(Showtime s) throws IOException, InterruptedException {request("POST","api/showtimes",mapper.writeValueAsString(showtimeInput(s)));}
    public void updateShowtime(Showtime s) throws IOException, InterruptedException {request("PUT","api/showtimes/"+s.getId(),mapper.writeValueAsString(showtimeInput(s)));}
    public void deleteShowtime(long id) throws IOException, InterruptedException {request("DELETE","api/showtimes/"+id,null);}
    public record RemoteSeat(long id, String row, int number, String type, boolean booked) {}
    public record RemoteBooking(long id, String date, long totalAmount, String status, String customerName) {}
    public record RemoteTicket(long id, long price, String status, String movie, String seat) {}
    public record BookResult(long id, String status, long totalAmount, int ticketCount) {}
    public List<RemoteSeat> availableSeats(long showtimeId) throws IOException,InterruptedException {
        List<Map<String,Object>> data=mapper.readValue(request("GET","api/reservations/showtimes/"+showtimeId+"/seats",null),new TypeReference<>(){});
        return data.stream().map(r->new RemoteSeat(((Number)r.get("id")).longValue(),
                (String)r.get("seat_row"),((Number)r.get("seat_number")).intValue(),(String)r.get("seat_type"),
                (r.get("booked") instanceof Boolean flag ? flag : ((Number)r.get("booked")).intValue()!=0))).toList();
    }
    public BookResult bookSeats(long showtimeId, List<Long> seatIds) throws IOException,InterruptedException {
        return mapper.readValue(request("POST","api/reservations",mapper.writeValueAsString(Map.of("showtimeId",showtimeId,"seatIds",seatIds))),BookResult.class);
    }
    public List<RemoteBooking> bookings() throws IOException,InterruptedException {
        List<Map<String,Object>> data=mapper.readValue(request("GET","api/reservations",null),new TypeReference<>(){});
        return data.stream().map(r->new RemoteBooking(((Number)r.get("id")).longValue(),String.valueOf(r.get("booking_date")),
                ((Number)r.get("total_amount")).longValue(),(String)r.get("status"),String.valueOf(r.get("customer_name")))).toList();
    }
    public List<RemoteTicket> bookingTickets(long id) throws IOException,InterruptedException {
        List<Map<String,Object>> data=mapper.readValue(request("GET","api/reservations/"+id+"/tickets",null),new TypeReference<>(){});
        return data.stream().map(r->new RemoteTicket(((Number)r.get("id")).longValue(),((Number)r.get("price")).longValue(),
                (String)r.get("status"),(String)r.get("movie_title"),r.get("seat_row").toString()+r.get("seat_number"))).toList();
    }
    public void cancelBooking(long id) throws IOException,InterruptedException {request("POST","api/reservations/"+id+"/cancel","{}");}
    public void logout() {
        com.cinema.remote.RemoteUpdates.stop();
        try {if (bearer != null) request("POST", "api/auth/logout", "{}");}
        catch (Exception ignored) {} finally {bearer=null;}
    }
    private String request(String method, String path, String json) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(15));
        if (bearer != null) builder.header("Authorization", "Bearer " + bearer);
        if (json != null) builder.header("Content-Type", "application/json; charset=utf-8");
        HttpRequest request = builder.method(method,json == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json,StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = http.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode()/100 != 2)
            throw new IOException("Server returned HTTP " + response.statusCode() + " for " + method + " " + path + (response.body().length() < 250 ? ": " + response.body() : ""));
        return response.body();
    }
}
