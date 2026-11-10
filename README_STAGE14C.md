# Stage 14C – Central API: Cinema, Room and Seat Management

**Source:** copied from Stage 14B. This is an incremental backend release, not a fully converted JavaFX online app.

## Included
- Spring Boot: Admin POST/PUT/DELETE `/api/cinemas`, `/api/rooms`, `/api/seats`; existing GET endpoints and movie APIs unchanged.
- Validation: required fields, capacity check, duplicate seats via existing MySQL unique constraint, deletion restrictions via existing foreign keys.
- Clearer error JSON for rejected requests (including invalid account role); Stage 14B HTTP 403 **is not presumed fixed** until verified against the real `users.role` data.
- Existing JavaFX UI and all Stage 14B source retained; new management actions are **API-only**. No new UI wiring yet.

## Run
1. From `cinema-server`, set `DB_URL`, `DB_USER`, `DB_PASSWORD` in PowerShell (do not put secrets in git).
2. `mvn spring-boot:run`
3. `GET http://localhost:8080/api/health`.
4. In Workbench, verify `SELECT username,role,CASE WHEN password LIKE 'pbkdf2_sha256$%' THEN 'HASHED' ELSE 'LEGACY' END AS password_status FROM cinema_booking.users;`.
5. Login: POST `/api/auth/login` with `{"username":"...","password":"..."}`; valid role `ADMIN` or `USER`. Do not share passwords or bearer tokens.
6. Admin create cinema: POST `/api/cinemas` with `{"name":"Demo Cinema","address":"Demo Address"}` and `Authorization: Bearer <token>`.
7. Use GET `/api/cinemas` in a separate client to observe the shared data.

## Important restrictions
- Before deploying online, use HTTPS, central logging, secure token storage, backups, and no exposed MySQL port.
- IDs are still allocated with `MAX(id)+1` in this learning-phase code, safe only for a **single server instance**. Migrate to AUTO_INCREMENT before running multiple API replicas.
- Room capacity and seat changes are not fully concurrency-safe; production implementation requires transaction/locking.
- Stage 14C does not move Booking/Ticket writes to server and does not implement WebSocket auto refresh. Do not distribute it to real customers yet.
- No database schema migration required. Create a backup before testing write operations.
