-- Execute in MySQL 8.0+ as an administrator (or a schema owner).
CREATE DATABASE IF NOT EXISTS cinema_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cinema_booking;

CREATE TABLE IF NOT EXISTS cinemas (id BIGINT PRIMARY KEY, name VARCHAR(255) NOT NULL, address VARCHAR(500) NOT NULL);
CREATE TABLE IF NOT EXISTS movies (id BIGINT PRIMARY KEY, title VARCHAR(255) NOT NULL, duration INT NOT NULL, genre VARCHAR(150), description TEXT);
CREATE TABLE IF NOT EXISTS customers (id BIGINT PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255), phone VARCHAR(40));
CREATE TABLE IF NOT EXISTS rooms (id BIGINT PRIMARY KEY, room_name VARCHAR(100) NOT NULL, capacity INT NOT NULL, cinema_id BIGINT,
 CONSTRAINT fk_rooms_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas(id) ON DELETE RESTRICT);
CREATE TABLE IF NOT EXISTS seats (id BIGINT PRIMARY KEY, seat_row VARCHAR(30) NOT NULL, seat_number INT NOT NULL, seat_type VARCHAR(30) NOT NULL, room_id BIGINT,
 CONSTRAINT fk_seats_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT,
 UNIQUE KEY uq_seat_per_room (room_id,seat_row,seat_number));
CREATE TABLE IF NOT EXISTS showtimes (id BIGINT PRIMARY KEY, start_time VARCHAR(40) NOT NULL, end_time VARCHAR(40) NOT NULL, status VARCHAR(40) NOT NULL,
 movie_id BIGINT, room_id BIGINT, ticket_price INT NOT NULL,
 CONSTRAINT fk_showtime_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE RESTRICT,
 CONSTRAINT fk_showtime_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT);
CREATE TABLE IF NOT EXISTS users (id BIGINT PRIMARY KEY, username VARCHAR(150) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL, role VARCHAR(30) NOT NULL, customer_id BIGINT,
 CONSTRAINT fk_user_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT);
CREATE TABLE IF NOT EXISTS bookings (id BIGINT PRIMARY KEY, booking_date VARCHAR(40) NOT NULL, total_amount DECIMAL(14,2) NOT NULL, status VARCHAR(30) NOT NULL, customer_id BIGINT,
 CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT);
CREATE TABLE IF NOT EXISTS tickets (id BIGINT PRIMARY KEY, price INT NOT NULL, status VARCHAR(30) NOT NULL, booking_id BIGINT, seat_id BIGINT, showtime_id BIGINT,
 CONSTRAINT fk_ticket_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
 CONSTRAINT fk_ticket_seat FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE RESTRICT,
 CONSTRAINT fk_ticket_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(id) ON DELETE RESTRICT);

-- Create a dedicated MySQL user and grant SELECT/INSERT/UPDATE/DELETE on cinema_booking.*.
-- For production, replace plaintext passwords with salted password hashes and move
-- ticket/booking writes to a single DB transaction with a uniqueness lock.
