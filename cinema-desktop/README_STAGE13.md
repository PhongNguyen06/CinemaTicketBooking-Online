# Stage 13 — Windows Installer and Release Verification

Stage 13 is based directly on Stage 12. It preserves all JavaFX views, MySQL connection behavior, account security, booking logic and CSV compatibility. Stage 13 changes packaging and adds **read-only** database diagnostics. It does **not** modify the MySQL schema or import data.

## New files
- `package-msi.ps1`: builds a `.msi` installer from a Windows `app-image`; uses `jpackage`, Maven and WiX. Installer contains the application and bundled Java runtime but **not** MySQL Server, database credentials or example CSV data.
- `verify-release.ps1`: compiles project, checks FXML syntax, and conditionally runs DB diagnostics.
- `src/main/java/com/cinema/DatabaseHealthCheck.java`: verifies JDBC connectivity, row counts and duplicate active ticket seats. This is a **read-only** diagnostic tool.
- `package-windows.ps1`: updated version to 1.3.0, still builds the portable app-image.

## Prerequisites
Windows, JDK 21 (`java`, `javac`, `jpackage`), Maven, and a compatible WiX Toolset on `PATH` for MSI generation. `jpackage` cannot cross-compile a Windows installer on Linux/macOS. Official documentation: https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html

## Before packaging
1. **Back up** your working MySQL database (`cinema_booking`).
2. Extract Stage 13 to a new folder. **Do not overwrite Stage 12**.
3. Ensure your existing account can log in with `mvn clean javafx:run`.
4. Open PowerShell in the project root, set the same MySQL variables you already use (never send or commit the password):

```powershell
$env:CINEMA_STORAGE = 'mysql'
$env:CINEMA_DB_URL = 'jdbc:mysql://localhost:3306/cinema_booking'
$env:CINEMA_DB_USER = 'cinema_app' # or your existing MySQL user
$env:CINEMA_DB_PASSWORD = 'YOUR_PASSWORD'
.erify-release.ps1
```

Read-only DB check may also be run by itself:

```powershell
mvn compile exec:java '-Dexec.mainClass=com.cinema.DatabaseHealthCheck'
```

A successful DB diagnostic is not a substitute for manually verifying Login, Change Password, Cinema/Room/Seat/Showtime CRUD, Booking/Cancel Booking, and duplicate-seat protection using two independent app instances.

## Create a portable EXE folder (no WiX required)
```powershell
powershell -ExecutionPolicy Bypass -File .\package-windows.ps1
```
Output: `dist\CinemaTicketBooking\CinemaTicketBooking.exe` (+ app and runtime folders). This is **not** an installer. Start the portable MySQL client from the same PowerShell environment as the DB credentials, or configure the environment variables for the Windows user before opening normally.

## Create a Windows MSI installer (WiX required)
```powershell
powershell -ExecutionPolicy Bypass -File .\package-msi.ps1
```
Output: `.msi` file in `dist\installer\`. The installer is deliberately **MySQL-only**: it excludes the sample `database` CSV folder because Windows `Program Files` is generally not writable for CSV-based persistence. If you want CSV mode, keep using the **portable** app-image (`LaunchCinema.bat`) in a writable location.

**Security:** Never hardcode your MySQL password into `package-msi.ps1`, ZIP, launcher `.cfg`, or a distributable `.bat`. An MSI installs an application client, **not** MySQL Server; users must already have connectivity and credentials. For distributing to many users, direct desktop-to-database access is not an ideal architecture; use a backend API and separate DB credentials.

## Acceptance checklist
- [ ] Stage 13 JavaFX logs into existing database.
- [ ] New accounts and password changes persist, and old password fails after changing.
- [ ] Admin adds/edits/deletes a test movie; database changes confirmed.
- [ ] Customer books at least two seats; one booking and matching tickets appear.
- [ ] Two app instances cannot book the same seat concurrently.
- [ ] Cancelling a booking updates all associated ticket statuses.
- [ ] Portable app-image starts on Windows without IntelliJ.
- [ ] MSI installs/uninstalls cleanly, shortcut works, and the installed application can reach MySQL with externally configured environment settings.

## Known limitations
- The full JavaFX workflow, two-client booking race, Windows app-image launch and MSI creation **have not been executed** in this Linux environment. Test on your Windows machine.
- The existing repositories' full-table updates and direct DB credentials still require security and concurrency review before real multi-user deployment.
- The installer is unsigned; Windows SmartScreen or organizational policy may warn. Signing needs a trusted code-signing certificate.
