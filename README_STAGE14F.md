# Stage 14F — Online Booking & Ticket

This edition builds on Stage 14E; no existing Desktop screens or API controllers are deleted.

## IMPORTANT: before running the new booking APIs
1. Back up the existing MySQL `cinema_booking` database.
2. Review bookings and tickets for active duplicate `(showtime_id,seat_id)` pairs.
3. Run `cinema-server/sql/stage14f_booking_migration.sql` **ONCE** in MySQL Workbench with schema administrator rights. The migration aborts before schema changes if active duplicates exist. Reconcile actual reservations before retrying; do not blindly delete history. This migration can be re-run when the Stage 10 active-seat guard already exists.
4. Use `cinema_app` for ordinary runtime access (SELECT/INSERT/UPDATE/DELETE). Do not use root in the app.

## Run in IntelliJ (two terminals)
From `cinema-server`:
```powershell
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/cinema_booking"
$env:DB_USER = "cinema_app"
$env:DB_PASSWORD = "YOUR_PASSWORD"
mvn spring-boot:run
```
From `cinema-desktop` (new Terminal):
```powershell
$env:CINEMA_API_URL = "http://localhost:8080/"
Remove-Item Env:CINEMA_STORAGE -ErrorAction SilentlyContinue
mvn clean javafx:run
```

## New endpoints
- `GET /api/reservations/showtimes/{showtimeId}/seats`: live per-showtime seat status
- `POST /api/reservations` body `{"showtimeId":1,"seatIds":[1,2]}`: customer only, creates Booking + multiple Tickets atomically
- `GET /api/reservations`: owner-scoped to customer; Admin can view all
- `GET /api/reservations/{id}/tickets`: owner or Admin
- `POST /api/reservations/{id}/cancel`: owner or Admin, before showtime only

The API ignores arbitrary client-supplied customer IDs, ticket prices and status. Authentication determines owner, MySQL determines ticket price. An InnoDB `FOR UPDATE` showtime lock serializes concurrent bookings for one screening, and the unique generated active-seat guard is the database backstop. The old local CSV/MySQL workflow remains available **only when CINEMA_API_URL is unset**. Never distribute this local mode to client computers.

## Tests to perform
- Customer opens Select Seats; a future SCHEDULED screening must have seats.
- Book two seats, verify exactly one Booking and two Tickets in MySQL.
- Open a second JavaFX instance, refresh and verify those seats are unavailable.
- Concurrent booking attempts for the same seat must result in exactly one success.
- My Bookings displays only the signed-in customer's bookings; Admin sees all.
- Cancel a future booking, check both Booking and Tickets become CANCELLED and seats are available again.
- Reject attempts to cancel another customer's booking; reject bookings for an expired screening.

## Limitations
- JavaFX API calls are currently synchronous and can temporarily pause the UI on a slow connection; refactor to JavaFX Task for production.
- Authentication sessions still use an in-memory token store from Stage 14A (restart logs everyone out; not multi-instance ready).
- API reads are refresh-based. WebSocket automatic sync is Stage 14G.
- Schema migration requires manual review of existing data and must be executed before booking tests.
- This artifact has not been tested against a real Windows/MySQL installation in this environment.
