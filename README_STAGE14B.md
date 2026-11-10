# Cinema Ticket Booking – Stage 14B (first remote desktop integration)

This is a direct continuation of Stage 14A. The `cinema-server` Spring Boot backend is unchanged. The `cinema-desktop` JavaFX app has a new **opt-in remote mode**.

## What works in remote mode

- Login using `/api/auth/login`, storing the bearer token **in memory only**.
- Load movies from `/api/movies` after login and when clicking Refresh.
- Admin can add movies using `POST /api/movies` and delete movies using `DELETE /api/movies/{id}`.
- Logout revokes the token using `/api/auth/logout` and clears the client token.
- Movie changes become visible on other clients when they click Refresh. Real-time push is **not** implemented yet.

## What does not work in remote mode yet

- Registration, password change, cinema/room/seat/showtime management, booking, ticket management, customer management. Their old UI and code are preserved, but the corresponding navigation buttons are hidden in remote mode to prevent accidental direct database access. **Do not deploy this as a complete multi-PC booking solution yet.**
- Full desktop / backend builds have not been tested here against a live MySQL Server; execute and verify on Windows.
- HTTP calls currently occur on the JavaFX application thread. A production client must move network calls to background Tasks and provide visible offline/retry UX.

## Run locally (two terminals)

Terminal A in `cinema-server`:

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/cinema_booking'
$env:DB_USER = 'cinema_app'
$env:DB_PASSWORD = 'YOUR_DB_PASSWORD'
mvn spring-boot:run
```

Terminal B in `cinema-desktop`:

```powershell
$env:CINEMA_API_URL = 'http://localhost:8080/'
Remove-Item Env:CINEMA_STORAGE -ErrorAction SilentlyContinue
mvn clean javafx:run
```

`CINEMA_API_URL` enables remote mode. **Do not set DB credentials on desktop clients.** When testing on another machine or deploying online, set `CINEMA_API_URL` to your actual **HTTPS** API origin, for example `https://api.example.com/`. An online URL using plain HTTP is intentionally rejected.

The existing DB must contain an ADMIN user whose password was migrated to a PBKDF2 hash in Stage 11–13. Stage 14A server does not authenticate plaintext legacy passwords.

## Smoke test

1. Confirm server responds at `/api/health`.
2. Start JavaFX in remote mode; login with a hashed ADMIN account.
3. View the movie catalog and add a test movie.
4. Open a second client in remote mode; login and click Refresh. The test movie should appear on both.
5. Delete the test movie in the first client, refresh in the second. If the movie has showtimes, the API should reject deletion.
6. Verify the JavaFX client has **no** MySQL credentials configured.

## Cloud deployment prerequisites

Deploy `cinema-server` behind HTTPS, place MySQL behind private network access, use a dedicated DB account (not `root`), configure backups and authentication/session security, and keep API secrets only on the server. Stage 14B is an incremental integration, not a hardened production release.

## Files changed from Stage 14A

- `cinema-desktop/pom.xml`: Jackson JSON parser dependency.
- `cinema-desktop/src/main/java/com/cinema/remote/CinemaApiClient.java`: typed API methods, bearer authorization.
- `cinema-desktop/src/main/java/com/cinema/remote/RemoteSession.java`: opt-in remote configuration.
- `cinema-desktop/src/main/java/com/cinema/controller/LoginController.java`: remote login, remote-registration guard.
- `cinema-desktop/src/main/java/com/cinema/controller/DashboardController.java`: remote movie listing, add/delete, remote-only controls, logout.
