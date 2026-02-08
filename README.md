# Spring Boot ECS Fargate Example

A complete example of deploying a Spring Boot application to AWS ECS Fargate with Application Load Balancer, demonstrating containerization and cloud-native deployment practices.

## Architecture

This example demonstrates:
- **Spring Boot 3.2** application with REST API endpoints
- **Docker** containerization with multi-stage builds
- **AWS ECS Fargate** for serverless container orchestration
- **Application Load Balancer** for traffic distribution
- **CloudWatch** for logging and monitoring
- **Terraform** infrastructure as code for automated deployment

## Project Structure

```
.
├── src/
│   └── main/
│       ├── java/com/example/ecs/
│       │   ├── EcsApplication.java          # Main Spring Boot application
│       │   └── controller/
│       │       └── HealthController.java    # REST API controller
│       └── resources/
│           └── application.yml              # Application configuration
├── terraform/
│   └── main.tf                              # Complete Terraform infrastructure
├── Dockerfile                               # Multi-stage Docker build
├── ecs-task-definition.json                 # ECS task definition template
└── pom.xml                                  # Maven configuration
```

## Prerequisites

- **Java 17** or later
- **Maven 3.6+**
- **Docker** and Docker Compose
- **AWS CLI** configured with appropriate credentials
- **Terraform** (for infrastructure deployment)
- **AWS Account** with permissions for ECS, ECR, VPC, IAM, and CloudWatch

## Local Development

### Build the Application

```bash
mvn clean package
```

### Run Locally

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Test the Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Hello endpoint
curl http://localhost:8080/api/hello

# Info endpoint
curl http://localhost:8080/api/info
```

## Docker Build and Run

### Build Docker Image

The default Dockerfile expects a pre-built JAR file for simplicity and to avoid network/certificate issues during Docker build:

```bash
# Build the JAR first
mvn clean package -DskipTests

# Build Docker image
docker build -t springboot-ecs-fargate .
```

For production CI/CD pipelines with proper Maven repository access, use the multi-stage Dockerfile:

```bash
# Build using multi-stage Dockerfile (builds JAR inside Docker)
docker build -f Dockerfile.multistage -t springboot-ecs-fargate .
```

### Run Docker Container

```bash
docker run -p 8080:8080 springboot-ecs-fargate
```

### Test Container

```bash
curl http://localhost:8080/api/hello
```

## AWS Deployment

### Step 1: Create ECR Repository

```bash
# Create ECR repository
aws ecr create-repository --repository-name springboot-ecs-fargate --region us-east-1

# Get ECR login token
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com
```

### Step 2: Build and Push Docker Image

```bash
# Build image
docker build -t springboot-ecs-fargate .

# Tag image
docker tag springboot-ecs-fargate:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/springboot-ecs-fargate:latest

# Push to ECR
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/springboot-ecs-fargate:latest
```

### Step 3: Deploy with Terraform

```bash
cd terraform

# Initialize Terraform
terraform init

# Create terraform.tfvars file
cat > terraform.tfvars << EOF
aws_region      = "us-east-1"
app_name        = "springboot-ecs-fargate"
environment     = "dev"
container_image = "<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/springboot-ecs-fargate:latest"
EOF

# Preview changes
terraform plan

# Apply infrastructure
terraform apply
```

### Step 4: Access the Application

After Terraform completes, get the ALB DNS name:

```bash
terraform output alb_dns_name
```

Test the deployed application:

```bash
# Replace with your ALB DNS name
ALB_DNS="your-alb-dns-name.us-east-1.elb.amazonaws.com"

curl http://$ALB_DNS/api/hello
curl http://$ALB_DNS/api/info
curl http://$ALB_DNS/actuator/health
```

## Alternative: Deploy with AWS CLI

If you prefer using AWS CLI instead of Terraform:

### 1. Register Task Definition

Update `ecs-task-definition.json` with your values, then:

```bash
aws ecs register-task-definition \
    --cli-input-json file://ecs-task-definition.json \
    --region us-east-1
