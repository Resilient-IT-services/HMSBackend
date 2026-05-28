# Manual Deployment Script for HMS Backend
# This script builds the project and runs the JAR with the necessary environment variables.

# 1. Build the project
Write-Host "Building HMS Backend..." -ForegroundColor Cyan
./gradlew bootJar -x test --no-daemon

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Please check the errors above." -ForegroundColor Red
    exit $LASTEXITCODE
}

# 2. Read environment variables from .env
if (Test-Path .env) {
    Write-Host "Loading environment variables from .env..." -ForegroundColor Cyan
    Get-Content .env | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            if ($line -match '^(?<name>[^=]+)=(?<value>.*)$') {
                $name = $Matches.name.Trim()
                $value = $Matches.value.Trim()
                [System.Environment]::SetEnvironmentVariable($name, $value)
                Write-Host "Set $name" -ForegroundColor Gray
            }
        }
    }
} else {
    Write-Host ".env file not found! Using default environment variables." -ForegroundColor Yellow
}

# 3. Override DB_URL for local deployment if it points to 'postgres'
$dbUrl = [System.Environment]::GetEnvironmentVariable("DB_URL")
if ($dbUrl -match "//postgres:") {
    Write-Host "Redirecting DB_URL from 'postgres' to 'localhost'..." -ForegroundColor Yellow
    $dbUrl = $dbUrl -replace "//postgres:", "//localhost:"
    [System.Environment]::SetEnvironmentVariable("DB_URL", $dbUrl)
}

# 4. Run the application
$jarPath = Get-ChildItem "build/libs/HMSBackend-*.jar" | Where-Object { $_.Name -notmatch "plain" } | Select-Object -First 1
if ($jarPath) {
    Write-Host "Starting HMS Backend: $($jarPath.Name)" -ForegroundColor Green
    java -jar $jarPath.FullName
} else {
    Write-Host "Executable JAR not found in build/libs/!" -ForegroundColor Red
}
