# Starts the EventSphere backend on your local MySQL, optionally with real e-mail (Gmail) for password reset.
# Passwords are typed hidden and only live in this PowerShell session; nothing is written to disk.
#
# Usage (from the project folder):   powershell -ExecutionPolicy Bypass -File .\run-backend.ps1

param(
    [int]$Port = 8081,
    [string]$DbUser = "root"
)

function Read-Secret([string]$prompt) {
    $secure = Read-Host -Prompt $prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

$env:DB_USERNAME = $DbUser
$env:DB_PASSWORD = Read-Secret "MySQL password for user '$DbUser'"
$env:PORT = "$Port"

$gmail = Read-Host "Gmail address to send reset codes from (press Enter to skip e-mail and use demo mode)"
if ($gmail) {
    $env:MAIL_HOST = "smtp.gmail.com"
    $env:MAIL_PORT = "587"
    $env:MAIL_USERNAME = $gmail
    $env:MAIL_PASSWORD = (Read-Secret "Gmail app password (16 characters, spaces are fine)") -replace '\s', ''
    $env:MAIL_FROM = "EventSphere <$gmail>"
    $env:PASSWORD_RESET_DEMO_MODE = "false"
    Write-Host "E-mail enabled: reset codes will be sent from $gmail" -ForegroundColor Green
} else {
    Write-Host "E-mail not configured: reset codes will be shown on screen (demo mode)" -ForegroundColor Yellow
}

Write-Host "Starting backend on http://localhost:$Port ..." -ForegroundColor Cyan
Set-Location (Join-Path $PSScriptRoot "backend")
mvn spring-boot:run
