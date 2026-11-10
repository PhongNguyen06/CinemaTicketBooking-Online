$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven (mvn) is not on PATH.' }
if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) { throw 'JDK 21 jpackage is not on PATH. Set JAVA_HOME to JDK 21.' }

Write-Host 'Building Cinema Ticket Booking...' -ForegroundColor Cyan
mvn -B clean package dependency:copy-dependencies '-DincludeScope=runtime' '-DoutputDirectory=target/stage13-app'
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }

$jar = 'cinema-ticket-booking-javafx-1.0-SNAPSHOT.jar'
Copy-Item "target/$jar" "target/stage13-app/$jar" -Force
New-Item -ItemType Directory -Force -Path 'dist' | Out-Null

# app-image makes a local Windows .exe launcher, not an installer.
# JavaFX Windows dependency JARs include the platform native libraries.
jpackage --type app-image `
    --name CinemaTicketBooking `
    --app-version 1.3.0 `
    --input 'target/stage13-app' `
    --main-jar $jar `
    --main-class 'com.cinema.DesktopLauncher' `
    --dest 'dist' `
    --java-options '-Dfile.encoding=UTF-8' `
    --win-console
if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }

# Place writable data beside launcher. CSV mode relies on relative database/*.csv.
Copy-Item -Recurse -Force 'database' 'dist/CinemaTicketBooking/database'
@'
@echo off
cd /d "%~dp0"
"%~dp0CinemaTicketBooking.exe" %*
'@ | Set-Content -Encoding ascii 'dist/CinemaTicketBooking/LaunchCinema.bat'
Write-Host 'App image created at dist/CinemaTicketBooking/' -ForegroundColor Green
Write-Host 'For CSV mode, start via LaunchCinema.bat from a writable location.'
