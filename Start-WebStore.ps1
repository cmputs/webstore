$ErrorActionPreference = 'Stop'

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java 21 or later must be available on PATH.'
}
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = 'root' }
if (-not $env:DB_PASSWORD) {
    $databasePassword = Read-Host 'Enter your local MySQL password (input is hidden)' -AsSecureString
    $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $databasePassword).Password
}

Push-Location -LiteralPath $PSScriptRoot
try {
    Write-Host 'Starting WebStore. Open http://localhost:8080/ after the Started message.'
    Write-Host 'Press Ctrl+C to stop the server.'
    if (Test-Path -LiteralPath '.\webstore.jar') {
        & java -jar '.\webstore.jar'
    } else {
        & '.\mvnw.cmd' spring-boot:run
    }
} finally {
    Pop-Location
}
