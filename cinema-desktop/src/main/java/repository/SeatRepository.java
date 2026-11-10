package repository;

import entity.Room;
import entity.Seat;
import enums.SeatType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SeatRepository {

    private static final String FILE_NAME = "database/seats.csv";

    public void save(List<Seat> seats) {
        File directory = new File("database");

        if (!directory.exists()) {
            directory.mkdirs();
        }


        try (BufferedWriter bw = StorageIO.writer(FILE_NAME)) {

            for (Seat seat : seats) {

                long roomId = seat.getRoom() != null ? seat.getRoom().getId() : 0;

                bw.write( seat.getId() + "," + seat.getRow() + "," + seat.getNumber() + "," + seat.getType() + "," + roomId);

                bw.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error writing seat data: " + e.getMessage(), e);
        }
    }

    public List<Seat> findAll(List<Room> rooms) {

        List<Seat> seats = new ArrayList<>();

        try (BufferedReader br = StorageIO.reader(FILE_NAME)) {

            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                long roomId = Long.parseLong(data[4]);

                Room room = null;

                for (Room r : rooms) {
                    if (r.getId() == roomId) {
                        room = r;
                        break;
                    }
                }

                Seat seat = new Seat( Long.parseLong(data[0]), data[1], Integer.parseInt(data[2]), SeatType.valueOf(data[3]), room);

                seats.add(seat);
            }
        } catch (FileNotFoundException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Seat data file not found.");
        } catch (IOException | IllegalArgumentException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Error reading seat data: " + e.getMessage());
        }

        return seats;
    }
}
