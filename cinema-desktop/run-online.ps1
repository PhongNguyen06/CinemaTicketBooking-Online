param([Parameter(Mandatory=$true)][string]$ApiUrl)
if (-not $ApiUrl.StartsWith('https://')) { throw 'Online deployment requires an HTTPS URL.' }
$env:CINEMA_API_URL = $ApiUrl.TrimEnd('/') + '/'
Remove-Item Env:CINEMA_STORAGE -ErrorAction SilentlyContinue
mvn clean javafx:run
