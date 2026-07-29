# Register the SSH reverse tunnel to auto-start at user logon (no admin required).
# Creates a hidden-launch VBS in the user's Startup folder pointing at start_tunnel.ps1.
$ErrorActionPreference = 'Stop'
$startup = [Environment]::GetFolderPath('Startup')
$script  = Join-Path $PSScriptRoot 'start_tunnel.ps1'
$vbsPath = Join-Path $startup 'rehealth_tunnel.vbs'
$vbs = 'CreateObject("Wscript.Shell").Run "powershell -NoProfile -ExecutionPolicy Bypass -File ""' + $script + '""", 0, False'
[System.IO.File]::WriteAllText($vbsPath, $vbs, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ('STARTUP_INSTALLED: ' + $vbsPath)
