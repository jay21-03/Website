[CmdletBinding()]
param(
    [string]$Profile = "website-deploy",
    [string]$Region = "ap-southeast-1",
    [string]$ExpectedSolutionStack = "64bit Amazon Linux 2023 v4.12.8 running Corretto 21",
    [string]$ExpectedPostgresVersion = "16.15"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "AWS CLI v2 was not found."
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$beRoot = Split-Path -Parent $scriptDir
$infraDir = Join-Path $beRoot "infra\aws"

Write-Host "============================================================"
Write-Host "PHASE 2A2 READ-ONLY IAC VALIDATION"
Write-Host "============================================================"

# ============================================================
# 1. IDENTITY
# ============================================================

Write-Host ""
Write-Host "[1/6] AWS identity"

& aws sts get-caller-identity `
    --profile $Profile `
    --output json

if ($LASTEXITCODE -ne 0) {
    throw "AWS identity validation failed."
}

# ============================================================
# 2. CLOUDFORMATION
# ============================================================

Write-Host ""
Write-Host "[2/6] CloudFormation template validation"

Push-Location $infraDir

try {
    foreach ($template in @(
        "product-images.yml",
        "regional.yml",
        "edge.yml"
    )) {
        Write-Host "Validating $template ..."

        & aws cloudformation validate-template `
            --template-body "file://$template" `
            --profile $Profile `
            --region $Region `
            --output json | Out-Null

        if ($LASTEXITCODE -ne 0) {
            throw "CloudFormation validation failed: $template"
        }

        Write-Host "PASS: $template"
    }
}
finally {
    Pop-Location
}

# ============================================================
# 3. ELASTIC BEANSTALK
# Use JSON, not --output text pagination-sensitive comparison.
# ============================================================

Write-Host ""
# ============================================================
# SEMANTIC SAFETY GUARD
# ============================================================

$regionalTemplateText = Get-Content (Join-Path $infraDir "regional.yml") -Raw

if ($regionalTemplateText -match '!GetAtt\s+BackendEnvironment\.EndpointURL') {
    throw "regional.yml must not use BackendEnvironment.EndpointURL as a CloudFront origin. Query the EB CNAME after environment creation."
}

Write-Host "PASS: regional.yml does not expose SingleInstance EndpointURL as CloudFront origin."

Write-Host "[3/6] Elastic Beanstalk Corretto 21 platform"

$ebJson = & aws elasticbeanstalk list-available-solution-stacks `
    --profile $Profile `
    --region $Region `
    --output json

if ($LASTEXITCODE -ne 0) {
    throw "Could not query Elastic Beanstalk solution stacks."
}

$ebData = $ebJson | ConvertFrom-Json

$matchingStack = @(
    $ebData.SolutionStacks |
    Where-Object {
        $_ -eq $ExpectedSolutionStack
    }
)

if ($matchingStack.Count -lt 1) {
    Write-Host "Available Java SE Corretto 21 stacks:"

    $ebData.SolutionStacks |
        Where-Object {
            $_ -like "*running Corretto 21*"
        } |
        ForEach-Object {
            Write-Host "  $_"
        }

    throw "Expected Elastic Beanstalk solution stack is unavailable: $ExpectedSolutionStack"
}

Write-Host "PASS: $ExpectedSolutionStack"

# ============================================================
# 4. CLOUDFRONT MANAGED PREFIX LIST
# ============================================================

Write-Host ""
Write-Host "[4/6] CloudFront origin-facing prefix list"

$prefixJson = & aws ec2 describe-managed-prefix-lists `
    --profile $Profile `
    --region $Region `
    --filters `
        "Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing" `
    --output json

if ($LASTEXITCODE -ne 0) {
    throw "Could not query CloudFront origin-facing prefix list."
}

$prefixData = $prefixJson | ConvertFrom-Json

$prefix = @(
    $prefixData.PrefixLists |
    Where-Object {
        $_.PrefixListName -eq "com.amazonaws.global.cloudfront.origin-facing"
    }
) |
    Select-Object -First 1

if ($null -eq $prefix) {
    throw "CloudFront origin-facing managed prefix list was not found."
}

Write-Host "PASS: $($prefix.PrefixListId)"

# ============================================================
# 5. RDS
#
# IMPORTANT:
# Fetch JSON first and count locally.
# This avoids AWS CLI --output text applying a JMESPath query
# once per pagination page and returning values such as "0 1".
# ============================================================

Write-Host ""
Write-Host "[5/6] PostgreSQL $ExpectedPostgresVersion db.t4g.micro gp3 availability"

$rdsJson = & aws rds describe-orderable-db-instance-options `
    --profile $Profile `
    --region $Region `
    --engine postgres `
    --db-instance-class db.t4g.micro `
    --output json

if ($LASTEXITCODE -ne 0) {
    throw "Could not query RDS orderable options."
}

$rdsData = $rdsJson | ConvertFrom-Json

$rdsMatches = @(
    $rdsData.OrderableDBInstanceOptions |
    Where-Object {
        $_.EngineVersion -eq $ExpectedPostgresVersion -and
        $_.DBInstanceClass -eq "db.t4g.micro" -and
        $_.StorageType -eq "gp3"
    }
)

if ($rdsMatches.Count -lt 1) {
    Write-Host "Matching PostgreSQL 16 options currently returned:"

    $rdsData.OrderableDBInstanceOptions |
        Where-Object {
            $_.EngineVersion -like "16.*" -and
            $_.DBInstanceClass -eq "db.t4g.micro"
        } |
        Select-Object `
            EngineVersion,
            DBInstanceClass,
            StorageType |
        Format-Table |
        Out-Host

    throw "PostgreSQL $ExpectedPostgresVersion / db.t4g.micro / gp3 is unavailable."
}

Write-Host "PASS: PostgreSQL $ExpectedPostgresVersion / db.t4g.micro / gp3"

# ============================================================
# 6. ACTIVE CLOUDFORMATION STACKS
# Read-only sanity check.
# ============================================================

Write-Host ""
Write-Host "[6/6] Active CloudFormation stacks"

$stackJson = & aws cloudformation list-stacks `
    --profile $Profile `
    --region $Region `
    --stack-status-filter `
        CREATE_IN_PROGRESS `
        CREATE_COMPLETE `
        UPDATE_IN_PROGRESS `
        UPDATE_COMPLETE_CLEANUP_IN_PROGRESS `
        UPDATE_COMPLETE `
        UPDATE_ROLLBACK_IN_PROGRESS `
        UPDATE_ROLLBACK_COMPLETE `
    --output json

if ($LASTEXITCODE -ne 0) {
    throw "Could not inspect CloudFormation stacks."
}

$stackData = $stackJson | ConvertFrom-Json
$stackCount = @($stackData.StackSummaries).Count

Write-Host "Existing active CloudFormation stack count: $stackCount"

Write-Host ""
Write-Host "============================================================"
Write-Host "PHASE 2A2 VALIDATION: PASS"
Write-Host "No AWS resources were created or modified."
Write-Host "============================================================"
