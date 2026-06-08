param(
    [string]$BaseUrl = "http://localhost:9000",
    [int]$TimeoutSec = 10,
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "admin123",
    [long]$StudentId,
    [long]$CourseId,
    [string]$Semester = "2024-2025-1",
    [switch]$ExerciseSelectionWrite,
    [switch]$RequireSeedData
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

$cookieContainer = [System.Net.CookieContainer]::new()
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.CookieContainer = $cookieContainer
$handler.UseCookies = $true
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds($TimeoutSec)
$courseIdProvided = $PSBoundParameters.ContainsKey("CourseId")

function New-Url {
    param([string]$Path)
    return "$BaseUrl$Path"
}

function Invoke-CoreRequest {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [System.Net.Http.HttpContent]$Content,
        [int[]]$ExpectedStatus = @(200)
    )

    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), (New-Url $Path))
    if ($null -ne $Content) {
        $request.Content = $Content
    }

    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $status = [int]$response.StatusCode
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    } finally {
        $request.Dispose()
    }

    if ($ExpectedStatus -notcontains $status) {
        throw "$Name expected HTTP $($ExpectedStatus -join '/') but got $status from $(New-Url $Path). Body: $body"
    }

    $json = $null
    if (-not [string]::IsNullOrWhiteSpace($body)) {
        try {
            $json = $body | ConvertFrom-Json
        } catch {
            throw "$Name returned non-JSON body from $(New-Url $Path): $body"
        }
    }

    Write-Host "$Name OK ($status): $(New-Url $Path)"
    return [pscustomobject]@{
        Status = $status
        Body = $body
        Json = $json
    }
}

function Assert-ResultSuccess {
    param(
        [string]$Name,
        [object]$Json
    )

    if ($null -eq $Json) {
        throw "$Name did not return a JSON result body."
    }
    if ($Json.PSObject.Properties.Name -contains "success" -and -not [bool]$Json.success) {
        throw "$Name returned success=false. Body: $($Json | ConvertTo-Json -Depth 8)"
    }
    if ($Json.PSObject.Properties.Name -contains "code" -and [int]$Json.code -ge 400) {
        throw "$Name returned code=$($Json.code). Body: $($Json | ConvertTo-Json -Depth 8)"
    }
}

function Get-ResultData {
    param([object]$Json)

    if ($null -eq $Json) {
        return $null
    }
    if ($Json.PSObject.Properties.Name -contains "data") {
        return $Json.data
    }
    return $Json
}

function Get-FirstItem {
    param([object]$Data)

    if ($null -eq $Data) {
        return $null
    }

    if ($Data -is [System.Array]) {
        return @($Data)[0]
    }

    foreach ($propertyName in @("content", "records", "list", "items")) {
        if ($Data.PSObject.Properties.Name -contains $propertyName) {
            $items = @($Data.$propertyName)
            if ($items.Count -gt 0) {
                return $items[0]
            }
        }
    }

    return $Data
}

function Get-PropertyValue {
    param(
        [object]$Object,
        [string[]]$Names
    )

    if ($null -eq $Object) {
        return $null
    }

    foreach ($name in $Names) {
        if ($Object.PSObject.Properties.Name -contains $name) {
            return $Object.$name
        }
    }
    return $null
}

