[CmdletBinding()]
param(
    [ValidateSet("dev", "staging", "prod")]
    [string]$Environment = "dev",
    [string]$Region = "ap-southeast-1",
    [string]$StackName = "bautruc-product-images-dev",
    [string]$BucketName = "",
    [string]$Profile = ""
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "AWS CLI is not installed. Install AWS CLI v2, authenticate, then run this script again."
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$templatePath = Join-Path $projectRoot "infra\aws\product-images.yml"
if (-not (Test-Path -LiteralPath $templatePath)) {
    throw "CloudFormation template was not found: $templatePath"
}

$identityArgs = @("sts", "get-caller-identity", "--region", $Region, "--output", "json")
if (-not [string]::IsNullOrWhiteSpace($Profile)) {
    $identityArgs += @("--profile", $Profile)
}
& aws @identityArgs
if ($LASTEXITCODE -ne 0) {
    throw "AWS authentication failed. Configure a profile or runtime credentials first."
}

$parameterOverrides = @("Environment=$Environment")
if (-not [string]::IsNullOrWhiteSpace($BucketName)) {
    $parameterOverrides += "BucketName=$BucketName"
}

$deployArgs = @(
    "cloudformation", "deploy",
    "--template-file", $templatePath,
    "--stack-name", $StackName,
    "--region", $Region,
    "--capabilities", "CAPABILITY_IAM",
    "--parameter-overrides"
) + $parameterOverrides + @("--no-fail-on-empty-changeset")
if (-not [string]::IsNullOrWhiteSpace($Profile)) {
    $deployArgs += @("--profile", $Profile)
}

& aws @deployArgs
if ($LASTEXITCODE -ne 0) {
    throw "CloudFormation deployment failed."
}

$outputArgs = @(
    "cloudformation", "describe-stacks",
    "--stack-name", $StackName,
    "--region", $Region,
    "--query", "Stacks[0].Outputs",
    "--output", "json"
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

Write-Output ""
Write-Output "Set these backend environment variables:"
Write-Output "AWS_REGION=$($values.AwsRegion)"
Write-Output "S3_BUCKET_NAME=$($values.BucketName)"
Write-Output "S3_PUBLIC_BASE_URL=$($values.PublicBaseUrl)"
Write-Output ""
Write-Output "Attach this managed policy to the backend runtime role:"
Write-Output $values.BackendManagedPolicyArn
