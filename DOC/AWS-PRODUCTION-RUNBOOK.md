# AWS Production Runbook — Đàng Xem Ecommerce

> Tài liệu vận hành production cho website Đàng Xem.  
> Mục tiêu: một thành viên khác trong nhóm có thể build, deploy, kiểm tra, rollback và xử lý backup mà không cần nhớ lại toàn bộ quá trình MIG-1 → MIG-9.

**Trạng thái baseline được xác nhận:** 2026-09-16  
**Production release baseline:** `9f6c2a6` (`infra: finalize production domain and image recovery`)  
**AWS region:** `ap-southeast-1`  
**AWS CLI profile dùng trong runbook:** `website-deploy`  
**Payment/payOS:** chưa nằm trong production acceptance hiện tại.

---

## 1. Nguyên tắc vận hành

1. `main` là source tương ứng với production đã được chấp nhận.
2. Mỗi task FE/BE làm trên một branch riêng `feature/*` hoặc `fix/*`.
3. Các branch task được merge vào `feature/aws-deployment-iac` để integration test và deploy.
4. Chỉ sau khi production smoke test PASS mới merge/sync `feature/aws-deployment-iac` vào `main`.
5. Không deploy trực tiếp một working tree đang dirty.
6. Không đưa secret, `.env`, AWS access key hoặc credential vào Git/artifact deploy.
7. Mọi thay đổi CloudFormation phải qua `validate-template` + Change Set review trước khi execute.
8. Không xóa/recreate bucket, EC2 hoặc stack production chỉ để xử lý một update thông thường.
9. Trước một backend release có thay đổi DB/Flyway, phải có database backup mới.
10. Rollback application không đồng nghĩa rollback database. Migration DB phải được xử lý riêng.

---

## 2. Production endpoints

| Mục | Giá trị hiện tại |
|---|---|
| Canonical website | `https://dangxembautruc.com` |
| WWW | `https://www.dangxembautruc.com` |
| Health | `https://dangxembautruc.com/actuator/health` |
| Backend origin DNS | `origin.dangxembautruc.com` |
| AWS region | `ap-southeast-1` |
| Main edge stack | `bautruc-edge-prod` |
| Backend stack | `bautruc-single-node-prod` |
| Product image stack | `bautruc-product-images-prod` |

Current resolved resources at the 2026-09-16 baseline:

| Resource | Current value |
|---|---|
| Backend EC2 | `i-00f29cdbd6345f237` (`t4g.small`) |
| Backend EIP | `47.131.99.184` |
| Main CloudFront | `E3QMIX18974EDK` |
| Main CF default domain | `dnp4w49hrgpvl.cloudfront.net` |
| Frontend S3 | `bautruc-edge-prod-frontendbucket-sra3i9amgip6` |
| Product image S3 | `bautruc-product-images-prod-productimagesbucket-ggkx1tasg5ec` |
| Product image CloudFront | `E2FWZZAMFCXF30` |
| Product image public base | `https://dgsoag6dejink.cloudfront.net` |
| DB backup S3 | `bautruc-single-node-prod-databasebackupbucket-skhu8wte81zs` |

**Không hard-code các ID trên vào script dài hạn.** Nếu resource được recreate, lấy lại giá trị từ CloudFormation Outputs như các lệnh trong runbook này.

---

## 3. Kiến trúc production

```text
Browser
  |
  | HTTPS
  v
Route53
  |
  v
CloudFront: bautruc-edge-prod / MainDistribution
  |
  |-- default behavior ----------------------> private S3 frontend
  |                                             React/Vite dist
  |
  |-- /api/* + /actuator/health ------------> origin.dangxembautruc.com
                                                |
                                                v
                                             EC2 t4g.small
                                                |
                                                +-- nginx :80
                                                +-- Spring Boot :8080 loopback
                                                +-- PostgreSQL :5432 loopback
                                                +-- 2 GB swap

Product images:
Backend EC2 --> private S3 product-images
Browser -----> product-image CloudFront + OAC --> private S3 product-images

Database backup:
PostgreSQL --> /opt/bautruc/backup-db.sh --> private S3 backup bucket
                                             lifecycle: 35 days
```

### Security properties currently expected

