package com.cinema;

import repository.StorageIO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Read-only MySQL diagnostics; does not print database credentials. */
public final class DatabaseHealthCheck {
    private static final String[] TABLES = {
        "cinemas", "rooms", "seats", "movies", "showtimes",
        "customers", "users", "bookings", "tickets"
    };
    private DatabaseHealthCheck() {}

    public static void main(String[] args) {
        if (!StorageIO.mysqlEnabled()) {
            System.err.println("Set CINEMA_STORAGE=mysql before running diagnostics.");
            System.exit(2);
        }
        try (Connection connection = StorageIO.connection()) {
            if (connection.getCatalog() == null || !"cinema_booking".equalsIgnoreCase(connection.getCatalog())) {
                System.out.println("Connected schema: " + connection.getCatalog() + " (verify intended database)");
            }
            for (String table : TABLES) {
                // Table names are constants, not user-controlled.
                try (PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
                     ResultSet results = query.executeQuery()) {
                    results.next();
                    System.out.println(table + ": " + results.getLong(1));
                }
            }
            long duplicates;
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT COUNT(*) FROM (SELECT showtime_id, seat_id " +
                    "FROM tickets WHERE status = 'BOOKED' " +
                    "GROUP BY showtime_id, seat_id HAVING COUNT(*) > 1) duplicates");
                 ResultSet results = query.executeQuery()) {
                results.next(); duplicates = results.getLong(1);
            }
            System.out.println("Active duplicate seat pairs: " + duplicates);
            if (duplicates > 0) {
                System.err.println("FAILED: duplicate BOOKED seats; resolve before releasing.");
                System.exit(3);
            }
            System.out.println("Database connectivity and duplicate-seat check: PASS");
            System.out.println("This does not prove the entire app is production-ready.");
        } catch (SQLException exception) {
            System.err.println("Database check failed: " + exception.getMessage());
            System.exit(1);
        }
    }
}
