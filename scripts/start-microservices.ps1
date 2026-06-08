param(
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$SkipBuild,
    [int]$TimeoutSeconds = 180,
    [string[]]$Service
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
            $body = Convert-ResponseBodyToString $response.Content
            if ($response.StatusCode -eq 200 -and $body.Contains('"status":"UP"')) {
                Write-Host "$Name is ready: $Url"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    throw "Timed out waiting for $Name at $Url"
}

function Convert-ResponseBodyToString {
    param([object]$Content)

    if ($null -eq $Content) {
        return ""
    }

    if ($Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Content)
    }

    return [string]$Content
}

function Wait-EurekaRegistration {
    param(
        [string]$Name,
        [int]$Timeout
    )

    $serviceId = $Name.ToUpperInvariant()
    $url = "http://localhost:8761/eureka/apps/$serviceId"
    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $url -UseBasicParsing -Headers @{ Accept = "application/json" } -TimeoutSec 5
            $body = Convert-ResponseBodyToString $response.Content
            if ($response.StatusCode -eq 200 -and $body.Contains('"status":"UP"')) {
                Write-Host "$Name is registered in Eureka as UP."
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }

        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for $Name to register in Eureka as UP."
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
    [pscustomobject]@{ Name = "eureka-server"; Module = "eureka-server"; Port = 8761; HealthPath = "/actuator/health" },
    [pscustomobject]@{ Name = "web-service"; Module = "web-service"; Port = 8080; HealthPath = "/actuator/health" },
    [pscustomobject]@{ Name = "user-service"; Module = "user-service"; Port = 8101; HealthPath = "/actuator/health" },
    [pscustomobject]@{ Name = "student-service"; Module = "student-service"; Port = 8102; HealthPath = "/actuator/health" },
    [pscustomobject]@{ Name = "teacher-service"; Module = "teacher-service"; Port = 8103; HealthPath = "/actuator/health" },
    [pscustomobject]@{ Name = "course-service"; Module = "course-service"; Port = 8104; HealthPath = "/actuator/health" },
    [pscustomobject]@{ Name = "selection-service"; Module = "selection-service"; Port = 8105; HealthPath = "/actuator/health" },
    [pscustomobject]@{ Name = "gateway-server"; Module = "gateway-server"; Port = 9000; HealthPath = "/actuator/health" }
)

$selectedServices = if ($null -ne $Service -and $Service.Count -gt 0) {
    @($services | Where-Object { $Service -contains $_.Name })
} else {
    @($services)
}

if ($selectedServices.Count -eq 0) {
    throw "No matching services found. Requested: $($Service -join ', ')"
}

$requestedNames = @($selectedServices | ForEach-Object { $_.Name })
$existingEntries = @()
if (Test-Path -LiteralPath $pidFile) {
    $existing = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
    if ($null -ne $existing) {
        $existingEntries = @($existing)
    }
}

foreach ($entry in $existingEntries) {
    if ($requestedNames -contains $entry.Name) {
        $process = Get-Process -Id $entry.Pid -ErrorAction SilentlyContinue
        if ($null -ne $process) {
            throw "$($entry.Name) is already tracked as running with PID $($entry.Pid). Stop it before starting it again."
        }
    }
}

foreach ($serviceConfig in $selectedServices) {
    $serviceName = $serviceConfig.Name
    $servicePort = $serviceConfig.Port
    $listener = Get-NetTCPConnection -LocalPort $servicePort -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1
    if ($null -ne $listener) {
        throw "Port $servicePort is already in use by PID $($listener.OwningProcess). Stop existing services before starting $serviceName."
    }
}

$started = @()

try {
    foreach ($serviceConfig in $selectedServices) {
        $serviceName = $serviceConfig.Name
        $serviceModule = $serviceConfig.Module
        $servicePort = $serviceConfig.Port
        $serviceHealthPath = $serviceConfig.HealthPath
        $outLogFile = Join-Path $runtimeDir "$serviceName.out.log"
        $errLogFile = Join-Path $runtimeDir "$serviceName.err.log"
        $jarPath = Join-Path $repoRoot "$serviceModule\target\$serviceModule-0.0.1-SNAPSHOT.jar"
        if (-not (Test-Path -LiteralPath $jarPath)) {
            throw "Executable jar not found for ${serviceName}: $jarPath"
        }

        Write-Host "Starting $serviceName on port $servicePort..."
        $process = Start-Process -FilePath (Join-Path $jdkRoot "bin\java.exe") `
            -ArgumentList @("-jar", $jarPath) `
            -WorkingDirectory $repoRoot `
            -RedirectStandardOutput $outLogFile `
            -RedirectStandardError $errLogFile `
            -WindowStyle Hidden `
            -PassThru

        $started += [pscustomobject]@{
            Name = $serviceName
            Module = $serviceModule
            Port = $servicePort
            Pid = $process.Id
            StdoutLog = $outLogFile
            StderrLog = $errLogFile
        }

        $healthUrl = "http://localhost:${servicePort}${serviceHealthPath}"
        Wait-HttpReady -Name $serviceName -Url $healthUrl -Timeout $TimeoutSeconds

        if ($serviceName -ne "eureka-server") {
            Wait-EurekaRegistration -Name $serviceName -Timeout $TimeoutSeconds
        }
    }
} catch {
    Write-Host "Startup failed. Stopping services that were already started..."
    foreach ($entry in $started) {
        Stop-Process -Id $entry.Pid -Force -ErrorAction SilentlyContinue
    }
    throw
}

$serviceOrder = @{}
for ($i = 0; $i -lt $services.Count; $i++) {
    $serviceOrder[$services[$i].Name] = $i
}
$trackedEntries = @($existingEntries | Where-Object { $requestedNames -notcontains $_.Name }) + @($started)
$trackedEntries |
        Sort-Object { $serviceOrder[$_.Name] } |
        ConvertTo-Json -Depth 4 |
        Set-Content -LiteralPath $pidFile -Encoding UTF8

Write-Host ""
Write-Host "Microservice service(s) started: $($started.Name -join ', ')."
Write-Host "PID file: $pidFile"
Write-Host "Run scripts\smoke-test-gateway.ps1 to verify Gateway routes."
