# Stage 14I: Windows JavaFX app-image and optional MSI.
# Run from PowerShell on Windows: .\build-windows.ps1
param([switch]$Msi)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

foreach ($cmd in @('mvn', 'jpackage')) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        throw "$cmd not found. Install JDK 21+ and Maven, configure PATH/JAVA_HOME."
    }
}

Write-Host 'Building JavaFX desktop...' -ForegroundColor Cyan
& mvn -B clean package
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }

$jarName = 'cinema-ticket-booking-javafx-1.0-SNAPSHOT.jar'
$jarFile = Join-Path $PSScriptRoot "target\$jarName"
$libs = Join-Path $PSScriptRoot 'target\lib'
if (-not (Test-Path $jarFile)) { throw "JAR missing: $jarFile" }
if (-not (Test-Path $libs)) { throw "Runtime libraries missing: $libs" }

# Runtime classpath jars must be beside the main JAR in lib/ as declared in MANIFEST.MF.
$inputDir = Join-Path $PSScriptRoot 'target\jpackage-input'
if (Test-Path $inputDir) { Remove-Item $inputDir -Recurse -Force }
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
Copy-Item $jarFile $inputDir
Copy-Item $libs (Join-Path $inputDir 'lib') -Recurse

# Only the executable and its runtime are distributed; NEVER copy database CSV,
# backups, .env, SSL keys or source into the app image.
$dest = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Path $dest -Force | Out-Null
$appDir = Join-Path $dest 'CinemaTicketBooking'
if (Test-Path $appDir) { Remove-Item $appDir -Recurse -Force }

Write-Host 'Packaging Windows application image...' -ForegroundColor Cyan
& jpackage --type app-image `
    --name CinemaTicketBooking `
    --app-version 1.0.0 `
    --input $inputDir `
    --main-jar $jarName `
    --main-class com.cinema.Launcher `
    --dest $dest `
    --java-options '-Dfile.encoding=UTF-8' `
    --win-console
if ($LASTEXITCODE -ne 0) { throw 'jpackage app-image failed.' }
Write-Host "Created: $appDir\CinemaTicketBooking.exe" -ForegroundColor Green

if ($Msi) {
    $installerDir = Join-Path $dest 'installer'
    New-Item -ItemType Directory -Path $installerDir -Force | Out-Null
    Write-Host 'Packaging MSI; WiX Toolset compatible with the JDK is required...' -ForegroundColor Cyan
    & jpackage --type msi `
        --name CinemaTicketBooking `
        --app-version 1.0.0 `
        --app-image $appDir `
        --dest $installerDir `
        --win-menu --win-shortcut
    if ($LASTEXITCODE -ne 0) { throw 'MSI packaging failed; check WiX installation and JDK version.' }
    Write-Host "MSI output: $installerDir" -ForegroundColor Green
}