- Frontend S3 is private; CloudFront uses OAC.
- Product image S3 is private; CloudFront uses OAC.
- Product image bucket has S3 Versioning enabled.
- Product image noncurrent versions are retained for 30 days.
- Backend IAM can `PutObject` and normal `DeleteObject` for product images, but does **not** receive `s3:DeleteObjectVersion`.
- EC2 inbound port 80 is limited to the AWS CloudFront origin-facing managed prefix list.
- No public SSH rule is required; use Systems Manager Session Manager / Run Command.
- PostgreSQL listens on `127.0.0.1:5432` only.
- Spring Boot listens on loopback `:8080` behind nginx.
- Production Swagger/OpenAPI is disabled.
- Runtime secrets come from SSM Parameter Store / server runtime config, not committed files.

Production SSM SecureString names:

```text
/bautruc/prod/DB_PASSWORD
/bautruc/prod/JWT_SECRET_BASE64
```

Never write their values into this runbook, Git, logs or screenshots.

---

## 4. Git workflow

### 4.1 Developer starts a task

Always start from the latest stable `main`:

```powershell
cd E:\Website

git switch main
git fetch origin
git pull --ff-only origin main

git switch -c feature/<task-name>
```

Examples:

```text
feature/fe-home-layout
feature/fe-admin-orders
feature/be-product-api
fix/be-auth-cookie
```

After coding:

```powershell
git status
git add <intended-files>
git commit -m "fix(fe): <description>"
git push -u origin feature/<task-name>
```

Do not push task code directly into `main` or directly code on the deployment branch unless it is an emergency release fix owned by the release manager.

### 4.2 Release manager integrates a task

```powershell
cd E:\Website

git fetch origin --prune
git switch feature/aws-deployment-iac
git pull --ff-only origin feature/aws-deployment-iac

git merge --no-ff origin/feature/<task-name>
```

If there is a conflict, resolve it before build/deploy. Do not deploy with unresolved or partially staged changes.

### 4.3 After production PASS

Sync the verified deployment branch to `main`:

```powershell
cd E:\Website

git fetch origin --prune
git switch main

git merge-base --is-ancestor origin/main feature/aws-deployment-iac
if ($LASTEXITCODE -ne 0) {
    throw "origin/main contains commits not yet integrated into the deployment branch."
}

git merge --ff-only feature/aws-deployment-iac
git push origin main

git switch feature/aws-deployment-iac
```

If `--ff-only` fails, stop and inspect the branch graph. Do not use `reset --hard` or force push as a routine release operation.

Recommended release tag after a successful production release:

```powershell
$tag = "prod-" + (Get-Date -Format "yyyyMMdd-HHmm")
git tag -a $tag -m "Production release $tag"
git push origin $tag
```

---

## 5. Pre-deploy checklist

Before every release:

```powershell
cd E:\Website

$ErrorActionPreference = "Stop"
$env:AWS_PAGER = ""
$env:GIT_PAGER = "cat"

# Git must be clean.
if (@(git status --porcelain).Count -ne 0) {
    git --no-pager status --short
    throw "Working tree is not clean."
}

# Verify branch.
git branch --show-current

# AWS identity.
aws sts get-caller-identity `
    --profile website-deploy `
    --output json `
    --no-cli-pager

# Production health before deployment.
Invoke-RestMethod `
    -Uri "https://dangxembautruc.com/actuator/health" `
    -Method Get
```

Expected health:

```json
{"status":"UP"}
```

If production is already unhealthy, do not hide that fact by deploying an unrelated release first. Diagnose the existing incident.

---

# 6. Frontend build and deploy

## 6.1 Frontend requirements

From `FE/package.json` the project provides:

```text
npm run lint
npm run typecheck
npm run test
npm run e2e
npm run build
```

Vite outputs to `FE/dist`.

`VITE_GOOGLE_CLIENT_ID` must match the Google OAuth Client ID used by the backend. The client ID is not a client secret, but keep environment-specific configuration explicit.

## 6.2 Build FE

```powershell
cd E:\Website\FE

npm ci
npm run lint
npm run typecheck
npm run test
npm run build
```

Expected result:

```text
FE/dist/
```

For E2E, start the required local services first and then run:

```powershell
npm run e2e
```

Do not treat `npm run build` alone as proof that login/admin/write flows work.

## 6.3 Resolve the production frontend bucket and distribution

```powershell
$profile = "website-deploy"
$region  = "ap-southeast-1"
$edgeStack = "bautruc-edge-prod"