```

### 2. Create ECS Service

```bash
aws ecs create-service \
    --cluster your-cluster-name \
    --service-name springboot-ecs-service \
    --task-definition springboot-ecs-fargate:1 \
    --desired-count 2 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-xxx],assignPublicIp=ENABLED}" \
    --load-balancers targetGroupArn=arn:aws:elasticloadbalancing:region:account-id:targetgroup/xxx,containerName=springboot-app,containerPort=8080 \
    --region us-east-1
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/actuator/health` | GET | Health check endpoint (used by ALB) |
| `/actuator/info` | GET | Application information |
| `/actuator/metrics` | GET | Application metrics |
| `/api/hello` | GET | Returns greeting message with timestamp |
| `/api/info` | GET | Returns application and system information |

## Monitoring and Logging

### View Logs in CloudWatch

```bash
# Get log stream names
aws logs describe-log-streams \
    --log-group-name /ecs/springboot-ecs-fargate \
    --region us-east-1

# Tail logs
aws logs tail /ecs/springboot-ecs-fargate --follow --region us-east-1
```

### Monitor ECS Service

```bash
# Check service status
aws ecs describe-services \
    --cluster springboot-ecs-fargate-cluster \
    --services springboot-ecs-fargate-service \
    --region us-east-1
```

## Scaling

### Manual Scaling

```bash
aws ecs update-service \
    --cluster springboot-ecs-fargate-cluster \
    --service springboot-ecs-fargate-service \
    --desired-count 4 \
    --region us-east-1
```

### Auto Scaling (Add to Terraform)

```hcl
resource "aws_appautoscaling_target" "ecs_target" {
  max_capacity       = 10
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.app.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_policy" {
  name               = "scale-down"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value = 75.0
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}
```

## Cost Optimization

- **Fargate Spot**: Use Fargate Spot for non-critical workloads (up to 70% savings)
- **Right-sizing**: Start with 256 CPU / 512 MB memory and adjust based on metrics
- **Auto-scaling**: Scale down during low-traffic periods
- **CloudWatch Logs**: Set appropriate retention periods (7-30 days)

## Cleanup

### With Terraform

```bash
cd terraform
terraform destroy
```

### Manual Cleanup

```bash
# Delete ECS service
aws ecs delete-service --cluster springboot-ecs-fargate-cluster --service springboot-ecs-fargate-service --force

# Delete ECS cluster
aws ecs delete-cluster --cluster springboot-ecs-fargate-cluster

# Delete ECR repository
aws ecr delete-repository --repository-name springboot-ecs-fargate --force
```

## Troubleshooting

### Container Health Checks Failing

- Ensure the application starts within the `startPeriod` (60 seconds)
- Check CloudWatch logs for startup errors
- Verify security group allows traffic on port 8080

### Service Not Reaching Steady State

```bash
# Check service events
aws ecs describe-services \
    --cluster springboot-ecs-fargate-cluster \
    --services springboot-ecs-fargate-service \
    | jq '.services[].events[:5]'
```

### High Memory Usage

- Increase task memory in task definition
- Add JVM memory constraints: `-Xmx384m -Xms384m`

## Best Practices

1. **Security**: Use AWS Secrets Manager for sensitive configuration
2. **Networking**: Use private subnets with NAT Gateway for production
3. **Monitoring**: Enable Container Insights and custom CloudWatch metrics
4. **CI/CD**: Integrate with AWS CodePipeline or GitHub Actions
5. **Blue/Green Deployments**: Use AWS CodeDeploy for zero-downtime updates

## Further Reading

- [AWS ECS Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Terraform ECS Module](https://registry.terraform.io/modules/terraform-aws-modules/ecs/aws/latest)

## License

MIT License - Feel free to use this example for learning and production projects.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.