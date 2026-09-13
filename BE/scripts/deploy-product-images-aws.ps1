[CmdletBinding()]
param(
    [ValidateSet("dev", "staging", "prod")]
    [string]$Environment = "dev",

    [string]$Region = "ap-southeast-1",

    [string]$StackName = "",

    [string]$BucketName = "",

    [string]$Profile = "",

    [switch]$ConfirmDeploy
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "AWS CLI is not installed. Install AWS CLI v2 and authenticate first."
}

if ([string]::IsNullOrWhiteSpace($StackName)) {
    $StackName = "bautruc-product-images-$Environment"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$templatePath = Join-Path $projectRoot "infra\aws\product-images.yml"

if (-not (Test-Path -LiteralPath $templatePath)) {
    throw "CloudFormation template was not found: $templatePath"
}

$identityArgs = @(
    "sts",
    "get-caller-identity",
    "--output",
    "json"
)

if (-not [string]::IsNullOrWhiteSpace($Profile)) {
    $identityArgs += @("--profile", $Profile)
}

& aws @identityArgs

if ($LASTEXITCODE -ne 0) {
    throw "AWS authentication failed."
}

Write-Host ""
Write-Host "Planned product-image deployment:"
Write-Host "  Environment : $Environment"
Write-Host "  Region      : $Region"
Write-Host "  Stack       : $StackName"

if ([string]::IsNullOrWhiteSpace($BucketName)) {
    Write-Host "  Bucket      : CloudFormation-generated NEW bucket"
}
else {
    Write-Host "  Bucket      : $BucketName"
}

Write-Host ""

if (-not $ConfirmDeploy) {
    throw "Deployment blocked by safety guard. Re-run with -ConfirmDeploy only during the approved Phase 2B deployment."
}

$parameterOverrides = @(
    "Environment=$Environment"
)

if (-not [string]::IsNullOrWhiteSpace($BucketName)) {
    $parameterOverrides += "BucketName=$BucketName"
}

$deployArgs = @(
    "cloudformation",
    "deploy",
    "--template-file",
    $templatePath,
    "--stack-name",
    $StackName,
    "--region",
    $Region,
    "--capabilities",
    "CAPABILITY_IAM",
    "--parameter-overrides"
) + $parameterOverrides + @(
    "--no-fail-on-empty-changeset"
)

if (-not [string]::IsNullOrWhiteSpace($Profile)) {
    $deployArgs += @("--profile", $Profile)
}

& aws @deployArgs

if ($LASTEXITCODE -ne 0) {
    throw "CloudFormation deployment failed."
}

$outputArgs = @(
    "cloudformation",
    "describe-stacks",
    "--stack-name",
    $StackName,
    "--region",
    $Region,
    "--query",
    "Stacks[0].Outputs",
    "--output",
    "json"
)

if (-not [string]::IsNullOrWhiteSpace($Profile)) {
    $outputArgs += @("--profile", $Profile)
}

$outputs = (& aws @outputArgs | ConvertFrom-Json)

if ($LASTEXITCODE -ne 0) {
    throw "Could not read CloudFormation outputs."
}

$values = @{}

foreach ($item in $outputs) {
    $values[$item.OutputKey] = $item.OutputValue
}

Write-Host ""
Write-Host "Backend environment values:"
Write-Host "AWS_REGION=$($values.AwsRegion)"
Write-Host "S3_BUCKET_NAME=$($values.BucketName)"
Write-Host "S3_PUBLIC_BASE_URL=$($values.PublicBaseUrl)"
Write-Host ""
Write-Host "Backend managed policy ARN:"
Write-Host $values.BackendManagedPolicyArn
