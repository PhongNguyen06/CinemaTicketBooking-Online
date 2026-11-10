# Stage 11 — Password security and Windows packaging

Stage 11 is a direct copy of Stage 10 with targeted improvements. Existing screens, controllers, MySQL tables and booking transactions are retained.

## Password handling
- New customer passwords are stored as salted `pbkdf2_sha256$310000$...` hashes. This is **hashing**, not reversible encryption.
- Existing plain-text credentials remain readable only for legacy sign-in. Upon a successful login, that account is upgraded to a hash and the `users` repository is saved. If the save fails, sign-in is denied so an apparent upgrade is not silently lost.
- Back up `cinema_booking` and CSV before the first legacy login. Until each old account logs in, its password remains plain text in the database. Reset or migrate dormant accounts before production.
- Stage 11 does NOT automatically change existing admin credentials. On a new empty database/CSV, the default admin username is `admin` and you must set `CINEMA_INITIAL_ADMIN_PASSWORD` to a strong password (at least 12 characters) before first launch. The old `admin123` password works only for a pre-existing legacy account until it logs in; change it through a proper password-reset workflow before production.
- `CINEMA_INITIAL_ADMIN_PASSWORD` is only used if no Admin role exists. Remove it from your environment once the first administrator is created.
- PBKDF2 passwords contain only ASCII characters safe for the existing CSV layout. No MySQL schema change required (ensure `users.password` can hold 255 characters).

## Running on Windows (PowerShell)
```powershell
$env:CINEMA_STORAGE = 'mysql'
$env:CINEMA_DB_URL = 'jdbc:mysql://localhost:3306/cinema_booking'
$env:CINEMA_DB_USER = 'cinema_app'
$env:CINEMA_DB_PASSWORD = '<your local password>'
# Only if the database does not contain an Admin yet:
# $env:CINEMA_INITIAL_ADMIN_PASSWORD = '<a unique 12+ character password>'
mvn clean javafx:run
```

## Package as Windows app-image
Require JDK 21 with `jpackage` and Maven on PATH. From the project directory in PowerShell:
```powershell
powershell -ExecutionPolicy Bypass -File .\package-windows.ps1
```
The output is `dist/CinemaTicketBooking/CinemaTicketBooking.exe`, plus a JRE runtime and dependencies. This is an **app-image**, not an MSI/installer. Run `LaunchCinema.bat` for CSV mode so the relative CSV directory resolves. The app-image does not bundle MySQL Server; for MySQL mode, set `CINEMA_STORAGE`, `CINEMA_DB_URL`, `CINEMA_DB_USER`, and `CINEMA_DB_PASSWORD` in the launch environment. Use a low-permission DB account, not root. Avoid storing DB passwords in the launcher script or bundling them in the ZIP.

## Checks before distributing
1. Back up the database; confirm `users.password` length supports hashes (`VARCHAR(255)` or similar).
2. Authenticate an existing account; inspect `users.password` to confirm it now begins `pbkdf2_sha256$`. Do not share the hash or password.
3. Register a new account, restart the app and sign in successfully.
4. Test Booking: two tickets in one booking, reject duplicate seat, cancel and re-book, inspect MySQL statuses.
5. Build the app-image on Windows and run the EXE on a clean test machine with database connectivity.
6. Finish production-hardening: password-reset/change UI, role authorization on sensitive service operations, restricted DB credentials, concurrent booking integration tests and installation/signing.

### Known limitations
- This environment did not run Windows `jpackage` or a live MySQL JavaFX GUI. The .exe is not included in this ZIP.
- Legacy passwords remain plain text until successfully logged in and upgraded; do not consider this a fully migrated credential store.
- UserRepository saves whole user lists, so concurrent account updates across processes require a row-level SQL update design before multi-user deployment.