$frontendBucket = aws cloudformation describe-stacks `
    --profile $profile `
    --region $region `
    --stack-name $edgeStack `
    --query "Stacks[0].Outputs[?OutputKey=='FrontendBucketName'].OutputValue | [0]" `
    --output text `
    --no-cli-pager

$distributionId = aws cloudformation describe-stacks `
    --profile $profile `
    --region $region `
    --stack-name $edgeStack `
    --query "Stacks[0].Outputs[?OutputKey=='DistributionId'].OutputValue | [0]" `
    --output text `
    --no-cli-pager

Write-Host "Frontend bucket : $frontendBucket"
Write-Host "Distribution ID : $distributionId"

if (
    [string]::IsNullOrWhiteSpace("$frontendBucket") -or
    "$frontendBucket" -eq "None" -or
    [string]::IsNullOrWhiteSpace("$distributionId") -or
    "$distributionId" -eq "None"
) {
    throw "Could not resolve frontend production resources."
}
```

## 6.4 Deploy FE

From `E:\Website\FE` after a successful build:

```powershell
aws s3 sync `
    .\dist `
    "s3://$frontendBucket" `
    --delete `
    --profile $profile `
    --region $region `
    --no-progress

if ($LASTEXITCODE -ne 0) {
    throw "Frontend S3 sync failed."
}

$invalidationId = aws cloudfront create-invalidation `
    --profile $profile `
    --distribution-id $distributionId `
    --paths "/*" `
    --query "Invalidation.Id" `
    --output text `
    --no-cli-pager

Write-Host "Invalidation: $invalidationId"
```

Wait for invalidation if the release requires deterministic verification:

```powershell
aws cloudfront wait invalidation-completed `
    --profile $profile `
    --distribution-id $distributionId `
    --id $invalidationId `
    --no-cli-pager
