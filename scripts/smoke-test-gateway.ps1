param(
    [string]$BaseUrl = "http://localhost:9000",
    [switch]$ExpectCourseFallback,
    [int]$TimeoutSec = 10
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

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

function Invoke-RouteCheck {
    param(
        [string]$Name,
        [string]$Path,
        [int[]]$ExpectedStatus,
        [string]$ExpectedBodyText
    )

    $uri = "$BaseUrl$Path"
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds($TimeoutSec)

    try {
        $response = $client.GetAsync($uri).GetAwaiter().GetResult()
        $status = [int]$response.StatusCode
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    } finally {
        if ($null -ne $client) {
            $client.Dispose()
        }
        if ($null -ne $handler) {
            $handler.Dispose()
        }
    }

    if ($ExpectedStatus -notcontains $status) {
        throw "$Name expected HTTP $($ExpectedStatus -join '/') but got $status from $uri. Body: $body"
    }

    if (-not [string]::IsNullOrWhiteSpace($ExpectedBodyText) -and -not $body.Contains($ExpectedBodyText)) {
        throw "$Name response did not contain expected text '$ExpectedBodyText'. Body: $body"
    }

    Write-Host "$Name OK ($status): $uri"
}

Invoke-RouteCheck -Name "Gateway health" -Path "/actuator/health" -ExpectedStatus @(200) -ExpectedBodyText "UP"
Invoke-RouteCheck -Name "Login page" -Path "/login" -ExpectedStatus @(200) -ExpectedBodyText ""
Invoke-RouteCheck -Name "Admin page route" -Path "/admin/index" -ExpectedStatus @(200, 302) -ExpectedBodyText ""
Invoke-RouteCheck -Name "Student page route" -Path "/student/index" -ExpectedStatus @(200, 302) -ExpectedBodyText ""
Invoke-RouteCheck -Name "Teacher page route" -Path "/teacher/index" -ExpectedStatus @(200, 302) -ExpectedBodyText ""

if ($ExpectCourseFallback) {
    $fallbackText = -join ([char[]](0x670d, 0x52a1, 0x6682, 0x65f6, 0x4e0d, 0x53ef, 0x7528))
    Invoke-RouteCheck -Name "Course API fallback" -Path "/api/v1/courses/list?pageNum=1&pageSize=10" -ExpectedStatus @(503) -ExpectedBodyText $fallbackText
} else {
    Invoke-RouteCheck -Name "Course API route" -Path "/api/v1/courses/list?pageNum=1&pageSize=10" -ExpectedStatus @(200) -ExpectedBodyText ""
}

Write-Host "Gateway smoke test completed."
