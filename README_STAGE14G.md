# Stage 14G — Automatic UI refresh with WebSocket

This is a direct upgrade of Stage 14F. It preserves the database schema and Booking/Ticket transaction code.

## What changes
- Spring Boot `/ws/updates` WebSocket. After login, JavaFX sends an `AUTH <bearer-token>` frame; tokens are **never placed into URLs**.
- Only authenticated subscribers receive `CHANGED movies|cinemas|rooms|seats|showtimes|reservations` events. The message contains only a topic and timestamp, never personal data.
- On successfully completed POST/PUT/DELETE requests, Spring Boot emits an invalidation event **after the HTTP handler transaction has returned**. Rejected requests do not emit an event.
- JavaFX refreshes the currently visible list/screen when matching events arrive. Availability and booking history refresh after Booking creation/cancellation. Automatic reconnect with bounded retry delay if the WebSocket connection drops.
- Manual Refresh remains available, especially when a WebSocket is blocked by a proxy or the server restarts.

## Run locally
Terminal 1 (from project root):
```powershell
cd .\cinema-server
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/cinema_booking"
$env:DB_USER = "cinema_app"
$env:DB_PASSWORD = "YOUR_DATABASE_PASSWORD"
mvn spring-boot:run
```
Terminal 2 (new terminal from project root):
```powershell
cd .\cinema-desktop
$env:CINEMA_API_URL = "http://localhost:8080/"
Remove-Item Env:CINEMA_STORAGE -ErrorAction SilentlyContinue
mvn clean javafx:run
```
Repeat Terminal 2 for another desktop window. Log into two accounts, navigate to movies / seat selection / bookings, and mutate data in the first window. The second window should refresh without clicking Refresh.

## Security and release limitations
- Use **HTTPS + WSS** for public deployment, not the local HTTP example above. Route `/api/*` and `/ws/updates` through the same TLS reverse proxy.
- Server-side auth is checked on the first WebSocket message and again before pushing each event. Token sessions remain in RAM (existing Stage 14A behavior) and are not shared between multiple Spring Boot server instances.
- This stage is suitable for **one Spring Boot instance**. Multi-instance deployments require a shared message broker and durable/shared session store; local events otherwise don't propagate between instances.
- All list refreshes currently run in the JavaFX UI thread. For production, move network fetches to background tasks, coalesce rapid updates, and retain focus/selection when possible.
- An invalidation event is not a guarantee of delivery; clients must re-fetch on login/reconnect and may use manual refresh.
- Existing Movie/Cinema/Room/Seat/Showtime ID allocation must be upgraded for multi-instance server deployment; don't scale horizontally yet.
- No MySQL schema migration is required for Stage 14G. Keep Stage 14F migration and uniqueness guard in place.

## Testing
1. Start server and desktop; verify login and booking work as in Stage 14F.
2. Start second desktop. Admin adds a movie; second window automatically refreshes movie list.
3. Customer A books a seat. Customer B, on the same screening's seat selection screen, should see the seat become unavailable. Server unique seat guard remains authoritative even if the UI becomes stale.
4. Cancel a booking. Other clients refresh booking history/seat availability.
5. Stop/restart Spring Boot. After reconnect, manually refresh to confirm no missed changes.

**Build status:** Full Maven builds and live Windows/MySQL integration have not been verified in this environment. Run both modules using IntelliJ JDK 21 and share the exact error log if compilation fails.
