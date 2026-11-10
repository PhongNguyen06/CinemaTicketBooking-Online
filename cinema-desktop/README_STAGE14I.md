# Stage 14I: Windows packaging (non-modular JavaFX)

## Changes
- `src/main/java/com/cinema/remote/RemoteSession.java`: online mode by default, HTTPS Render URL `https://cinema-booking-api-8dsw.onrender.com/`; `CINEMA_API_URL` overrides the default for testing.
- `build-windows.ps1`: Maven package → JAR and dependency JARs → `jpackage --type app-image`; optional `-Msi` for MSI.
- Stage 14I step 1/2 Maven `Launcher.java` and manifest/dependency plugins retained.

## On Windows

```powershell
cd .\cinema-desktop
java -version
jpackage --version
mvn -version
mvn clean package
java -jar .\target\cinema-ticket-booking-javafx-1.0-SNAPSHOT.jar
.\build-windows.ps1
& '.\dist\CinemaTicketBooking\CinemaTicketBooking.exe'
```

For MSI, install a WiX Toolset compatible with the installed JDK and run:

```powershell
.\build-windows.ps1 -Msi
```

The generated app-image contains a runtime, the app JAR, and dependencies. It does not include a standalone MySQL server or local CSV data. Users need an Internet connection to the Render-hosted backend. The free service may sleep when idle.

## Development override

```powershell
$env:CINEMA_API_URL='http://localhost:8080/'
mvn javafx:run
```

Use `https://` for nonlocal endpoints. Do not embed database passwords in the desktop application.

## Release checks
1. Ensure Render/TiDB are reachable, backend passwords are hashed, and server authorization is tested.
2. On a machine without Java/Maven, launch the app-image or install MSI; verify Admin/Customer login, catalog, booking, and WebSocket reconnection.
3. Note `jpackage` and WiX must run on **Windows**; this ZIP is source+packaging scripts, **not a compiled EXE/MSI**.

> Note: old `package-windows.ps1` and `package-msi.ps1` are legacy Stage 13 scripts for CSV/local storage. Use `build-windows.ps1` for Stage 14I.

The old local `database/*.csv` seed files (including test credentials) were excluded from this Stage 14I ZIP because this version is online-only. The original Stage 14H ZIP remains unchanged.
