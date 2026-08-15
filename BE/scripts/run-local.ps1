$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $ProjectRoot ".env"

if (-not (Test-Path $EnvFile)) {
    Write-Error ".env file not found at: $EnvFile"
    exit 1
}

Write-Host "Loading environment variables from .env..."

Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()

    if (
        $line -and
        -not $line.StartsWith("#") -and
        $line.Contains("=")
    ) {
        $name, $value = $line -split "=", 2

        $name = $name.Trim()
        $value = $value.Trim()

        if (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [Environment]::SetEnvironmentVariable(
            $name,
            $value,
            "Process"
        )
    }
}

# Ensure the JVM uses the project's authoritative business timezone.
if ([string]::IsNullOrWhiteSpace($env:JAVA_TOOL_OPTIONS)) {
    $env:JAVA_TOOL_OPTIONS = "-Duser.timezone=Asia/Ho_Chi_Minh"
}
elseif ($env:JAVA_TOOL_OPTIONS -notmatch "user\.timezone") {
    $env:JAVA_TOOL_OPTIONS =
        "$($env:JAVA_TOOL_OPTIONS) -Duser.timezone=Asia/Ho_Chi_Minh"
}

$RequiredVariables = @(
    "DB_URL",
    "DB_USERNAME",
    "DB_PASSWORD"
)

foreach ($variable in $RequiredVariables) {
    $value = [Environment]::GetEnvironmentVariable(
        $variable,
        "Process"
    )

    if ([string]::IsNullOrWhiteSpace($value)) {
        Write-Error "Required environment variable '$variable' is missing or empty."
        exit 1
    }
}

Write-Host ""
Write-Host "Local configuration loaded."
Write-Host "DB_URL=$env:DB_URL"
Write-Host "DB_USERNAME=$env:DB_USERNAME"
Write-Host "JAVA_TOOL_OPTIONS=$env:JAVA_TOOL_OPTIONS"
Write-Host ""
Write-Host "Starting Spring Boot with profile: local"
Write-Host ""

Push-Location $ProjectRoot

try {
    mvn spring-boot:run "-Dspring-boot.run.profiles=local"

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}