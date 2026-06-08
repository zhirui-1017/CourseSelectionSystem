param(
    [string[]]$Service,
    [switch]$All
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runtimeDir = Join-Path $repoRoot ".runtime"
$pidFile = Join-Path $runtimeDir "microservice-pids.json"

if (-not (Test-Path -LiteralPath $pidFile)) {
    throw "PID file not found: $pidFile"
}

$entries = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
if ($null -eq $entries) {
    throw "No services found in PID file: $pidFile"
}

if (-not $All -and ($null -eq $Service -or $Service.Count -eq 0)) {
    throw "Pass -All to stop the stack or -Service service-name to stop selected services."
}

$selected = if ($All) {
    @($entries)
} else {
    @($entries | Where-Object { $Service -contains $_.Name })
}

if ($selected.Count -eq 0) {
    throw "No matching services found. Requested: $($Service -join ', ')"
}

foreach ($entry in $selected) {
    $process = Get-Process -Id $entry.Pid -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Write-Host "$($entry.Name) is not running with PID $($entry.Pid)."
        continue
    }

    Write-Host "Stopping $($entry.Name) with PID $($entry.Pid)..."
    Stop-Process -Id $entry.Pid -Force
}

$remaining = @($entries | Where-Object { $selected.Name -notcontains $_.Name })
if ($All -or $remaining.Count -eq 0) {
    Remove-Item -LiteralPath $pidFile -Force
} else {
    $remaining | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $pidFile -Encoding UTF8
}

Write-Host "Stop command completed."
