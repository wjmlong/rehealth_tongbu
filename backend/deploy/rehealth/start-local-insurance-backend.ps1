[CmdletBinding()]
param(
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$runtimeDir = Join-Path $PSScriptRoot '.local-runtime'
$secretsDir = Join-Path $PSScriptRoot 'secrets'
$java = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    (Get-Command java -ErrorAction Stop).Source
}
$jar = Join-Path $backendRoot 'jeecg-boot\jeecg-server-cloud\jeecg-system-cloud-start\target\jeecg-system-cloud-start-3.9.2.jar'
$config = (Join-Path $backendRoot 'jeecg-boot\jeecg-module-system\jeecg-system-start\src\main\resources\application-dev.yml').Replace('\', '/')
$pidFile = Join-Path $runtimeDir 'jeecg-insurance.pid'

foreach ($path in @(
    $java,
    $jar,
    $config,
    (Join-Path $secretsDir 'software_db_password'),
    (Join-Path $secretsDir 'redis_password')
)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required local file is missing: $path"
    }
}

if (Test-Path -LiteralPath $pidFile) {
    $existingPid = [int](Get-Content -Raw -LiteralPath $pidFile)
    if (Get-Process -Id $existingPid -ErrorAction SilentlyContinue) {
        throw "The local insurance backend is already running (PID $existingPid)."
    }
}

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
$softwarePassword = (Get-Content -Raw -LiteralPath (Join-Path $secretsDir 'software_db_password')).Trim()
$redisPassword = (Get-Content -Raw -LiteralPath (Join-Path $secretsDir 'redis_password')).Trim()
$databaseUrl = 'jdbc:mysql://127.0.0.1:3306/rehealth_software?characterEncoding=UTF-8&useUnicode=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'

$env:SPRING_PROFILES_ACTIVE = 'development'
$env:SPRING_CLOUD_NACOS_CONFIG_ENABLED = 'false'
$env:SPRING_CLOUD_NACOS_DISCOVERY_ENABLED = 'false'
$env:SPRING_CLOUD_NACOS_CONFIG_IMPORT_CHECK_ENABLED = 'false'
$env:SPRING_DATASOURCE_DYNAMIC_PRIMARY = 'master'
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL = $databaseUrl
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME = 'rehealth_software'
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD = $softwarePassword
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_HARDWARE_URL = $databaseUrl
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_HARDWARE_USERNAME = 'rehealth_software'
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_HARDWARE_PASSWORD = $softwarePassword
$env:SPRING_DATA_REDIS_HOST = '127.0.0.1'
$env:SPRING_DATA_REDIS_PORT = '6379'
$env:SPRING_DATA_REDIS_PASSWORD = $redisPassword
$env:JEECG_REDISSON_ADDRESS = '127.0.0.1:6379'
$env:JEECG_REDISSON_PASSWORD = $redisPassword
$env:REHEALTH_RUNTIME_MODE = 'development'
$env:REHEALTH_SOFTWARE_DB_ENABLED = 'true'
$env:REHEALTH_HARDWARE_DB_ENABLED = 'true'
$env:REHEALTH_DEVICE_SERVICE_ENABLED = 'false'
$env:REHEALTH_TIMESCALE_ENABLED = 'false'
$env:REHEALTH_KAFKA_CONSUMER_ENABLED = 'false'
$env:REHEALTH_INSURANCE_TENANT_MEMBERSHIP_DEV_SCOPE_ENABLED = 'true'

$arguments = @(
    '-Xms512m',
    '-Xmx1536m',
    '-jar',
    $jar,
    "--server.address=127.0.0.1",
    "--server.port=$Port",
    '--rehealth.runtime.mode=development',
    '--rehealth.insurance.tenant-membership-dev-scope-enabled=true',
    "--spring.config.additional-location=file:///$config"
)
$process = Start-Process `
    -FilePath $java `
    -ArgumentList $arguments `
    -WorkingDirectory (Split-Path $jar) `
    -RedirectStandardOutput (Join-Path $runtimeDir 'jeecg-insurance.out.log') `
    -RedirectStandardError (Join-Path $runtimeDir 'jeecg-insurance.err.log') `
    -WindowStyle Hidden `
    -PassThru

Set-Content -LiteralPath $pidFile -Value $process.Id -Encoding Ascii
Write-Output "Local insurance backend started on 127.0.0.1:$Port (PID $($process.Id))."
Write-Output "Logs: $runtimeDir"
