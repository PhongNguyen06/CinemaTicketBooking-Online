package repository;

import entity.Movie;
import entity.Room;
import entity.Showtime;
import enums.ShowtimeStatus;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShowtimeRepository {

    private static final String FILE_NAME = "database/showtimes.csv";

    public void save(List<Showtime> showtimes) {
        File directory = new File("database");

        if (!directory.exists()) {
            directory.mkdirs();
        }


        try (BufferedWriter bw = StorageIO.writer(FILE_NAME)) {

            for (Showtime showtime : showtimes) {

                long movieId = showtime.getMovie() != null ? showtime.getMovie().getId() : 0;

                long roomId = showtime.getRoom() != null ? showtime.getRoom().getId() : 0;

                bw.write( showtime.getId() + "," + showtime.getStartTime() + "," + showtime.getEndTime() + "," + showtime.getStatus() + "," + movieId + "," + roomId + "," + showtime.getTicketPrice());

                bw.newLine();
            }
        } catch (IOException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println( "Error writing showtime data: " + e.getMessage());
        }
    }

    public List<Showtime> findAll(List<Movie> movies, List<Room> rooms) {

        List<Showtime> showtimes = new ArrayList<>();

        try (BufferedReader br = StorageIO.reader(FILE_NAME)) {

            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                long movieId = Long.parseLong(data[4]);
                long roomId = Long.parseLong(data[5]);

                Movie movie = null;
                Room room = null;

                for (Movie m : movies) {
                    if (m.getId() == movieId) {
                        movie = m;
                        break;
                    }
                }

                for (Room r : rooms) {
                    if (r.getId() == roomId) {
                        room = r;
                        break;
                    }
                }

                int ticketPrice = data.length > 6 ? Integer.parseInt(data[6]) : 0;

                Showtime showtime = new Showtime(Long.parseLong(data[0]), LocalDateTime.parse(data[1]), LocalDateTime.parse(data[2]), ShowtimeStatus.valueOf(data[3]), movie, room, ticketPrice);

                showtimes.add(showtime);
            }
        } catch (FileNotFoundException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Showtime data file not found.");
        } catch (IOException | IllegalArgumentException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println( "Error reading showtime data: " + e.getMessage());
        }

        return showtimes;
    }
}