try {
    $loginFields = [System.Collections.Generic.List[System.Collections.Generic.KeyValuePair[string,string]]]::new()
    $loginFields.Add([System.Collections.Generic.KeyValuePair[string,string]]::new("username", $AdminUsername))
    $loginFields.Add([System.Collections.Generic.KeyValuePair[string,string]]::new("password", $AdminPassword))
    $loginFields.Add([System.Collections.Generic.KeyValuePair[string,string]]::new("role", "admin"))
    $loginContent = [System.Net.Http.FormUrlEncodedContent]::new($loginFields)
    $login = Invoke-CoreRequest -Name "Admin login" -Method "POST" -Path "/login/auth" -Content $loginContent
    Assert-ResultSuccess -Name "Admin login" -Json $login.Json

    $current = Invoke-CoreRequest -Name "Current session" -Method "GET" -Path "/login/current"
    Assert-ResultSuccess -Name "Current session" -Json $current.Json

    $courses = Invoke-CoreRequest -Name "Query courses" -Method "GET" -Path "/api/v1/courses/list?pageNum=1&pageSize=5"
    Assert-ResultSuccess -Name "Query courses" -Json $courses.Json
    $firstCourse = Get-FirstItem (Get-ResultData $courses.Json)
    if ($CourseId -le 0 -and $null -ne $firstCourse) {
        $CourseId = [long](Get-PropertyValue -Object $firstCourse -Names @("id", "courseId"))
    }

    $activeCourseItems = @()
    if ($CourseId -le 0 -or $ExerciseSelectionWrite) {
        $activeCourses = Invoke-CoreRequest -Name "Query active courses" -Method "GET" -Path "/api/v1/courses/active"
        Assert-ResultSuccess -Name "Query active courses" -Json $activeCourses.Json
        $activeCourseItems = @((Get-ResultData $activeCourses.Json))
        if ($CourseId -le 0 -and $activeCourseItems.Count -gt 0) {
            $CourseId = [long](Get-PropertyValue -Object $activeCourseItems[0] -Names @("id", "courseId"))
        }
    }

    $students = Invoke-CoreRequest -Name "Query students" -Method "GET" -Path "/api/v1/students/list?pageNum=1&pageSize=5"
    Assert-ResultSuccess -Name "Query students" -Json $students.Json
    $firstStudent = Get-FirstItem (Get-ResultData $students.Json)
    if ($StudentId -le 0 -and $null -ne $firstStudent) {
        $StudentId = [long](Get-PropertyValue -Object $firstStudent -Names @("id", "studentId"))
    }

    if ($StudentId -le 0) {
        if ($RequireSeedData -or $ExerciseSelectionWrite) {
            throw "No student id was provided or discovered from /api/v1/students/list."
        }
        Write-Warning "No student id was provided or discovered; skipping student selection, current course, and credit checks."
    } else {
        $studentSelections = Invoke-CoreRequest -Name "Query selected courses" -Method "GET" -Path "/api/v1/selections/student/${StudentId}?pageNum=1&pageSize=5"
        Assert-ResultSuccess -Name "Query selected courses" -Json $studentSelections.Json

        $currentCourses = Invoke-CoreRequest -Name "Query current courses" -Method "GET" -Path "/api/v1/selections/current/${StudentId}?semester=$([uri]::EscapeDataString($Semester))"
        Assert-ResultSuccess -Name "Query current courses" -Json $currentCourses.Json

        $credits = Invoke-CoreRequest -Name "Query selected credits" -Method "GET" -Path "/api/v1/selections/credits/${StudentId}?semester=$([uri]::EscapeDataString($Semester))"
        Assert-ResultSuccess -Name "Query selected credits" -Json $credits.Json
    }

    if ($CourseId -le 0) {
        if ($RequireSeedData -or $ExerciseSelectionWrite) {
            throw "No course id was provided or discovered from /api/v1/courses/list."
        }
        Write-Warning "No course id was provided or discovered; skipping selected-state check."
    } elseif ($StudentId -gt 0) {
        $check = Invoke-CoreRequest -Name "Check selected state" -Method "GET" -Path "/api/v1/selections/check?studentId=${StudentId}&courseId=${CourseId}"
        Assert-ResultSuccess -Name "Check selected state" -Json $check.Json
    }

    if ($ExerciseSelectionWrite) {
        if ($StudentId -le 0 -or $CourseId -le 0) {
            throw "ExerciseSelectionWrite requires a student id and a course id."
        }

        if (-not $courseIdProvided -and $activeCourseItems.Count -gt 0) {
            foreach ($candidate in $activeCourseItems) {
                $candidateId = [long](Get-PropertyValue -Object $candidate -Names @("id", "courseId"))
                if ($candidateId -le 0) {
                    continue
                }

                $candidateCheck = Invoke-CoreRequest -Name "Find writable course candidate" -Method "GET" -Path "/api/v1/selections/check?studentId=${StudentId}&courseId=${candidateId}"
                Assert-ResultSuccess -Name "Find writable course candidate" -Json $candidateCheck.Json
                if (-not [bool](Get-ResultData $candidateCheck.Json)) {
                    $CourseId = $candidateId
                    Write-Host "Using writable course candidate: courseId=$CourseId"
                    break
                }
            }
        }

        $beforeCheck = Invoke-CoreRequest -Name "Pre-write selected-state check" -Method "GET" -Path "/api/v1/selections/check?studentId=${StudentId}&courseId=${CourseId}"
        Assert-ResultSuccess -Name "Pre-write selected-state check" -Json $beforeCheck.Json
        $alreadySelected = [bool](Get-ResultData $beforeCheck.Json)

        if ($alreadySelected) {
            Write-Warning "Student $StudentId already selected course $CourseId; skipping write exercise to avoid dropping an existing active selection."
        } else {
            $select = Invoke-CoreRequest -Name "Select course" -Method "POST" -Path "/api/v1/selections?studentId=${StudentId}&courseId=${CourseId}"
            Assert-ResultSuccess -Name "Select course" -Json $select.Json

            $afterSelect = Invoke-CoreRequest -Name "Verify selected state" -Method "GET" -Path "/api/v1/selections/check?studentId=${StudentId}&courseId=${CourseId}"
            Assert-ResultSuccess -Name "Verify selected state" -Json $afterSelect.Json
            if (-not [bool](Get-ResultData $afterSelect.Json)) {
                throw "Course selection did not become active after select request."
            }

            $selectionQuery = Invoke-CoreRequest -Name "Find selected record" -Method "GET" -Path "/api/v1/selections/query?studentId=${StudentId}&courseId=${CourseId}"
            Assert-ResultSuccess -Name "Find selected record" -Json $selectionQuery.Json
            $selection = @((Get-ResultData $selectionQuery.Json) | Where-Object { [int]$_.status -ne 2 } | Select-Object -First 1)
            if ($selection.Count -eq 0 -or $null -eq $selection[0].id) {
                throw "Unable to find the selected record to drop. Body: $($selectionQuery.Json | ConvertTo-Json -Depth 8)"
            }

            $selectionId = [long]$selection[0].id
            $drop = Invoke-CoreRequest -Name "Drop course" -Method "DELETE" -Path "/api/v1/selections/${selectionId}?studentId=${StudentId}"
            Assert-ResultSuccess -Name "Drop course" -Json $drop.Json

            $afterDrop = Invoke-CoreRequest -Name "Verify dropped state" -Method "GET" -Path "/api/v1/selections/check?studentId=${StudentId}&courseId=${CourseId}"
            Assert-ResultSuccess -Name "Verify dropped state" -Json $afterDrop.Json
            if ([bool](Get-ResultData $afterDrop.Json)) {
                throw "Course selection is still active after drop request."
            }
        }
    }

    Write-Host "Core business regression test completed."
} finally {
    if ($null -ne $client) {
        $client.Dispose()
    }
    if ($null -ne $handler) {
        $handler.Dispose()
    }
}
