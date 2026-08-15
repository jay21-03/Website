# Product image AWS infrastructure

`product-images.yml` creates:

- a private, encrypted S3 bucket with all public access blocked;
- a CloudFront distribution using Origin Access Control (OAC);
- a bucket policy that permits CloudFront to read only `products/*`;
- a managed IAM policy that permits the backend to put/delete only `products/*`.

The S3 bucket is retained if the CloudFormation stack is deleted. Delete retained objects and the bucket manually only when data removal is intentional.

## Deploy

Install AWS CLI v2 and authenticate first. From the repository root:

```powershell
.\scripts\deploy-product-images-aws.ps1 `
  -Environment dev `
  -Region ap-southeast-1 `
  -StackName bautruc-product-images-dev
```

To use a named profile:

```powershell
.\scripts\deploy-product-images-aws.ps1 -Profile your-profile
```

CloudFormation can generate a globally unique bucket name. To request a specific name, pass `-BucketName`; deployment fails if another AWS account already owns it.

The script prints `AWS_REGION`, `S3_BUCKET_NAME`, `S3_PUBLIC_BASE_URL`, and the backend managed-policy ARN. Attach the policy to the ECS task role, EC2 instance role, EKS workload role, or other role that runs the application. Do not attach it to end users and do not store AWS secret keys in this repository.
