param(
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$SkipBuild,
    [int]$StartupTimeoutSeconds = 180,
    [int]$RouteTimeoutSec = 10,
    [ValidateSet("course-service", "selection-service", "user-service", "student-service", "teacher-service", "web-service")]
    [string[]]$Services = @(
        "course-service",
        "selection-service",
        "user-service",
        "student-service",
        "teacher-service",
        "web-service"
    )
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$startScript = Join-Path $PSScriptRoot "start-microservices.ps1"
$stopScript = Join-Path $PSScriptRoot "stop-microservices.ps1"
$smokeScript = Join-Path $PSScriptRoot "smoke-test-gateway.ps1"
$startedStack = $false

function Start-Stack {
    param([bool]$SkipBuildForRun)

    $arguments = @{
        JavaHome = $JavaHome
        TimeoutSeconds = $StartupTimeoutSeconds
    }
    if ($SkipBuildForRun) {
        $arguments.SkipBuild = $true
    }

    & $startScript @arguments
}

function Start-Service {
    param([string]$ServiceName)

    & $startScript `
        -JavaHome $JavaHome `
        -SkipBuild `
        -TimeoutSeconds $StartupTimeoutSeconds `
        -Service $ServiceName
}

Push-Location $repoRoot
try {
    Start-Stack -SkipBuildForRun ([bool]$SkipBuild)
    $startedStack = $true

    foreach ($serviceName in $Services) {
        Write-Host ""
        Write-Host "Verifying Gateway fallback for $serviceName..."

        & $stopScript -Service $serviceName

        & $smokeScript -ExpectFallbackFor $serviceName -TimeoutSec $RouteTimeoutSec

        Start-Service -ServiceName $serviceName
        & $smokeScript -WaitForService $serviceName -TimeoutSec $RouteTimeoutSec
    }

    Write-Host ""
    Write-Host "Gateway fallback matrix verification completed."
} finally {
    if ($startedStack) {
        & $stopScript -All
    }
    Pop-Location
}
