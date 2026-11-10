package repository;

import entity.Cinema;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CinemaRepository {

    private static final String FILE_NAME = "database/cinemas.csv";

    public void save(List<Cinema> cinemas) {
        File directory = new File("database");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (BufferedWriter bw = StorageIO.writer(FILE_NAME)){

            for (Cinema cinema : cinemas) {
                bw.write(cinema.getId() + "," + cinema.getName() + "," + cinema.getAddress());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error writing cinema data: " + e.getMessage(), e);
        }
    }

    public List<Cinema> findAll() {
        List<Cinema> cinemas = new ArrayList<>();

        try (BufferedReader br = StorageIO.reader(FILE_NAME)){

            String line;

            while ((line = br.readLine()) != null){

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                Cinema cinema = new Cinema(Long.parseLong(data[0]), data[1], data[2]);

                cinemas.add(cinema);
            }
        } catch (FileNotFoundException e){
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Cinema data file not found.");
        } catch (IOException | NumberFormatException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Error reading cinema data: " + e.getMessage());
        }

        return cinemas;
    }
}