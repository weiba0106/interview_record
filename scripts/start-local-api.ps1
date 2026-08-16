[CmdletBinding()]
param(
    [string]$DbUrl = 'jdbc:mysql://127.0.0.1:3306/interview_record_local?serverTimezone=UTC',
    [string]$DbUsername = 'interview_local',
    [string]$DbPassword,
    [string]$MailHost = 'smtp.qq.com',
    [int]$MailPort = 465,
    [string]$MailUsername,
    [string]$MailPassword,
    [string]$AppBaseUrl = 'http://localhost:5173'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$apiDirectory = Join-Path $repoRoot 'apps\api'
$mavenWrapper = Join-Path $apiDirectory 'mvnw.cmd'

if (-not (Test-Path -LiteralPath $mavenWrapper)) {
    throw "Maven Wrapper not found: $mavenWrapper"
}

function Read-SecretValue([string]$Prompt, [string]$Value) {
    if ($Value) { return $Value }
    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

if (-not $DbPassword) {
    $DbPassword = Read-SecretValue 'MySQL password'
}

$useSmtp = -not (($MailHost -eq 'localhost' -or $MailHost -eq '127.0.0.1') -and $MailPort -eq 1025)
if ($useSmtp) {
    if (-not $MailUsername) {
        $MailUsername = Read-Host 'SMTP username (QQ email address)'
    }
    if (-not $MailPassword) {
        $MailPassword = Read-SecretValue 'SMTP password or email authorization code'
    }
}

$environmentNames = @(
    'SPRING_PROFILES_ACTIVE', 'DB_URL', 'DB_USERNAME', 'DB_PASSWORD',
    'MAIL_HOST', 'MAIL_PORT', 'SPRING_MAIL_USERNAME', 'SPRING_MAIL_PASSWORD',
    'SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH', 'SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE',
    'APP_BASE_URL'
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    $env:SPRING_PROFILES_ACTIVE = 'local'
    $env:DB_URL = $DbUrl
    $env:DB_USERNAME = $DbUsername
    $env:DB_PASSWORD = $DbPassword
    $env:MAIL_HOST = $MailHost
    $env:MAIL_PORT = [string]$MailPort
    $env:APP_BASE_URL = $AppBaseUrl

    if ($useSmtp) {
        $env:SPRING_MAIL_USERNAME = $MailUsername
        $env:SPRING_MAIL_PASSWORD = $MailPassword
        $env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = 'true'
        if ($MailPort -eq 465) {
            $env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE = "true"
        } else {
            $env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE = "false"
        }
    } else {
        Remove-Item Env:SPRING_MAIL_USERNAME -ErrorAction SilentlyContinue
        Remove-Item Env:SPRING_MAIL_PASSWORD -ErrorAction SilentlyContinue
        Remove-Item Env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH -ErrorAction SilentlyContinue
        Remove-Item Env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE -ErrorAction SilentlyContinue
    }

    Write-Host "Starting Interview Record API" -ForegroundColor Cyan
    Write-Host "Database: $DbUrl" -ForegroundColor DarkGray
    Write-Host ("Mail: {0}:{1}" -f $MailHost, $MailPort) -ForegroundColor DarkGray
    Write-Host "API: http://localhost:8080" -ForegroundColor Green
    Write-Host "Press Ctrl+C to stop" -ForegroundColor DarkGray

    Push-Location $apiDirectory
    try {
        & $mavenWrapper spring-boot:run
        if ($LASTEXITCODE -ne 0) {
            throw "API startup failed with exit code: $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
} finally {
    foreach ($name in $environmentNames) {
        $previous = $previousEnvironment[$name]
        if ($null -eq $previous) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        } else {
            Set-Item "Env:$name" $previous
        }
    }
}
