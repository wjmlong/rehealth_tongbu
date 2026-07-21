<#
.SYNOPSIS
    Bridge the Android emulator / physical device to the ReHealth backend running in WSL2.
.DESCRIPTION
    WSL2 is a separate network namespace, so the Android emulator (which lives on the
    Windows host loopback 10.0.2.2) and a physical phone on the LAN cannot reach the
    JeecgBoot ( :8080 ) or model-service ( :8000 ) processes running inside WSL2 by default.

    This script:
      1. Detects the WSL2 IP (e.g. 172.28.x.x).
      2. Creates Windows `netsh interface portproxy` rules so that
         localhost:8080 / localhost:8000 on the Windows host forward into WSL2.
      3. Prints the exact `rehealth.api.base.url` value for emulator vs real device.

    Run from an ADMINISTRATOR PowerShell. The emulator then uses 10.0.2.2 (which maps
    to the Windows host loopback); a real device on the same Wi-Fi uses the WSL2 IP
    directly (ensure Windows firewall allows inbound 8080/8000).

.PARAMETER Remove
    Delete the portproxy rules instead of creating them.
#>
param(
    [switch]$Remove
)

$ErrorActionPreference = "Stop"

# --- 1. Detect WSL2 IP ---
function Get-Wsl2Ip {
    $raw = wsl hostname -I 2>$null
    if ($raw) {
        $ip = ($raw.Trim() -split '\s+')[0]
        if ($ip -match '^\d{1,3}(\.\d{1,3}){3}$') { return $ip }
    }
    # Fallback: parse ip addr
    $ipLine = (wsl ip -4 addr show eth0 2>$null) -join "`n"
    if ($ipLine -match 'inet (\d{1,3}(\.\d{1,3}){3})/\d+') { return $Matches[1] }
    return $null
}

$wslIp = Get-Wsl2Ip
if (-not $wslIp) {
    Write-Error "无法获取 WSL2 IP。请确认 WSL2 已启动（wsl -d <distro>）。"
    exit 1
}

$Ports = @(8080, 8000)  # 8080 = JeecgBoot, 8000 = model-service (FastAPI)

# --- 2. Remove mode ---
if ($Remove) {
    foreach ($p in $Ports) {
        netsh interface portproxy delete v4tov4 listenport=$p listenaddress=0.0.0.0 | Out-Null
        Write-Host "已删除端口转发: *:$p -> $wslIp`:$p" -ForegroundColor Gray
    }
    Write-Host "完成。已清理 WSL2 端口转发规则。" -ForegroundColor Green
    exit 0
}

# --- 3. Create rules ---
# Admin check
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Error "请以管理员身份运行 PowerShell（右键 -> 以管理员身份运行）。"
    exit 1
}

foreach ($p in $Ports) {
    netsh interface portproxy delete v4tov4 listenport=$p listenaddress=0.0.0.0 | Out-Null
    $out = netsh interface portproxy add v4tov4 listenport=$p listenaddress=0.0.0.0 connectport=$p connectaddress=$wslIp
    Write-Host "已建立端口转发: 0.0.0.0:$p -> $wslIp`:$p" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "WSL2 IP = $wslIp" -ForegroundColor Yellow
Write-Host "=== 在 Android-apk/local.properties 中设置 ===" -ForegroundColor Green
Write-Host "Android 模拟器（同机 Windows）:"
Write-Host "  rehealth.api.base.url=http://10.0.2.2:8080/jeecg-boot"
Write-Host "  rehealth.model.service.base.url=http://10.0.2.2:8000/api/pias/v2"
Write-Host "真机（同 Wi-Fi，需放行 Windows 防火墙 8080/8000）:"
Write-Host "  rehealth.api.base.url=http://${wslIp}:8080/jeecg-boot"
Write-Host "  rehealth.model.service.base.url=http://${wslIp}:8000/api/pias/v2"
Write-Host ""
Write-Host "注意: 8000 = PIAS 模型服务(rehealth-algorithms/api)，8080 = JeecgBoot。" -ForegroundColor Gray
Write-Host "然后在 Android-apk 目录执行: ./gradlew assembleDebug" -ForegroundColor Green
Write-Host "清理转发规则: .\tools\wsl2-android-connect.ps1 -Remove" -ForegroundColor Gray
