package service;

import entity.Movie;
import entity.Room;
import entity.Showtime;
import repository.ShowtimeRepository;

import java.util.ArrayList;
import java.util.List;

public class ShowtimeService {

    private final ShowtimeRepository repository;
    private final List<Showtime> showtimes;

    public ShowtimeService(List<Movie> movies, List<Room> rooms) {
        repository = new ShowtimeRepository();
        showtimes = repository.findAll(movies, rooms);
    }

    public void addShowtime(Showtime showtime) {
        showtime.setId(getNextId());
        showtimes.add(showtime);
        repository.save(showtimes);
    }

    public List<Showtime> getAllShowtimes() {
        return showtimes;
    }

    public Showtime findById(long id) {
        for (Showtime showtime : showtimes) {
            if (showtime.getId() == id) {
                return showtime;
            }
        }
        return null;
    }

    public List<Showtime> findByMovie(long movieId) {
        List<Showtime> result = new ArrayList<>();

        for (Showtime showtime : showtimes) {
            if (showtime.getMovie() != null
                    && showtime.getMovie().getId() == movieId) {
                result.add(showtime);
            }
        }

        return result;
    }

    public List<Showtime> findByRoom(long roomId) {
        List<Showtime> result = new ArrayList<>();

        for (Showtime showtime : showtimes) {
            if (showtime.getRoom() != null
                    && showtime.getRoom().getId() == roomId) {
                result.add(showtime);
            }
        }

        return result;
    }

    public void updateShowtime(Showtime updatedShowtime) {
        Showtime showtime = findById(updatedShowtime.getId());

        if (showtime != null) {
            showtime.setStartTime(updatedShowtime.getStartTime());
            showtime.setEndTime(updatedShowtime.getEndTime());
            showtime.setStatus(updatedShowtime.getStatus());
            showtime.setMovie(updatedShowtime.getMovie());
            showtime.setRoom(updatedShowtime.getRoom());
            showtime.setTicketPrice(updatedShowtime.getTicketPrice());
            repository.save(showtimes);
        }
    }

    public void deleteShowtime(long id) {
        Showtime showtime = findById(id);

        if (showtime != null) {
            showtimes.remove(showtime);
            repository.save(showtimes);
        }
    }

    private long getNextId() {
        long maxId = 0;

        for (Showtime showtime : showtimes) {
            if (showtime.getId() > maxId) {
                maxId = showtime.getId();
            }
        }

        return maxId + 1;
    }
}
