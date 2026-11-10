# Stage 10 — Atomic MySQL Booking / Ticket Operations

Stage 10 continues from Stage 9. JavaFX screens, CSV fallback and legacy entities remain available.

## What changed

- `repository/MySqlBookingStore.java` adds a MySQL-specific booking and cancellation path using one JDBC transaction for `bookings` + `tickets`.
- Before confirmation, it checks `showtimes`, validates seats against MySQL and checks whether a seat is already BOOKED.
- A named MySQL advisory lock coordinates operations across instances **using this workflow**. Existing other writers must migrate to this workflow as well.
- `sql/stage10_active_seat_guard.sql` offers a MySQL UNIQUE constraint on active booked seats. This is the essential database-level protection even when different writers race.
- `BookingWorkflow.java` uses the MySQL store only when `CINEMA_STORAGE=mysql` and retains the old CSV flow otherwise.
- The Seat Selection screen checks current seat status from MySQL rather than only from a startup cache.
- Booking list navigation reloads booking/ticket caches when using MySQL.

## Installation

1. **Back up** your `cinema_booking` database. Do not re-import the old CSV over your working MySQL data.
2. In MySQL Workbench, open `sql/stage10_active_seat_guard.sql` and run **only the SELECT query first**. If it reports duplicate active tickets, resolve the business records before continuing. Then run **only the ALTER TABLE** once. If the unique index already exists, skip ALTER.
3. In IntelliJ open Stage 10 and configure your PowerShell terminal:

   ```powershell
   $env:CINEMA_STORAGE = 'mysql'
   $env:CINEMA_DB_URL = 'jdbc:mysql://localhost:3306/cinema_booking'
   $env:CINEMA_DB_USER = 'root'
   $env:CINEMA_DB_PASSWORD = 'YOUR_OWN_PASSWORD'
   mvn clean javafx:run
   ```

   Use a limited-permission DB account when ready for deployment, and never commit passwords to Git.

4. Verify: Customer books two seats -> one Booking plus two Tickets; second instance cannot book an already BOOKED seat; cancellation changes both booking and ticket rows; cancelled seats become available. Use MySQL Workbench to verify. Test using a separate test database or a backup first.

## Important limitations

- This build was compiled only for **Java Core** in the creation environment. JavaFX and Windows packaging must be tested on your PC.
- Other legacy service methods that write `bookings` and `tickets` directly do not use `MySqlBookingStore`; route those paths through the new workflow before a multi-user deployment.
- The MySQL schema currently stores password values as plaintext; replace password storage with slow salted hashes (Argon2id or bcrypt), including migration/verification, before deployment. No password migration is performed in this stage.
- The MySQL server must remain reachable from the Windows application, and DB credentials must be configured at runtime.
- `package-windows.ps1` builds a Windows app-image (folder containing an exe), **not an installer**. Run it in Windows with JDK 21 + Maven after verifying the app.
- This does not implement online payment processing, login sessions or a hosted server API.