```

Then run the smoke tests in section 11.

---

# 7. Backend build

## 7.1 Local test requirements

The backend uses Maven and Testcontainers. A full:

```powershell
mvn clean verify
```

can require Docker Desktop because integration tests use Testcontainers/PostgreSQL.

Preferred release build:

```powershell
cd E:\Website\BE
mvn clean verify
```

Only use the following after tests have already passed elsewhere and the release manager intentionally accepts skipping them:

```powershell
mvn clean package -DskipTests
```

The executable Spring Boot JAR is produced under:

```text
BE/target/*.jar
```

The backend uses Java 21 / Spring Boot 3.5.x.

## 7.2 Identify the executable JAR

```powershell
$beRoot = "E:\Website\BE"

$jar = @(
    Get-ChildItem "$beRoot\target" -Filter "*.jar" -File |
    Where-Object {
        $_.Name -notlike "*.original" -and
        $_.Name -notlike "*-sources.jar" -and
        $_.Name -notlike "*-javadoc.jar"
    }
)

if ($jar.Count -ne 1) {
    throw "Expected exactly one executable backend JAR."
}

$jar = $jar[0]
Write-Host $jar.FullName
```

---

# 8. Database backup before backend deployment

The production server has:

```text
/opt/bautruc/backup-db.sh
bautruc-db-backup.service
bautruc-db-backup.timer
```

The timer is expected to run daily around `19:00 UTC` (~`02:00 Asia/Ho_Chi_Minh`) with the configured randomized delay.

Before a release containing Flyway/schema changes, create a fresh backup by starting the existing oneshot service via SSM.

Resolve instance ID:

```powershell
$profile = "website-deploy"
$region = "ap-southeast-1"
$singleStack = "bautruc-single-node-prod"

$instanceId = aws cloudformation describe-stacks `
    --profile $profile `
    --region $region `
    --stack-name $singleStack `
    --query "Stacks[0].Outputs[?OutputKey=='InstanceId'].OutputValue | [0]" `
    --output text `
    --no-cli-pager

$backupBucket = aws cloudformation describe-stacks `
    --profile $profile `
    --region $region `
    --stack-name $singleStack `
    --query "Stacks[0].Outputs[?OutputKey=='DatabaseBackupBucketName'].OutputValue | [0]" `
    --output text `
    --no-cli-pager
```

Run backup:

```powershell
$payloadFile = Join-Path $env:TEMP "bautruc-predeploy-backup.json"

$payload = @{
    commands = @(
        "sudo systemctl start bautruc-db-backup.service",
        "sudo systemctl is-failed bautruc-db-backup.service >/dev/null 2>&1 && exit 1 || true",
        "sudo systemctl status bautruc-db-backup.service --no-pager || true"
    )
} | ConvertTo-Json -Depth 5

[System.IO.File]::WriteAllText(
    $payloadFile,
    $payload,
    (New-Object System.Text.UTF8Encoding($false))
)

$commandId = aws ssm send-command `
    --profile $profile `
    --region $region `
    --instance-ids $instanceId `
    --document-name "AWS-RunShellScript" `
    --parameters ("file://" + ($payloadFile -replace '\\','/')) `
    --query "Command.CommandId" `
    --output text `
    --no-cli-pager

aws ssm wait command-executed `
    --profile $profile `
    --region $region `
    --command-id $commandId `
    --instance-id $instanceId `
    --no-cli-pager

Remove-Item $payloadFile -Force -ErrorAction SilentlyContinue
```

Verify the newest `.dump` and `.sha256` are present:

```powershell
aws s3api list-objects-v2 `
    --profile $profile `
    --region $region `
    --bucket $backupBucket `
    --query "reverse(sort_by(Contents,&LastModified))[0:6].{Key:Key,LastModified:LastModified,Size:Size}" `
    --output table `
    --no-cli-pager
```

Do not deploy a schema-changing backend release if the pre-release backup failed.

---

# 9. Backend deploy to the single EC2

## 9.1 Runtime paths

Current server contract:

```text
systemd service : bautruc-backend.service
launcher        : /opt/bautruc/backend-launch.sh
active JAR      : /opt/bautruc/current/app.jar
release storage : /opt/bautruc/releases/
local backups   : /opt/bautruc/backups/
```

Production runtime configuration must continue to include:

```text
DB_URL=jdbc:postgresql://127.0.0.1:5432/bautruc_ecommerce
FRONTEND_BASE_URL=https://dangxembautruc.com
ALLOWED_ORIGINS=https://dangxembautruc.com,https://www.dangxembautruc.com
AUTH_COOKIE_DOMAIN=<blank>
```

Do not print DB password, JWT secret or other secret values during deployment.

## 9.2 Upload release artifact to the private backup/artifact bucket

Use the database backup bucket's `deploy/` prefix as the release transfer location. The EC2 instance role has access to this bucket.

```powershell
$commit = git -C E:\Website rev-parse --short HEAD
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$releaseName = "backend-$commit-$stamp.jar"
$releaseKey = "deploy/$releaseName"

aws s3 cp `
    $jar.FullName `
    "s3://$backupBucket/$releaseKey" `
    --profile $profile `
    --region $region `
    --no-progress

if ($LASTEXITCODE -ne 0) {
    throw "Backend artifact upload failed."
}
```

## 9.3 Deploy through SSM with local rollback guard

The following pattern backs up the currently running JAR, downloads the new JAR, atomically moves it into place, restarts the service and checks loopback health. If the new process does not become healthy, it restores the previous JAR.

```powershell
$serverRelease = "/opt/bautruc/releases/$releaseName"
$serverBackup = "/opt/bautruc/backups/app-pre-$commit-$stamp.jar"

$commands = @(
    "set -euo pipefail",
    "sudo mkdir -p /opt/bautruc/current /opt/bautruc/releases /opt/bautruc/backups",
    "aws s3 cp 's3://$backupBucket/$releaseKey' '$serverRelease' --region '$region'",
    "test -s '$serverRelease'",
    "if [ -f /opt/bautruc/current/app.jar ]; then sudo cp -a /opt/bautruc/current/app.jar '$serverBackup'; fi",
    "sudo install -m 0644 '$serverRelease' /opt/bautruc/current/app.jar.new",
    "sudo mv -f /opt/bautruc/current/app.jar.new /opt/bautruc/current/app.jar",
    "sudo systemctl restart bautruc-backend.service",
    "ok=0; for i in `$(seq 1 30); do if curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '\"status\":\"UP\"'; then ok=1; break; fi; sleep 2; done",
    "if [ `$ok -ne 1 ]; then echo 'NEW RELEASE UNHEALTHY - ROLLING BACK'; if [ -f '$serverBackup' ]; then sudo cp -a '$serverBackup' /opt/bautruc/current/app.jar; sudo systemctl restart bautruc-backend.service; fi; exit 1; fi",
    "sudo systemctl --no-pager status bautruc-backend.service || true",
    "curl -fsS http://127.0.0.1:8080/actuator/health"
)

$payloadFile = Join-Path $env:TEMP "bautruc-backend-deploy.json"
$payload = @{ commands = $commands } | ConvertTo-Json -Depth 5

[System.IO.File]::WriteAllText(
    $payloadFile,
    $payload,
    (New-Object System.Text.UTF8Encoding($false))
)

$commandId = aws ssm send-command `
    --profile $profile `
    --region $region `
    --instance-ids $instanceId `
    --document-name "AWS-RunShellScript" `
    --parameters ("file://" + ($payloadFile -replace '\\','/')) `
    --query "Command.CommandId" `
    --output text `
    --no-cli-pager

aws ssm wait command-executed `
    --profile $profile `
    --region $region `
    --command-id $commandId `
    --instance-id $instanceId `
    --no-cli-pager

aws ssm get-command-invocation `
    --profile $profile `
    --region $region `
    --command-id $commandId `
    --instance-id $instanceId `
    --output json `
    --no-cli-pager

Remove-Item $payloadFile -Force -ErrorAction SilentlyContinue
```

After SSM reports success, run the public smoke tests. Do not merge the release to `main` until public health and functional tests pass.

---

# 10. Backend rollback

## 10.1 Application-only rollback

Use when the new JAR is bad **and no incompatible DB migration has been applied**.

Find a known-good artifact under `deploy/`:

```powershell
aws s3api list-objects-v2 `
    --profile $profile `
    --region $region `
    --bucket $backupBucket `
    --prefix "deploy/" `
    --query "reverse(sort_by(Contents,&LastModified))[0:10].{Key:Key,LastModified:LastModified,Size:Size}" `
    --output table `
    --no-cli-pager
```

Then copy the chosen artifact to `/opt/bautruc/current/app.jar`, restart `bautruc-backend.service`, and verify loopback plus public health through SSM.

**Do not blindly rollback an old JAR after a non-backward-compatible Flyway migration.** In that case, assess DB compatibility first.

## 10.2 Git rollback

Do not rewrite public history or force-push `main`. Prefer a revert commit:

```powershell
git switch feature/aws-deployment-iac
git pull --ff-only origin feature/aws-deployment-iac

git revert <bad-commit-or-merge>
git push origin feature/aws-deployment-iac
```

Build, deploy, smoke-test the revert as a normal release. After PASS, sync it to `main`.

---

# 11. Mandatory production smoke tests

Run after **every FE or BE production deployment**.

## 11.1 Automated health

```powershell
$urls = @(
    "https://dangxembautruc.com",
    "https://www.dangxembautruc.com"
)

foreach ($base in $urls) {
    $health = Invoke-RestMethod `
        -Uri "$base/actuator/health?release=$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())" `
        -Method Get

    if ("$($health.status)" -ne "UP") {
        throw "$base health is not UP."
    }

    Write-Host "PASS: $base"
}
```

## 11.2 Required functional checks

Before merging release to `main`, verify:

- Homepage loads on the apex domain.
- `www` loads correctly.
- Refresh a deep React Router route; it must not return S3/CloudFront 404.
- Google login works on the real production domain.
- Authenticated session survives normal navigation.
- Admin page loads for an authorized admin account.
- CSRF-protected write request succeeds after obtaining/refeshing CSRF token.
- Admin SSE connection remains functional.
- Product listing/detail data loads from `/api/v1` through same-origin CloudFront routing.
- Product image delivery through the image CDN works.
- If the release changes image management: test upload and normal delete on a dedicated test image/product only.
- Verify `/actuator/health` after all functional checks.

Payment/payOS is explicitly excluded until that feature is enabled for production.

## 11.3 Server health checks

```text
bautruc-backend.service         active
bautruc-db-backup.timer         enabled + active
PostgreSQL                      127.0.0.1:5432
Spring Boot                     loopback :8080
nginx                           :80
swap                            2 GB configured
```

Use SSM rather than opening SSH:

```bash
systemctl is-active bautruc-backend.service
systemctl is-enabled bautruc-db-backup.timer
systemctl is-active bautruc-db-backup.timer
systemctl list-timers bautruc-db-backup.timer --no-pager
ss -lntp | grep ':5432'
ss -lntp | grep ':8080'
ss -lntp | grep ':80'
free -h
swapon --show
```

---

# 12. Database backup operations

## 12.1 Current backup policy

- Backup bucket: CloudFormation output `DatabaseBackupBucketName`.
- Encryption: S3 SSE-S3 (`AES256`).
- Lifecycle: delete objects after 35 days.
- Daily dump prefix: `daily/`.
- Integrity sidecar: `.sha256`.
- A restore drill was successfully completed during production migration.

List latest backups:

```powershell
aws s3api list-objects-v2 `
    --profile website-deploy `
    --region ap-southeast-1 `
    --bucket $backupBucket `
    --query "reverse(sort_by(Contents,&LastModified))[0:10].{Key:Key,LastModified:LastModified,Size:Size}" `
    --output table `
    --no-cli-pager
```

A healthy daily backup normally has both:

```text
daily/postgres-<timestamp>.dump
daily/postgres-<timestamp>.dump.sha256
```

## 12.2 Safe restore drill

A restore drill must use a temporary database, not overwrite production.

High-level procedure:

1. Select one `.dump` and matching `.sha256`.
2. Download both from S3.
3. Verify SHA-256.
4. Create a temporary PostgreSQL database such as `bautruc_restore_drill`.
5. Restore the custom-format dump with `pg_restore`.
6. Verify schema, `flyway_schema_history`, and representative application tables.
7. Drop only the temporary drill database.
8. Confirm production health remained UP throughout.

Do not use production restore commands as a routine test.

## 12.3 Emergency production restore

A production DB restore is destructive/high impact. Before doing it:

- stop or isolate write traffic;
- take one final backup if the DB is still readable;
- identify the exact desired dump + checksum;
- verify application/Flyway compatibility;
- document the incident timestamp and release commit;
- restore only after explicit release/incident owner approval;
- restart backend and execute the full smoke-test suite.

Because the backend and PostgreSQL share one EC2 instance, do not terminate/recreate the instance as the first response to a database incident.

---

# 13. Product image recovery

Product image bucket protections:

```text
Versioning                    Enabled
Noncurrent version retention  30 days
Expired delete-marker cleanup Enabled
Backend DeleteObjectVersion   NOT granted
```

A normal object delete creates a delete marker; prior versions remain recoverable inside the recovery window.

Inspect one key:

```powershell
aws s3api list-object-versions `
    --profile website-deploy `
    --region ap-southeast-1 `
    --bucket $productBucket `
    --prefix "products/<product-id>/<file>" `
    --output table `
    --no-cli-pager
```

To recover an accidentally deleted object, remove **the delete marker version**, not the historical content version. `s3:DeleteObjectVersion` is intentionally an admin/operator action, not a backend permission.

Do not perform recovery tests against a real production image. The verified test prefix is:

```text
products/__versioning-test__/
```

---

# 14. Frontend rollback

The frontend bucket is not the application source of truth. Git is.

Preferred FE rollback:

1. Identify the previous known-good production Git commit/tag.
2. Checkout/revert that source on the deployment branch.
3. Re-run `npm ci` and `npm run build`.
4. Sync the rebuilt `dist` to the frontend bucket with `--delete`.
5. Invalidate `/*` on main CloudFront.
6. Run mandatory smoke tests.

Do not manually edit built JS files inside S3 as a normal rollback procedure.

---

# 15. Infrastructure / CloudFormation changes

Production IaC lives under:

```text
BE/infra/aws/
  single-node.yml
  edge.yml
  product-images.yml
```

`regional.yml` and old Elastic Beanstalk/RDS documentation are historical infrastructure and are not the current active production architecture.

Current production is single-node EC2 + local PostgreSQL. There are currently:

```text
Active RDS instances            0
Active Elastic Beanstalk envs   0
Legacy RDS snapshot             absent
Legacy backend artifact bucket  absent
```

## 15.1 Validate first

```powershell
aws cloudformation validate-template `
    --profile website-deploy `
    --region ap-southeast-1 `
    --template-body "file://BE/infra/aws/<template>.yml" `
    --no-cli-pager
```

## 15.2 Use a Change Set

For production changes, create an UPDATE Change Set, reuse the existing parameter values, and review:

- every `LogicalResourceId`;
- `Action`;
- `Replacement`;
- property-level `Details` / `RequiresRecreation`.

Stop if a routine update unexpectedly proposes:

```text
Replacement=True
Delete of production bucket
Delete/recreate EC2
Unrelated IAM policy changes
Unrelated CloudFront changes
```

Dynamic dependency changes can appear in Change Sets for resources using `Ref`/`GetAtt`; inspect property-level details rather than relying only on the top-level `Modify` label.

Never blindly execute a Change Set simply because `validate-template` passed.

---

# 16. CloudFront / DNS checks

Expected main CloudFront aliases:

```text
dangxembautruc.com
www.dangxembautruc.com
```

Expected backend origin:

```text
origin.dangxembautruc.com
```

Check main distribution:

```powershell
aws cloudfront get-distribution `
    --profile website-deploy `
    --id $distributionId `
    --query "Distribution.{Status:Status,Domain:DomainName,Enabled:DistributionConfig.Enabled,Aliases:DistributionConfig.Aliases.Items}" `
    --output json `
    --no-cli-pager
```

Expected:

```text
Status  = Deployed
Enabled = true
Aliases = apex + www
```

Google OAuth Authorized JavaScript origins should use the real app origins:

```text
https://dangxembautruc.com
https://www.dangxembautruc.com
```

The CloudFront default domain should not be treated as the canonical credentialed application origin.

---

# 17. Routine monitoring

At minimum, periodically verify:

```text
/actuator/health             UP
bautruc-backend.service      active
bautruc-db-backup.timer      active + enabled
latest daily DB backup       present
EC2 memory/swap              healthy
CloudFront                   Deployed
product image Versioning     Enabled
Git main/deploy branch       expected release relationship
```

Useful commands:

```powershell
Invoke-RestMethod https://dangxembautruc.com/actuator/health

aws ec2 describe-instances `
    --profile website-deploy `
    --region ap-southeast-1 `
    --instance-ids $instanceId `
    --query "Reservations[0].Instances[0].{State:State.Name,Type:InstanceType,PublicIp:PublicIpAddress}" `
    --output table `
    --no-cli-pager

aws s3api get-bucket-versioning `
    --profile website-deploy `
    --region ap-southeast-1 `
    --bucket $productBucket `
    --output json `
    --no-cli-pager
```

---

# 18. Incident triage order

If production is down, inspect in this order:

1. Public `/actuator/health` on apex domain.
2. CloudFront status and aliases.
3. EC2 state + SSM Online status.
4. nginx `:80` listener.
5. Spring Boot service + loopback health `127.0.0.1:8080`.
6. PostgreSQL listener `127.0.0.1:5432`.
7. backend logs (`journalctl -u bautruc-backend.service`).
8. disk space, memory and swap.
9. most recent release commit/artifact.
10. DB backup availability before any destructive recovery.

Do not start by deleting/recreating infrastructure.

---

# 19. Release acceptance checklist

Use this at the end of every production release:

```text
[ ] Correct feature/fix branches merged into feature/aws-deployment-iac
[ ] Working tree clean
[ ] FE lint PASS (when FE changed)
[ ] FE typecheck PASS (when FE changed)
[ ] FE tests PASS (when FE changed)
[ ] FE build PASS (when FE changed)
[ ] BE mvn clean verify PASS (when BE changed)
[ ] Fresh DB backup present before schema-changing BE deployment
[ ] FE/BE production deployment completed
[ ] CloudFront invalidation completed after FE deployment
[ ] Apex health UP
[ ] WWW health UP
[ ] Google login PASS
[ ] Admin authenticated flow PASS
[ ] CSRF write PASS
[ ] SSE PASS
[ ] Product data PASS
[ ] Product image delivery PASS
[ ] Image upload/delete tested if image code changed
[ ] Database backup timer active
[ ] No unexpected RDS/Elastic Beanstalk resource created
[ ] Git deployment branch pushed
[ ] Release synced/merged into main only after production PASS
[ ] Optional production tag created
```

Payment/payOS remains excluded until production payment implementation is explicitly activated.

---

# 20. Known production baseline — 2026-09-16

The final production audit confirmed:

```text
Production EC2             running
Backend service            active
Backup timer               enabled + active
PostgreSQL                 loopback only
Spring Boot                loopback only
CloudFront                 Deployed
Apex health                HTTP 200 / UP
WWW health                 HTTP 200 / UP
Active RDS                 0
Active Elastic Beanstalk   0
Legacy RDS snapshot        absent
Legacy artifact bucket     absent
Product image Versioning   Enabled
Image recovery drill       PASS
DB restore drill           PASS
Git                        clean + remote matched
```

When this architecture materially changes, update this runbook in the same pull request/release as the IaC change.
