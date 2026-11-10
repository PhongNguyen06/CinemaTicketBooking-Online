package repository;

import entity.Booking;
import entity.Seat;
import entity.Showtime;
import entity.Ticket;
import enums.TicketStatus;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TicketRepository {

    private static final String FILE_NAME = "database/tickets.csv";

    public void save(List<Ticket> tickets) {
        File directory = new File("database");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (BufferedWriter bw = StorageIO.writer(FILE_NAME)) {

            for (Ticket ticket : tickets) {

                long bookingId = ticket.getBooking() != null ? ticket.getBooking().getId() : 0;

                long seatId = ticket.getSeat() != null ? ticket.getSeat().getId() : 0;

                long showtimeId = ticket.getShowtime() != null ? ticket.getShowtime().getId() : 0;

                bw.write( ticket.getId() + "," + ticket.getPrice() + "," + ticket.getStatus() + "," + bookingId + "," + seatId + "," + showtimeId);

                bw.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error saving tickets", e);
        }
    }

    public List<Ticket> findAll(List<Booking> bookings, List<Seat> seats, List<Showtime> showtimes) {

        List<Ticket> tickets = new ArrayList<>();

        try (BufferedReader br = StorageIO.reader(FILE_NAME)) {

            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                if (data.length < 6) {
                    System.out.println("Invalid ticket data: " + line);
                    continue;
                }

                try {
                    long bookingId = Long.parseLong(data[3]);
                    long seatId = Long.parseLong(data[4]);
                    long showtimeId = Long.parseLong(data[5]);

                    Booking booking = null;
                    Seat seat = null;
                    Showtime showtime = null;

                    for (Booking b : bookings) {
                        if (b.getId() == bookingId) {
                            booking = b;
                            break;
                        }
                    }

                    for (Seat s : seats) {
                        if (s.getId() == seatId) {
                            seat = s;
                            break;
                        }
                    }

                    for (Showtime s : showtimes) {
                        if (s.getId() == showtimeId) {
                            showtime = s;
                            break;
                        }
                    }

                    Ticket ticket = new Ticket(
                            Long.parseLong(data[0]),
                            Integer.parseInt(data[1]),
                            TicketStatus.valueOf(data[2]),
                            booking,
                            seat,
                            showtime
                    );

                    tickets.add(ticket);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid ticket data: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Ticket data file not found.");
        } catch (IOException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Error reading ticket data: " + e.getMessage());
        }

        return tickets;
    }
}
