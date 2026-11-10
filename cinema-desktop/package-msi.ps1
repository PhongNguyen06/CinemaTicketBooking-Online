# Build a Windows MSI from the Stage 13 app-image. Requires JDK 21 and WiX.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not $IsWindows -and $PSVersionTable.PSEdition -eq 'Core') {
    throw 'Windows is required for jpackage MSI.'
}
foreach ($command in @('mvn', 'jpackage')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "$command was not found. Install JDK 21 and Maven and add them to PATH."
    }
}
if (-not (Get-Command 'candle.exe' -ErrorAction SilentlyContinue) -and
    -not (Get-Command 'wix.exe' -ErrorAction SilentlyContinue)) {
    throw 'WiX Toolset was not found on PATH (candle.exe or wix.exe). Install a version supported by your JDK 21 distribution.'
}

# Build the app-image first, using the existing Stage 13 packaging workflow.
& "$PSScriptRoot\package-windows.ps1"
if ($LASTEXITCODE -ne 0) { throw 'App-image build failed.' }

# MSI is MySQL-only: do not install CSV seed data into read-only Program Files.
# Also do not package a batch launcher intended for portable CSV mode.
$staging = Join-Path $PSScriptRoot 'target\stage13-msi-app-image\CinemaTicketBooking'
if (Test-Path (Split-Path $staging)) { Remove-Item (Split-Path $staging) -Recurse -Force }
New-Item -ItemType Directory -Force -Path (Split-Path $staging) | Out-Null
Copy-Item 'dist\CinemaTicketBooking' $staging -Recurse -Force
foreach ($obsolete in @('database', 'LaunchCinema.bat')) {
    $path = Join-Path $staging $obsolete
    if (Test-Path $path) { Remove-Item $path -Recurse -Force }
}
New-Item -ItemType Directory -Force -Path 'dist\installer' | Out-Null

jpackage --type msi `
    --name CinemaTicketBooking `
    --app-version 1.3.0 `
    --app-image $staging `
    --dest 'dist\installer' `
    --vendor 'Cinema Ticket Booking Project' `
    --description 'Cinema Ticket Booking System - JavaFX Desktop' `
    --win-menu `
    --win-shortcut `
    --win-dir-chooser
if ($LASTEXITCODE -ne 0) { throw 'MSI build failed. Check JDK 21 jpackage and WiX compatibility.' }

Write-Host 'MSI generated in dist\installer' -ForegroundColor Green
Write-Warning 'MSI installs the client only. MySQL is NOT included.'
Write-Warning 'Set CINEMA_STORAGE, CINEMA_DB_URL, CINEMA_DB_USER and CINEMA_DB_PASSWORD for the signed-in Windows user before launching.'
