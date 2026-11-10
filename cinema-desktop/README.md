# Cinema Ticket Booking JavaFX — Stage 13

Latest release instructions: [README_STAGE13.md](README_STAGE13.md).

# Cinema Ticket Booking System — JavaFX (Stage 1)

This is a **first working migration stage** of the uploaded CMD project, not the finished cinema-booking application.

## Requirements
- JDK 21 (set IntelliJ Project SDK to 21)
- Maven (IntelliJ has Maven support)
- Windows, macOS or Linux with graphical desktop

## Run
1. Extract ZIP and open the **CinemaTicketBookingJavaFX** folder in IntelliJ IDEA as a Maven project.
2. Set Maven JDK / Project SDK to Java 21 and reload Maven.
3. In IntelliJ terminal, from the folder containing `pom.xml`, run `mvn clean javafx:run`, or run `javafx:run` from the Maven tool window.
4. **Working directory must be the project root** containing the `database/` folder. CSV repositories use relative file paths.

## Current features
- Real login using the old `UserService` and `database/users.csv`.
- Create a customer account (basic UI validation).
- Admin/Customer dashboard differentiated by role.
- Movie list from the old `MovieService` and `database/movies.csv`.
- Admin can add/delete movies with confirmation.
- Logout confirmation.

## Still to migrate
- Cinema, rooms, seats, showtimes, booking, tickets, full CRUD screens, seat selection and all original CMD menu functions.
- Password security: the inherited project uses **plain-text passwords**, including a default admin (`admin` / `admin123` in UserService). This is not production-ready. Change credentials before deploying and later add password hashing.
- CSV data storage is retained for compatibility, not safe for simultaneous multi-user booking. Migrate to a transactional database before production.
- The original console Main.java and ui/login packages are not part of this GUI project. Original entity, enums, repository, service source was copied without rewriting it.

## Packages
- `entity`, `enums`, `repository`, `service`: copied from original code.
- `com.cinema.MainApp`: launches JavaFX.
- `com.cinema.AppContext`: shares old service objects.
- `com.cinema.controller`: screen handlers.
- `src/main/resources/com/cinema/view`: FXML layouts.
- `src/main/resources/com/cinema/css`: styles.


## Stage 2 - Cinema Management (Admin)
- Sign in as admin; click Cinema Management in the left navigation.
- View all cinema records, search by name or address, add, edit and delete records.
- Existing CinemaService and CinemaRepository persist changes to database/cinemas.csv.
- Cinema deletion is blocked if there are related rooms.
- Use Back to Dashboard to return to the movie list.
- Customer users do not see the Cinema Management button.
- Before migrating your real CSV data, back up the entire database folder.


## Stage 3 - Room Management

- Admin Dashboard > Room Management: list rooms, search by name/cinema, add/edit/delete.
- Room IDs are auto-assigned by existing RoomService, saved to `database/rooms.csv`.
- Each room requires a cinema and positive capacity. CSV room names cannot contain commas.
- Safety: prevent deleting rooms with any seats or showtimes, and prevent reducing capacity below the assigned seat count.
- Current dataset contains rooms with assigned seats/showtimes; deletion can be blocked as intended.
- Run from the project root: `mvn clean javafx:run` (JDK 21).
- Use the backed-up `database/` folder from Stage 2 if you want to preserve changes.
- No MySQL migration yet. Seat management, showtime UI, booking UI are future stages.


## Stage 4 — Showtime Management

Admin Dashboard -> Showtime Management. Search (Movie, Room, Cinema, Status), Add, Edit, Delete.
- Pick movie, room/cinema, start date/time, end time (blank end uses movie duration), ticket price and status.
- Showtime ID is allocated by the old `ShowtimeService`; data persists to `database/showtimes.csv`.
- Overlap checks prevent scheduled/started/ended screenings in the same room overlapping (cancelled entries excluded).
- Existing showtimes with ticket records (including cancelled tickets) cannot be edited or deleted.
- A movie referenced by any showtime cannot be deleted from dashboard.
- This stage retains CSV storage. The old CSV may include historical dates; they remain visible.
- Files added: `ShowtimeController.java`, `showtime.fxml`; changed: `DashboardController.java`, `dashboard.fxml`, `README.md`.
- Requires Java 21 and Maven. Run from project root with `mvn clean javafx:run`.
- Please keep a backup of the `database` folder before changing versions.

