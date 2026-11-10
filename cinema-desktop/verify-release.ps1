# Read-only local release validation; no MySQL import or schema changes.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven not found.' }

Write-Host '1/3 Building source...' -ForegroundColor Cyan
mvn -B clean package
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }

Write-Host '2/3 Validating FXML resources...' -ForegroundColor Cyan
$fxml = Get-ChildItem -Path 'src\main\resources' -Filter '*.fxml' -Recurse
if ($fxml.Count -eq 0) { throw 'No FXML screens found.' }
foreach ($file in $fxml) {
    $xml = New-Object System.Xml.XmlDocument
    try { $xml.Load($file.FullName) }
    catch { throw "Invalid FXML XML: $($file.FullName) - $_" }
}
Write-Host "Validated $($fxml.Count) FXML files."

Write-Host '3/3 Checking DB connectivity (only when CINEMA_STORAGE=mysql)...' -ForegroundColor Cyan
if ($env:CINEMA_STORAGE -eq 'mysql') {
    mvn -B compile exec:java '-Dexec.mainClass=com.cinema.DatabaseHealthCheck'
    if ($LASTEXITCODE -ne 0) { throw 'Database health check failed.' }
} else {
    Write-Host 'Skipped DB check (CINEMA_STORAGE is not mysql).'
}
Write-Host 'Release checks completed. Manual JavaFX/Windows acceptance tests still required.' -ForegroundColor Green
