-- Stage 14F: Backup the entire cinema_booking database first.
-- Run as schema administrator in MySQL Workbench. Existing data is NOT deleted.
USE cinema_booking;

DELIMITER $$
DROP PROCEDURE IF EXISTS stage14f_upgrade_booking $$
CREATE PROCEDURE stage14f_upgrade_booking()
BEGIN
 DECLARE duplicate_pairs INT DEFAULT 0;
 DECLARE has_guard_col INT DEFAULT 0;
 DECLARE has_guard_index INT DEFAULT 0;
 SELECT COUNT(*) INTO duplicate_pairs FROM (
   SELECT showtime_id,seat_id FROM tickets WHERE status='BOOKED'
   GROUP BY showtime_id,seat_id HAVING COUNT(*)>1
 ) AS duplicates;
 IF duplicate_pairs>0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Active duplicate seats exist; reconcile before upgrading';
 END IF;
 ALTER TABLE bookings MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
 ALTER TABLE tickets MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
 SELECT COUNT(*) INTO has_guard_col FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='tickets' AND column_name='active_seat_guard';
 IF has_guard_col=0 THEN
  ALTER TABLE tickets ADD COLUMN active_seat_guard TINYINT GENERATED ALWAYS AS
   (CASE WHEN status='BOOKED' THEN 1 ELSE NULL END) STORED;
 END IF;
 SELECT COUNT(*) INTO has_guard_index FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='tickets' AND index_name='uq_one_active_ticket_per_showtime_seat';
 IF has_guard_index=0 THEN
  ALTER TABLE tickets ADD UNIQUE KEY uq_one_active_ticket_per_showtime_seat
   (showtime_id,seat_id,active_seat_guard);
 END IF;
END $$
DELIMITER ;

CALL stage14f_upgrade_booking();
DROP PROCEDURE stage14f_upgrade_booking;