## Stage 5 – Seat Management and Seat Selection

Stage 5 adds two screens while reusing the original Java Core entity/service/repository classes and CSV files.

- **Admin → Seat Management**: choose room, list seats, create seats with automatic IDs, edit, delete with confirmation. Validate row/number uniqueness per room and capacity. Seats referenced by ticket history cannot be edited/deleted.
- **Customer → Select Seats**: choose an upcoming, non-cancelled showtime, view the seating chart, identify booked seats from ticket history, select/unselect multiple seats and preview an estimated total. **Preview does NOT save bookings or tickets.**
- Seat colors: green available normal; yellow available VIP; gray booked; blue selected. Price preview uses the ticket price configured on the showtime (VIP does not have a separate price in the existing domain model).
- Existing data continues to be stored in `database/*.csv`. Open IntelliJ with project root as working directory. The customer view requires an upcoming showtime and seats assigned to its room.

### Run

```powershell
mvn clean javafx:run
```

Test as Admin by opening **Seat Management**, adding a seat to an existing room, and verifying that `database/seats.csv` changes. Test as Customer by selecting **Select Seats**, choosing a future showtime, and clicking several seats. The booking confirmation/write flow is scheduled for Stage 6.

### Known limitation

CSV file storage is not transactional and has no multi-user concurrency protection. Do not use for real simultaneous ticket sales; later stages should migrate booking writes to a database transaction with seat uniqueness protection.


## Stage 6 – Booking & Ticket Management

- **Customer**: Select Seats → select one or more available seats → **Confirm Booking**. The workflow checks account ownership, showtime availability, room membership and seat availability again immediately before saving. A single booking is created with one ticket per selected seat; booking and ticket data are written to `database/bookings.csv` and `database/tickets.csv`.
- **Customer → My Bookings**: See only your bookings, select one to see its tickets, cancel with confirmation (only before the showtime begins).
- **Admin → Booking & Ticket Management**: View all bookings and linked tickets, and cancel bookings.
- **CSV safety**: Writes are serialized *within the running process*, with in-memory rollback and best-effort restoration of both CSV files on write failure. This **is not a real transaction** and does **not** protect against multiple application processes, power failures, or simultaneous sales across machines. Migrate to MySQL transactions with a `(showtime_id, seat_id)` uniqueness rule for production.
- The current app still stores user passwords as plaintext, inherited from the original code. Not production-safe.
- Run from project root: `mvn clean javafx:run`. Back up your `database` folder before upgrading, and copy it into Stage 6 if you want to preserve modified Stage 5 data.


## Stage 7 - Customer Management & Admin Dashboard

Admin dashboard now shows live summary totals for Movies, Cinemas, Rooms, Customers and Bookings. The new **Customer Management** screen provides:
- View all customers, search by name, phone or email.
- Add and update customer information, with basic input validation and duplicate-email check.
- Delete customers only if they have **no login account and no bookings**. Existing account/booking references are preserved.
- Select a customer to inspect booking history (ID, date/time, status and total).

New files: `CustomerController.java`, `customer.fxml`. Updated: `DashboardController.java`, `dashboard.fxml`.

### Run
From the directory containing pom.xml, using JDK 21:
`mvn clean javafx:run`

Default legacy admin: `admin` / `admin123`. This is a learning project: passwords are still stored in plaintext, and the CSV repository does not provide multi-user transactions. Run with the project directory as working directory so `database/*.csv` can be found. Back up the entire `database` directory before copying data from an earlier stage.
