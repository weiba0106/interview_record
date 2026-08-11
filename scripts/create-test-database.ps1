[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)] [string]$AdminUser,
    [Parameter(Mandatory)] [securestring]$AdminPassword,
    [string]$HostName = 'localhost',
    [ValidateRange(1, 65535)] [int]$Port = 3306
)

$ErrorActionPreference = 'Stop'
$databaseName = 'interview_record_test'

if (-not $PSCmdlet.ShouldProcess("$HostName`:$Port/$databaseName", 'create the dedicated Phase 1 test schema if absent')) {
    return
}
$mysql = Get-Command mysql -ErrorAction Stop

$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($AdminPassword)
try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $mysql.Source
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    foreach ($argument in @('--protocol=tcp', "--host=$HostName", "--port=$Port", "--user=$AdminUser", "--execute=CREATE DATABASE IF NOT EXISTS $databaseName CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")) {
        [void]$startInfo.ArgumentList.Add($argument)
    }
    $startInfo.Environment['MYSQL_PWD'] = $plainPassword
    $process = [System.Diagnostics.Process]::Start($startInfo)
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "MySQL refused to create the dedicated $databaseName schema." }
} finally {
    if ($passwordPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer) }
    Remove-Variable plainPassword -ErrorAction SilentlyContinue
}

Write-Host "Verified or created only the dedicated $databaseName schema. This script never drops, truncates, or grants access to any schema."
