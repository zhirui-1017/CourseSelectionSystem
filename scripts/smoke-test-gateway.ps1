param(
    [string]$BaseUrl = "http://localhost:9000",
    [switch]$ExpectCourseFallback,
    [ValidateSet("course-service", "selection-service", "user-service", "student-service", "teacher-service", "web-service")]
    [string[]]$ExpectFallbackFor = @(),
    [ValidateSet("course-service", "selection-service", "user-service", "student-service", "teacher-service", "web-service")]
    [string]$WaitForService,
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
        [string]$ExpectedBodyText,
        [int]$RetrySeconds = 0
    )

    $uri = "$BaseUrl$Path"
    $deadline = (Get-Date).AddSeconds($RetrySeconds)
    $lastError = $null

    do {
        $handler = [System.Net.Http.HttpClientHandler]::new()
        $client = [System.Net.Http.HttpClient]::new($handler)
        $client.Timeout = [TimeSpan]::FromSeconds($TimeoutSec)

        try {
            $response = $client.GetAsync($uri).GetAwaiter().GetResult()
            $status = [int]$response.StatusCode
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

            if ($ExpectedStatus -contains $status) {
                if ([string]::IsNullOrWhiteSpace($ExpectedBodyText) -or $body.Contains($ExpectedBodyText)) {
                    Write-Host "$Name OK ($status): $uri"
                    return
                }
            }

            $lastError = "$Name expected HTTP $($ExpectedStatus -join '/') from $uri and body text '$ExpectedBodyText', but got $status. Body: $body"
        } catch {
            $lastError = "$Name request failed for $uri. $($_.Exception.Message)"
        } finally {
            if ($null -ne $client) {
                $client.Dispose()
            }
            if ($null -ne $handler) {
                $handler.Dispose()
            }
        }

        if ($RetrySeconds -le 0 -or (Get-Date) -ge $deadline) {
            throw $lastError
        }

        Start-Sleep -Seconds 2
    } while ($true)
}

$fallbackTargets = @($ExpectFallbackFor)
if ($ExpectCourseFallback -and $fallbackTargets -notcontains "course-service") {
    $fallbackTargets += "course-service"
}
$fallbackTargets = @($fallbackTargets | Select-Object -Unique)

function Test-ExpectFallback {
    param([string]$ServiceName)

    return $fallbackTargets -contains $ServiceName
}

$fallbackText = -join ([char[]](0x670d, 0x52a1, 0x6682, 0x65f6, 0x4e0d, 0x53ef, 0x7528))
$fallbackRoutes = @{
    "course-service" = @{
        Name = "Course API fallback"
        Path = "/api/v1/courses/list?pageNum=1&pageSize=10"
    }
    "selection-service" = @{
        Name = "Selection API fallback"
        Path = "/api/v1/selections/check?studentId=1&courseId=1"
    }
    "user-service" = @{
        Name = "User API fallback"
        Path = "/api/v1/users/list?pageNum=1&pageSize=1"
    }
    "student-service" = @{
        Name = "Student API fallback"
        Path = "/api/v1/students/list?pageNum=1&pageSize=1"
    }
    "teacher-service" = @{
        Name = "Teacher API fallback"
        Path = "/api/v1/teachers/list?pageNum=1&pageSize=1"
    }
    "web-service" = @{
        Name = "Web route fallback"
        Path = "/login"
    }
}

$normalRoutes = @{
    "course-service" = @{
        Name = "Course API route"
        Path = "/api/v1/courses/list?pageNum=1&pageSize=10"
        ExpectedStatus = @(200)
    }
    "selection-service" = @{
        Name = "Selection API route"
        Path = "/api/v1/selections/check?studentId=1&courseId=1"
        ExpectedStatus = @(200)
    }
    "user-service" = @{
        Name = "User API route"
        Path = "/api/v1/users/list?pageNum=1&pageSize=1"
        ExpectedStatus = @(200)
    }
    "student-service" = @{
        Name = "Student API route"
        Path = "/api/v1/students/list?pageNum=1&pageSize=1"
        ExpectedStatus = @(200)
    }
    "teacher-service" = @{
        Name = "Teacher API route"
        Path = "/api/v1/teachers/list?pageNum=1&pageSize=1"
        ExpectedStatus = @(200)
    }
    "web-service" = @{
        Name = "Login page"
        Path = "/login"
        ExpectedStatus = @(200)
    }
}

function Wait-ServiceRouteReady {
    param([string]$ServiceName)

    $route = $normalRoutes[$ServiceName]
    Invoke-RouteCheck `
        -Name $route.Name `
        -Path $route.Path `
        -ExpectedStatus $route.ExpectedStatus `
        -ExpectedBodyText "" `
        -RetrySeconds 60
}

Invoke-RouteCheck -Name "Gateway health" -Path "/actuator/health" -ExpectedStatus @(200) -ExpectedBodyText "UP"

if (-not [string]::IsNullOrWhiteSpace($WaitForService)) {
    Wait-ServiceRouteReady -ServiceName $WaitForService
    Write-Host "Gateway service route is ready for $WaitForService."
    return
}

if (Test-ExpectFallback "web-service") {
    $route = $fallbackRoutes["web-service"]
    Invoke-RouteCheck -Name $route.Name -Path $route.Path -ExpectedStatus @(503) -ExpectedBodyText $fallbackText -RetrySeconds 45
} else {
    Invoke-RouteCheck -Name "Login page" -Path "/login" -ExpectedStatus @(200) -ExpectedBodyText ""
    Invoke-RouteCheck -Name "Legacy login route" -Path "/login.html" -ExpectedStatus @(200, 302) -ExpectedBodyText ""
    Invoke-RouteCheck -Name "Admin page route" -Path "/admin/index" -ExpectedStatus @(200, 302) -ExpectedBodyText ""
    Invoke-RouteCheck -Name "Student page route" -Path "/student/index" -ExpectedStatus @(200, 302) -ExpectedBodyText ""
    Invoke-RouteCheck -Name "Teacher page route" -Path "/teacher/index" -ExpectedStatus @(200, 302) -ExpectedBodyText ""
}

if (Test-ExpectFallback "course-service") {
    $route = $fallbackRoutes["course-service"]
    Invoke-RouteCheck -Name $route.Name -Path $route.Path -ExpectedStatus @(503) -ExpectedBodyText $fallbackText -RetrySeconds 45
} elseif (-not (Test-ExpectFallback "web-service")) {
    Invoke-RouteCheck -Name "Course API route" -Path "/api/v1/courses/list?pageNum=1&pageSize=10" -ExpectedStatus @(200) -ExpectedBodyText ""
}

foreach ($serviceName in $fallbackTargets) {
    if ($serviceName -eq "course-service" -or $serviceName -eq "web-service") {
        continue
    }

    $route = $fallbackRoutes[$serviceName]
    Invoke-RouteCheck -Name $route.Name -Path $route.Path -ExpectedStatus @(503) -ExpectedBodyText $fallbackText -RetrySeconds 45
}

Write-Host "Gateway smoke test completed."
