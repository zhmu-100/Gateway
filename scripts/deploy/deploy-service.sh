#!/bin/bash
# Service Deployment Script
# Usage: ./deploy-service.sh <service-name> <environment> <version>

set -e

# Check arguments
if [ "$#" -lt 3 ]; then
    echo "Usage: $0 <service-name> <environment> <version>"
    echo "Example: $0 gateway dev 1.0.0"
    exit 1
fi

SERVICE_NAME=$1
ENVIRONMENT=$2
VERSION=$3
REGISTRY=${4:-"ghcr.io/zhmu-100"}
WORKSPACE=$(pwd)

echo "Starting deployment of $SERVICE_NAME version $VERSION to $ENVIRONMENT environment"

# Create deployment directory
DEPLOY_DIR="/opt/deployment/$SERVICE_NAME"
mkdir -p $DEPLOY_DIR

# Prepare environment variables
export SERVICE_NAME=$SERVICE_NAME
export SERVICE_VERSION=$VERSION
export DEPLOYMENT_ENV=$ENVIRONMENT
export REGISTRY=$REGISTRY
export SERVICE_PORT=$(grep "SERVICE_PORT" $WORKSPACE/config/$ENVIRONMENT/$SERVICE_NAME.env | cut -d'=' -f2)

# Copy configuration files
echo "Copying configuration files..."
cp $WORKSPACE/config/$ENVIRONMENT/$SERVICE_NAME.env $DEPLOY_DIR/
cp $WORKSPACE/docker/docker-compose-template.yml $DEPLOY_DIR/docker-compose.yml

# Ensure necessary directories exist
mkdir -p $DEPLOY_DIR/config/$ENVIRONMENT
cp -r $WORKSPACE/config/$ENVIRONMENT/$SERVICE_NAME.* $DEPLOY_DIR/config/$ENVIRONMENT/ 2>/dev/null || true

# Process docker-compose template with environment variables
echo "Processing docker-compose template..."
cd $DEPLOY_DIR
envsubst <docker-compose.yml >docker-compose.processed.yml
mv docker-compose.processed.yml docker-compose.yml

# Pull the latest image
echo "Pulling latest image: $REGISTRY/$SERVICE_NAME:$VERSION"
docker pull $REGISTRY/$SERVICE_NAME:$VERSION

# Check if service is already running
if docker ps | grep -q "$SERVICE_NAME"; then
    echo "Service $SERVICE_NAME is already running, updating..."
    # Stop and remove the existing container
    docker-compose down $SERVICE_NAME
fi

# Start the service
echo "Starting service..."
docker-compose up -d $SERVICE_NAME

# Verify service is running
echo "Verifying service is running..."
sleep 5
if docker ps | grep -q "$SERVICE_NAME"; then
    echo "Service $SERVICE_NAME is running"
else
    echo "Error: Service $SERVICE_NAME failed to start"
    docker-compose logs $SERVICE_NAME
    exit 1
fi

# Wait for health check to pass
echo "Waiting for service health check to pass..."
ATTEMPTS=0
MAX_ATTEMPTS=30
while [ $ATTEMPTS -lt $MAX_ATTEMPTS ]; do
    if docker ps | grep -q "$SERVICE_NAME.*healthy"; then
        echo "Service $SERVICE_NAME is healthy"
        break
    fi
    ATTEMPTS=$((ATTEMPTS + 1))
    echo "Waiting for service to become healthy... ($ATTEMPTS/$MAX_ATTEMPTS)"
    sleep 10
done

if [ $ATTEMPTS -eq $MAX_ATTEMPTS ]; then
    echo "Error: Service $SERVICE_NAME failed health check"
    docker-compose logs $SERVICE_NAME
    exit 1
fi

echo "Deployment of $SERVICE_NAME version $VERSION to $ENVIRONMENT environment completed successfully"
exit 0
