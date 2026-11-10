# Stage 14D — JavaFX Cinema and Room Management via Spring Boot API

Based on Stage 14C. No database reset or CSV import.

## Online mode functionality
- JavaFX Admin: movie catalog (from 14B), Cinema Management and Room Management now read/write through Spring Boot API.
- Admin can add, edit, delete cinemas and rooms, search and refresh lists. API enforces Admin role; MySQL foreign keys protect related data.
- Other clients see updates after clicking Refresh. WebSocket auto-update is not implemented.
- Seat, Showtime, Customer and Booking screens remain disabled in online mode to avoid direct database access.
- CSV/MySQL legacy mode remains available for local development; do not release the legacy mode to remote customers.

## Run with two terminals (PowerShell, from project root)

Server terminal:
```powershell
cd .\cinema-server
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/cinema_booking'
$env:DB_USER = 'cinema_app'
$env:DB_PASSWORD = '<your password>'
mvn spring-boot:run
```
Desktop terminal:
```powershell
cd .\cinema-desktop
$env:CINEMA_API_URL = 'http://localhost:8080/'
Remove-Item Env:CINEMA_STORAGE -ErrorAction SilentlyContinue
mvn clean javafx:run
```

## Test
1. Log in as an Admin account with `role=ADMIN` and a PBKDF2 hashed password.
2. Open Cinema Management; add a sample cinema, edit it and refresh.
3. Open Room Management; choose that cinema and add a room, edit its capacity.
4. Open a second desktop instance in online mode and refresh to see the same data.
5. Avoid testing deletion on live data. Make a backup first.

## Login HTTP 403 diagnostics
The earlier HTTP 403 has not been conclusively diagnosed on your machine. Query `SELECT username,role FROM users` in MySQL. API expects exactly `ADMIN` or `USER`; credentials also must be valid PBKDF2. A 403 persists until the actual cause is corrected. Do not disable Spring Security.

## Limitations / release safety
- This stage is not suitable for production: no server-side booking yet, no WebSocket notifications, no secure online deployment or release audit.
- `CinemaApiClient` requests are synchronous and may freeze the UI during network delays. Move them to JavaFX `Task` in a later stage.
- Stage 14C server still uses `MAX(id)+1` for keys; convert to database-generated keys before scaling to more than one server instance.
- HTTPS is mandatory over the Internet; never expose MySQL to clients or place DB credentials in the desktop installer.
