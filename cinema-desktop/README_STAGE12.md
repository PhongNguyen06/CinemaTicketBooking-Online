# Stage 12 — Account Security and Windows Release

Based directly on Stage 11; retains the existing MySQL schema, JavaFX views and prior business logic.

## What's new
- `UserService.changePassword`: checks the existing password and saves a new salted PBKDF2 hash. Keeps old stored hash if persistence fails.
- Dashboard `Change Password` button for both Admin and Customer. The button requires old password, new password (12–128 chars), and confirmation.
- Registration now requires a minimum of 12 characters.
- No database migration is needed. Existing MySQL records remain unchanged until a user changes their password.

## Windows development run
1. Extract ZIP to a NEW folder; do not overwrite your working copy. Make a MySQL backup first.
2. Open this folder in IntelliJ, set Project SDK and Maven JDK to JDK 21.
3. Configure in a *new* PowerShell terminal (replace placeholders; don't send passwords to anyone):

```powershell
$env:CINEMA_STORAGE = 'mysql'
$env:CINEMA_DB_URL = 'jdbc:mysql://localhost:3306/cinema_booking'
$env:CINEMA_DB_USER = 'cinema_app'
$env:CINEMA_DB_PASSWORD = 'YOUR_MYSQL_PASSWORD'
mvn clean javafx:run
```

If your database login is still root, use root temporarily instead of cinema_app; prefer a least-privilege database user for regular use.

4. Login, press **Change Password** in sidebar and test old vs new passwords.

## Windows app-image (Windows only)
Run `powershell -ExecutionPolicy Bypass -File .\package-windows.ps1` with JDK 21's `jpackage` on PATH. Expected output: `dist/CinemaTicketBooking/CinemaTicketBooking.exe` (app-image, NOT MSI installer). This cannot be built on Linux for Windows.

### MySQL runtime setup
The EXE requires the MySQL server reachable at the configured JDBC URL. Set CINEMA_STORAGE, CINEMA_DB_URL, CINEMA_DB_USER, CINEMA_DB_PASSWORD in the environment of the launching process. Never bake the database password into the release ZIP. For first-time admin provisioning only, you may also need CINEMA_INITIAL_ADMIN_PASSWORD.

Do not run `ImportCsvToMySql` on an already initialized database. If you use CSV mode, keep the writable database folder beside the launcher and run LaunchCinema.bat.

## Test checklist
- Confirm old password can login before changing it.
- Wrong current password refuses change; wrong confirmation refuses.
- Correct current password + 12-character new password saves.
- Logout: old password fails, new password logs in.
- MySQL: inspect password status without selecting raw hashes: `SELECT username, CASE WHEN password LIKE 'pbkdf2_sha256$%' THEN 'HASHED' ELSE 'LEGACY' END AS status FROM users;`.
- Confirm existing Booking, Ticket, Showtime and Customer screens still function.
- Test EXE generation on your Windows machine; this package has not been run with Windows jpackage here.

**Security boundary:** This is a classroom/local desktop project. It does not implement password reset via email, account lockout/rate limiting, or a remotely authenticated application server. Local clients connected directly to the database require careful access control; avoid distributing shared database credentials.
