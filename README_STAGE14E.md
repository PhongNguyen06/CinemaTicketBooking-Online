# Stage 14E — Seat & Showtime via Spring Boot API

Based on **Stage 14D**, preserving both the original `cinema-desktop` and `cinema-server` modules. No new tables or CSV reimport required.

## New in Stage 14E
- Online JavaFX Admin can open Seat Management and Showtime Management, list/create/update/delete with server API.
- Server validates seat row/number duplication, room capacity, ticket history for seat edits, valid showtime time range, movie/room references and scheduling overlaps.
- Movie, cinema, room screens are retained. Customer online seat selection, booking, tickets, account administration and password change remain disabled or pending migration.
- Refresh on each screen fetches server data; WebSocket auto-refresh is **not** implemented yet.

## Run in PowerShell — two terminals
From project root:

Server:
```powershell
cd .\cinema-server
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/cinema_booking'
$env:DB_USER = 'cinema_app'
$env:DB_PASSWORD = '<your own password>'
mvn spring-boot:run
```

Desktop in a separate terminal from project root:
```powershell
cd .\cinema-desktop
$env:CINEMA_API_URL = 'http://localhost:8080/'
Remove-Item Env:CINEMA_STORAGE -ErrorAction SilentlyContinue
mvn clean javafx:run
```

Sign in as ADMIN, open Seat Management and Showtime Management, create entries, then refresh a second instance. Do not test deletion on irreplaceable production records.

## Safety and limitations
- This version has **not** been fully compiled/tested on Windows with a real database. Verify before deployment.
- Seat and showtime write paths still use `MAX(id)+1` and Java synchronized, which is NOT safe across multiple server instances. Move to AUTO_INCREMENT and enforce unique keys in MySQL for production.
- Additional booking-write API and multi-client booking tests must be completed before customers can book in online mode.
- API calls are synchronous on the JavaFX thread and could freeze UI on slow connections; later migrate to JavaFX Task.
- In Internet deployment use HTTPS, secure session persistence, server-only DB credentials and proper MySQL backups.
