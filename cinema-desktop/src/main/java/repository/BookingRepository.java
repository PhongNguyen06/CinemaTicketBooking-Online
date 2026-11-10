package repository;

import entity.Booking;
import entity.Customer;
import enums.BookingStatus;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookingRepository {

    private static final String FILE_NAME = "database/bookings.csv";

    public void save(List<Booking> bookings) {
        File directory = new File("database");

        if (!directory.exists()) {
            directory.mkdirs();
        }


        try (BufferedWriter bw = StorageIO.writer(FILE_NAME)) {

            for (Booking booking : bookings) {

                long customerId = booking.getCustomer() != null ? booking.getCustomer().getId() : 0;

                bw.write( booking.getId() + "," + booking.getBookingDate() + "," + booking.getTotalAmount() + "," + booking.getStatus() + "," + customerId);

                bw.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error saving bookings", e);
        }
    }

    public List<Booking> findAll(List<Customer> customers) {

        List<Booking> bookings = new ArrayList<>();

        try (BufferedReader br = StorageIO.reader(FILE_NAME)) {

            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                long customerId = Long.parseLong(data[4]);

                Customer customer = null;

                for (Customer c : customers) {
                    if (c.getId() == customerId) {
                        customer = c;
                        break;
                    }
                }

                Booking booking = new Booking( Long.parseLong(data[0]), LocalDateTime.parse(data[1]), Double.parseDouble(data[2]), BookingStatus.valueOf(data[3]), customer);

                bookings.add(booking);
            }
        } catch (FileNotFoundException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Booking data file not found.");
        } catch (IOException | IllegalArgumentException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println( "Error reading booking data: " + e.getMessage());
        }

        return bookings;
    }
}