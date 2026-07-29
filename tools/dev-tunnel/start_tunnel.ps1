# ReHealth dev tunnel: expose local jeecg-boot (127.0.0.1:8080) to the ECS jump host
# via an SSH reverse tunnel over port 22 (no extra security-group rules needed).
#
# Chain: phone -> http://rehealth.<ECS_IP>.sslip.io (ECS nginx :80)
#        -> ECS 127.0.0.1:18080 (sshd remote forward)
#        -> local 127.0.0.1:8080 (jeecg-boot)
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File start_tunnel.ps1 [-KeyPath <pem>] [-Dest <user@host>]
param(
    [string]$KeyPath = 'E:/aawjmlong/rehealth.pem',
    [string]$Dest = 'root@47.80.30.228',
    [int]$RemotePort = 18080,
    [int]$LocalPort = 8080
)
$ErrorActionPreference = 'Stop'

if (-not (Test-Path $KeyPath)) { Write-Output ("KEY_NOT_FOUND: " + $KeyPath); exit 1 }

# Kill any previous tunnel ssh (identified by the remote-forward port argument)
Get-CimInstance Win32_Process -Filter "Name='ssh.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match [string]$RemotePort } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        Write-Output ("killed old tunnel pid=" + $_.ProcessId)
    }

$sshArgs = @(
    '-i', $KeyPath,
    '-o', 'StrictHostKeyChecking=no',
    '-o', 'ServerAliveInterval=30',
    '-o', 'ServerAliveCountMax=3',
    '-o', 'ExitOnForwardFailure=yes',
    '-N',
    '-R', ('127.0.0.1:{0}:127.0.0.1:{1}' -f $RemotePort, $LocalPort),
    $Dest
)
$p = Start-Process -FilePath 'ssh' -ArgumentList $sshArgs -WindowStyle Hidden -PassThru
Start-Sleep -Seconds 4
$alive = -not $p.HasExited
Write-Output ("TUNNEL_PID=" + $p.Id + " ALIVE=" + $alive)
if (-not $alive) { Write-Output 'TUNNEL_FAILED'; exit 1 }
