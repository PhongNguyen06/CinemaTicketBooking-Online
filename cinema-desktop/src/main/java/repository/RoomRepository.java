package repository;

import entity.Cinema;
import entity.Room;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {

    private static final String FILE_NAME = "database/rooms.csv";

    public void save(List<Room> rooms) {
        File directory = new File("database");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (BufferedWriter bw = StorageIO.writer(FILE_NAME)) {

            for (Room room : rooms) {

                long cinemaId = room.getCinema() != null ? room.getCinema().getId() : 0;

                bw.write( room.getId() + "," + room.getRoomName() + "," + room.getCapacity() + "," + cinemaId);

                bw.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error writing room data: " + e.getMessage(), e);
        }
    }

    public List<Room> findAll(List<Cinema> cinemas) {

        List<Room> rooms = new ArrayList<>();

        try (BufferedReader br = StorageIO.reader(FILE_NAME)) {

            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                long cinemaId = Long.parseLong(data[3]);

                Cinema cinema = null;

                for (Cinema c : cinemas) {
                    if (c.getId() == cinemaId) {
                        cinema = c;
                        break;
                    }
                }

                Room room = new Room( Long.parseLong(data[0]), data[1], Integer.parseInt(data[2]), cinema);

                rooms.add(room);
            }
        } catch (FileNotFoundException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Room data file not found.");
        } catch (IOException | NumberFormatException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Error reading room data: " + e.getMessage());
        }

        return rooms;
    }
}
