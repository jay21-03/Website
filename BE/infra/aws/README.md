# AWS production deployment

This directory contains the AWS infrastructure for Bau Truc ecommerce.

## Architecture

Production uses:

- one main CloudFront distribution for the website;
- a private S3 frontend bucket behind CloudFront OAC;
- `/api/*` routed by CloudFront to one Elastic Beanstalk Single Instance backend;
- Elastic Beanstalk Java SE on Amazon Linux 2023 / Corretto 21;
- RDS PostgreSQL 16, Single-AZ, `db.t4g.micro`;
- a separate private S3 + CloudFront OAC stack for product images;
- SSM Parameter Store for production runtime secrets;
- IAM instance roles instead of static AWS credentials.

Not used:

- NAT Gateway;
- application load balancer;
- Multi-AZ RDS;
- ECS/EKS;
- Redis;
- static production AWS access keys.

Payment/payOS remains part of the project but production payment configuration is deferred to a later phase.

## Files

`product-images.yml`

Creates:

- a new private product-image S3 bucket;
- CloudFront distribution + OAC;
- bucket policy allowing CloudFront read access to `products/*`;
- least-privilege backend managed policy for `PutObject` and `DeleteObject`.

The current development bucket is NOT imported into the production stack.

`regional.yml`

Creates:

- backend and database security groups;
- PostgreSQL RDS;
- private backend deployment-artifact S3 bucket;
- Elastic Beanstalk service role;
- Elastic Beanstalk EC2 instance profile;
- Elastic Beanstalk application;
- optionally, an Elastic Beanstalk Single Instance environment.

The first regional deployment may leave `ApplicationBundleS3Key` empty.
That creates the base infrastructure without the EB environment.

After the backend ZIP is uploaded to the artifact bucket,
update the same stack with `ApplicationBundleS3Key`.

The backend environment is created only when all of the following are set:

- `ApplicationBundleS3Key`;
- `GoogleClientId`;
- `AdminEmails`.

`edge.yml`

Creates:

- private frontend S3;
- CloudFront OAC;
- CloudFront Function for React Router SPA rewrites;
- main CloudFront distribution;
- `/api/*` routing to the Elastic Beanstalk origin;
- `/actuator/health` routing to the backend.

The initial deployment uses the CloudFront-generated domain.
Custom domain and ACM are deferred until the AWS deployment is fully validated.

## Production secrets

The following SSM SecureString parameters must exist before the regional production stack is deployed:

`/bautruc/prod/DB_PASSWORD`

`/bautruc/prod/JWT_SECRET_BASE64`

Do not commit their values.

Elastic Beanstalk obtains them using its native environment-secrets integration.

Payment/payOS SSM parameters are intentionally not required yet.

## Deployment order for Phase 2B

Do NOT execute these deployment steps during Phase 2A2.

Phase 2B will follow this order:

1. Create production DB/JWT SSM SecureString parameters.
2. Deploy `product-images.yml` with `Environment=prod`.
3. Deploy `regional.yml` base infrastructure with an empty `ApplicationBundleS3Key`.
4. Build `BE/artifacts/bautruc-ecommerce-eb.zip`.
5. Upload that bundle to the regional stack's backend artifact bucket.
6. Update `regional.yml` with the uploaded object key, Google Client ID and admin email configuration.
7. Wait for Elastic Beanstalk health to become green.
8. Query the actual Elastic Beanstalk CNAME after the environment is ready:

   ```powershell
   $backendOrigin = (
       aws elasticbeanstalk describe-environments `
           --profile website-deploy `
           --region ap-southeast-1 `
           --environment-names bautruc-backend-prod `
           --query "Environments[0].CNAME" `
           --output text
   ).Trim()

   if ([string]::IsNullOrWhiteSpace($backendOrigin) -or $backendOrigin -eq "None") {
       throw "Elastic Beanstalk CNAME was not returned."
   }

   Write-Host $backendOrigin
   ```

   Use this CNAME as `BackendOriginDomainName` when deploying `edge.yml`.

   Do not use `AWS::ElasticBeanstalk::Environment.EndpointURL` for the Single Instance backend.
9. Build the React frontend.
10. Upload `FE/dist` contents to the frontend S3 bucket.

    After each frontend release, invalidate the main CloudFront distribution:

    ```powershell
    aws cloudfront create-invalidation `
        --profile website-deploy `
        --distribution-id <MAIN_DISTRIBUTION_ID> `
        --paths "/*"
    ```
11. Update the regional stack:
    - `FrontendBaseUrl=https://<main-cloudfront-domain>`
    - `AllowedOrigins=https://<main-cloudfront-domain>`
12. Run smoke tests.
13. Copy the required product images from the dev bucket to the new prod bucket while preserving object keys.
14. Verify production image delivery before considering cleanup of any development resources.

## Development product-image bucket

`shopbautruc-product-images-dev` is currently a development resource.

Do not modify its public policy during Phase 2A2.

Production uses a separate generated private bucket.

Only after production image migration is verified should the development bucket be reviewed for cleanup/hardening.

## Safety

`deploy-product-images-aws.ps1` refuses deployment unless `-ConfirmDeploy` is explicitly supplied.

`validate-aws-iac.ps1` performs read-only validation only.

Production backend must not contain:

- `AWS_ACCESS_KEY_ID`;
- `AWS_SECRET_ACCESS_KEY`;
- `.env` files.

Use IAM roles and SSM Parameter Store instead.
