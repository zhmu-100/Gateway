#!/bin/bash
# Add New Service Script
# Usage: ./add-new-service.sh <service-name>

set -e

# Check arguments
if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <service-name>"
  echo "Example: $0 auth-service"
  exit 1
fi

SERVICE_NAME=$1
WORKSPACE=$(pwd)

echo "Setting up infrastructure for new service: $SERVICE_NAME"

# Create service directory
echo "Creating service directory..."
mkdir -p $WORKSPACE/services/$SERVICE_NAME

# Create Dockerfile
echo "Creating Dockerfile..."
cat >$WORKSPACE/services/$SERVICE_NAME/Dockerfile <<EOL
FROM infrastructure/base:kotlin

ARG SERVICE_NAME=$SERVICE_NAME
ARG SERVICE_VERSION=latest
ARG JAR_FILE=./$SERVICE_NAME.jar

# Set service-specific environment variables
ENV SERVICE_NAME=\${SERVICE_NAME}
ENV SERVICE_VERSION=\${SERVICE_VERSION}

# Copy the service JAR
COPY \${JAR_FILE} /app/service.jar

# Service-specific configurations
ENV SERVICE_CONFIG=/app/config/application.conf
COPY ./config/\${SERVICE_NAME}.conf /app/config/application.conf

# Expose service port
EXPOSE 8080

# Service-specific health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\
  CMD wget -q --spider http://localhost:8080/health || exit 1
EOL

# Create dev environment configuration
echo "Creating development environment configuration..."
mkdir -p $WORKSPACE/config/dev
cat >$WORKSPACE/config/dev/$SERVICE_NAME.env <<EOL
# $SERVICE_NAME - Development Environment Configuration

# Service Configuration
SERVICE_PORT=8080
LOG_LEVEL=DEBUG
KTOR_ENV=development

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=${SERVICE_NAME//-/_}_dev
DB_USERNAME=dev_user
DB_PASSWORD=dev_password

# Security
JWT_SECRET=dev_jwt_secret_key_for_development_only
JWT_ISSUER=sport-tracker-dev
JWT_AUDIENCE=sport-tracker-users

# Microservice Endpoints
GATEWAY_URL=http://gateway:8080
MESSAGE_BROKER_URL=http://message-broker:5672
LOG_SERVICE_URL=http://log-service:8080

# Monitoring
METRICS_ENABLED=true
PROMETHEUS_ENDPOINT=/metrics
EOL

# Create prod environment configuration
echo "Creating production environment configuration..."
mkdir -p $WORKSPACE/config/prod
cat >$WORKSPACE/config/prod/$SERVICE_NAME.env <<EOL
# $SERVICE_NAME - Production Environment Configuration

# Service Configuration
SERVICE_PORT=8080
LOG_LEVEL=INFO
KTOR_ENV=production

# Database Configuration
DB_HOST=db-server
DB_PORT=5432
DB_NAME=${SERVICE_NAME//-/_}_prod
DB_USERNAME=prod_user
DB_PASSWORD=secure_prod_password_should_be_replaced_with_secrets

# Security
JWT_SECRET=secure_jwt_secret_key_should_be_replaced_with_secrets
JWT_ISSUER=sport-tracker-prod
JWT_AUDIENCE=sport-tracker-users

# Microservice Endpoints
GATEWAY_URL=http://gateway:8080
MESSAGE_BROKER_URL=http://message-broker:5672
LOG_SERVICE_URL=http://log-service:8080

# Monitoring
METRICS_ENABLED=true
PROMETHEUS_ENDPOINT=/metrics

# Performance Tuning
CACHE_ENABLED=true
CACHE_TTL=3600
CONNECTION_POOL_SIZE=10
RESPONSE_COMPRESSION=true
EOL

# Create GitHub Actions workflow
echo "Creating GitHub Actions workflow..."
mkdir -p $WORKSPACE/.github/workflows/service-specific
cat >$WORKSPACE/.github/workflows/service-specific/$SERVICE_NAME.yml <<EOL
name: $SERVICE_NAME CI/CD

on:
  push:
    branches: [ main, dev ]
    paths:
      - '$SERVICE_NAME/**'
      - '.github/workflows/service-specific/$SERVICE_NAME.yml'
  pull_request:
    branches: [ main, dev ]
    paths:
      - '$SERVICE_NAME/**'
  workflow_dispatch:

jobs:
  call-service-template:
    uses: ./.github/workflows/service-template.yml
    with:
      service-name: $SERVICE_NAME
      java-version: '17'
      run-integration-tests: true
      registry: 'ghcr.io'
      auto-deploy-prod: false
    secrets:
      registry-token: \${{ secrets.REGISTRY_TOKEN }}
      ssh-key: \${{ secrets.SSH_KEY }}
      dev-host: \${{ secrets.DEV_HOST }}
      prod-host: \${{ secrets.PROD_HOST }}

  notify:
    needs: call-service-template
    runs-on: ubuntu-latest
    if: always()
    steps:
      - name: Set status
        id: status
        run: |
          if [[ "\${{ needs.call-service-template.result }}" == "success" ]]; then
            echo "success=true" >> \$GITHUB_OUTPUT
            echo "message=$SERVICE_NAME deployment completed successfully" >> \$GITHUB_OUTPUT
          else
            echo "success=false" >> \$GITHUB_OUTPUT
            echo "message=$SERVICE_NAME deployment failed" >> \$GITHUB_OUTPUT
          fi

      - name: Send notification
        uses: rtCamp/action-slack-notify@v2
        if: always()
        env:
          SLACK_WEBHOOK: \${{ secrets.SLACK_WEBHOOK }}
          SLACK_CHANNEL: deployments
          SLACK_COLOR: \${{ steps.status.outputs.success == 'true' && 'good' || 'danger' }}
          SLACK_TITLE: $SERVICE_NAME Deployment
          SLACK_MESSAGE: \${{ steps.status.outputs.message }}
          SLACK_FOOTER: "Sport Activity Tracking App CI/CD"
EOL

echo "Setup complete for $SERVICE_NAME"
echo "Don't forget to:"
echo "1. Create your Kotlin service project"
echo "2. Customize the configuration files as needed"
echo "3. Add service-specific dependencies in docker-compose if required"
echo ""
echo "To manually deploy this service after setup, run:"
echo "  ./scripts/deploy/deploy-service.sh $SERVICE_NAME dev 1.0.0"

exit 0
