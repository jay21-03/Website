[CmdletBinding()]
param(
    [switch]$SkipTests = $false,
    [string]$OutputDirectory = ""
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$beRoot = Split-Path -Parent $scriptDir

Write-Host "=================================================="
Write-Host "Building AWS Elastic Beanstalk Java SE Bundle"
Write-Host "Source directory: $beRoot"
Write-Host "=================================================="

# Check Maven command
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven ('mvn') was not found in PATH. Ensure Apache Maven is installed and accessible."
}

# Resolve target artifacts directory
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $artifactsDir = Join-Path $beRoot "artifacts"
} else {
    $artifactsDir = $OutputDirectory
}

if (-not (Test-Path $artifactsDir)) {
    New-Item -ItemType Directory -Path $artifactsDir -Force | Out-Null
}

$bundleZipPath = Join-Path $artifactsDir "bautruc-ecommerce-eb.zip"

# Build executable JAR with Maven
Push-Location $beRoot
try {
    Write-Host ""
    if ($SkipTests) {
        Write-Host "Running: mvn clean package -DskipTests"
        & mvn clean package "-DskipTests"
    } else {
        Write-Host "Running: mvn clean verify"
        & mvn clean verify
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

# Locate Spring Boot executable JAR in target/
$targetDir = Join-Path $beRoot "target"
$candidateJars = Get-ChildItem -Path $targetDir -Filter "*.jar" -File | Where-Object {
    $_.Name -notlike "*.original" -and
    $_.Name -notlike "*-sources.jar" -and
    $_.Name -notlike "*-javadoc.jar"
}

if ($candidateJars.Count -eq 0) {
    throw "No executable Spring Boot JAR found in $targetDir"
}

if ($candidateJars.Count -gt 1) {
    $jarNames = ($candidateJars | ForEach-Object { $_.Name }) -join ", "
    throw "Ambiguous JAR files found in ${targetDir}: $jarNames. Expected exactly 1 executable JAR."
}

$sourceJar = $candidateJars[0]
Write-Host ""
Write-Host "Identified executable JAR: $($sourceJar.Name) ($([math]::Round($sourceJar.Length / 1MB, 2)) MB)"

# Create temporary staging directory
$stagingDir = Join-Path $targetDir "eb-staging-$([System.Guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null

try {
    # 1. Copy JAR as app.jar
    $stagedJarPath = Join-Path $stagingDir "app.jar"
    Copy-Item -LiteralPath $sourceJar.FullName -Destination $stagedJarPath -Force

    # 2. Create Procfile for Elastic Beanstalk Java SE platform
    $procfilePath = Join-Path $stagingDir "Procfile"
    [System.IO.File]::WriteAllText($procfilePath, "web: java -jar app.jar`n", [System.Text.Encoding]::ASCII)

    # 3. Create zip bundle containing only staging contents (no parent folder)
    if (Test-Path $bundleZipPath) {
        Remove-Item -LiteralPath $bundleZipPath -Force
    }

    Write-Host "Compressing staging contents into $bundleZipPath..."
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::CreateFromDirectory($stagingDir, $bundleZipPath, [System.IO.Compression.CompressionLevel]::Optimal, $false)

    # 4. Verify bundle contents and safety
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($bundleZipPath)
    $entryNames = @($zip.Entries | ForEach-Object { $_.FullName })
    $zip.Dispose()

    Write-Host "Bundle contents:"
    foreach ($name in $entryNames) {
        Write-Host "  - $name"
    }

    # Safety assertions
    if ($entryNames -notcontains "app.jar") {
        throw "Bundle verification failed: app.jar is missing from the bundle."
    }
    if ($entryNames -notcontains "Procfile") {
        throw "Bundle verification failed: Procfile is missing from the bundle."
    }
    foreach ($entry in $entryNames) {
        if ($entry -like "*.env*" -or $entry -like "*docker-compose*" -or $entry -like "*credentials*") {
            throw "SECURITY VIOLATION: Prohibited file found in deployment bundle: $entry"
        }
    }

    $zipInfo = Get-Item $bundleZipPath
    $sizeMb = [math]::Round($zipInfo.Length / 1MB, 2)

    Write-Host ""
    Write-Host "=================================================="
    Write-Host "STATUS: PASS"
    Write-Host "Bundle Path: $bundleZipPath"
    Write-Host "Bundle Size: $sizeMb MB ($($zipInfo.Length) bytes)"
    Write-Host "Platform: AWS Elastic Beanstalk (Corretto 21 / AL2023 Java SE)"
    Write-Host "Runtime Command: web: java -jar app.jar"
    Write-Host "=================================================="
}
finally {
    if (Test-Path $stagingDir) {
        Remove-Item -LiteralPath $stagingDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}
