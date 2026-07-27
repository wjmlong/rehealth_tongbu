[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$runtimeDir = Join-Path $PSScriptRoot '.local-runtime'

foreach ($name in @('device-service', 'jeecg', 'pias', 'model-service')) {
    $pidFile = Join-Path $runtimeDir "$name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) {
        continue
    }
    $managedPid = [int](Get-Content -LiteralPath $pidFile -Raw)
    $process = Get-Process -Id $managedPid -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $managedPid
        $process.WaitForExit(10000)
        Write-Output "$name stopped."
    }
    Remove-Item -LiteralPath $pidFile -Force
}
