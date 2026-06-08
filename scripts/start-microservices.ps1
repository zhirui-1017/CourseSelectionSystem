param(
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$SkipBuild,
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runtimeDir = Join-Path $repoRoot ".runtime"
$pidFile = Join-Path $runtimeDir "microservice-pids.json"

function Resolve-JavaHome {
    param([string]$Candidate)

    if ([string]::IsNullOrWhiteSpace($Candidate)) {
        throw "JAVA_HOME is not set. Pass -JavaHome or set JAVA_HOME to the JDK root directory."
    }

    $resolved = (Resolve-Path -LiteralPath $Candidate).Path
    if ((Split-Path -Leaf $resolved) -ieq "bin") {
        $resolved = Split-Path -Parent $resolved
    }

    $javaExe = Join-Path $resolved "bin\java.exe"
    if (-not (Test-Path -LiteralPath $javaExe)) {
        throw "JAVA_HOME does not contain bin\java.exe: $resolved"
    }

    return $resolved
}

function Wait-HttpReady {
    param(
        [string]$Name,
        [string]$Url,
        [int]$Timeout
    )

    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                Write-Host "$Name is ready: $Url"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    throw "Timed out waiting for $Name at $Url"
}

$jdkRoot = Resolve-JavaHome $JavaHome
$env:JAVA_HOME = $jdkRoot

New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null

if (-not $SkipBuild) {
    Push-Location $repoRoot
    try {
        & .\mvnw.cmd -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

$services = @(
    @{ Name = "eureka-server"; Module = "eureka-server"; Port = 8761; HealthPath = "/actuator/health" },
    @{ Name = "web-service"; Module = "web-service"; Port = 8080; HealthPath = "/actuator/health" },
    @{ Name = "user-service"; Module = "user-service"; Port = 8101; HealthPath = "/actuator/health" },
    @{ Name = "student-service"; Module = "student-service"; Port = 8102; HealthPath = "/actuator/health" },
    @{ Name = "teacher-service"; Module = "teacher-service"; Port = 8103; HealthPath = "/actuator/health" },
    @{ Name = "course-service"; Module = "course-service"; Port = 8104; HealthPath = "/actuator/health" },
    @{ Name = "selection-service"; Module = "selection-service"; Port = 8105; HealthPath = "/actuator/health" },
    @{ Name = "gateway-server"; Module = "gateway-server"; Port = 9000; HealthPath = "/actuator/health" }
)

$started = @()

try {
    foreach ($service in $services) {
        $outLogFile = Join-Path $runtimeDir "$($service.Name).out.log"
        $errLogFile = Join-Path $runtimeDir "$($service.Name).err.log"
        $jarPath = Join-Path $repoRoot "$($service.Module)\target\$($service.Module)-0.0.1-SNAPSHOT.jar"
        if (-not (Test-Path -LiteralPath $jarPath)) {
            throw "Executable jar not found for $($service.Name): $jarPath"
        }

        Write-Host "Starting $($service.Name) on port $($service.Port)..."
        $process = Start-Process -FilePath (Join-Path $jdkRoot "bin\java.exe") `
            -ArgumentList @("-jar", $jarPath) `
            -WorkingDirectory $repoRoot `
            -RedirectStandardOutput $outLogFile `
            -RedirectStandardError $errLogFile `
            -WindowStyle Hidden `
            -PassThru

        $started += [pscustomobject]@{
            Name = $service.Name
            Module = $service.Module
            Port = $service.Port
            Pid = $process.Id
            StdoutLog = $outLogFile
            StderrLog = $errLogFile
        }

        $healthUrl = "http://localhost:$($service.Port)$($service.HealthPath)"
        Wait-HttpReady -Name $service.Name -Url $healthUrl -Timeout $TimeoutSeconds
    }
} catch {
    Write-Host "Startup failed. Stopping services that were already started..."
    foreach ($entry in $started) {
        Stop-Process -Id $entry.Pid -Force -ErrorAction SilentlyContinue
    }
    throw
}

$started | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $pidFile -Encoding UTF8

Write-Host ""
Write-Host "Microservice stack started."
Write-Host "PID file: $pidFile"
Write-Host "Run scripts\smoke-test-gateway.ps1 to verify Gateway routes."
