-- Stage 10: Run this ONCE after backing up the database.
-- First inspect any conflicting currently BOOKED tickets:
USE cinema_booking;
SELECT showtime_id, seat_id, COUNT(*) AS active_count,
       GROUP_CONCAT(id ORDER BY id) AS ticket_ids
FROM tickets
WHERE status='BOOKED'
GROUP BY showtime_id, seat_id
HAVING COUNT(*) > 1;

-- If the query above returns rows, STOP here and resolve duplicated bookings.
-- Do not delete tickets without confirming their corresponding booking histories.
-- Run the ALTER statement below ONLY if there are no active duplicate tickets.

-- MySQL 8+ permits multiple NULL values in a unique index. Thus cancelled
-- tickets do not block reselection, while only one BOOKED ticket may exist.
ALTER TABLE tickets
    ADD COLUMN active_seat_guard TINYINT
        GENERATED ALWAYS AS (CASE WHEN status='BOOKED' THEN 1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uq_one_active_ticket_per_showtime_seat
        (showtime_id, seat_id, active_seat_guard);

-- Note: Do not rerun ALTER if index already exists.
