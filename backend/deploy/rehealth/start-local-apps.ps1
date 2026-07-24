[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$runtimeDir = Join-Path $PSScriptRoot '.local-runtime'
$secretsDir = Join-Path $PSScriptRoot 'secrets'
$python = Join-Path $repoRoot 'model-service\.venv\Scripts\python.exe'
$java = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    'D:\Android_Studio\jbr\bin\java.exe'
}

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null

function Require-File {
    param([Parameter(Mandatory)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Required local file is missing: $Path"
    }
}

function Read-Secret {
    param([Parameter(Mandatory)][string]$Name)
    $path = Join-Path $secretsDir $Name
    Require-File $path
    return (Get-Content -LiteralPath $path -Raw).Trim()
}

function Test-ManagedProcess {
    param([Parameter(Mandatory)][string]$Name)
    $pidFile = Join-Path $runtimeDir "$Name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) {
        return $false
    }
    $managedPid = [int](Get-Content -LiteralPath $pidFile -Raw)
    return $null -ne (Get-Process -Id $managedPid -ErrorAction SilentlyContinue)
}

function Start-ManagedProcess {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$FilePath,
        [Parameter(Mandatory)][string[]]$ArgumentList,
        [Parameter(Mandatory)][string]$WorkingDirectory
    )
    if (Test-ManagedProcess $Name) {
        Write-Output "$Name is already running."
        return
    }
    $process = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput (Join-Path $runtimeDir "$Name.out.log") `
        -RedirectStandardError (Join-Path $runtimeDir "$Name.err.log") `
        -WindowStyle Hidden `
        -PassThru
    Set-Content -LiteralPath (Join-Path $runtimeDir "$Name.pid") -Value $process.Id -Encoding Ascii
    Write-Output "$Name started (PID $($process.Id))."
}

Require-File $python
Require-File $java

$internalCredentialFile = Join-Path $secretsDir 'internal_service_credential'
Require-File $internalCredentialFile

$env:REHEALTH_RUNTIME_MODE = 'development'
$env:REHEALTH_MODEL_DIR = Join-Path $repoRoot 'model-service\models'
$env:REHEALTH_AGENT_PROVIDER_ENABLED = 'false'
$env:REHEALTH_AGENT_INTERNAL_TOKEN_FILE = $internalCredentialFile
Start-ManagedProcess `
    -Name 'model-service' `
    -FilePath $python `
    -ArgumentList @('-m', 'uvicorn', 'app.main:app', '--host', '127.0.0.1', '--port', '8000', '--no-access-log') `
    -WorkingDirectory (Join-Path $repoRoot 'model-service')

$env:PYTHONPATH = Join-Path $repoRoot 'rehealth-algorithms'
$env:REHEALTH_INTERNAL_SERVICE_CREDENTIAL_FILE = $internalCredentialFile
Start-ManagedProcess `
    -Name 'pias' `
    -FilePath $python `
    -ArgumentList @('-m', 'uvicorn', 'api.production_main:app', '--host', '127.0.0.1', '--port', '8010', '--no-access-log') `
    -WorkingDirectory (Join-Path $repoRoot 'rehealth-algorithms')

$softwarePassword = Read-Secret 'software_db_password'
$redisPassword = Read-Secret 'redis_password'
$env:SPRING_PROFILES_ACTIVE = 'development'
$env:SPRING_CLOUD_NACOS_CONFIG_ENABLED = 'false'
$env:SPRING_CLOUD_NACOS_DISCOVERY_ENABLED = 'false'
$env:SPRING_CLOUD_NACOS_CONFIG_IMPORT_CHECK_ENABLED = 'false'
$env:SPRING_DATASOURCE_DYNAMIC_PRIMARY = 'master'
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL = 'jdbc:mysql://127.0.0.1:3306/rehealth_software?characterEncoding=UTF-8&useUnicode=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME = 'rehealth_software'
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD = $softwarePassword
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_HARDWARE_URL = $env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_HARDWARE_USERNAME = 'rehealth_software'
$env:SPRING_DATASOURCE_DYNAMIC_DATASOURCE_HARDWARE_PASSWORD = $softwarePassword
$env:SPRING_DATA_REDIS_HOST = '127.0.0.1'
$env:SPRING_DATA_REDIS_PORT = '6379'
$env:SPRING_DATA_REDIS_PASSWORD = $redisPassword
$env:JEECG_REDISSON_ADDRESS = '127.0.0.1:6379'
$env:JEECG_REDISSON_PASSWORD = $redisPassword
$env:REHEALTH_SOFTWARE_DB_ENABLED = 'true'
$env:REHEALTH_DEVICE_SERVICE_ENABLED = 'true'
$env:REHEALTH_DEVICE_SERVICE_BASE_URL = 'http://127.0.0.1:8091'
$env:REHEALTH_TIMESCALE_ENABLED = 'true'
$env:REHEALTH_MODEL_SERVICE_BASE_URL = 'http://127.0.0.1:8000'
$env:REHEALTH_REQUIRE_REAL_MODEL = 'true'
$env:REHEALTH_ATTRIBUTION_SERVICE_BASE_URL = 'http://127.0.0.1:8010'
$env:REHEALTH_ATTRIBUTION_MODE = 'pias'
$env:REHEALTH_ATTRIBUTION_PROVENANCE = 'pias'
$env:REHEALTH_KAFKA_CONSUMER_ENABLED = 'true'
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = '127.0.0.1:29092'

$jeecgJar = Join-Path $repoRoot 'backend\jeecg-boot\jeecg-server-cloud\jeecg-system-cloud-start\target\jeecg-system-cloud-start-3.9.2.jar'
$jeecgConfig = (Join-Path $repoRoot 'backend\jeecg-boot\jeecg-module-system\jeecg-system-start\src\main\resources\application-dev.yml').Replace('\', '/')
Require-File $jeecgJar
Start-ManagedProcess `
    -Name 'jeecg' `
    -FilePath $java `
    -ArgumentList @(
        '-Xms512m',
        '-Xmx1536m',
        '-jar',
        $jeecgJar,
        '--server.address=127.0.0.1',
        "--spring.config.additional-location=file:///$jeecgConfig"
    ) `
    -WorkingDirectory (Split-Path $jeecgJar)

$env:REHEALTH_HARDWARE_DB_ENABLED = 'true'
$env:REHEALTH_HARDWARE_DB_URL = 'jdbc:postgresql://127.0.0.1:5432/rehealth_hardware'
$env:REHEALTH_HARDWARE_DB_USERNAME = 'rehealth_hardware'
$env:REHEALTH_HARDWARE_DB_PASSWORD_FILE = Join-Path $secretsDir 'hardware_db_password'
$env:REHEALTH_IDENTITY_ENABLED = 'true'
$env:REHEALTH_IDENTITY_BASE_URL = 'http://127.0.0.1:8080/jeecg-boot/rehealth/internal/v1/identity'
$env:REHEALTH_IDENTITY_READINESS_URL = 'http://127.0.0.1:8080/jeecg-boot/rehealth/mobile/health'
$env:REHEALTH_KAFKA_BOOTSTRAP_SERVERS = '127.0.0.1:29092'
$env:REHEALTH_KAFKA_PUBLISHER_ENABLED = 'true'
$env:REHEALTH_KAFKA_SECURITY_PROTOCOL = 'PLAINTEXT'
$env:MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS = 'always'
$deviceJar = Join-Path $repoRoot 'backend\device-service\target\device-service.jar'
Require-File $deviceJar
Start-ManagedProcess `
    -Name 'device-service' `
    -FilePath $java `
    -ArgumentList @('-Xms256m', '-Xmx768m', '-jar', $deviceJar) `
    -WorkingDirectory (Split-Path $deviceJar)

Write-Output 'Local application processes started. Logs and PID files are under .local-runtime.'
