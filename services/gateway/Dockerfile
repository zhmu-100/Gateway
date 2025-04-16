FROM openjdk:17-slim

ARG SERVICE_NAME=gateway
ARG SERVICE_VERSION=latest
ARG JAR_FILE=./gateway.jar

# Set service-specific environment variables
ENV SERVICE_NAME=${SERVICE_NAME}
ENV SERVICE_VERSION=${SERVICE_VERSION}

# Copy the service JAR
COPY ${JAR_FILE} /app/service.jar

# Gateway-specific configurations
ENV GATEWAY_CONFIG=/app/config/application.conf
COPY ./config/application/${SERVICE_NAME}/application.conf /app/config/application.conf

# Expose gateway's port
EXPOSE 8080

# Gateway-specific health check that overrides the base one
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --spider http://localhost:8080/health || exit 1
