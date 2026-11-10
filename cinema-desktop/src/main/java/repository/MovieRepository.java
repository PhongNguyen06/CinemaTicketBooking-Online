package repository;

import entity.Movie;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MovieRepository {

    private static final String FILE_NAME = "database/movies.csv";

    public void save(List<Movie> movies) {
        File directory = new File("database");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (BufferedWriter bw = StorageIO.writer(FILE_NAME)) {

            for (Movie movie : movies) {
                bw.write( movie.getId() + "," + movie.getTitle() + "," + movie.getDuration() + "," + movie.getGenre() + "," + movie.getDescription());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error writing movie data: " + e.getMessage(), e);
        }
    }

    public List<Movie> findAll() {
        List<Movie> movies = new ArrayList<>();

        try (BufferedReader br = StorageIO.reader(FILE_NAME)) {

            String line;

            while ((line = br.readLine()) != null){

                if(line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                Movie movie = new Movie( Long.parseLong(data[0]), data[1], Integer.parseInt(data[2]), data[3], data[4]);

                movies.add(movie);
            }
        } catch (FileNotFoundException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Movie data file not found.");
        } catch (IOException | NumberFormatException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Error reading movie data: " + e.getMessage());
        }

        return movies;
    }
}
