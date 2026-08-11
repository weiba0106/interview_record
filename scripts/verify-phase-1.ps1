[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$apiDirectory = Join-Path $repoRoot 'apps/api'
$webDirectory = Join-Path $repoRoot 'apps/web'
$mailboxPath = Join-Path $apiDirectory 'target/e2e-mailbox/messages.jsonl'
$apiProcess = $null
$webProcess = $null

function Require-ExactTestDatabase {
    $required = 'TEST_DB_URL', 'TEST_DB_USERNAME', 'TEST_DB_PASSWORD'
    $missing = @($required | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_)) })
    if ($missing.Count -gt 0) {
        throw 'Phase 1 verification requires TEST_DB_URL, TEST_DB_USERNAME, and TEST_DB_PASSWORD for the existing dedicated interview_record_test database.'
    }
    $jdbcUrl = $env:TEST_DB_URL
    if (-not $jdbcUrl.StartsWith('jdbc:mysql://', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'TEST_DB_URL must be a MySQL JDBC URL.'
    }
    $connectionUri = [uri]$jdbcUrl.Substring('jdbc:'.Length)
    if ($connectionUri.AbsolutePath.Trim('/') -cne 'interview_record_test') {
        throw 'TEST_DB_URL must select exactly the interview_record_test schema.'
    }
    return $connectionUri
}

function Confirm-MySqlConnection([uri]$connectionUri) {
    $mysql = Get-Command mysql -ErrorAction Stop
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $mysql.Source
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($argument in @('--protocol=tcp', "--host=$($connectionUri.Host)", "--port=$($connectionUri.Port)", "--user=$env:TEST_DB_USERNAME", '--database=interview_record_test', '--batch', '--skip-column-names', '--execute=SELECT DATABASE()')) {
        [void]$startInfo.ArgumentList.Add($argument)
    }
    $startInfo.Environment['MYSQL_PWD'] = $env:TEST_DB_PASSWORD
    $process = [System.Diagnostics.Process]::Start($startInfo)
    $output = $process.StandardOutput.ReadToEnd().Trim()
    $errors = $process.StandardError.ReadToEnd().Trim()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0 -or $output -cne 'interview_record_test') {
        throw "Cannot connect to the dedicated interview_record_test database. $errors"
    }
}

function Start-Child([string]$fileName, [string]$arguments, [string]$workingDirectory, [hashtable]$environment) {
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $fileName
    $startInfo.Arguments = $arguments
    $startInfo.WorkingDirectory = $workingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    foreach ($name in 'TEST_DB_URL', 'TEST_DB_USERNAME', 'TEST_DB_PASSWORD') { [void]$startInfo.Environment.Remove($name) }
    foreach ($entry in $environment.GetEnumerator()) { $startInfo.Environment[$entry.Key] = $entry.Value }
    return [System.Diagnostics.Process]::Start($startInfo)
}

function Wait-Until([string]$description, [scriptblock]$check) {
    $deadline = (Get-Date).AddSeconds(45)
    do {
        try { if (& $check) { return } } catch { }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for $description."
}

function Stop-StartedProcess($process) {
    if ($null -ne $process -and -not $process.HasExited) { $process.Kill($true); $process.WaitForExit() }
}

try {
    $testDatabase = Require-ExactTestDatabase
    Confirm-MySqlConnection $testDatabase

    Push-Location $apiDirectory
    & .\mvnw.cmd verify
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
    Pop-Location

    Push-Location $webDirectory
    npm.cmd run test:unit -- --run
    if ($LASTEXITCODE -ne 0) { throw 'Frontend unit tests failed.' }
    npm.cmd run type-check
    if ($LASTEXITCODE -ne 0) { throw 'Frontend type checking failed.' }
    npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend production build failed.' }
    Pop-Location

    $mailboxDirectory = Split-Path -Parent $mailboxPath
    New-Item -ItemType Directory -Force -Path $mailboxDirectory | Out-Null
    Remove-Item -LiteralPath $mailboxPath -Force -ErrorAction SilentlyContinue
    $env:E2E_MAILBOX_PATH = [IO.Path]::GetFullPath($mailboxPath)

    $apiEnvironment = @{
        DB_URL = $env:TEST_DB_URL
        DB_USERNAME = $env:TEST_DB_USERNAME
        DB_PASSWORD = $env:TEST_DB_PASSWORD
        E2E_MAILBOX_PATH = $env:E2E_MAILBOX_PATH
    }
    $apiProcess = Start-Child (Join-Path $apiDirectory 'mvnw.cmd') '-Dspring-boot.run.profiles=e2e spring-boot:run' $apiDirectory $apiEnvironment
    $webProcess = Start-Child 'npm.cmd' 'run dev -- --host 127.0.0.1' $webDirectory @{}

    Wait-Until 'API health endpoint' { (Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/actuator/health').StatusCode -eq 200 }
    Wait-Until 'Vite development server' {
        $client = [Net.Sockets.TcpClient]::new()
        try { $client.Connect('127.0.0.1', 5173); $true } finally { $client.Dispose() }
    }

    Push-Location $webDirectory
    npm.cmd run test:e2e -- account-lifecycle.spec.ts
    if ($LASTEXITCODE -ne 0) { throw 'Playwright account lifecycle failed.' }
    Pop-Location
} finally {
    Stop-StartedProcess $webProcess
    Stop-StartedProcess $apiProcess
    Remove-Item -LiteralPath $mailboxPath -Force -ErrorAction SilentlyContinue
    if ((Get-Location).Path -ne $repoRoot) { Set-Location $repoRoot }
}
